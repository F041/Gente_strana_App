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
import com.google.firebase.messaging.FirebaseMessaging

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
                Log.e("FCM_TOKEN_DEBUG", "Failure Exception Class: ${e::class.java.name}")
                Log.e("FCM_TOKEN_DEBUG", "Failure Exception Message: ${e.message}")
                e.printStackTrace()

                // NUOVI LOG EXTRA PER CERCARE ERRORI DI PERMESSO:
                if (e is com.google.firebase.firestore.FirebaseFirestoreException) { // <-- CONTROLLO TIPO DI ERRORE
                    if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) { // <-- CODICE ERRORE PER PERMESSO NEGATO
                        Log.e("FCM_TOKEN_DEBUG", "POSSIBILE ERRORE DI PERMESSI DI FIRESTORE! (PERMISSION_DENIED)")
                        Log.e("FCM_TOKEN_DEBUG", "Verifica le REGOLE DI SICUREZZA di FIRESTORE per la collezione 'users'!")
                    } else {
                        Log.e("FCM_TOKEN_DEBUG", "Errore FIRESTORE (codice: ${e.code}) - NON sembra essere PERMISSION_DENIED")
                    }
                } else {
                    Log.e("FCM_TOKEN_DEBUG", "Errore di aggiornamento FIRESTORE - NON è FirebaseFirestoreException")
                }
            }

        fun forceTokenRefresh() {
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
