package com.gentestrana.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gentestrana.utils.formatTimestamp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.gentestrana.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

private val profilePicCache = mutableMapOf<String, String>()

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageRow(
    chatMessage: ChatMessage,
    currentUserId: String,
    currentUserName: String, // Nome dell'utente che sta usando l'app
    recipientName: String, // Nome dell'altro utente
    showAvatar: Boolean,
    onDelete: (() -> Unit)?,
    onReplySwipe: (() -> Unit)?
) {
    val isCurrentUser = (chatMessage.sender == currentUserId)
    val rowAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val context = LocalContext.current
    val avatarDataState = remember { mutableStateOf<Any?>(null) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val swipeThreshold = 80.dp.value * LocalContext.current.resources.displayMetrics.density

    LaunchedEffect(chatMessage.sender) {
        val senderId = chatMessage.sender
        var urlToLoad: String?

        if (profilePicCache.containsKey(senderId)) {
            urlToLoad = profilePicCache[senderId]
        } else {
            try {
                val userDoc = Firebase.firestore.collection("users")
                    .document(senderId)
                    .get()
                    .await()
                val profilePicData = userDoc.get("profilePicUrl")
                var picList: List<String>? = null

                if (profilePicData is List<*>) {
                    @Suppress("UNCHECKED_CAST")
                    picList = profilePicData as? List<String>
                }

                val originalUrl = picList?.firstOrNull() ?: ""
                profilePicCache[senderId] = originalUrl
                urlToLoad = originalUrl

            } catch (e: Exception) {
                profilePicCache[senderId] = ""
                urlToLoad = ""
            }
        }

        if (urlToLoad.isNullOrEmpty()) {
            avatarDataState.value = R.drawable.random_user
        } else {
            avatarDataState.value = urlToLoad
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {


        Row(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .wrapContentWidth()
                .pointerInput(Unit) {
                    if (onReplySwipe != null) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX > swipeThreshold) {
                                    onReplySwipe()
                                }
                                scope.launch {
                                    offsetX = 0f
                                }
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            val newOffsetX = (offsetX + dragAmount).coerceIn(0f, swipeThreshold * 1.5f)
                            offsetX = newOffsetX
                        }
                    }
                },
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isCurrentUser && showAvatar) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(avatarDataState.value)
                        .placeholder(R.drawable.random_user)
                        .error(R.drawable.random_user)
                        .crossfade(true)
                        .build(),
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (!isCurrentUser && !showAvatar) {
                Spacer(modifier = Modifier.width(40.dp + 8.dp))
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .background(
                        color = if (isCurrentUser)
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            if (isCurrentUser && onDelete != null) {
                                onDelete()
                            }
                        }
                    )
            ) {
                Column {
                    if (chatMessage.isReply) {
                        val senderNameToDisplay = if (chatMessage.replyToMessageSender == currentUserId) {
                            currentUserName
                        } else {
                            recipientName
                        }
                        QuotedMessage(
                            senderName = senderNameToDisplay, // Usa il nome che abbiamo appena determinato
                            text = chatMessage.replyToMessageText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = chatMessage.message,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimestamp(chatMessage.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(4.dp))
                            val statusEnum = try {
                                MessageStatus.valueOf(chatMessage.status.uppercase())
                            } catch (e: IllegalArgumentException) {
                                MessageStatus.SENT
                            }
                            MessageStatusIcon(status = statusEnum)
                        }
                    }
                }
            }
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

@Composable
private fun QuotedMessage(
    senderName: String,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(start = 4.dp) // Piccolo padding per la linea verticale
    ) {
        // Linea verticale a sinistra
        Box(
            modifier = Modifier
                .width(4.dp)
                .heightIn(min = 36.dp) // Altezza minima per allinearsi bene
                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        // Colonna con nome del mittente e testo del messaggio
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = senderName, // Per ora mostra l'ID, in futuro si potrà migliorare
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis // Aggiunge "..." se il testo è troppo lungo
            )
        }
    }
}