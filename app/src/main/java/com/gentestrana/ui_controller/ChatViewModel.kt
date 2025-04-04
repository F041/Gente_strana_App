// File: \app\src\main\java\com\gentestrana\ui_controller\ChatViewModel.kt

package com.gentestrana.ui_controller

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentestrana.chat.ChatMessage
import com.gentestrana.chat.ChatRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.DocumentChange



sealed class SendMessageEvent {
    data object Success : SendMessageEvent()
    data class Error(val message: String) : SendMessageEvent()
}

class ChatViewModel(
    private val chatId: String,
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId

    private val _recipientName = MutableStateFlow<String?>(null)
    val recipientName: StateFlow<String?> = _recipientName

    private val _recipientDocId = MutableStateFlow<String?>(null)
    val recipientDocId: StateFlow<String?> = _recipientDocId

    private val _sendMessageEvent = MutableSharedFlow<SendMessageEvent>()
    val sendMessageEvent = _sendMessageEvent.asSharedFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoadingOlderMessages = MutableStateFlow(false)
    val isLoadingOlderMessages: StateFlow<Boolean> = _isLoadingOlderMessages

    private val _hasMoreMessages = MutableStateFlow(true)
    val hasMoreMessages: StateFlow<Boolean> = _hasMoreMessages

    private var messagesLimit = 20
    private var messageListenerRegistration: ListenerRegistration? = null
    // --- RINOMINATO isLoading in isListenerRunning ---
    private var isListenerRunning = false
    // --- FINE RINOMINA ---


    init {
        fetchCurrentUserId()
        viewModelScope.launch {
            currentUserId.collect { userId ->
                if (userId != null) {
                    fetchRecipientInfo(userId)
                    markMessagesAsDeliveredAndRead(userId)
                    // Avvia l'ascolto solo se non è già attivo
                    if (messageListenerRegistration == null) {
                        listenForMessages()
                    }
                } else {
                    stopListeningForMessages()
                    _messages.value = emptyList()
                }
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                repository.deleteMessage(chatId, messageId)
                Log.d("ChatViewModel", "Message $messageId deleted successfully from chat $chatId.")
                // Nota: Non aggiorniamo manualmente _messages.value qui.
                // Il listener Firestore (listenForMessages) dovrebbe rilevare
                // la rimozione del documento e aggiornare automaticamente la lista.
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error deleting message $messageId from chat $chatId", e)
                // Potremmo emettere un evento di errore qui se necessario per la UI
                // _deleteMessageEvent.emit(DeleteMessageEvent.Error("Errore durante l'eliminazione"))
            }
        }
    }

    private fun listenForMessages() {
        if (isListenerRunning) {
//            Log.d("ChatViewModel_Debug", "listenForMessages SKIPPED - Listener already running.")
            return
        }
        isListenerRunning = true // Imposta subito il flag
        _isLoadingOlderMessages.value = true // Mostra l'indicatore UI all'inizio/riavvio del listener


//        Log.d("ChatViewModel_Debug", "listenForMessages STARTED - Limit: $messagesLimit")

        messageListenerRegistration?.remove() // Rimuovi listener precedente

        messageListenerRegistration = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(messagesLimit.toLong())
            .addSnapshotListener { snapshot, e ->

                // --- RESETTA SEMPRE I FLAG ALLA FINE DEL CALLBACK ---
                isListenerRunning = false
                _isLoadingOlderMessages.value = false
                // --- FINE RESET ---

                if (e != null) {
                    Log.e("ChatViewModel_Debug", "listenForMessages ERROR: ${e.message}", e)
                    _messages.value = emptyList()
                    _hasMoreMessages.value = false
                    // isListenerRunning e _isLoadingOlderMessages sono già false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    Log.d("ChatVM_Listener", "Processing ${snapshot.documentChanges.size} changes.")
                    var listChanged = false // Flag per sapere se la lista è cambiata

                    snapshot.documentChanges.forEach { change ->
                        val changedDoc = change.document
                        val message = try {
                            changedDoc.toObject(ChatMessage::class.java)?.copy(id = changedDoc.id)
                        } catch (parseError: Exception) {
                            Log.e("ChatVM_Listener", "PARSE ERROR for doc ${changedDoc.id}", parseError)
                            null
                        }

                        if (message != null) {
                            listChanged = true // Marco che almeno un cambiamento valido c'è stato
                            when (change.type) {
                                DocumentChange.Type.ADDED -> {
                                    // Aggiungi il nuovo messaggio SE non è già presente (utile per la prima lettura)
                                    if (_messages.value.none { it.id == message.id }) {
                                        _messages.value = (_messages.value + message).sortedBy { it.timestamp }
                                        Log.d("ChatVM_Listener", "ADDED: ${message.id}")
                                    } else {
                                        Log.d("ChatVM_Listener", "ADDED skipped (already exists?): ${message.id}")
                                    }
                                }
                                DocumentChange.Type.MODIFIED -> {
                                    // Aggiorna il messaggio esistente
                                    _messages.value = _messages.value.map {
                                        if (it.id == message.id) message else it
                                    }
                                    Log.d("ChatVM_Listener", "MODIFIED: ${message.id}")
                                }
                                DocumentChange.Type.REMOVED -> {
                                    // Rimuovi il messaggio
                                    _messages.value = _messages.value.filterNot { it.id == message.id }
                                    Log.d("ChatVM_Listener", "REMOVED: ${message.id}")
                                }
                            }
                        }
                    } // Fine forEach

                    // Aggiorna _hasMoreMessages SOLO se la lista è cambiata E se lo snapshot
                    // originale (che contiene il set limitato corrente) ha raggiunto il limite.
                    // Questo è un compromesso: _hasMoreMessages è meno preciso ora,
                    // ma si aggiorna solo quando arrivano nuovi dati.
                    if (listChanged) {
                        _hasMoreMessages.value = snapshot.documents.size >= messagesLimit
                        Log.d("ChatVM_Listener", "List changed. Updated _hasMoreMessages: ${_hasMoreMessages.value} (based on snapshot size: ${snapshot.documents.size} >= limit: $messagesLimit)")
                    }

                } else {
                    Log.w("ChatViewModel_Debug", "listenForMessages SNAPSHOT is NULL")
                    _messages.value = emptyList()
                    _hasMoreMessages.value = false
                }

            }
//        Log.d("ChatViewModel_Debug", "listenForMessages Listener attached.")
    }

    fun loadOlderMessages() {
        if (isListenerRunning || !_hasMoreMessages.value) {
//            Log.d("ChatViewModel_Debug", "loadOlderMessages SKIPPED. isListenerRunning: $isListenerRunning, hasMore: ${_hasMoreMessages.value}")
            return
        }

//        Log.d("ChatViewModel_Debug", "loadOlderMessages called. Current limit: $messagesLimit")
        messagesLimit += 20 // Aumenta limite

        // Richiama listenForMessages. Gestirà isListenerRunning e _isLoadingOlderMessages.
        listenForMessages()
    }

    private fun stopListeningForMessages() {
//        Log.d("ChatViewModel_Debug", "Stopping message listener.")
        messageListenerRegistration?.remove()
        messageListenerRegistration = null
        isListenerRunning = false
        _isLoadingOlderMessages.value = false // Assicurati che sia false quando si ferma
    }

    override fun onCleared() {
        super.onCleared()
        stopListeningForMessages()
        Log.d("ChatViewModel_Debug", "ViewModel cleared.")
    }


    private fun fetchCurrentUserId() {
        _currentUserId.value = Firebase.auth.currentUser?.uid
    }
    private suspend fun fetchRecipientInfo(userId: String) {
        try {
            val chatDocSnapshot = db.collection("chats").document(chatId).get().await()
            val participants = chatDocSnapshot.get("participants") as? List<String> ?: emptyList()
            val otherUserId = participants.firstOrNull { it != userId && it.isNotBlank() }
            if (otherUserId != null) {
                _recipientDocId.value = otherUserId
                val userDoc = db.collection("users").document(otherUserId).get().await()
                _recipientName.value = userDoc.getString("username") ?: "Sconosciuto"
            } else {
                Log.w("ChatViewModel", "Other user ID not found or is blank in chat $chatId")
                _recipientDocId.value = null
                _recipientName.value = "Sconosciuto"
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error fetching recipient info for chat $chatId: ${e.message}")
            _recipientDocId.value = null
            _recipientName.value = "Errore"
        }
    }
    private suspend fun markMessagesAsDeliveredAndRead(userId: String) {
        try {
            repository.markMessagesAsDelivered(chatId, userId)
            repository.markMessagesAsRead(chatId)
            Log.d("ChatViewModel", "Messages marked as delivered/read for chat $chatId")
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error marking messages as delivered/read for chat $chatId: ${e.message}")
        }
    }
    fun sendMessage(messageText: String, context: Context) {
        if (messageText.isBlank()) {
            return
        }
        val senderId = _currentUserId.value
        if (senderId == null) {
            Log.e("ChatViewModel", "Cannot send message, user ID is null")
            viewModelScope.launch {
                _sendMessageEvent.emit(SendMessageEvent.Error("Utente non identificato."))
            }
            return
        }
        viewModelScope.launch {
            try {
                repository.sendMessage(chatId, senderId, messageText, context)
                _sendMessageEvent.emit(SendMessageEvent.Success)
                Log.d("ChatViewModel", "Message sent successfully by $senderId in chat $chatId")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message in chat $chatId", e)
                _sendMessageEvent.emit(SendMessageEvent.Error(e.message ?: "Errore sconosciuto durante l'invio"))
            }
        }
    }
}