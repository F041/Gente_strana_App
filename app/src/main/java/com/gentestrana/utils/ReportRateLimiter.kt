package com.gentestrana.utils

import java.util.concurrent.ConcurrentHashMap

/**
 * Un semplice rate limiter per le segnalazioni.
 * Permette al massimo 3 segnalazioni per utente in una finestra di 60 secondi.
 */

object ReportRateLimiter {
    private val userReportTimestamps = ConcurrentHashMap<String, MutableList<Long>>()
    private const val TIME_WINDOW = 60 * 1000L  // 60 secondi
    private const val MAX_REPORTS = 3           // Massimo segnalazioni consentite nella finestra

    fun canReport(userId: String): Boolean {
        val now = System.currentTimeMillis()
        val timestamps = userReportTimestamps.getOrPut(userId) { mutableListOf() }
        // Rimuovi i timestamp più vecchi della finestra
        timestamps.removeAll { now - it > TIME_WINDOW }
        return if (timestamps.size < MAX_REPORTS) {
            timestamps.add(now)
            true
        } else {
            false
        }
    }
}
