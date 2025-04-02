package com.gentestrana.components

import android.util.Log
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
import com.gentestrana.ui.theme.LocalDimensions
import com.gentestrana.utils.formatTimestamp
import com.gentestrana.utils.getDateSeparator
import com.gentestrana.utils.getThumbnailUrl
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.gentestrana.R


private const val DEFAULT_PROFILE_IMAGE_URL = "https://icons.veryicon.com/png/o/system/ali-mom-icon-library/random-user.png"

@Composable
fun ChatListItem(chat: Chat, onClick: () -> Unit) {
    // Utilizziamo remember per memorizzare i valori formattati finché chat.timestamp non cambia
    val dateSeparator = remember(chat.timestamp) { getDateSeparator(chat.timestamp) }
    val formattedTime = remember(chat.timestamp) { formatTimestamp(chat.timestamp) }
    // Recupera le dimensioni dinamiche dal tema
    val dimensions = LocalDimensions.current
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // Sostituiamo il padding hardcoded con il valore dinamico
            .padding(horizontal = dimensions.smallPadding, vertical = dimensions.smallPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            val originalPhotoUrl = chat.photoUrl // Questo ora può essere ""
            val imageUrlToLoad: Any
            if (originalPhotoUrl.isEmpty()) {
                // Se l'URL è vuoto, usa la risorsa locale
                imageUrlToLoad = R.drawable.random_user
                Log.d("ChatListItem", "Utente ${chat.participantName}: URL vuoto, uso placeholder locale.") // Log opzionale
            } else {
                // Altrimenti, usa l'URL originale direttamente
                imageUrlToLoad = originalPhotoUrl
                Log.d("ChatListItem", "Utente ${chat.participantName}: URL originale: $originalPhotoUrl. Tento caricamento.") // Log opzionale
            }
            // Immagine profilo (dimensione mantenuta fissa, ma potrebbe essere resa dinamica se aggiungi una proprietà apposita)
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data(imageUrlToLoad) // Usa l'URL determinato
                        .placeholder(R.drawable.random_user) // Placeholder locale
                        .error(R.drawable.random_user) // Fallback locale
                        .crossfade(true)
                        .build()
                ),
                contentDescription = "Profile picture of ${chat.participantName}",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            // Indicatore online/offline
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

        Spacer(modifier = Modifier.width(dimensions.smallPadding))

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

                Spacer(modifier = Modifier.height(dimensions.smallPadding))

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

                    Spacer(modifier = Modifier.width(dimensions.smallPadding))

                    // Indicatore di stato
                    MessageStatusIcon(status = chat.lastMessageStatus)
                }

                Spacer(modifier = Modifier.height(dimensions.smallPadding))

                // Data e ora
                Text(
                    text = "$dateSeparator $formattedTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(dimensions.mediumPadding))
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
