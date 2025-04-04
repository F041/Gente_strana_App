package com.gentestrana.ui_controller

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentestrana.chat.Chat
import com.gentestrana.chat.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {

    // Stato delle chat
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats

    // Mappa dei listener registrati per ogni chat
    private val messageListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()

    private val currentUserId = repository.getCurrentUserId()

    init {
        // Carica le chat iniziali
        loadChats()

        // Registra il listener per aggiornamenti delle chat
        currentUserId?.let { userId ->
            repository.registerChatsListener(userId) { docs ->
                viewModelScope.launch {
                    val updatedChats = docs.mapNotNull { doc ->
                        repository.processChatDocument(doc, userId)
                    }.sortedByDescending { it.timestamp }
                    _chats.value = updatedChats
                    updateMessageListeners(updatedChats)
                }
            }
        }
    }

    private fun loadChats() {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                val chatsList = repository.getChats(userId).sortedByDescending { it.timestamp }
                _chats.value = chatsList
                updateMessageListeners(chatsList)
            }
        }
    }

    private fun updateMessageListeners(chatsList: List<Chat>) {
        // Rimuovi listener per chat non più presenti
        val currentChatIds = chatsList.map { it.id }.toSet()
        messageListeners.keys.filterNot { it in currentChatIds }.forEach { chatId ->
            messageListeners[chatId]?.remove()
            messageListeners.remove(chatId)
        }

        // Aggiungi nuovi listener per le chat presenti
        chatsList.forEach { chat ->
            if (chat.id !in messageListeners) {
                val listener = repository.listenForMessageStatusUpdates(chat.id) { newStatus ->
                    viewModelScope.launch { // Usa la coroutine scope del ViewModel
                        val currentChats = _chats.value.toMutableList()
                        val chatIndex = currentChats.indexOfFirst { it.id == chat.id }
                        if (chatIndex != -1) {
                            // Crea una copia aggiornata della chat specifica
                            val updatedChat = currentChats[chatIndex].copy(lastMessageStatus = newStatus)
                            // Sostituisci l'elemento nella lista mutabile
                            currentChats[chatIndex] = updatedChat
                            // Aggiorna lo StateFlow con la lista modificata (e riordinata se serve)
                            _chats.value = currentChats.sortedByDescending { it.timestamp }
//                            Log.d("ChatListVM_Status", "Updated status for chat ${chat.id} to $newStatus")
                        } else {
//                            Log.w("ChatListVM_Status", "Chat ${chat.id} not found in current state for status update.")
                            // Potrebbe valere la pena ricaricare tutto in questo caso raro?
                            // loadChats() // Opzionale: fallback se la chat non viene trovata
                        }
                    }
                }
                messageListeners[chat.id] = listener
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Rimuovi tutti i listener
        messageListeners.values.forEach { it.remove() }
        messageListeners.clear()
    }
}
