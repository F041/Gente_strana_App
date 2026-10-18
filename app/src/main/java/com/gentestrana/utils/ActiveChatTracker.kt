package com.gentestrana.utils

import android.app.NotificationManager
import android.content.Context
import android.util.Log

object ActiveChatTracker {
    @Volatile
    var activeChatId: String? = null

    fun clearNotificationForChat(context: Context, chatId: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(chatId.hashCode())
            Log.d("ActiveChatTracker", "Cancellata notifica per chatId: $chatId (id: ${chatId.hashCode()})")
        } catch (e: Exception) {
            Log.e("ActiveChatTracker", "Errore durante la cancellazione della notifica per $chatId", e)
        }
    }
}
