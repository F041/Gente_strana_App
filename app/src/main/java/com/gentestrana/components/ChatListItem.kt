package com.gentestrana.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.gentestrana.chat.Chat
import com.gentestrana.chat.MessageStatusIcon
import com.gentestrana.utils.formatTimestamp
import com.gentestrana.utils.getDateSeparator

@Composable
fun ChatListItem(chat: Chat, onClick: () -> Unit) {
    // Utilizziamo remember per memorizzare i valori formattati finché chat.timestamp non cambia
    val dateSeparator = remember(chat.timestamp) { getDateSeparator(chat.timestamp) }
    val formattedTime = remember(chat.timestamp) { formatTimestamp(chat.timestamp) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            // Immagine profilo
            Image(
                painter = rememberAsyncImagePainter(chat.photoUrl),
                contentDescription = "Profile picture of ${chat.participantName}",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            // Indicatore online dinamico
            if (chat.isOnline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .border(1.dp, Color.White, CircleShape)
                )
            } else {
                // Indicatore offline (cerchio grigio)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                        .border(1.dp, Color.White, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Nome e dettagli della chat
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Nome partecipante
                Text(
                    text = chat.participantName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Ultimo messaggio e stato
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chat.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Indicatore di stato
                    MessageStatusIcon(status = chat.lastMessageStatus)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Data e ora
                Text(
                    text = "$dateSeparator $formattedTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
