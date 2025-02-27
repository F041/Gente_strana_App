package com.gentestrana.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import com.gentestrana.chat.Chat

@Composable
fun ChatList(
    chats: List<Chat>,
    onChatClick: (Chat) -> Unit
) {
    LazyColumn {
        items(
            items = chats,
            key = { chat -> chat.id } // Usa chat.id come chiave univoca
        ) { chat ->
            ChatListItem(chat = chat, onClick = { onChatClick(chat) })
        }
    }
}
