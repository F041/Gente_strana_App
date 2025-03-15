package com.gentestrana.utils

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

/**
 * Utility function per eliminare i dati utente da Firestore, incluso il documento utente.
 * Al momento, questa funzione elimina SOLO il documento utente principale.
 * TODO: Estendere per eliminare anche chat e altri dati correlati in futuro.
 */
object FirestoreDeletionUtils {
    suspend fun deleteUserDataFromFirestore(
        userId: String,
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit
    ) {
        val firestore = Firebase.firestore
        val userDocumentRef = firestore.collection("users").document(userId)

        withContext(Dispatchers.IO) {
            // Esegui operazioni Firestore in background
            try {
                userDocumentRef.delete().await()
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.message)
            }
        }
    }
}