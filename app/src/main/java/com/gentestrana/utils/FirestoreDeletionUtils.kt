package com.gentestrana.utils

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await

object FirestoreDeletionUtils {
    suspend fun deleteUserDataFromFirestore(userId: String) {
        val firestore = Firebase.firestore
        // Ottieni un riferimento allo storage
        val storage = Firebase.storage

        Log.d("FirestoreDeletion", "Inizio eliminazione dati per utente: $userId") // Log Inizio

        try {

            // 1. Recupera il documento utente
            // PRIMA di eliminarlo per ottenere gli URL delle immagini
            val userDocSnapshot = firestore.collection("users").document(userId).get().await()
            if (userDocSnapshot.exists()) {
                val imageUrls = userDocSnapshot.get("profilePicUrl") as? List<String> ?: emptyList()
                Log.d("FirestoreDeletion", "Trovati ${imageUrls.size} URL immagine per l'utente $userId.")

                // 2. Itera sugli URL e tenta di eliminare ogni immagine dallo Storage
                for (imageUrl in imageUrls) {
                    if (imageUrl.isNotBlank() && imageUrl.startsWith("gs://") || imageUrl.startsWith("https://firebasestorage.googleapis.com/")) { // Controllo base sulla validità dell'URL
                        try {
                            val imageRef = storage.getReferenceFromUrl(imageUrl)
                            imageRef.delete().await() // Usiamo await qui perché siamo in una suspend function
                            Log.d("FirestoreDeletion", "Immagine eliminata da Storage: $imageUrl")
                        } catch (storageEx: Exception) {
                            // Logga l'errore ma continua con le altre eliminazioni
                            Log.e("FirestoreDeletion", "Errore eliminazione immagine $imageUrl da Storage: ${storageEx.message}")
                        }
                    } else {
                        Log.w("FirestoreDeletion", "URL immagine non valido o vuoto skipped: '$imageUrl'")
                    }
                }
            } else {
                Log.w("FirestoreDeletion", "Documento utente $userId non trovato per recuperare URL immagini.")
            }

            // 3. Elimina il documento utente
            Log.d("FirestoreDeletion", "Eliminazione documento utente $userId da Firestore...")
            firestore.collection("users").document(userId).delete().await()
            Log.d("FirestoreDeletion", "Documento utente $userId eliminato.")

            // 4. Elimina le chat associate
            Log.d("FirestoreDeletion", "Ricerca chat per utente $userId...")
            val chatsSnapshot = firestore.collection("chats")
                .whereArrayContains("participants", userId)
                .get()
                .await()
            Log.d("FirestoreDeletion", "Trovate ${chatsSnapshot.documents.size} chat da eliminare.")

            for (chatDoc in chatsSnapshot.documents) {
                Log.d("FirestoreDeletion", "Eliminazione chat ${chatDoc.id}...")
                chatDoc.reference.delete().await()
                Log.d("FirestoreDeletion", "Chat ${chatDoc.id} eliminata.")
                // Qui potresti aggiungere la logica per eliminare anche la sotto-collezione 'messages' di ogni chat, se necessario.
            }

            // Aggiungi qui altre eliminazioni correlate

            Log.d("FirestoreDeletion", "Eliminazione dati per utente $userId completata con successo.") // Log Successo

        } catch (e: Exception) {
            Log.e("FirestoreDeletion", "Errore generale durante eliminazione dati per utente $userId: ${e.message}", e) // Log Errore Generale
            throw e // Propaga l'eccezione per gestirla nel chiamante (esistente)
        }
    }
}