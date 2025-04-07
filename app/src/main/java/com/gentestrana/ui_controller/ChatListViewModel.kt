package com.gentestrana.ui_controller

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentestrana.chat.Chat
import com.gentestrana.chat.ChatRepository
import com.gentestrana.chat.MessageStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore

class ChatListViewModel(
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {

    // Stato per le chat processate dal repository (potrebbe includere temporaneamente utenti bloccati)
    private val _processedChats = MutableStateFlow<List<Chat>>(emptyList())
    // Non esporre direttamente _processedChats

    // Stato per gli ID degli utenti bloccati
    private val _blockedUserIds = MutableStateFlow<Set<String>>(emptySet())

    // Stato finale filtrato per la UI
    val chats: StateFlow<List<Chat>> = combine(
        _processedChats,
        _blockedUserIds
    ) { processedList, blockedIds ->
        processedList.filterNot { chat ->
            // Filtra la chat se il participantId è nella lista degli ID bloccati
            blockedIds.contains(chat.participantId)
        }
        // L'ordinamento viene già fatto quando si aggiorna _processedChats
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList() // Valore iniziale per la UI
    )

    // Mappa dei listener per lo stato dei messaggi (invariata)
    private val messageListeners = mutableMapOf<String, ListenerRegistration>()
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val currentUserId = repository.getCurrentUserId()
    private var chatsListenerRegistration: ListenerRegistration? = null // Per gestire il listener principale
    private var blockedUsersListenerRegistration: ListenerRegistration? = null

    init {
        currentUserId?.let { userId ->
//            Log.d("ChatListVM", "Initializing ViewModel for user: $userId")
            // Registra il listener per gli utenti bloccati ---
            registerBlockedUsersListener(userId)
            registerChatsListenerIfNeeded(userId)
        } ?: Log.e("ChatListVM", "Cannot initialize ViewModel: currentUserId is null")

        // La logica di updateMessageListeners verrà chiamata quando _processedChats si aggiorna.
        viewModelScope.launch {
            // Ascolta le modifiche alla *lista filtrata* per aggiornare i listeners dei messaggi
            // Questo assicura che aggiorniamo i listener solo quando la lista VISIBILE cambia.
            chats.collect { visibleChats ->
                Log.d("ChatListVM", "Filtered chats list changed, updating message listeners.")
                updateMessageListeners(visibleChats)
            }
        }
    }

    private fun registerBlockedUsersListener(userId: String) {
        if (blockedUsersListenerRegistration != null) {
            Log.d("ChatListVM", "Blocked users listener already registered.")
            return // Già registrato
        }
        // Rimuovi listener precedente se esiste (sicurezza extra)
        blockedUsersListenerRegistration?.remove()

        Log.d("ChatListVM_Listener", "Registrazione listener per blockedUsers di $userId")
        val userDocRef = firestore.collection("users").document(userId)
        blockedUsersListenerRegistration = userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("ChatListVM_Listener", "Errore nell'ascolto di blockedUsers: ${error.message}")
                _blockedUserIds.value = emptySet() // Errore, resetta
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val blockedList = snapshot.get("blockedUsers") as? List<String> ?: emptyList()
                val newBlockedSet = blockedList.toSet()
                if (newBlockedSet != _blockedUserIds.value) { // Aggiorna solo se cambia
                    _blockedUserIds.value = newBlockedSet
                    Log.d("ChatListVM_Listener", "Listener blockedUsers aggiornato (ChatListVM): ${newBlockedSet.size} utenti.")
                    // L'aggiornamento di _blockedUserIds triggererà il 'combine' e aggiornerà 'chats'.
                    // Il collect su 'chats' nell'init si occuperà di chiamare updateMessageListeners.
                }
            } else {
                Log.w("ChatListVM_Listener", "Documento utente $userId non trovato o snapshot nullo.")
                _blockedUserIds.value = emptySet() // Documento non trovato
            }
        }
    }

    // --- Registra il listener per le modifiche alla lista chat ---
    private fun registerChatsListenerIfNeeded(userId: String) {
        if (chatsListenerRegistration != null) {
            Log.d("ChatListVM", "Chats listener already registered.")
            return
        }
        Log.d("ChatListVM", "Registering chats listener for user $userId")
        chatsListenerRegistration = repository.registerChatsListener(userId) { docs ->
            Log.d("ChatListVM", "Chats listener received ${docs.size} documents.")
            viewModelScope.launch(Dispatchers.IO) {
                val updatedProcessedChats = docs.mapNotNull { doc ->
                    repository.processChatDocument(doc, userId)
                }.sortedByDescending { it.timestamp }

                // Aggiorna _processedChats. Combine farà il resto.
                _processedChats.value = updatedProcessedChats
                Log.d("ChatListVM", "Updated _processedChats with ${updatedProcessedChats.size} chats.")
                // La chiamata a updateMessageListeners verrà gestita dal collect su chats nell'init
            }
        }
    }

    // Aggiorna i listener per lo stato dei messaggi
    private fun updateMessageListeners(visibleChats: List<Chat>) {
        val currentVisibleChatIds = visibleChats.map { it.id }.toSet()
        Log.d("ChatListVM_Listen", "Updating message listeners based on ${currentVisibleChatIds.size} visible chats.")

        // Rimuovi listener per chat non più visibili
        val listenersToRemove = messageListeners.filterKeys { it !in currentVisibleChatIds }
        listenersToRemove.forEach { (chatId, listener) ->
            listener.remove()
            messageListeners.remove(chatId)
            Log.d("ChatListVM_Listen", "Removed listener for chat $chatId (filtered out or gone)")
        }

        // Aggiungi nuovi listener per le chat visibili che non ne hanno uno
        visibleChats.forEach { chat ->
            if (chat.id !in messageListeners) {
                Log.d("ChatListVM_Listen", "Adding listener for visible chat ${chat.id}")
                val listener = repository.listenForMessageStatusUpdates(chat.id) { newStatus ->
                    viewModelScope.launch {
                        val currentProcessed = _processedChats.value.toMutableList()
                        val chatIndex = currentProcessed.indexOfFirst { it.id == chat.id }
                        if (chatIndex != -1) {
                            val updatedChat = currentProcessed[chatIndex].copy(lastMessageStatus = newStatus)
                            currentProcessed[chatIndex] = updatedChat
                            _processedChats.value = currentProcessed.sortedByDescending { it.timestamp }
                            // Log.d("ChatListVM_Status", "Updated status in _processedChats for ${chat.id} to $newStatus")
                        } else {
                            Log.w("ChatListVM_Status", "Chat ${chat.id} not found in _processedChats for status update.")
                        }
                    }
                }
                messageListeners[chat.id] = listener
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ChatListVM", "ViewModel cleared. Removing listeners.")
        chatsListenerRegistration?.remove() // Rimuovi listener principale chat
        blockedUsersListenerRegistration?.remove() // Rimuovi listener utenti bloccati
        chatsListenerRegistration = null
        blockedUsersListenerRegistration = null
        messageListeners.values.forEach { it.remove() } // Rimuovi listener stato messaggi
        messageListeners.clear()
    }
}
