package com.gentestrana.utils

import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import java.util.concurrent.ConcurrentHashMap

/**
 * Rate limiter per l'invio dei messaggi.
 * Legge i limiti da Remote Config.
 */
object MessageRateLimiter {
    private val userMessageTimestamps = ConcurrentHashMap<String, MutableList<Long>>()

    // Chiavi per Remote Config
    private const val REMOTE_CONFIG_WINDOW_KEY = "message_rate_limit_window_seconds"
    private const val REMOTE_CONFIG_MAX_MSG_KEY = "message_rate_limit_max_messages"
    // Valori di default DA USARE nel codice
    private const val DEFAULT_WINDOW_SECONDS = 50L
    private const val DEFAULT_MAX_MESSAGES = 4L

    fun canSendMessage(userId: String): Boolean {
        val now = System.currentTimeMillis()

        // Leggi i limiti da Remote Config ---
        val remoteConfig = Firebase.remoteConfig
        val windowSeconds = remoteConfig.getLong(REMOTE_CONFIG_WINDOW_KEY)
        val maxMessages = remoteConfig.getLong(REMOTE_CONFIG_MAX_MSG_KEY)

        // Usa i default se Remote Config restituisce valori non validi (es. 0)
        val actualWindowMillis = (if (windowSeconds > 0) windowSeconds else DEFAULT_WINDOW_SECONDS) * 1000L
        val actualMaxMessages = if (maxMessages > 0) maxMessages else DEFAULT_MAX_MESSAGES

        val timestamps = userMessageTimestamps.getOrPut(userId) { mutableListOf() }

        // Rimuove i timestamp più vecchi della finestra (usa actualWindowMillis)
        timestamps.removeAll { now - it > actualWindowMillis }

        val canSend = timestamps.size < actualMaxMessages
        Log.d("RateLimiter", "User $userId: Count=${timestamps.size}, Limit=$actualMaxMessages, Window=${actualWindowMillis}ms -> CanSend=$canSend") // Log debug

        if (canSend) {
            timestamps.add(now)
        }
        return canSend
    }
}