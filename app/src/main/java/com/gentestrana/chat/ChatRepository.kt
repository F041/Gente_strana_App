package com.gentestrana.chat

import com.gentestrana.users.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object ChatRepository {
    suspend fun createNewChat(user: User): String {
        val db = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Utente non loggato")

        // Check if a chat already exists between the two users
        val existingChat = db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .get()
            .await()
            .documents
            .firstOrNull { doc ->
                val participants = doc.get("participants") as? List<String> ?: emptyList()
                participants.contains(user.docId)
            }

        // If a chat already exists, return its ID
        if (existingChat != null) {
            return existingChat.id
        }

        // Otherwise, create a new chat
        val chatData = hashMapOf(
            "participants" to listOf(currentUserId, user.docId),
            "createdAt" to FieldValue.serverTimestamp()
        )

        val documentRef = db.collection("chats").add(chatData).await()
        return documentRef.id
    }
}
