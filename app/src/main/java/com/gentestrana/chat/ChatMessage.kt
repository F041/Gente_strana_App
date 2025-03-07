package com.gentestrana.chat

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties


@IgnoreExtraProperties
data class ChatMessage(
    val id: String = "",
    val sender: String = "",
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val status: String = "SENT"
) {
    constructor() : this("", "", "", Timestamp.now(), "SENT") // Costruttore vuoto per Firestore
}
// TODO: agganciare notifiche MyFirebaseMessaginService qui?

