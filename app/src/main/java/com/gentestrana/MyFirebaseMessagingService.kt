package com.gentestrana

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.auth.ktx.auth
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

// cambiare nome?
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "From: ${remoteMessage.from}")

        // 1. Verifica impostazioni utente (già presente)
        val sharedPreferences = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val pushNotificationsEnabled = sharedPreferences.getBoolean("push_notifications_enabled", true)
        if (!pushNotificationsEnabled) {
            Log.d("FCM", "Push notifications disabled by user.")
            return
        }

        // **2. CONTROLLO PERMESSO NOTIFICHE (aggiunto ora)**
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU è il nome in codice di Android 13
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("FCM", "Notification permission NOT granted, cannot show notification.")
                // Potresti anche decidere di NON mostrare la notifica in questo caso, o mostrare un messaggio diverso all'utente
                return // Esci dalla funzione se il permesso non è concesso
            }
        }

        // 2. Estrai i dati della notifica dal messaggio remoto
        val notificationTitle = remoteMessage.notification?.title ?: "Nuovo Messaggio" // Titolo di default se non presente
        val notificationBody = remoteMessage.notification?.body ?: "Hai ricevuto un nuovo messaggio." // Corpo di default

        // 3. Crea un canale di notifica (solo per Android Oreo e versioni successive)
        val channelId = getString(R.string.notification_channel_id)
        // Assicurati di avere questa stringa in strings.xml
        createNotificationChannel(channelId)

        // 4. Costruisci la notifica
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            // Sostituisci con la tua icona di notifica (creala in drawable)
            .setContentTitle(notificationTitle)
            .setContentText(notificationBody)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        // La notifica scompare quando l'utente la tocca

        // 5. Mostra la notifica
        NotificationManagerCompat.from(this)
            .notify(System.currentTimeMillis().toInt(), builder.build()) // ID notifica univoco
    }

    private fun createNotificationChannel(channelId: String) {
        // Crea il NotificationChannel, ma solo per API 26+ perché
        // la classe NotificationChannel è nuova e non disponibile nelle librerie di supporto precedenti
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name) // Nome visibile all'utente
            val descriptionText = getString(R.string.notification_channel_description) // Descrizione del canale
            val importance = NotificationManager.IMPORTANCE_DEFAULT // Importanza del canale (default)
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            // Registra il canale con il sistema NotificationManager
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
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
