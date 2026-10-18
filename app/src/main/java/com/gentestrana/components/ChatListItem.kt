package com.gentestrana.components

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.res.stringResource
import com.gentestrana.R


private const val DEFAULT_PROFILE_IMAGE_URL = "https://icons.veryicon.com/png/o/system/ali-mom-icon-library/random-user.png"

/**
 * Riga di una conversazione nella lista chat.
 *
 * Tocco breve: apre la chat. Pressione lunga: mostra un popup con "delete" e "pin".
 *
 * @param onPin chiamato quando l'utente sceglie "pin" (fissa/sblocca la chat).
 * @param onDelete chiamato quando l'utente sceglie "delete" (elimina la chat per lui).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListItem(
    chat: Chat,
    onClick: () -> Unit,
    onPin: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    // Utilizziamo remember per memorizzare i valori formattati finché chat.timestamp non cambia
    val dateSeparator = remember(chat.timestamp) { getDateSeparator(chat.timestamp) }
    val formattedTime = remember(chat.timestamp) { formatTimestamp(chat.timestamp) }
    // Recupera le dimensioni dinamiche dal tema
    val dimensions = LocalDimensions.current
    val context = LocalContext.current

    // Stato del popup (menu contestuale) mostrato con la pressione lunga
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuExpanded = true }
                )
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
                    // Nome partecipante e eventuale icona pin
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = chat.participantName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (chat.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = stringResource(R.string.chat_pin),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(dimensions.smallPadding))

                    // Anteprima card ricordo: mostra "💬 testo" invece del prefisso grezzo "MEMORY_CARD|…"
                    val lastMessageDisplay = when {
                        chat.lastMessage.startsWith("MEMORY_CARD|") || chat.lastMessage.startsWith("RECOMMENDATION_CARD|") -> {
                            val parts = chat.lastMessage.split("|")
                            if (parts.size > 2) "💬 ${parts.drop(2).joinToString("|")}" else chat.lastMessage
                        }
                        else -> chat.lastMessage
                    }

                    // Ultimo messaggio e stato
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = lastMessageDisplay,
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

        // Popup mostrato con la pressione lunga sulla conversazione
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(
                            if (chat.isPinned) R.string.chat_unpin else R.string.chat_pin
                        )
                    )
                },
                onClick = {
                    menuExpanded = false
                    onPin()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.chat_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    menuExpanded = false
                    onDelete()
                }
            )
        }
    }
}