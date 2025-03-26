package com.gentestrana.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import coil.compose.rememberAsyncImagePainter
import com.gentestrana.R
import com.gentestrana.users.User
import com.gentestrana.utils.computeAgeFromTimestamp
import com.gentestrana.utils.getFlagEmoji
import com.gentestrana.utils.getLanguageName

@Composable
fun PersonalProfilePreviewCard(
    user: User,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp) // Aggiunto padding esterno per separarlo dal bordo schermo
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

        // Immagine profilo
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f) // Larghezza ridotta per l'anteprima
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(user.profilePicUrl.firstOrNull()),
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Contenitore esterno per TopicsBox e BioBox (larghezza ridotta per anteprima)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.8f) // Larghezza ridotta per l'anteprima
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

        // Sezione lingue parlate (larghezza ridotta per anteprima)
        Column(modifier = Modifier
            .fillMaxWidth(0.8f) // Larghezza ridotta per l'anteprima
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp) // Spazio dalla BioBox
        ) {
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
    }
}