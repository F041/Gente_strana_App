package com.gentestrana.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentestrana.components.OnlineStatusRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val onlineStatusRepository: OnlineStatusRepository
) : ViewModel() {

    // Otteniamo l'ID dell'utente corrente
    private val currentUserId: String = chatRepository.getCurrentUserId()
        ?: throw IllegalStateException("Utente non loggato")

    // Ora passiamo currentUserId al metodo getChatsFlow
    val chatsFlow = chatRepository.getChatsFlow(currentUserId)

    // Flow degli stati online
    val onlineStatusesFlow = onlineStatusRepository.getOnlineStatuses()

    // Combiniamo i due flow: per ogni chat, aggiorniamo isOnline in base alla mappa
    val combinedChatsFlow = chatsFlow.combine(onlineStatusesFlow) { chats, statuses ->
        chats.map { chat ->
            chat.copy(isOnline = statuses[chat.participantId] ?: false)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}
