
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

        // If a chat already exists, return its ID and unhide if deletedFor contains currentUserId
        if (existingChat != null) {
            val deletedFor = existingChat.get("deletedFor") as? List<String> ?: emptyList()
            if (deletedFor.contains(currentUserId)) {
                db.collection("chats").document(existingChat.id)
                    .update("deletedFor", FieldValue.arrayRemove(currentUserId))
                    .await()
            }
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

                // Chat eliminata dall'utente corrente: non deve apparire nella sua lista.
                val deletedFor = doc.get("deletedFor") as? List<String> ?: emptyList()
                if (deletedFor.contains(currentUserId)) {
                    Log.d("ChatRepository", "Chat ${doc.id} saltata (eliminata da $currentUserId)")
                    return@coroutineScope null
                }

                // Chat fissata in cima dall'utente corrente
                val pinnedBy = doc.get("pinnedBy") as? List<String> ?: emptyList()
                val isPinned = pinnedBy.contains(currentUserId)

                // **FIX 1**: Se hasMessages è false, la chat è vuota (appena creata senza messaggi).
                // Non mostrarla nella lista di nessuno.
                val hasMessages = doc.getBoolean("hasMessages") ?: false
                if (!hasMessages) {
                    Log.d("ChatRepository", "Chat ${doc.id} saltata (hasMessages=false)")
                    return@coroutineScope null
                }

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

                // **FIX 2**: Doppio check di sicurezza — se non ci sono messaggi, non mostrare la chat
                val lastMessageDoc = lastMessageQuerySnapshot.documents.firstOrNull()
                if (lastMessageDoc == null) {
                    Log.d("ChatRepository", "Chat ${doc.id} saltata (nessun messaggio trovato)")
                    return@coroutineScope null
                }

                // --- ELABORA I RISULTATI ---
                val userObj = otherUserSnapshot.toObject(User::class.java)
                val photoUrlFromUser = userObj?.profilePicUrl?.firstOrNull { it.isNotBlank() } ?: ""
                val rawPhotoData = otherUserSnapshot.get("profilePicUrl")
                val firstPhotoUrl = if (photoUrlFromUser.isNotBlank()) {
                    photoUrlFromUser
                } else if (rawPhotoData is String && rawPhotoData.isNotBlank()) {
                    rawPhotoData
                } else if (rawPhotoData is List<*>) {
                    (rawPhotoData.firstOrNull() as? String) ?: ""
                } else {
                    ""
                }

                val lastMessageText = lastMessageDoc.getString("message") ?: ""
                val lastMessageSenderId = lastMessageDoc.getString("sender") ?: ""
                val timestamp = lastMessageDoc.getTimestamp("timestamp") ?: Timestamp.now()
                val lastMessageStatus = when (lastMessageDoc.getString("status")) {
                    "DELIVERED" -> MessageStatus.DELIVERED
                    "READ" -> MessageStatus.READ
                    else -> MessageStatus.SENT
                }

                // Online status: lastActive entro 5 minuti e mezzo (330.000 ms)
                val lastActiveMs = userObj?.lastActive?.toDate()?.time ?: 0L
                val nowMs = System.currentTimeMillis()
                val isOnline = (nowMs - lastActiveMs) in 0..330_000L

                // --- CREA E RESTITUISCI L'OGGETTO CHAT ---
                Chat(
                    id = doc.id,
                    participantId = otherUserId,
                    participantName = userObj?.username ?: "Sconosciuto",
                    lastMessage = lastMessageText,
                    lastMessageSenderId = lastMessageSenderId,
                    photoUrl = firstPhotoUrl,
                    lastMessageStatus = lastMessageStatus,
                    timestamp = timestamp,
                    isOnline = isOnline,
                    isPinned = isPinned,
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

            val messagesRef = db.collection("chats/$chatId/messages")
            val query = messagesRef
                .whereEqualTo("status", "DELIVERED")
                .get()
                .await()

            if (query.isEmpty) return

            val batch = db.batch()
            var count = 0
            query.documents.forEach { doc ->
                if (doc.getString("sender") != currentUserId) {
                    batch.update(doc.reference, "status", "READ")
                    count++
                }
            }
            if (count > 0) {
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error in markMessagesAsRead: ${e.message}")
        }
    }

    suspend fun markMessagesAsDelivered(chatId: String, currentUserId: String) {
        try {
            val messagesRef = db.collection("chats/$chatId/messages")

            val query = messagesRef
                .whereEqualTo("status", "SENT")
                .whereNotEqualTo("sender", currentUserId)
                .get()
                .await()

            if (query.isEmpty) return

            val batch = db.batch()
            query.documents.forEach { doc ->
                batch.update(doc.reference, "status", "DELIVERED")
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error in markMessagesAsDelivered: ${e.message}")
        }
    }

    suspend fun sendMessage(
        chatId: String,
        sender: String,
        message: String,
        context: Context,
        replyingTo: ChatMessage?
    ) {
        if (!MessageRateLimiter.canSendMessage(sender)) {
            throw Exception("Rate limit exceeded. Attendi qualche secondo prima di inviare altri messaggi.")
        }
        if (!MessageDailyLimitManager.canSendMessage(context, sender)) {
            throw Exception("Daily message limit exceeded. Please try again tomorrow.")
        }
        try {
            val chatDocRef = db.collection("chats").document(chatId)

            val chatDocSnapshot = chatDocRef.get().await()
            val currentlyHasMessages = chatDocSnapshot.getBoolean("hasMessages") ?: false
            val deletedFor = chatDocSnapshot.get("deletedFor") as? List<String> ?: emptyList()

            if (!currentlyHasMessages || deletedFor.isNotEmpty()) {
                val updates = hashMapOf<String, Any>("hasMessages" to true)
                if (deletedFor.isNotEmpty()) {
                    updates["deletedFor"] = FieldValue.delete()
                }
                chatDocRef.update(updates).await()
                Log.d("ChatRepository", "Flag 'hasMessages' e 'deletedFor' aggiornati per chat $chatId")
            }

            val sanitizedMessage = sanitizeInput(removeSpaces(message))
            Log.d("ChatRepository", "Invio messaggio: '$sanitizedMessage'")

            // ---> INIZIO MODIFICA FONDAMENTALE <---
            // 1. Crea una mappa MUTABILE con i campi base obbligatori
            val messageData = mutableMapOf<String, Any>(
                "sender" to sender,
                "message" to sanitizedMessage,
                "timestamp" to Timestamp.now(),
                "status" to "SENT"
            )

            // 2. Aggiungi i campi della risposta SOLO SE replyingTo non è nullo
            if (replyingTo != null) {
                messageData["isReply"] = true
                messageData["replyToMessageText"] = replyingTo.message
                messageData["replyToMessageSender"] = replyingTo.sender
            }

            Log.d("REPLY_DEBUG", "Dati inviati a Firestore: $messageData")

            chatDocRef.collection("messages")
                .add(messageData) // Invia la mappa, che ora ha 4 o 7 campi a seconda del caso
                .await()

            MessageDailyLimitManager.incrementCount(context, sender)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Errore durante l'invio del messaggio o aggiornamento flag", e)
            throw e
        }
    }

    suspend fun toggleMessageReaction(chatId: String, messageId: String, userId: String, emoji: String) {
        try {
            val messageRef = db.collection("chats/$chatId/messages").document(messageId)
            val snapshot = messageRef.get().await()
            @Suppress("UNCHECKED_CAST")
            val currentReactions = snapshot.get("reactions") as? Map<String, String> ?: emptyMap()

            if (currentReactions[userId] == emoji) {
                messageRef.update("reactions.$userId", FieldValue.delete()).await()
            } else {
                messageRef.update("reactions.$userId", emoji).await()
            }
            Log.d("ChatRepository", "Reazione $emoji aggiornata per messaggio $messageId da $userId")
        } catch (e: Exception) {
            Log.e("ChatRepository", "Errore durante l'aggiornamento della reazione", e)
        }
    }



    /**
     * Fissa o rimuove il fissaggio di una chat per l'utente corrente.
     * Il flag e' salvato nel documento chat come array `pinnedBy`.
     */
    suspend fun setChatPinnedForMe(chatId: String, pinned: Boolean) {
        val currentUserId = auth.currentUser?.uid
            ?: throw IllegalStateException("Utente non loggato")

        val field = if (pinned) FieldValue.arrayUnion(currentUserId)
                    else FieldValue.arrayRemove(currentUserId)

        db.collection("chats").document(chatId)
            .update("pinnedBy", field)
            .await()
        Log.d("ChatRepository", "Chat $chatId ${if (pinned) "fissata" else "sbloccata"} per $currentUserId")
    }

    /**
     * Elimina (per l'utente corrente) una conversazione: la chat resta visibile
     * all'altro partecipante ma non appare piu' nella lista di chi l'ha eliminata.
     * Implementata come soft-delete via array `deletedFor`.
     */
    suspend fun deleteChatForMe(chatId: String) {
        val currentUserId = auth.currentUser?.uid
            ?: throw IllegalStateException("Utente non loggato")

        db.collection("chats").document(chatId)
            .update("deletedFor", FieldValue.arrayUnion(currentUserId))
            .await()
        Log.d("ChatRepository", "Chat $chatId eliminata per $currentUserId")
    }

    suspend fun deleteMessage(chatId: String, messageId: String) {
        try {
            val messagesRef = db.collection("chats")
                .document(chatId)
                .collection("messages")

            // Cancella il messaggio specifico
            messagesRef
                .document(messageId)
                .delete()
                .await()
            Log.d("ChatRepository", "Messaggio $messageId eliminato")

            // Verifica se nella chat rimangono altri messaggi
            val remainingMessages = messagesRef
                .limit(1)
                .get()
                .await()
                .documents

            // Se non ci sono più messaggi, imposta hasMessages = false anziché cancellare la chat
            if (remainingMessages.isEmpty()) {
                db.collection("chats")
                    .document(chatId)
                    .update("hasMessages", false)
                    .await()
                Log.d("ChatRepository", "Chat $chatId aggiornata: hasMessages = false")
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Errore durante l'eliminazione del messaggio", e)
            throw e
        }
    }
}








