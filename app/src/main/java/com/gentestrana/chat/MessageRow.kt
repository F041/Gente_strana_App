package com.gentestrana.chat

import android.util.Log
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
import com.gentestrana.utils.getThumbnailUrl
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.gentestrana.R

// La cache può rimanere, ma conterrà solo URL originali o ""
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
    val context = LocalContext.current

    // Stato per l'URL dell'avatar - Cambiato tipo in Any? per supportare Int (Risorsa)
    val avatarDataState = remember { mutableStateOf<Any?>(null) }

    LaunchedEffect(chatMessage.sender) {
        val senderId = chatMessage.sender
        var urlToLoad: String?

        // 1. Controlla la cache
        if (profilePicCache.containsKey(senderId)) {
            urlToLoad = profilePicCache[senderId] // Può essere "" o un URL valido
//            Log.d("MessageRow_Debug", "Cache hit per $senderId: urlToLoad = '$urlToLoad'")
        } else {
//            Log.d("MessageRow_Debug", "Cache miss per $senderId, recupero da Firestore...")
            // 2. Se non in cache, recupera da Firestore
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

                // Se la lista è nulla o vuota, l'URL originale è ""
                val originalUrl = picList?.firstOrNull() ?: ""
                Log.d("MessageRow_Debug", "Firestore recuperato per $senderId: originalUrl = '$originalUrl'")

                // Salva nella cache (anche la stringa vuota)
                profilePicCache[senderId] = originalUrl
                urlToLoad = originalUrl

            } catch (e: Exception) {
                Log.e("MessageRow_Debug", "Errore recupero Firestore per $senderId: ${e.message}")
                // In caso di errore, salva "" nella cache e usa ""
                profilePicCache[senderId] = ""
                urlToLoad = ""
            }
        }

        // 3. Imposta lo stato per AsyncImage
        if (urlToLoad.isNullOrEmpty()) {
            // Se l'URL finale è nullo o vuoto, usa la risorsa drawable
            avatarDataState.value = R.drawable.random_user
            Log.d("MessageRow_Debug", "Impostato stato per $senderId a R.drawable.random_user")
        } else {
            // Altrimenti, usa l'URL originale
            avatarDataState.value = urlToLoad
            Log.d("MessageRow_Debug", "Impostato stato per $senderId a URL: $urlToLoad")
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .align(rowAlignment)
                .wrapContentWidth(),
            horizontalArrangement = Arrangement.Start, // Questo allinea l'avatar a sinistra del testo
            verticalAlignment = Alignment.Bottom // Allinea l'avatar al fondo della bolla
        ) {
            // Mostra l'avatar solo se NON è l'utente corrente e showAvatar è true
            if (!isCurrentUser && showAvatar) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(avatarDataState.value) // Usa lo stato (URL String o Risorsa Int)
                        .placeholder(R.drawable.random_user) // Placeholder
                        .error(R.drawable.random_user)       // Fallback ESSENZIALE
                        .crossfade(true)
                        .listener(onError = { _, result -> // Log opzionale per Coil
                            Log.e("MessageRow_CoilError", "Errore caricando ${avatarDataState.value} per ${chatMessage.sender}: ${result.throwable}")
                        })
                        .build(),
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp)) // Spazio tra avatar e bolla
            } else if (!isCurrentUser && !showAvatar) {
                // Aggiungi uno Spacer della stessa larghezza dell'avatar + spacer
                // per mantenere l'allineamento quando l'avatar non è mostrato
                Spacer(modifier = Modifier.width(40.dp + 8.dp))
            }

            // Bolla del messaggio
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
                    Text(
                        text = chatMessage.message,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.align(Alignment.End), // Allinea ora e stato a destra
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimestamp(chatMessage.timestamp), // Usa l'import corretto
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Aggiungi spazio e icona stato solo se è il messaggio dell'utente corrente
                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(4.dp))
                            // Assicurati che MessageStatusIcon sia importato e enum MessageStatus
                            val statusEnum = try {
                                MessageStatus.valueOf(chatMessage.status.uppercase())
                            } catch (e: IllegalArgumentException) {
                                MessageStatus.SENT // Fallback
                            }
                            MessageStatusIcon(status = statusEnum)
                        }
                    }
                }
            }
            // Nessun avatar/spacer a destra per l'utente corrente
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