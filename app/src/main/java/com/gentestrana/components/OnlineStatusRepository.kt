package com.gentestrana.components

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class OnlineStatusRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun getOnlineStatuses(): Flow<Map<String, Boolean>> = callbackFlow {
        val listenerRegistration: ListenerRegistration = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val statuses = mutableMapOf<String, Boolean>()
                    for (doc in snapshot.documents) {
                        val userId = doc.id
                        val isOnline = doc.getBoolean("isOnline") ?: false
                        statuses[userId] = isOnline
                    }
                    trySend(statuses)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }
}
