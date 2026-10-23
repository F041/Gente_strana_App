// File: \app\src\main\java\com\gentestrana\ui_controller\ChatViewModel.kt

package com.gentestrana.ui_controller

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentestrana.chat.ChatMessage
import com.gentestrana.chat.ChatRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

sealed class SendMessageEvent {
    data object Success : SendMessageEvent()
    data class Error(val message: String) : SendMessageEvent()
}

data class ConversationMetrics(
    val recipientMessageCount: Int = 0,
    val conversationAgeText: String = "",
    val startDateText: String = "",
    val avgWordsPerMessage: Int = 0,
    val isMemoryUnlocked: Boolean = false
)

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
    // Per Fase 1 & 2 — Autopilot Smart Matching
    private val _recipientBio = MutableStateFlow<String?>(null)
    val recipientBio: StateFlow<String?> = _recipientBio
    private val _recipientTopics = MutableStateFlow<List<String>>(emptyList())
    val recipientTopics: StateFlow<List<String>> = _recipientTopics
    private val _recipientAutopilotEnabled = MutableStateFlow<Boolean>(false)
    val recipientAutopilotEnabled: StateFlow<Boolean> = _recipientAutopilotEnabled
    private val _recipientSocialBatteryStatus = MutableStateFlow<String>("HIGH")
    val recipientSocialBatteryStatus: StateFlow<String> = _recipientSocialBatteryStatus.asStateFlow()
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
    var isListenerRunning = false

    // Metriche conversazione indipendenti dallo scroll (totale messaggi, età, metrica neurodivergente e trigger memorie a 50)
    private val _conversationMetrics = MutableStateFlow(ConversationMetrics())
    val conversationMetrics: StateFlow<ConversationMetrics> = _conversationMetrics.asStateFlow()

    init {
        fetchCurrentUserId()
        viewModelScope.launch {
            currentUserId.collect { userId ->
                if (userId != null) {
                    // Avvia IMMEDIATAMENTE l'ascolto dei messaggi in parallelo per abbattere i tempi di caricamento
                    if (messageListenerRegistration == null) {
                        listenForMessages()
                    }
                    // Esegui recupero info e mark as read/delivered in coroutine separate in parallelo
                    viewModelScope.launch { fetchRecipientInfo(userId) }
                    viewModelScope.launch { markMessagesAsDeliveredAndRead(userId) }
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
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error deleting message $messageId from chat $chatId", e)
            }
        }
    }

    private fun listenForMessages() {
        if (isListenerRunning) return
        isListenerRunning = true
        _isLoadingOlderMessages.value = true

        messageListenerRegistration?.remove()
        messageListenerRegistration = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(messagesLimit.toLong())
            .addSnapshotListener { snapshot, e ->
                isListenerRunning = false
                _isLoadingOlderMessages.value = false
                if (e != null) {
                    Log.e("ChatViewModel_Debug", "listenForMessages ERROR: ${e.message}", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    var listChanged = false
                    snapshot.documentChanges.forEach { change ->
                        val changedDoc = change.document
                        val data = change.document.data

                        @Suppress("UNCHECKED_CAST")
                        val reactionsMap = data["reactions"] as? Map<String, String> ?: emptyMap()

                        // ---> COSTRUZIONE MANUALE CORRETTA <---
                        val message = ChatMessage(
                            id = changedDoc.id,
                            sender = data["sender"] as? String ?: "",
                            message = data["message"] as? String ?: "",
                            timestamp = data["timestamp"] as? Timestamp ?: Timestamp.now(),
                            status = data["status"] as? String ?: "SENT",
                            isReply = data["isReply"] as? Boolean ?: false,
                            replyToMessageText = data["replyToMessageText"] as? String ?: "",
                            replyToMessageSender = data["replyToMessageSender"] as? String ?: "",
                            reactions = reactionsMap
                        )

                        listChanged = true
                        when (change.type) {
                            DocumentChange.Type.ADDED -> {
                                if (_messages.value.none { it.id == message.id }) {
                                    _messages.value = (_messages.value + message).sortedBy { it.timestamp }
                                }
                            }
                            DocumentChange.Type.MODIFIED -> {
                                _messages.value = _messages.value.map { if (it.id == message.id) message else it }
                            }
                            DocumentChange.Type.REMOVED -> {
                                _messages.value = _messages.value.filterNot { it.id == message.id }
                            }
                        }
                    }

                    if (listChanged) {
                        _hasMoreMessages.value = snapshot.documents.size >= messagesLimit

                        val userId = _currentUserId.value
                        if (userId != null) {
                            val hasUnreadFromOther = snapshot.documents.any { doc ->
                                val sender = doc.getString("sender") ?: ""
                                val status = doc.getString("status") ?: ""
                                sender != userId && status != "READ"
                            }
                            if (hasUnreadFromOther) {
                                viewModelScope.launch {
                                    markMessagesAsDeliveredAndRead(userId)
                                }
                            }
                        }
                    }
                }
            }
    }

    fun loadOlderMessages() {
        if (isListenerRunning || !_hasMoreMessages.value) {
            return
        }
        messagesLimit += 20
        listenForMessages()
    }

    private fun stopListeningForMessages() {
        messageListenerRegistration?.remove()
        messageListenerRegistration = null
        isListenerRunning = false
        _isLoadingOlderMessages.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopListeningForMessages()
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
                // Fase 1 & 2 — Smart Matching & Autopilot: recupera bio, topics e stato autopilot
                _recipientBio.value = userDoc.getString("bio") ?: ""
                _recipientTopics.value = (userDoc.get("topics") as? List<String>) ?: emptyList()
                _recipientAutopilotEnabled.value = userDoc.getBoolean("autopilotEnabled") ?: false
                _recipientSocialBatteryStatus.value = userDoc.getString("socialBatteryStatus") ?: "HIGH"

                // Calcola le metriche della conversazione in background
                viewModelScope.launch { fetchConversationMetrics(otherUserId) }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error fetching recipient info for chat $chatId", e)
        }
    }

    private suspend fun fetchConversationMetrics(otherUserId: String) {
        try {
            // Usa query aggregata Firestore SERVER count() per esecuzione istantanea e costo zero
            val countSnapshot = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .whereEqualTo("sender", otherUserId)
                .count()
                .get(com.google.firebase.firestore.AggregateSource.SERVER)
                .await()

            val count = countSnapshot.count.toInt()

            // Calcolo veloce della media parole dai messaggi caricati
            val loadedMsgs = _messages.value.filter { it.sender == otherUserId }
            val avgWords = if (loadedMsgs.isNotEmpty()) {
                val totalWords = loadedMsgs.sumOf { msg ->
                    msg.message.split("\\s+".toRegex()).count { it.isNotBlank() }
                }
                totalWords / loadedMsgs.size
            } else 0

            val oldestDocQuery = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .await()

            val oldestTimestamp = oldestDocQuery.documents.firstOrNull()?.getTimestamp("timestamp")
            val ageText = oldestTimestamp?.let { com.gentestrana.utils.formatConversationAge(it) } ?: "Oggi"
            val startDateText = oldestTimestamp?.let {
                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(it.toDate())
            } ?: ""

            _conversationMetrics.value = ConversationMetrics(
                recipientMessageCount = count,
                conversationAgeText = ageText,
                startDateText = startDateText,
                avgWordsPerMessage = avgWords,
                isMemoryUnlocked = count >= 50
            )
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error fetching conversation metrics for chat $chatId", e)
        }
    }

    private suspend fun markMessagesAsDeliveredAndRead(userId: String) {
        try {
            repository.markMessagesAsDelivered(chatId, userId)
            repository.markMessagesAsRead(chatId)
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error marking messages for chat $chatId", e)
        }
    }

    fun sendMessage(messageText: String, context: Context, replyingTo: ChatMessage?) {
        if (messageText.isBlank()) return
        val senderId = _currentUserId.value ?: return

        viewModelScope.launch {
            try {
                repository.sendMessage(chatId, senderId, messageText, context, replyingTo)
                _sendMessageEvent.emit(SendMessageEvent.Success)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message in chat $chatId", e)
                _sendMessageEvent.emit(SendMessageEvent.Error(e.message ?: "Errore sconosciuto"))
            }
        }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        val userId = _currentUserId.value ?: return
        viewModelScope.launch {
            try {
                repository.toggleMessageReaction(chatId, messageId, userId, emoji)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error toggling reaction for message $messageId", e)
            }
        }
    }
}