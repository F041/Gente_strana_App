// File: FCMUtils.kt
package com.gentestrana.utils

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Forza il refresh del token FCM solo se necessario.
 * Salva il token in locale per evitare operazioni inutili.
 */
fun forceTokenRefreshIfNeeded(context: Context) {
    val sharedPrefs = context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
    val savedToken = sharedPrefs.getString("fcmToken", null)

    FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
        if (tokenTask.isSuccessful) {
            val currentToken = tokenTask.result
            if (currentToken != null && currentToken == savedToken) {
                Log.d("FCM_TOKEN_DEBUG", "Il token corrente è uguale a quello salvato: non serve forzare il refresh.")
                return@addOnCompleteListener
            } else {
                Log.d("FCM_TOKEN_DEBUG", "Token diverso o non presente, procedo con il refresh.")
                // Procediamo con il refresh (non passiamo currentToken perché non serve)
                refreshToken(sharedPrefs)
            }
        } else {
            Log.e("FCM_TOKEN_DEBUG", "Errore nel recupero del token corrente", tokenTask.exception)
        }
    }
}

/**
 * Esegue il refresh forzato del token e aggiorna Firestore e SharedPreferences.
 */
private fun refreshToken(sharedPrefs: android.content.SharedPreferences) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    if (currentUser == null) {
        Log.e("FCM_TOKEN_DEBUG", "Utente non autenticato. Impossibile aggiornare il token.")
        return
    }
    val userId = currentUser.uid
    val firestore = Firebase.firestore
    val userDocRef = firestore.collection("users").document(userId)

    // Recupera il vecchio token da Firestore
    userDocRef.get().addOnSuccessListener { document ->
        val oldToken = document.getString("fcmToken")
        Log.d("FCM_TOKEN_DEBUG", "Token vecchio recuperato da Firestore: $oldToken")

        // Cancellazione del token per forzare la generazione di un nuovo token
        FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { deleteTask ->
            if (deleteTask.isSuccessful) {
                Log.d("FCM_TOKEN_DEBUG", "Token cancellato con successo. Verrà generato un nuovo token.")
                // Recupera il nuovo token
                FirebaseMessaging.getInstance().token.addOnCompleteListener { newTokenTask ->
                    if (newTokenTask.isSuccessful) {
                        val newToken = newTokenTask.result
                        Log.d("FCM_TOKEN_DEBUG", "Nuovo token generato: $newToken")

                        // Aggiorna Firestore con il nuovo token
                        userDocRef.update("fcmToken", newToken).addOnSuccessListener {
                            Log.d("FCM_TOKEN_DEBUG", "Token aggiornato in Firestore per l'utente $userId.")
                            // Salva il nuovo token in SharedPreferences
                            sharedPrefs.edit().putString("fcmToken", newToken).apply()

                            // Verifica l'aggiornamento
                            userDocRef.get().addOnSuccessListener { updatedDocument ->
                                val updatedToken = updatedDocument.getString("fcmToken")
                                if (updatedToken == newToken) {
                                    Log.d("FCM_TOKEN_DEBUG", "Verifica completata: il nuovo token combacia con quello in Firestore.")
                                } else {
                                    Log.e("FCM_TOKEN_DEBUG", "Errore: il token in Firestore ($updatedToken) non combacia con il nuovo token ($newToken).")
                                }
                            }
                        }.addOnFailureListener { e ->
                            Log.e("FCM_TOKEN_DEBUG", "Errore nell'aggiornamento del token in Firestore", e)
                        }
                    } else {
                        Log.e("FCM_TOKEN_DEBUG", "Errore nel recupero del nuovo token", newTokenTask.exception)
                    }
                }
            } else {
                Log.e("FCM_TOKEN_DEBUG", "Errore nella cancellazione del token", deleteTask.exception)
            }
        }
    }.addOnFailureListener { e ->
        Log.e("FCM_TOKEN_DEBUG", "Errore nel recupero del vecchio token da Firestore", e)
    }
}
