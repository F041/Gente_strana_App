package com.gentestrana.utils

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

import kotlinx.coroutines.tasks.await

/**
 * Utility function per eliminare i dati utente da Firestore, incluso il documento utente.
 * e chat in cui è stato partecipante.
 */
// FirestoreDeletionUtils.kt
object FirestoreDeletionUtils {
    suspend fun deleteUserDataFromFirestore(userId: String) {
        val firestore = Firebase.firestore

        try {
            // Elimina il documento utente
            firestore.collection("users").document(userId).delete().await()

            // Elimina le chat associate
            val chatsSnapshot = firestore.collection("chats")
                .whereArrayContains("participants", userId)
                .get()
                .await()

            for (chatDoc in chatsSnapshot.documents) {
                chatDoc.reference.delete().await()
            }

            // Aggiungi qui altre eliminazioni correlate
        } catch (e: Exception) {
            throw e // Propaga l'eccezione per gestirla nel chiamante
        }
    }
}