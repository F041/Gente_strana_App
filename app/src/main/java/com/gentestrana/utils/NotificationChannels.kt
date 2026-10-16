package com.gentestrana.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log

/**
 * Creazione centralizzata dei canali di notifica.
 *
 * Perche' esiste questo helper:
 * i canali di un'app su Android 8+ DEVONO esistere prima che arrivi una notifica.
 * Prima venivano creati SOLO in MyFirebaseMessagingService.onCreate(), cioe' alla
 * prima notifica ricevuta. Quando l'app e' in background e arriva una notifica di
 * tipo "notification" (con payload notification, non solo data), il sistema mostra
 * la notifica SENZA avviare il service: se il canale indicato nel manifest
 * (`default_messages_channel_v1`) non esiste ancora, Android usa il canale di
 * fallback di FCM. Su molti dispositivi (Motorola, Xiaomi, OPPO, Huawei...) quel
 * canale di fallback e' creato con importanza minima o risulta disattivato, percio'
 * la notifica NON appare. Chiamando ensureCreated() all'avvio dell'app ci
 * assicuriamo che i canali esistano sempre con l'importanza corretta.
 */
object NotificationChannels {

    const val ADMIN_REPORTS_CHANNEL_ID = "admin_reports_channel_v1"
    const val NEW_USERS_CHANNEL_ID = "new_users_channel_v1"
    const val DEFAULT_MESSAGES_CHANNEL_ID = "default_messages_channel_v1"

    private const val TAG = "NotificationChannels"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val adminChannel = NotificationChannel(
            ADMIN_REPORTS_CHANNEL_ID,
            "Segnalazioni Admin",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifiche critiche per le segnalazioni degli utenti (Admin)"
        }

        val newUserChannel = NotificationChannel(
            NEW_USERS_CHANNEL_ID,
            "Nuovi Utenti",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifiche quando nuovi utenti si uniscono"
        }

        // IMPORTANCE_HIGH: i messaggi di chat devono comparire come heads-up,
        // altrimenti su alcuni dispositivi restano silenziosi nel cassetto.
        val defaultChannel = NotificationChannel(
            DEFAULT_MESSAGES_CHANNEL_ID,
            "Messaggi e Altro",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifiche per messaggi di chat e altre informazioni"
        }

        manager.createNotificationChannel(adminChannel)
        manager.createNotificationChannel(newUserChannel)
        manager.createNotificationChannel(defaultChannel)
        Log.d(TAG, "Canali di notifica creati/aggiornati.")
    }
}