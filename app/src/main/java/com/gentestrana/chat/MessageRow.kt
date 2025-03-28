package com.gentestrana.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gentestrana.utils.formatTimestamp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

// Cache globale per gli URL delle immagini profilo
private val profilePicCache = mutableMapOf<String, String>()

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageRow(
    chatMessage: ChatMessage,
    currentUserId: String,
    showAvatar: Boolean,
    onDelete: (() -> Unit)?

) {
    val isCurrentUser = (chatMessage.sender == currentUserId)
    val rowAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart

    // Stato per l'URL dell'avatar con caching
    val coroutineScope = rememberCoroutineScope()
    val profilePicUrlState = remember { mutableStateOf("") }

    LaunchedEffect(chatMessage.sender) {
        // Resto del codice invariato
        if (profilePicCache.containsKey(chatMessage.sender)) {
            profilePicUrlState.value = profilePicCache[chatMessage.sender]!!
        } else {
            try {
                val userDoc = Firebase.firestore.collection("users")
                    .document(chatMessage.sender)
                    .get()
                    .await()
                val picList = userDoc.get("profilePicUrl") as? List<String>
                val url = picList?.firstOrNull() ?: "android.resource://com.gentestrana/drawable/random_user"
                profilePicCache[chatMessage.sender] = url
                profilePicUrlState.value = url
            } catch (e: Exception) {
                profilePicUrlState.value = "android.resource://com.gentestrana/drawable/random_user"
            }
        }
        // }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .align(rowAlignment)
                .wrapContentWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            // Mostra l'avatar solo se NON è l'utente corrente e showAvatar è true
            if (!isCurrentUser && showAvatar) {
                AsyncImage(
                    model = profilePicUrlState.value,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Bolla del messaggio
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .background(
                        color = if (isCurrentUser)
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f) // Più opaco per il current user
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), // surfaceVariant per gli altri
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            // Doppio controllo per sicurezza
                            if (isCurrentUser && onDelete != null) {
                                onDelete()
                            }
                        }
                    )
            ) {
                Column {
                    Text(
                        text = chatMessage.message,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatTimestamp(chatMessage.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // NOTA: non mostriamo alcun avatar a destra per l'utente corrente.
        }
    }
}




@Composable
fun DateSeparatorRow(dateText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


