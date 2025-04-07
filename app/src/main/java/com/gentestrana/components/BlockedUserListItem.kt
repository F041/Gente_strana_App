package com.gentestrana.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen // Icona Sblocca
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gentestrana.R
import com.gentestrana.users.User

private const val DEFAULT_PROFILE_IMAGE_URL = "https://icons.veryicon.com/png/o/system/ali-mom-icon-library/random-user.png"

@Composable
fun BlockedUserListItem(
    user: User,
    onUnblockClick: (String) -> Unit // Callback che passa l'ID dell'utente da sbloccare
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp), // Aggiunto padding orizzontale
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // Spazio tra l'utente e il pulsante
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f) // Occupa lo spazio rimanente
        ) {
            // Immagine profilo
            val imageUrlToLoad: Any = user.profilePicUrl.firstOrNull()?.takeIf { it.isNotBlank() }
                ?: R.drawable.random_user // Usa placeholder se URL non valido o vuoto

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrlToLoad)
                    .placeholder(R.drawable.random_user)
                    .error(R.drawable.random_user)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.profile_picture),
                modifier = Modifier
                    .size(40.dp) // Dimensione ridotta per la lista
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            // Nome utente
            Text(
                text = user.username,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1 // Evita che nomi lunghi vadano a capo
            )
        }

        // Pulsante Sblocca
        Button(
            onClick = { onUnblockClick(user.docId) }, // Chiama la callback con l'ID utente
            contentPadding = PaddingValues(horizontal = 12.dp) // Padding ridotto
        ) {
            Icon(
                Icons.Filled.LockOpen,
                contentDescription =  stringResource(R.string.unblock_user_icon_description),
                modifier = Modifier.size(ButtonDefaults.IconSize) // Dimensione standard icona bottone
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing)) // Spazio standard tra icona e testo
            Text(stringResource(R.string.unblock_button))
        }
    }
}