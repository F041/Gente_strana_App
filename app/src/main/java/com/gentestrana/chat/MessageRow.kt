package com.gentestrana.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gentestrana.R
import com.gentestrana.utils.formatTimestamp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

private val profilePicCache = mutableMapOf<String, String>()

/**
 * Prepara un TextView nativo Android con link cliccabili e testo selezionabile.
 * L'utente può long-premere per selezionare porzioni di testo e usare
 * il menu contestuale nativo (Copia, Condividi, Seleziona tutto, ecc.).
 * I link (http/https) sono cliccabili e aprono il browser.
 */
@Composable
private fun SelectableLinkifiedText(
    text: String,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val textColorArgb = textColor.toArgb()
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                this.setTextIsSelectable(true)
                this.movementMethod = LinkMovementMethod.getInstance()
                this.setLinkTextColor(android.graphics.Color.parseColor("#1976D2"))
                this.maxLines = Int.MAX_VALUE
            }
        },
        update = { textView ->
            val spannable = makeLinkSpannable(text, context)
            textView.text = spannable
            textView.setTextColor(textColorArgb)
        }
    )
}

/**
 * Crea uno SpannableString dove gli URL (http/https) diventano
 * cliccabili, blu e sottolineati.
 */
private fun makeLinkSpannable(text: String, context: Context): SpannableString {
    val spannable = SpannableString(text)
    val urlPattern = Regex("https?://[\\w./?=&%:+@#!\\-]+")
    for (match in urlPattern.findAll(text)) {
        val url = match.value
        val start = match.range.first
        val end = match.range.last + 1
        // Link cliccabile
        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Impossibile aprire il link", Toast.LENGTH_SHORT).show()
                }
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        // Colore blu
        spannable.setSpan(
            ForegroundColorSpan(android.graphics.Color.parseColor("#1976D2")),
            start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        // Sottolineato
        spannable.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return spannable
}

/**
 * Copia il testo negli appunti.
 */
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("messaggio", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Messaggio copiato", Toast.LENGTH_SHORT).show()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageRow(
    chatMessage: ChatMessage,
    currentUserId: String,
    currentUserName: String,
    recipientName: String,
    showAvatar: Boolean,
    onDelete: (() -> Unit)?,
    onReplySwipe: (() -> Unit)?
) {
    if (chatMessage.message.startsWith("MEMORY_CARD|") || chatMessage.message.startsWith("RECOMMENDATION_CARD|")) {
        RecommendationSystemMessageBubble(
            cardMessage = chatMessage.message,
            isCurrentUser = (chatMessage.sender == currentUserId),
            recipientName = recipientName
        )
        return
    }

    val isCurrentUser = (chatMessage.sender == currentUserId)
    val context = LocalContext.current
    val avatarDataState = remember { mutableStateOf<Any?>(null) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val swipeThreshold = 80.dp.value * LocalContext.current.resources.displayMetrics.density
    var showActionsDialog by remember { mutableStateOf(false) }

    var translatedText by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }

    fun doTranslate() {
        val targetLang = context.resources.configuration.locales.get(0).language
        isTranslating = true
        com.gentestrana.utils.TranslationHelper.translateTextWithDetection(
            text = chatMessage.message,
            targetLanguageCode = targetLang,
            onSuccess = { result ->
                translatedText = result
                isTranslating = false
            },
            onFailure = {
                isTranslating = false
                Toast.makeText(context, "Impossibile tradurre il messaggio", Toast.LENGTH_SHORT).show()
            },
            onDownloadStarted = {},
            onDownloadCompleted = {}
        )
    }

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
                val userObj = userDoc.toObject(com.gentestrana.users.User::class.java)
                val originalUrl = userObj?.profilePicUrl?.firstOrNull { it.isNotBlank() }
                    ?: (userDoc.get("profilePicUrl") as? String)?.takeIf { it.isNotBlank() }
                    ?: ""
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
                            showActionsDialog = true
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
                            senderName = senderNameToDisplay,
                            text = chatMessage.replyToMessageText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // AndroidView TextView nativo con colore testo adattivo dal tema
                    val messageTextColor = if (isCurrentUser)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant

                    SelectableLinkifiedText(
                        text = translatedText ?: chatMessage.message,
                        textColor = messageTextColor
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isTranslating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        } else if (translatedText != null) {
                            Text(
                                text = "🌐 Tradotto ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { translatedText = null }
                                    .padding(end = 2.dp)
                            )
                        }
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

    // Dialog opzioni messaggio (Copia, Traduci, Elimina)
    if (showActionsDialog) {
        AlertDialog(
            onDismissRequest = { showActionsDialog = false },
            title = { Text("Opzioni messaggio") },
            text = { Text("Scegli un'azione per questo messaggio:") },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        showActionsDialog = false
                        copyToClipboard(context, chatMessage.message)
                    }) {
                        Text("Copia")
                    }
                    TextButton(onClick = {
                        showActionsDialog = false
                        if (translatedText != null) {
                            translatedText = null
                        } else {
                            doTranslate()
                        }
                    }) {
                        Text(if (translatedText != null) "Originale" else "Traduci")
                    }
                }
            },
            dismissButton = {
                if (isCurrentUser && onDelete != null) {
                    TextButton(onClick = {
                        showActionsDialog = false
                        onDelete.invoke()
                    }) {
                        Text("Elimina")
                    }
                }
            }
        )
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
            .padding(start = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .heightIn(min = 36.dp)
                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = senderName,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RecommendationSystemMessageBubble(
    cardMessage: String,
    isCurrentUser: Boolean = false,
    recipientName: String = "Utente"
) {
    val parts = cardMessage.split("|")
    val authorName = if (parts.size > 1) parts[1] else "Utente"
    val reviewText = if (parts.size > 2) parts.drop(2).joinToString("|") else ""

    val cardTitle = if (isCurrentUser) {
        "Hai lasciato una memoria a $recipientName"
    } else {
        "$authorName ti ha lasciato una memoria"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            modifier = Modifier
                .widthIn(max = 320.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("💬", fontSize = 18.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cardTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = reviewText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}