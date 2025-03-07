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

@Composable
fun ProfileContent(
    user: User,
    padding: PaddingValues,
    onProfileImageClick: () -> Unit, // da togliere?
    navController: NavHostController,
    onStartChat: () -> Unit
) {
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
                // <--- Usa una frazione di fillMaxWidth, es. 0.5f
                .aspectRatio(1f)
                // <--- Forza aspect ratio 1:1 (quadrato)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .align(Alignment.CenterHorizontally)
                .clickable {
                    // <--- Mantieni il comportamento clickable
                    navController.currentBackStackEntry?.savedStateHandle?.set("imageUrls", user.profilePicUrl)
                    navController.navigate("profile_pictures_screen")
                },
            contentAlignment = Alignment.Center // <--- Mantieni contentAlignment
        ) {
            Image(
                painter = rememberAsyncImagePainter(user.profilePicUrl.firstOrNull()), // Prendi la prima immagine per ora
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
                    .fillMaxWidth() // <== Usa fillMaxWidth() senza frazione (occupa tutta la larghezza)
                    .padding(horizontal = 16.dp)
            ) {
                TopicsBox(
                    topics = user.topics
                )
                Spacer(modifier = Modifier.height(16.dp))
                BioBox(
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
            val context = LocalContext.current
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


