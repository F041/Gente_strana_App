package com.gentestrana.utils

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Aggiorna il campo 'lastActive' dell'utente autenticato.
 * Questa funzione può essere richiamata ogni volta che l'utente interagisce con l'app.
 */
fun updateUserLastActive() {
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return
    val uid = currentUser.uid
    val firestore = Firebase.firestore
    firestore.collection("users").document(uid)
        .update("lastActive", Timestamp.now())
        .addOnSuccessListener {
            // Puoi aggiungere un log per il successo se necessario
        }
        .addOnFailureListener { error ->
            // Logga o gestisci l'errore se necessario
        }
}
