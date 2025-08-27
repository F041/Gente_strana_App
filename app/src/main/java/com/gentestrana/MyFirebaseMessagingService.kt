package com.gentestrana

// Import necessari (verifica che ci siano tutti)
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.HashMap

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // TAG per i log
    private val TAG = "MyFirebaseMsgService"

    // ID univoci per i canali di notifica (costanti)
    companion object {
        const val ADMIN_REPORTS_CHANNEL_ID = "admin_reports_channel_v1"
        // Aggiunto v1 per forzare ricreazione
        const val NEW_USERS_CHANNEL_ID = "new_users_channel_v1"
        const val DEFAULT_MESSAGES_CHANNEL_ID = "default_messages_channel_v1"
    }

    // Crea i canali all'avvio del servizio (o la prima volta che serve)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        Log.d(TAG, "onCreate: Service created, channels initialized.")
    }

    // Gestisce i messaggi in arrivo
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
//        Log.d(TAG, "-----------------------------------------") // Separatore per chiarezza
//        Log.d(TAG, "onMessageReceived: Message Received!")
//        Log.d(TAG, "From: ${remoteMessage.from}")
//        Log.d(TAG, "Message ID: ${remoteMessage.messageId}")
//        Log.d(TAG, "Data Payload: ${remoteMessage.data}")
//        Log.d(TAG, "Notification Payload: ${remoteMessage.notification?.title} / ${remoteMessage.notification?.body}")

        // Verifica permesso notifiche
        if (!checkNotificationPermissions()) {
            Log.w(TAG, "Notification permission denied. Skipping notification display.")
            return
        }

        // Estrai i payload
        val notificationPayload = remoteMessage.notification
        val dataPayload = remoteMessage.data

        var notificationTitle: String? = null
        var notificationBody: String? = null
        var targetChannelId = DEFAULT_MESSAGES_CHANNEL_ID // Default
        var clickActionData: Map<String, String> = dataPayload // Default: usa tutti i dati

        // --- Logica per determinare tipo, contenuto e canale ---
        val messageSource = remoteMessage.from ?: ""

        // 1. Notifica Segnalazione Admin (Controlla topic o campo 'type')
        if (messageSource.contains("/topics/adminReports") || dataPayload["type"] == "admin_report") {
            Log.d(TAG, "Type: Admin Report Notification")
            notificationTitle = notificationPayload?.title ?: getString(R.string.report_user_dialog_title) // Titolo specifico
            notificationBody = notificationPayload?.body ?: "Hai una nuova segnalazione." // Corpo specifico
            targetChannelId = ADMIN_REPORTS_CHANNEL_ID // Canale ad alta priorità!
            // clickActionData già contiene il reportId dal payload data
        }
        // 2. Notifica Nuovo Utente (Controlla campo 'type')
        else if (dataPayload["type"] == "new_user") {
            Log.d(TAG, "Type: New User Notification")
            val userName = dataPayload["userName"] ?: "Un nuovo utente"
            notificationTitle = notificationPayload?.title ?: getString(R.string.notification_new_user_title)
            notificationBody = notificationPayload?.body ?: getString(R.string.notification_new_user_body, userName)
            targetChannelId = NEW_USERS_CHANNEL_ID
            // clickActionData già contiene userId e userName
        }
        // 3. Notifica Chat o Altro (Default)
        else {
            Log.d(TAG, "Type: Default/Chat Notification")
            notificationTitle = notificationPayload?.title // Prendi dal payload notification
            notificationBody = notificationPayload?.body
            // targetChannelId rimane DEFAULT_MESSAGES_CHANNEL_ID
            // clickActionData contiene eventuali dati specifici della chat
        }

        // Fallback se titolo o corpo sono ancora nulli/vuoti
        if (notificationTitle.isNullOrBlank()) {
            notificationTitle = getString(R.string.default_notification_title)
            Log.w(TAG, "Notification title was blank, using default.")
        }
        if (notificationBody.isNullOrBlank()) {
            notificationBody = getString(R.string.default_notification_body)
            Log.w(TAG, "Notification body was blank, using default.")
        }

        // --- Mostra la notifica ---
        // Lo facciamo sempre qui per coerenza, sia in foreground che background
        Log.d(TAG, "Preparing to show notification: Title='$notificationTitle', Channel='$targetChannelId'")
        showNotification(
            title = notificationTitle!!, // Usiamo !! perché abbiamo messo i default
            content = notificationBody!!,
            channelId = targetChannelId,
            data = clickActionData // Passa i dati per l'intent
        )
        Log.d(TAG, "onMessageReceived processing finished.")
        Log.d(TAG, "-----------------------------------------")
    }

    // Crea i canali di notifica necessari (chiamato da onCreate)
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val adminChannel = NotificationChannel(
                ADMIN_REPORTS_CHANNEL_ID,
                "Segnalazioni Admin", // Nome visibile all'utente
                NotificationManager.IMPORTANCE_HIGH // ** Alta Importanza **
            ).apply {
                description = "Notifiche critiche per le segnalazioni degli utenti (Admin)"
                // Qui puoi aggiungere setSound, enableVibration, enableLights se vuoi personalizzarli
            }

            val newUserChannel = NotificationChannel(
                NEW_USERS_CHANNEL_ID,
                "Nuovi Utenti",
                NotificationManager.IMPORTANCE_DEFAULT // Importanza normale
            ).apply {
                description = "Notifiche quando nuovi utenti si uniscono"
            }

            val defaultChannel = NotificationChannel(
                DEFAULT_MESSAGES_CHANNEL_ID,
                "Messaggi e Altro",
                NotificationManager.IMPORTANCE_DEFAULT // Importanza normale
            ).apply {
                description = "Notifiche per messaggi di chat e altre informazioni"
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(adminChannel)
            notificationManager.createNotificationChannel(newUserChannel)
            notificationManager.createNotificationChannel(defaultChannel)
            Log.d(TAG, "Notification channels created/updated.")
        }
    }

    // Mostra effettivamente la notifica
    private fun showNotification(
        title: String,
        content: String,
        channelId: String,
        data: Map<String, String> = emptyMap()
    ) {
        // Intent per aprire MainActivity
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Inserisci la mappa 'data' come extra. Usiamo HashMap perché è Serializable.
            // MainActivity dovrà estrarre questa mappa e decidere dove navigare.
            putExtra("notification_data", HashMap(data))
            // Aggiungiamo un action univoco per forzare la creazione di un nuovo intent
            // invece di riutilizzare uno vecchio, specialmente per i dati extra.
            action = "ACTION_SHOW_NOTIFICATION_${System.currentTimeMillis()}"
        }

        // PendingIntent per quando l'utente clicca sulla notifica
        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(), // Request code quasi unico
            intent,
            pendingIntentFlag
        )

        // Costruisci la notifica usando il channelId corretto
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Assicurati che l'icona esista!
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Alta priorità per aiutare la visibilità
            .setContentIntent(pendingIntent) // Azione al click
            .setAutoCancel(true) // Chiude la notifica al click
            .setStyle(NotificationCompat.BigTextStyle().bigText(content)) // Mostra testo lungo
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Usa suoni/vibrazioni/luci di default del canale

        // Verifica nuovamente il permesso prima di notificare
        if (!checkNotificationPermissions()) {
            Log.w(TAG, "Permission denied in showNotification. Cannot notify.")
            return
        }

        // Mostra la notifica
        with(NotificationManagerCompat.from(this)) {
            val notificationId = System.currentTimeMillis().toInt() // ID univoco per la notifica
            Log.d(TAG, "Notifying with ID: $notificationId on Channel: $channelId")
            try {
                notify(notificationId, notificationBuilder.build())
                Log.d(TAG, "Notification successfully posted.")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException while trying to notify: ${e.message}", e)
            } catch (e: Exception) {
                Log.e(TAG, "Exception while trying to notify: ${e.message}", e)
            }
        }
    }

    // Gestisce la generazione di un nuovo token FCM
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "onNewToken: New FCM token generated: $token")
        // È importante inviare questo nuovo token al tuo backend per aggiornare
        // il documento utente corrispondente.
        sendRegistrationToServer(token)
    }

    // Funzione helper per inviare il token al server (Firestore)
    private fun sendRegistrationToServer(token: String?) {
        if (token == null) {
            Log.w(TAG, "sendRegistrationToServer: Token is null, cannot update.")
            return
        }
        val userId = Firebase.auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "sendRegistrationToServer: User not logged in, cannot update token.")
            // Potresti voler salvare il token in SharedPreferences per inviarlo dopo il login
            return
        }

        Log.d(TAG, "Attempting to update FCM token for user $userId in Firestore.")
        Firebase.firestore.collection("users").document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.i(TAG, "FCM token successfully updated in Firestore for user $userId.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating FCM token in Firestore for user $userId", e)
            }
    }

    // Controlla il permesso per le notifiche (necessario per Android 13+)
    private fun checkNotificationPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Il permesso non è richiesto per versioni precedenti
        }
    }
}