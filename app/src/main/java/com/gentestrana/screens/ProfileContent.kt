package com.gentestrana.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.gentestrana.components.TopicsBox
import com.gentestrana.components.BioBox

@Composable
fun ProfileContent(
    user: User,
    padding: PaddingValues,
    onProfileImageClick: () -> Unit,
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
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )

        // Immagine profilo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable(onClick = onProfileImageClick),
            contentAlignment = Alignment.Center
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(user.profilePicUrl) { imageUrl ->
                    Image(
                        painter = rememberAsyncImagePainter(imageUrl),
                        contentDescription = "Gallery image",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                }
            }
        }

        // Contenitore esterno con larghezza fissa per TopicsBox e BioBox
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 320.dp, max = 400.dp) // Larghezza più adattabile
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
