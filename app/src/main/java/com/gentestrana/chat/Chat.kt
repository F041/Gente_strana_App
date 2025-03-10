package com.gentestrana.chat
import com.google.firebase.Timestamp

data class Chat(
    val id: String,
    val participantId: String, // utile per status online, offline
    val participantName: String,
    val lastMessage: String,
    val lastMessageStatus: MessageStatus,
    val photoUrl: String,
    val timestamp: Timestamp,
    val isOnline: Boolean = false // Campo per lo stato online
)

enum class MessageStatus { SENT, DELIVERED, READ }