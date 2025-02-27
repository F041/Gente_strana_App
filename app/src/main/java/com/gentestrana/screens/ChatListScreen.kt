package com.gentestrana.screens

import android.util.Log
import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.gentestrana.chat.Chat
import com.gentestrana.chat.ChatRepository
import com.gentestrana.components.ChatList
import com.google.firebase.firestore.ListenerRegistration


@Composable
fun ChatListScreen(navController: NavController) {
    val messageListeners = remember { mutableStateMapOf<String, ListenerRegistration>() }
    val repository = remember { ChatRepository() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val chats = remember { mutableStateListOf<Chat>() }
    var isRefreshing by remember { mutableStateOf(false) }
    var listenerRegistration by remember { mutableStateOf<ListenerRegistration?>(null) }

    // Funzione di caricamento
    fun loadChats(forceUpdate: Boolean = false) {
        if (currentUserId == null) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (forceUpdate) isRefreshing = true
                val newChats = repository.getChats(currentUserId)
                    .sortedByDescending { it.timestamp } // from last message to first
                chats.clear()
                chats.addAll(newChats)
            } catch (e: Exception) {
                Log.e("ChatList", "Error loading chats", e)
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(chats) {
        // Rimuovi listener per chat non più presenti

        val currentChatIds = chats.map { it.id }.toSet()
        messageListeners.keys.filterNot { currentChatIds.contains(it) }.forEach { chatId ->
            messageListeners[chatId]?.remove()
            messageListeners.remove(chatId)
        }

        // Aggiungi nuovi listener per le chat correnti
        chats.forEach { chat ->
            if (!messageListeners.containsKey(chat.id)) {
                val listener = repository.listenForMessageStatusUpdates(chat.id) { newStatus ->
                    val index = chats.indexOfFirst { it.id == chat.id }
                    if (index != -1) {
                        val updatedChat = chats[index].copy(lastMessageStatus = newStatus)
                        chats[index] = updatedChat
                    }
                }
                messageListeners[chat.id] = listener
            }
        }
    }

    // Gestione degli aggiornamenti in tempo reale
    LaunchedEffect(currentUserId) {
        currentUserId?.let { userId ->
            listenerRegistration = repository.registerChatsListener(userId) { docs ->
                CoroutineScope(Dispatchers.IO).launch {
                    val updates = docs.mapNotNull { doc ->
                        repository.processChatDocument(doc, userId)
                    }
                    withContext(Dispatchers.Main) {
                        updates.forEach { updatedChat ->
                            val index = chats.indexOfFirst { it.id == updatedChat.id }
                            if (index != -1) chats[index] = updatedChat else chats.add(updatedChat)
                        }
                        chats.sortByDescending { it.timestamp } // from last message to first
                    }
                }
            }
        }
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = {
            loadChats(forceUpdate = true)
            chats.sortByDescending { it.timestamp }
        }
    ) {
        ChatList(
            chats = chats,
            onChatClick = { chat -> navController.navigate("chat/${chat.id}") }
        )

    }

    DisposableEffect(Unit) {
        onDispose {
            listenerRegistration?.remove()
        }
    }
}



