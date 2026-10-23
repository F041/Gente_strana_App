package com.gentestrana.calls

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class CallTokenResult(
    val token: String,
    val appId: String,
    val channel: String,
    val account: String,
    val expiresAt: Long
)

class CallRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val functions: FirebaseFunctions
        get() = Firebase.functions("europe-west1")

    suspend fun startCall(chatId: String): String {
        try {
            val result = functions
                .getHttpsCallable("startCall")
                .call(mapOf("chatId" to chatId))
                .await()

            val data = result.getData() as? Map<*, *>
            val callId = data?.get("callId") as? String
                ?: throw Exception("Risposta non valida dal server")
            return callId
        } catch (e: FirebaseFunctionsException) {
            val reason = (e.details as? Map<*, *>)?.get("reason") as? String ?: e.message
            throw Exception(mapCallErrorReason(reason))
        }
    }

    suspend fun getCallToken(callId: String): CallTokenResult {
        try {
            val result = functions
                .getHttpsCallable("getCallToken")
                .call(mapOf("callId" to callId))
                .await()

            val data = result.getData() as? Map<*, *>
                ?: throw Exception("Risposta non valida dal server")

            return CallTokenResult(
                token = data["token"] as? String ?: "",
                appId = data["appId"] as? String ?: "",
                channel = data["channel"] as? String ?: "",
                account = data["account"] as? String ?: "",
                expiresAt = (data["expiresAt"] as? Number)?.toLong() ?: 0L
            )
        } catch (e: FirebaseFunctionsException) {
            val reason = (e.details as? Map<*, *>)?.get("reason") as? String ?: e.message
            throw Exception(mapCallErrorReason(reason))
        }
    }

    suspend fun answerCall(callId: String) {
        firestore.collection("calls").document(callId)
            .update(
                mapOf(
                    "status" to "active",
                    "answeredAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    suspend fun declineCall(callId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        firestore.collection("calls").document(callId)
            .update(
                mapOf(
                    "status" to "declined",
                    "endedAt" to FieldValue.serverTimestamp(),
                    "endedBy" to currentUserId
                )
            ).await()
    }

    suspend fun cancelCall(callId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        firestore.collection("calls").document(callId)
            .update(
                mapOf(
                    "status" to "cancelled",
                    "endedAt" to FieldValue.serverTimestamp(),
                    "endedBy" to currentUserId
                )
            ).await()
    }

    suspend fun markMissedCall(callId: String) {
        firestore.collection("calls").document(callId)
            .update(
                mapOf(
                    "status" to "missed",
                    "endedAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    suspend fun endCall(callId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        firestore.collection("calls").document(callId)
            .update(
                mapOf(
                    "status" to "ended",
                    "endedAt" to FieldValue.serverTimestamp(),
                    "endedBy" to currentUserId
                )
            ).await()
    }

    fun observeCall(callId: String): Flow<Call?> = callbackFlow {
        val listener = firestore.collection("calls").document(callId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val call = snapshotToCall(snapshot.id, snapshot.data ?: emptyMap())
                trySend(call)
            }
        awaitClose { listener.remove() }
    }

    fun observeIncomingCalls(myUid: String): Flow<List<Call>> = callbackFlow {
        val listener = firestore.collection("calls")
            .whereArrayContains("participants", myUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val nowSec = System.currentTimeMillis() / 1000
                val calls = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val call = snapshotToCall(doc.id, data)
                    if (call.calleeId == myUid && call.status == CallStatus.RINGING) {
                        val createdSec = call.createdAt?.seconds ?: nowSec
                        if (nowSec - createdSec <= 20) {
                            return@mapNotNull call
                        }
                    }
                    null
                }
                trySend(calls)
            }
        awaitClose { listener.remove() }
    }

    private fun snapshotToCall(id: String, data: Map<String, Any>): Call {
        val statusStr = data["status"] as? String
        val participants = (data["participants"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        return Call(
            id = id,
            chatId = data["chatId"] as? String ?: "",
            callerId = data["callerId"] as? String ?: "",
            calleeId = data["calleeId"] as? String ?: "",
            callerName = data["callerName"] as? String ?: "",
            callerPhoto = data["callerPhoto"] as? String ?: "",
            participants = participants,
            status = CallStatus.fromString(statusStr),
            createdAt = data["createdAt"] as? com.google.firebase.Timestamp,
            expireAt = data["expireAt"] as? com.google.firebase.Timestamp,
            answeredAt = data["answeredAt"] as? com.google.firebase.Timestamp,
            activeUntil = data["activeUntil"] as? com.google.firebase.Timestamp,
            endedAt = data["endedAt"] as? com.google.firebase.Timestamp,
            endedBy = data["endedBy"] as? String
        )
    }

    private fun mapCallErrorReason(reason: String?): String {
        return when (reason) {
            "locked" -> "Le chiamate si sbloccano quando avete scritto almeno 5 messaggi ciascuno."
            "not-participant" -> "Non sei un partecipante della chat."
            "blocked" -> "Non puoi chiamare questa persona."
            "not-accepting" -> "L'altra persona non accetta chiamate."
            "not-supported" -> "L'altra persona non può ancora ricevere chiamate: serve la versione aggiornata dell'app."
            "monthly-limit" -> "Per questo mese le chiamate gratuite sono finite: riprendono il 1° del mese."
            "daily-limit" -> "Oggi hai usato tutti i minuti di chiamata: riprendono domani."
            "other-daily-limit" -> "L'altra persona oggi ha già usato tutti i minuti di chiamata: riprova domani."
            "busy" -> "L'altra persona è già in un'altra chiamata. Riprova più tardi."
            "self-busy" -> "Sei già in una chiamata."
            else -> reason ?: "Non è stato possibile avviare la chiamata. Controlla la connessione e riprova."
        }
    }
}
