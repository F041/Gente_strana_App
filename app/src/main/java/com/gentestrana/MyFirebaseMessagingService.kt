package com.gentestrana

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.auth.ktx.auth
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging


// cambiare nome?
class MyFirebaseMessagingService : FirebaseMessagingService() {
    private fun handleNewUserNotification(data: Map<String, String>) {
        val userId = data["userId"] ?: return
        val userName = data["userName"] ?: ""

        val title = getString(R.string.notification_new_user_title)
        val body = getString(R.string.notification_new_user_body, userName)

        showNotification(
            title = title,
            content = body,
            channelId = "new_users_channel",
            userId = userId
        )
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Verifica permessi notifiche
        if (!checkNotificationPermissions()) {
            Log.w(TAG, "Notification permission denied")
            return
        }

        // Gestione dati notifica
        remoteMessage.data.let { data ->
            when (data["type"]) {
                "new_user" -> handleNewUserNotification(data)
                else -> showDefaultNotification(remoteMessage)
            }
        }
    }

    private fun showDefaultNotification(remoteMessage: RemoteMessage) {
        if (!checkNotificationPermissions()) return

        val title = remoteMessage.notification?.title ?: getString(R.string.default_notification_title)
        val body = remoteMessage.notification?.body ?: getString(R.string.default_notification_body)

        createNotificationChannel(getString(R.string.notification_channel_id))
        showNotification(
            title = title,
            content = body,
            channelId = getString(R.string.notification_channel_id),
            userId = ""
        )
    }

    private fun checkNotificationPermissions(): Boolean {
        // Controllo per Android 13+ dove serve il permesso POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        // Per versioni precedenti non è necessario un controllo esplicito
        return true
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Crea un canale con un nome e descrizione definiti
            val channel = NotificationChannel(
                channelId,
                "Notifiche Principali", // Nome del canale
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Canale per le notifiche principali"
            }

            // Ottieni il NotificationManager e crea il canale
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun showNotification(
        title: String,
        content: String,
        channelId: String,
        userId: String = ""
    ) {
        // Crea un Intent per MainActivity con eventuali extra per la navigazione
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("NAVIGATE_TO", "userProfile/$userId")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // Configura il PendingIntent
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Costruisci la notifica
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Genera un ID univoco per la notifica
        val notificationId = System.currentTimeMillis().toInt()

        // Verifica il permesso per POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w("MyFirebaseMsgService", "Permesso per le notifiche non concesso. Notifica non inviata.")
            return
        }

        // Mostra la notifica
        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, notification)
        }
    }



    // 3. Aggiungi questa dichiarazione all'inizio della classe
    private val TAG = "MyFirebaseMsgService"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val userId = Firebase.auth.currentUser?.uid
        Log.d("FCM_TOKEN_DEBUG", "User ID recuperato da FirebaseAuth: $userId")
        if (userId == null) {
            Log.e("FCM_TOKEN_DEBUG", "Utente non autenticato, token NON verrà aggiornato in Firestore")
            return
        }
        Log.d("FCM_TOKEN_DEBUG", "Tentativo di aggiornare fcmToken per user ID: $userId")
        Firebase.firestore.collection("users").document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d("FCM_TOKEN_DEBUG", "Token aggiornato CON SUCCESSO in Firestore per user ID: $userId")
            }
            .addOnFailureListener { e ->
                Log.e("FCM_TOKEN_DEBUG", "Errore durante l'aggiornamento del token in Firestore per user ID: $userId", e)
                // Qui puoi gestire ulteriormente l'errore, ad esempio notificando l'utente o riprovando
            }

        fun forceTokenRefresh() {
            // TODO: da rimettere nel main?
            FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM_TOKEN_DEBUG", "Token cancellato con successo. Verrà generato un nuovo token.")
                    // Firebase genererà automaticamente un nuovo token, chiamando onNewToken.
                } else {
                    Log.e("FCM_TOKEN_DEBUG", "Errore nella cancellazione del token", task.exception)
                }
            }
        }
        Log.d("FCM_TOKEN_DEBUG", "** onNewToken EXIT **")
    }
}
