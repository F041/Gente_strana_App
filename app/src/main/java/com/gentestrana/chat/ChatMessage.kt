package com.gentestrana.chat

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val sender: String = "",
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val profilePicUrl: String = ""
)

