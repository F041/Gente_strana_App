package com.gentestrana.users

import com.google.firebase.Timestamp

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
