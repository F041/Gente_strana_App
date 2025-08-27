package com.gentestrana.utils

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

fun incrementProfileView(userId: String) {
    val db = FirebaseFirestore.getInstance()
    val viewData = hashMapOf(
        "timestamp" to Timestamp.now()
    )
    db.collection("users")
        .document(userId)
        .collection("profileViews")
        .add(viewData)
        .addOnSuccessListener {
//            Log.d("UserActivityUtils", "Visualizzazione registrata per l'utente: $userId")
        }
        .addOnFailureListener { e ->
//            Log.e("UserActivityUtils", "Errore durante la registrazione della visualizzazione per l'utente: $userId", e)
        }
}
