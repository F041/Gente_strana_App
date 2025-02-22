package com.gentestrana.screens

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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

@Composable
fun ChatListScreen(navController: NavController) {
    val db = Firebase.firestore
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val chats = remember { mutableStateListOf<Chat>() }
    val fetchedChatIds = remember { mutableSetOf<String>() } // Per evitare duplicati

    // Fetch chats from Firestore
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            val chatDocs = db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .await()

            chats.clear()
            fetchedChatIds.clear() // Reset per evitare duplicati

            chatDocs.documents.forEach { doc ->
                val chatId = doc.id

                // Se la chat è già stata aggiunta, la ignoriamo
                if (!fetchedChatIds.contains(chatId)) {
                    fetchedChatIds.add(chatId) // Segnala che questa chat è già stata elaborata

                    val participants = doc.get("participants") as? List<String> ?: emptyList()
                    val otherUserId = participants.firstOrNull { it != currentUserId && it.isNotBlank() }

                    if (otherUserId != null) {
                        val otherUserSnapshot = db.collection("users").document(otherUserId).get().await()
                        val otherUserName = otherUserSnapshot.getString("username") ?: "Unknown"

                        val lastMessageSnapshot = db.collection("chats")
                            .document(chatId)
                            .collection("messages")
                            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .limit(1)
                            .get().await()
                        val lastMessage = lastMessageSnapshot.documents.firstOrNull()?.getString("message") ?: "No messages yet"

                        chats.add(
                            Chat(
                                id = chatId,
                                participantName = otherUserName,
                                lastMessage = lastMessage
                            )
                        )
                    }
                }
            }
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
