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
import com.gentestrana.utils.getThumbnailUrl
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.gentestrana.R

private const val DEFAULT_PROFILE_IMAGE_URL = "https://icons.veryicon.com/png/o/system/ali-mom-icon-library/random-user.png"

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
    val context = LocalContext.current

    // Stato per l'URL dell'avatar con caching
    val profilePicUrlState = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(chatMessage.sender) {
        val senderId = chatMessage.sender
        val cachedOriginalUrl = profilePicCache[senderId]
        // Cerca l'URL originale nella cache


        if (cachedOriginalUrl != null) {
            // Se l'URL originale è in cache:
            if (cachedOriginalUrl == DEFAULT_PROFILE_IMAGE_URL) {
                // Se l'URL cachato è proprio il nostro default web, usa quello
                profilePicUrlState.value = DEFAULT_PROFILE_IMAGE_URL
            } else {
                // Altrimenti, genera la thumbnail dall'URL originale cachato
                profilePicUrlState.value = getThumbnailUrl(cachedOriginalUrl, "200x200") ?: cachedOriginalUrl
            }
        } else {
            // Se non è in cache, recupera l'URL originale da Firestore
            try {
                val userDoc = Firebase.firestore.collection("users")
                    .document(senderId)
                    .get()
                    .await()
                val profilePicData = userDoc.get("profilePicUrl") // Ottieni come Any?
                var picList: List<String>? = null // Inizializza a null

                if (profilePicData is List<*>) {
                    // Controlla se è una Lista (di qualsiasi cosa)
                    // Tentativo di cast sicuro a List<String>
                    // Questo è ancora tecnicamente "unchecked" per gli elementi interni,
                    // ma è il meglio che possiamo fare facilmente con Firestore.
                    // Il rischio è basso se siamo noi a scrivere sempre List<String>.
                    @Suppress("UNCHECKED_CAST") // Sopprimi il warning specifico qui
                    picList = profilePicData as? List<String>
                }

                if (picList.isNullOrEmpty()) {
                    // Se la lista è vuota o nulla, usa l'URL web di default
                    val defaultUrl = DEFAULT_PROFILE_IMAGE_URL
                    profilePicCache[senderId] = defaultUrl // Salva il default nella cache
                    profilePicUrlState.value = defaultUrl
                } else {
                    // Se la lista NON è vuota, prendi il primo URL (originale)
                    val originalUrl = picList.first()
                    profilePicCache[senderId] = originalUrl // Salva l'originale nella cache
                    // Genera l'URL della miniatura dall'originale recuperato
                    profilePicUrlState.value = getThumbnailUrl(originalUrl, "200x200") ?: originalUrl
                }

            } catch (e: Exception) {
                // In caso di errore nel recupero, usa l'URL di default (non serve thumbnail qui)
                val defaultUrl = "android.resource://com.gentestrana/drawable/random_user"
                profilePicCache[senderId] = defaultUrl // Salva anche il default nella cache
                profilePicUrlState.value = defaultUrl
            }
        }
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
                    model = ImageRequest.Builder(context)
                        .data(profilePicUrlState.value) // Usa l'URL dallo stato (thumbnail o default)
                        .placeholder(R.drawable.random_user) // Placeholder locale
                        .error(R.drawable.random_user)       // Fallback locale
                        .crossfade(true)
                        .build(),
                    contentDescription = "User Avatar",
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