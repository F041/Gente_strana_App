package com.gentestrana.users

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.gentestrana.ui.theme.*
import com.gentestrana.utils.computeAgeFromTimestamp
import androidx.compose.ui.res.stringResource
import com.gentestrana.R

@Composable
fun UserProfileCard(
    user: User,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayedAge = computeAgeFromTimestamp(user.birthTimestamp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 120.dp), // Enforce minimum card height
        colors = CardDefaults.cardColors(containerColor = NeuroSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize() // Fill entire card space
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Profile Image
            Image(
                painter = rememberAsyncImagePainter(
                    model = user.profilePicUrl.firstOrNull()?.takeIf { it.isNotEmpty() }
                        ?: "https://icons.veryicon.com/png/o/system/ali-mom-icon-library/random-user.png"
                ),
                contentDescription = "Profile",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))

            // User Details Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), // Take full height
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Username and Description
                Column {
                    Text(
                        text = "${user.username.uppercase()}, $displayedAge",
                        color = NeuroPrimary,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (user.description[0].length > 80) {
                            user.description[0].take(80) + "..."
                        } else {
                            user.description[0]
                        },
                        color = NeuroSecondary,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 20.sp
                    )
                }

                // Spacer to push languages to the bottom
                Spacer(modifier = Modifier.weight(1f))

                // Languages Section (Stuck to Bottom)
                if (user.spokenLanguages.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.speaks) + ": ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeuroPrimary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        user.spokenLanguages.forEach { code ->
                            Text(
                                text = "${getFlagEmoji(code)} ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NeuroPrimary,
                                modifier = Modifier.padding(vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


// Helper function to get flag emoji
private fun getFlagEmoji(code: String): String = when (code.lowercase()) {
    "it" -> "🇮🇹"
    "en" -> "🇬🇧"
    "es" -> "🇪🇸"
    "fr" -> "🇫🇷"
    "de" -> "🇩🇪"
    else -> "🌍"
}

// Helper function to get language name
private fun getLanguageName(code: String): String = when (code.lowercase()) {
    "it" -> "Italiano"
    "en" -> "English"
    "es" -> "Español"
    "fr" -> "Français"
    "de" -> "Deutsch"
    else -> code
}