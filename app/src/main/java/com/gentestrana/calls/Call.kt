package com.gentestrana.calls

import com.google.firebase.Timestamp

enum class CallStatus {
    RINGING,
    ACTIVE,
    DECLINED,
    CANCELLED,
    MISSED,
    ENDED;

    companion object {
        fun fromString(value: String?): CallStatus {
            return when (value?.lowercase()) {
                "active" -> ACTIVE
                "declined" -> DECLINED
                "cancelled" -> CANCELLED
                "missed" -> MISSED
                "ended" -> ENDED
                else -> RINGING
            }
        }
    }
}

data class Call(
    val id: String = "",
    val chatId: String = "",
    val callerId: String = "",
    val calleeId: String = "",
    val callerName: String = "",
    val callerPhoto: String = "",
    val participants: List<String> = emptyList(),
    val status: CallStatus = CallStatus.RINGING,
    val createdAt: Timestamp? = null,
    val expireAt: Timestamp? = null,
    val answeredAt: Timestamp? = null,
    val activeUntil: Timestamp? = null,
    val endedAt: Timestamp? = null,
    val endedBy: String? = null
)
