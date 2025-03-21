package com.gentestrana.users

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import com.gentestrana.ui.theme.*
import com.gentestrana.utils.computeAgeFromTimestamp
import androidx.compose.ui.res.stringResource
import com.gentestrana.R
import com.gentestrana.utils.getFlagEmoji
import androidx.compose.ui.platform.LocalContext

@Composable
fun UserProfileCard(
    user: User,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayedAge = computeAgeFromTimestamp(user.birthTimestamp)
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
// Ad esempio, vogliamo che l'immagine occupi circa il 30% della larghezza dello schermo
    val imageSize = screenWidth * 0.275f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 130.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min) // Altezza minima per adattarsi al contenuto
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Image
            val profilePicUrlList = user.profilePicUrl
            // Get the list directly
            val profileImageUrl: String? = profilePicUrlList.firstOrNull()
            // Safely get the first URL or null

            Image(
                painter = rememberAsyncImagePainter(
                    model = profileImageUrl ?: "res/drawable/random_user.webp"
                // Use default URL if profileImageUrl is null
                ),
                contentDescription = "Profile",
                modifier = Modifier
                    .size(imageSize)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))

            // User Details Column
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(imageSize) // Altezza fissa pari a quella dell'immagine
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Parte superiore: Titolo (username e età)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = "${user.username.uppercase()}, $displayedAge",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                    }
                    // Parte centrale: Topic
                    // Avvolgiamo il topic in un Box che, se necessario, permette lo scrolling
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Se il testo del topic è lungo, questo diventerà scrollabile
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                text = if (user.topics.isNotEmpty()) {
                                    user.topics[0]
                                } else {
                                    stringResource(R.string.no_topics_defined)
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.SansSerif,
                                lineHeight = 20.sp
                                // con 10 viene appiccicato
                            )
                        }
                    }
                    // Parte inferiore: Sezione lingue, sempre ancorata in fondo
                    if (user.spokenLanguages.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = 10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.speaks) + ": ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            user.spokenLanguages.forEach { code ->
                                Text(
                                    text = "${getFlagEmoji(LocalContext.current, code)} ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
        }
    }
    }
}

