package com.gentestrana.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.awaitCancellation

@Composable
fun ChatListScreen(navController: NavController) {
    val db = Firebase.firestore
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val chats = remember { mutableStateListOf<Chat>() }
    val fetchedChatIds = remember { mutableSetOf<String>() }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            val query = db.collection("chats")
                .whereArrayContains("participants", currentUserId)

            // Aggiungi lo SnapshotListener per aggiornamenti in tempo reale
            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatList", "Errore nel listener", error)
                    return@addSnapshotListener
                }

                // Elabora i documenti in un coroutine separato
                CoroutineScope(Dispatchers.IO).launch {
                    val newChats = mutableListOf<Chat>()
                    val tempFetchedIds = mutableSetOf<String>()

                    snapshot?.documents?.forEach { doc ->
                        val chatId = doc.id
                        if (tempFetchedIds.contains(chatId)) return@forEach

                        tempFetchedIds.add(chatId)

                        // Elaborazione partecipanti e ultimo messaggio
                        val participants = doc.get("participants") as? List<String> ?: emptyList()
                        val otherUserId = participants.firstOrNull { it != currentUserId }

                        otherUserId?.let {
                            val otherUser = db.collection("users").document(it).get().await()
                            val username = otherUser.getString("username") ?: "Sconosciuto"

                            val lastMessage = db.collection("chats")
                                .document(chatId)
                                .collection("messages")
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .await()
                                .documents
                                .firstOrNull()
                                ?.getString("message") ?: "Nessun messaggio"

                            newChats.add(Chat(chatId, username, lastMessage))
                        }
                    }

                    // Aggiorna lo stato UI sul main thread
                    withContext(Dispatchers.Main) {
                        chats.clear()
                        chats.addAll(newChats)
                        fetchedChatIds.clear()
                        fetchedChatIds.addAll(tempFetchedIds)
                    }
                }
            }

            // Pulizia quando il composable viene rimosso
            awaitCancellation()
            listener.remove()
        }
    }

    // Display the list of chats
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(chats) { chat ->
            ChatListItem(chat = chat) {
                navController.navigate("chat/${chat.id}")
            }
        }
    }
}

@Composable
fun ChatListItem(chat: Chat, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = chat.participantName,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = chat.lastMessage,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

data class Chat(
    val id: String,
    val participantName: String,
    val lastMessage: String
)
