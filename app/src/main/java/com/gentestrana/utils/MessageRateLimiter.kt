package com.gentestrana.utils

import java.util.concurrent.ConcurrentHashMap

/**
 * Rate limiter per l'invio dei messaggi.
 * Consente al massimo 5 messaggi ogni 10 secondi per utente.
 */
object MessageRateLimiter {
    private val userMessageTimestamps = ConcurrentHashMap<String, MutableList<Long>>()
    private const val TIME_WINDOW = 50 * 1000L  // 5 secondi
    private const val MAX_MESSAGES = 4          // massimo 4 messaggi per finestra

    fun canSendMessage(userId: String): Boolean {
        val now = System.currentTimeMillis()
        val timestamps = userMessageTimestamps.getOrPut(userId) { mutableListOf() }
        // Rimuove i timestamp più vecchi della finestra
        timestamps.removeAll { now - it > TIME_WINDOW }
        return if (timestamps.size < MAX_MESSAGES) {
            timestamps.add(now)
            true
        } else {
            false
        }
    }
}
