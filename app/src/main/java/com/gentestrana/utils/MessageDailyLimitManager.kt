package com.gentestrana.utils

import android.content.Context

object MessageDailyLimitManager {
    private const val PREFS_NAME = "message_limit_prefs"
    private const val KEY_COUNT = "daily_message_count"
    private const val KEY_TIMESTAMP = "daily_message_timestamp"
    private const val LIMIT = 300 // prima stava a 100
    private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

    /**
     * Verifica se l'utente (identificato da userId) può ancora inviare messaggi oggi.
     * Se il periodo è trascorso, resetta il conteggio.
     */
    fun canSendMessage(context: Context, userId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastReset = prefs.getLong("$userId-$KEY_TIMESTAMP", 0L)
        val currentTime = System.currentTimeMillis()
        var count = prefs.getInt("$userId-$KEY_COUNT", 0)

        // Se l'ultimo reset è più vecchio di 24 ore, resetta il conteggio
        if (currentTime - lastReset > DAY_MILLIS) {
            count = 0
            prefs.edit().putInt("$userId-$KEY_COUNT", count)
                .putLong("$userId-$KEY_TIMESTAMP", currentTime)
                .apply()
        }
        return count < LIMIT
    }

    /**
     * Incrementa il conteggio dei messaggi inviati per l'utente.
     */
    fun incrementCount(context: Context, userId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCount = prefs.getInt("$userId-$KEY_COUNT", 0)
        prefs.edit().putInt("$userId-$KEY_COUNT", currentCount + 1).apply()
    }
}
