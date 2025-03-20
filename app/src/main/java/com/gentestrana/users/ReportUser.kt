package com.gentestrana.users

import com.gentestrana.utils.ReportRateLimiter
import com.google.firebase.Timestamp

// TODO: questa roba forse fa a cazzotti con qualcosa su Cloud functions
//fun reportUser(reporterId: String, reportedUserId: String, reason: String) {
//    // Controlla il rate limiter per evitare spam
//    if (!ReportRateLimiter.canReport(reporterId)) {
//        throw IllegalStateException("Troppi report in poco tempo. Riprova più tardi.")
//    }
//}

enum class ReportReason {
    CONTENUTI_INAPPROPRIATI,
    COMPORTAMENTO_OFFENSIVO,
    ALTRO
}

data class ReportUser(
    val reportedUserId: String = "",
    val reporterUserId: String = "",
    val reason: ReportReason = ReportReason.ALTRO,
    val timestamp: Timestamp = Timestamp.now(),
    val additionalComments: String = "" // Opzionale, solo se si sceglie "ALTRO"
)
