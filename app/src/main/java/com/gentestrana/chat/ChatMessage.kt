package com.gentestrana.chat

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties


@IgnoreExtraProperties
data class ChatMessage(
    val id: String = "",
    val sender: String = "",
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val status: String = "SENT",
    val isReply: Boolean = false,
    val replyToMessageText: String = "",
    val replyToMessageSender: String = ""
) {
    constructor() : this("", "", "", Timestamp.now(), "SENT", false, "", "")
    // Costruttore vuoto per Firestore. A che serviva?
}


