package com.gentestrana

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.auth.ktx.auth

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Log the message for debugging
        Log.d("FCM", "From: ${remoteMessage.from}")

        // If the message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM", "Message data payload: ${remoteMessage.data}")
        }

        // If the message contains a notification payload, log it
        remoteMessage.notification?.let {
            Log.d("FCM", "Message Notification Body: ${it.body}")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")

        // Get the current user's ID (from Firebase Auth)
        val userId = Firebase.auth.currentUser?.uid
        if (userId == null) {
            Log.e("FCM", "User not authenticated, token not updated in Firestore")
            return
        }

        // Update the user's Firestore document with the new token
        Firebase.firestore.collection("users").document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d("FCM", "Token updated successfully in Firestore.")
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Error updating token in Firestore", e)
            }
    }
}
