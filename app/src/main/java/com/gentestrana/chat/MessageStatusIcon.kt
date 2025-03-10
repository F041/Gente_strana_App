package com.gentestrana.chat

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MessageStatusIcon(status: MessageStatus) {
    val (icon, color) = when (status) {
        MessageStatus.SENT -> Pair(Icons.Default.Check, MaterialTheme.colorScheme.onSurfaceVariant)
        MessageStatus.DELIVERED -> Pair(Icons.Default.DoneAll, MaterialTheme.colorScheme.onSurfaceVariant)
        MessageStatus.READ -> Pair(Icons.Default.DoneAll, MaterialTheme.colorScheme.primary)
    }

    Icon(
        imageVector = icon,
        contentDescription = "Message status",
        tint = color,
        modifier = Modifier.size(16.dp)
    )
}