package com.gentestrana.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.gentestrana.ChatMessage



@Composable
fun ChatScreen(docId: String) {
    val db = Firebase.firestore
    val messagesState = produceState<List<ChatMessage>>(initialValue = emptyList()) {
        db.collection("chats")
            .document(docId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                value = snapshot?.documents?.mapNotNull {
                    it.toObject(ChatMessage::class.java)
                } ?: emptyList()
            }
    }

    // Display the list of messages
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(messagesState.value) { message ->
            MessageRow(chatMessage = message) // Use a proper composable to render the row
        }
    }
}

@Composable
fun MessageRow(chatMessage: ChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(4.dp)
        ) {
            Text(
                text = chatMessage.sender,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = chatMessage.message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}


fun sendMessage(chatId: String, sender: String, message: String) {
    val currentUser = Firebase.auth.currentUser

    if (currentUser == null) {
        Log.e("sendMessage", "No user is signed in! Cannot send message.")
        return
    }

    val messageData = ChatMessage(
        sender = sender,
        message = message,
        timestamp = Timestamp.now()
    )

    Firebase.firestore.collection("chats").document(chatId).collection("messages")
        .add(messageData)
        .addOnSuccessListener {
            Log.i("sendMessage", "Message sent successfully.")
        }
        .addOnFailureListener { e ->
            Log.e("sendMessage", "Failed to send message: ${e.message}")
        }
}
