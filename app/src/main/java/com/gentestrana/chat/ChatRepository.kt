
package com.gentestrana.chat

import android.content.Context
import android.util.Log
import com.gentestrana.users.User
import com.gentestrana.utils.MessageDailyLimitManager
import com.gentestrana.utils.removeSpaces
import com.gentestrana.utils.sanitizeInput
import com.gentestrana.utils.MessageRateLimiter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers

class ChatRepository(
    //  iniezione dipendenze
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun createNewChat(user: User): String {
        val currentUserId = auth.currentUser?.uid
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
            "createdAt" to FieldValue.serverTimestamp(),
            "hasMessages" to false
        )

        val documentRef = db.collection("chats").add(chatData).await()
        return documentRef.id
    }
    suspend fun processChatDocument(doc: DocumentSnapshot, currentUserId: String): Chat? {
        return try {
            coroutineScope {
                val participants = doc.get("participants") as? List<String> ?: emptyList()
                val otherUserId = participants.firstOrNull { it != currentUserId } ?: return@coroutineScope null

                // --- LANCIA LA LETTURA UTENTE IN PARALLELO ---
                val deferredOtherUser = async(Dispatchers.IO) { // Usa async con Dispatchers.IO
                    db.collection("users")
                        .document(otherUserId)
                        .get()
                        .await() // await qui dentro async
                }

                // --- LANCIA LA LETTURA ULTIMO MESSAGGIO IN PARALLELO ---
                val deferredLastMessageQuery = async(Dispatchers.IO) { // Usa async con Dispatchers.IO
                    db.collection("chats")
                        .document(doc.id)
                        .collection("messages")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .await() // await qui dentro async
                }

                // --- ASPETTA I RISULTATI DI ENTRAMBE LE OPERAZIONI ---
                val otherUserSnapshot = deferredOtherUser.await() // Aspetta il risultato della lettura utente
                val lastMessageQuerySnapshot = deferredLastMessageQuery.await() // Aspetta il risultato della query messaggi

                // --- ELABORA I RISULTATI (logica simile a prima) ---
                val userObj = otherUserSnapshot.toObject(User::class.java)
                val profilePicList = otherUserSnapshot["profilePicUrl"] as? List<String> ?: emptyList()
                val firstPhotoUrl = profilePicList.firstOrNull() ?: ""

                val lastMessageDoc = lastMessageQuerySnapshot.documents.firstOrNull()
                val lastMessageText = lastMessageDoc?.getString("message") ?: "No messages" // Default se non ci sono messaggi
                val timestamp = lastMessageDoc?.getTimestamp("timestamp") ?: doc.getTimestamp("createdAt") ?: Timestamp.now() // Usa createdAt della chat o now() come fallback timestamp
                val lastMessageStatus = when (lastMessageDoc?.getString("status")) {
                    "DELIVERED" -> MessageStatus.DELIVERED
                    "READ" -> MessageStatus.READ
                    else -> MessageStatus.SENT // Default a SENT
                }

                // --- CREA E RESTITUISCI L'OGGETTO CHAT ---
                Chat(
                    id = doc.id,
                    participantId = otherUserId,
                    participantName = userObj?.username ?: "Sconosciuto",
                    lastMessage = lastMessageText,
                    photoUrl = firstPhotoUrl,
                    lastMessageStatus = lastMessageStatus,
                    timestamp = timestamp,
                    // isOnline non viene letto qui, verrà aggiornato nel ViewModel probabilmente
                )
            }

        } catch (e: Exception) {
            Log.e("ChatRepository", "Error processing document ${doc.id}", e)
            null // Restituisce null in caso di errore
        }
    }

    fun registerChatsListener(
        currentUserId: String,
        onUpdate: (List<DocumentSnapshot>) -> Unit
    ): ListenerRegistration {
        return db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Chats listener error", error)
                    return@addSnapshotListener
                }
                snapshot?.documents?.let(onUpdate)
            }
    }
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    suspend fun getChats(currentUserId: String): List<Chat> {
        return try {
            val chatDocs = db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .await()

            chatDocs.documents.mapNotNull { doc ->
                processChatDocument(doc, currentUserId)
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting chats", e)
            emptyList()
        }
    }

    fun listenForMessageStatusUpdates(
        chatId: String,
        onUpdate: (MessageStatus) -> Unit
    ): ListenerRegistration {
        Log.d("STATUS_DEBUG", "Registrato listener per chat: $chatId")

        return db.collection("chats/$chatId/messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("STATUS_DEBUG", "Listener error: ${error.message}")
                    return@addSnapshotListener
                }

                Log.d("STATUS_DEBUG", "Nuovo snapshot ricevuto. Docs: ${snapshot?.documents?.size}")

                snapshot?.documents?.firstOrNull()?.let { doc ->
                    val status = when (doc.getString("status")) {
                        "DELIVERED" -> MessageStatus.DELIVERED
                        "READ" -> MessageStatus.READ
                        else -> MessageStatus.SENT
                    }
                    Log.d("STATUS_DEBUG", "Nuovo stato: $status")
                    onUpdate(status)
                }
            }
    }
    suspend fun markMessagesAsRead(chatId: String) {
        try {
            val currentUserId = auth.currentUser?.uid ?: return
            Log.d("DEBUG", "UserID: $currentUserId - ChatID: $chatId")

            val messagesRef = db.collection("chats/$chatId/messages")
            val query = messagesRef
                .whereEqualTo("status", "DELIVERED")
                .get()
                .await()
            Log.d("DEBUG", "Trovati ${query.documents.size} messaggi da aggiornare")

            // Verifica che ci siano documenti da aggiornare
            if (query.isEmpty) return

            val batch = db.batch()
            query.documents.forEach { doc ->
                // Aggiungi controllo per evitare aggiornamenti non necessari
                if (doc.getString("sender") != currentUserId) {
                    batch.update(doc.reference, "status", "READ")
                }
            }
            batch.commit().await()

        } catch (e: Exception) {
            Log.e("DEBUG", "ERRORE in markMessagesAsRead: ${e.message}")
            throw e
        }
    }

    suspend fun markMessagesAsDelivered(chatId: String, currentUserId: String) {
        val messagesRef = db.collection("chats/$chatId/messages")

        // Cerca SOLO i messaggi dell'altro utente con status SENT
        val query = messagesRef
            .whereEqualTo("status", "SENT")
            .whereNotEqualTo("sender", currentUserId)
            .get()
            .await()

        // Controllo aggiuntivo
        val validDocs = query.documents.filter { doc ->
            val participants = db.collection("chats").document(chatId).get().await()
                .get("participants") as? List<String> ?: emptyList()
            participants.contains(currentUserId)
        }

        val batch = db.batch()
        validDocs.forEach { doc ->
            batch.update(doc.reference, "status", "DELIVERED")
        }
        batch.commit().await()
        Log.d("Repo", "${query.documents.size} messaggi marcati come DELIVERED")
    }

    suspend fun sendMessage(chatId: String,
                            sender: String, message: String
                            ,context: Context
    ) {
        if (!MessageRateLimiter.canSendMessage(sender)) {
            throw Exception("Rate limit exceeded. Attendi qualche secondo prima di inviare altri messaggi.")
        }
        if (!MessageDailyLimitManager.canSendMessage(context, sender)) {
            throw Exception("Daily message limit exceeded. Please try again tomorrow.")
        }
        try {
            // 1. Riferimento al documento chat principale
            val chatDocRef = db.collection("chats").document(chatId)

            // 2. Controlla e aggiorna il flag (se necessario)
            //    Usiamo una transazione per sicurezza, anche se un semplice get+update
            //    potrebbe bastare data la logica (lo impostiamo solo da false a true).
            //    Un get+update è più semplice:
            val chatDocSnapshot = chatDocRef.get().await()
            val currentlyHasMessages = chatDocSnapshot.getBoolean("hasMessages") ?: false // Default a false se non esiste

            if (!currentlyHasMessages) {
                // Se hasMessages è false, aggiornalo a true
                chatDocRef.update("hasMessages", true).await()
                Log.d("ChatRepository", "Flag 'hasMessages' impostato a true per chat $chatId")
            }

            // 3. Procedi con l'invio del messaggio (codice esistente)
            val sanitizedMessage = sanitizeInput(removeSpaces(message))
            Log.d("ChatRepository", "Invio messaggio: '$sanitizedMessage'")
            val messageData = hashMapOf(
                "sender" to sender,
                "message" to sanitizedMessage,
                "timestamp" to Timestamp.now(),
                "status" to "SENT"
            )
            chatDocRef.collection("messages")
                .add(messageData)
                .await()

            MessageDailyLimitManager.incrementCount(context, sender)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Errore durante l'invio del messaggio o aggiornamento flag", e)
            throw e // Rilancia l'eccezione
        }
    }


    suspend fun deleteMessage(chatId: String, messageId: String) {
        try {
            db.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .delete()
                .await()
            Log.d("ChatRepository", "Messaggio eliminato con successo")
        } catch (e: Exception) {
            Log.e("ChatRepository", "Errore durante l'eliminazione del messaggio", e)
            throw e
        }
    }
}








