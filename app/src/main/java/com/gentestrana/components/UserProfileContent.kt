package com.gentestrana.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.gentestrana.R
import com.gentestrana.users.User
import com.gentestrana.utils.computeAgeFromTimestamp
import com.gentestrana.utils.getFlagEmoji
import com.gentestrana.utils.getLanguageName
import coil.request.ImageRequest

private const val DEFAULT_PROFILE_IMAGE_URL = "https://icons.veryicon.com/png/o/system/ali-mom-icon-library/random-user.png"

@Composable
fun ProfileContent(
    user: User,
    padding: PaddingValues,
    navController: NavHostController,
    onProfileImageClick: () -> Unit,
    // se lo tolgo ho problemi in UserProfileScreen
    onStartChat: () -> Unit,
    showChatButton: Boolean
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
    ) {
        // Nome ed età
        Text(
            text = "${user.username}, ${computeAgeFromTimestamp(user.birthTimestamp)}",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))
        // 8 perché senno vedo uno spazio aggiuntivo
        // per niente elegante ma pazienza

        // Immagine profilo
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(1f)
                // Forza aspect ratio 1:1 (quadrato)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .align(Alignment.CenterHorizontally)
                .clickable {
                    // <--- Mantieni il comportamento clickable
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        "imageUrls",
                        user.profilePicUrl
                    )
                    navController.navigate("profile_pictures_screen")
                },
            contentAlignment = Alignment.Center
        ) {
            val imageUrlToLoad: String = if (user.profilePicUrl.isEmpty()) {
                DEFAULT_PROFILE_IMAGE_URL // Usa default se la lista è vuota
            } else {
                // Se non è vuota, prendi la prima (non serve thumbnail qui, è già piccola)
                user.profilePicUrl.first()
            }

            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data(imageUrlToLoad)
                        .placeholder(R.drawable.random_user)
                        .error(R.drawable.random_user)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Contenitore esterno con larghezza fissa per TopicsBox e BioBox
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                ReadOnlyTopicsBox(
                    topics = user.topics
                )
                Spacer(modifier = Modifier.height(16.dp))
                ReadOnlyBioBox(
                    bioText = user.bio
                )
            }
        }

        // Sezione lingue parlate
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.languages_spoken).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )


            user.spokenLanguages.forEach { code ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "${getFlagEmoji(context, code)} ${getLanguageName(context, code)}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        // Pulsante "Chatta"
        // TODO: deactivate if user received 100 messages in day?
        if (showChatButton) {
            Button(
                onClick = onStartChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(stringResource(R.string.chat_with_user, user.username).uppercase())
            }
        }
    }
}


