package com.gentestrana

import com.google.firebase.Timestamp

data class ChatMessage(
    val sender: String = "",
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

