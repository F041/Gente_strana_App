// File: app/src/main/java/com/gentestrana/users/UserProfileCard.kt

package com.gentestrana.users

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gentestrana.R
import com.gentestrana.screens.AppTheme
import com.gentestrana.ui.theme.GenteStranaTheme
import com.gentestrana.ui.theme.LocalDimensions
import com.gentestrana.utils.computeAgeFromTimestamp
import com.gentestrana.utils.getFlagEmoji

private const val DEFAULT_PROFILE_IMAGE_URL = "https://icons.veryicon.com/png/o/system/ali-mom-icon-library/random-user.png"

@Composable
fun UserProfileCard(
    user: User,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayedAge = computeAgeFromTimestamp(user.birthTimestamp)
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val imageSize = screenWidth * 0.275f
    val context = LocalContext.current
    val dimensions = LocalDimensions.current

    val isNewUser = remember(user.registrationDate) {
        val regTime = user.registrationDate?.toDate()?.time ?: 0L
        val now = System.currentTimeMillis()
        regTime > 0L && (now - regTime) < 24 * 60 * 60 * 1000L
    }

    val isHotUser = user.isHot

    val isNightOwl = remember(user.lastActive) {
        if (user.lastActive == null) false
        else {
            // Usa il fuso orario locale dell'utente
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getDefault())
            cal.time = user.lastActive.toDate()
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            hour in 0..5
        }
    }

    val isInfodumper = remember(user.bio) {
        user.bio.length >= 400
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = imageSize),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensions.mediumPadding)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Top
        ) {

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(user.profilePicUrl.firstOrNull()?.takeIf { it.isNotBlank() } ?: DEFAULT_PROFILE_IMAGE_URL)
                    .placeholder(R.drawable.random_user)
                    .error(R.drawable.random_user)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile",
                modifier = Modifier
                    .size(imageSize)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(dimensions.mediumPadding))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val nameAndAgeText = if (displayedAge > 0) {
                            "${user.username.uppercase()}, $displayedAge"
                        } else {
                            user.username.uppercase()
                        }

                        Text(
                            text = nameAndAgeText,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Seleziona AL MASSIMO 1 badge di tratto caratteristico in base alla priorità
                            val traitBadge = when {
                                isNewUser -> Pair(stringResource(R.string.badge_new), androidx.compose.ui.graphics.Color(0xFF4CAF50))
                                isHotUser -> Pair(stringResource(R.string.badge_hot), androidx.compose.ui.graphics.Color(0xFFFF5722))
                                isNightOwl -> Pair(stringResource(R.string.badge_night_owl), androidx.compose.ui.graphics.Color(0xFF673AB7))
                                isInfodumper -> Pair(stringResource(R.string.badge_infodumper), androidx.compose.ui.graphics.Color(0xFFE91E63))
                                else -> null
                            }

                            if (traitBadge != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = traitBadge.second,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text(
                                        text = traitBadge.first,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = androidx.compose.ui.graphics.Color.White,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Badge Memorie: Sempre visibile se l'utente ha memorie
                            if (user.memories.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = androidx.compose.ui.graphics.Color(0xFF00BCD4),
                                    modifier = Modifier.padding(start = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${user.memories.size} ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = androidx.compose.ui.graphics.Color.White,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                        Text(
                                            text = "💬",
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(dimensions.smallPadding))
                    Text(
                        text = user.topics.firstOrNull() ?: stringResource(R.string.no_topics_defined),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                        maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                if (user.spokenLanguages.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = dimensions.smallPadding)
                    ) {
                        Text(
                            text = stringResource(R.string.speaks) + ": ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        user.spokenLanguages.forEach { code ->
                            Text(
                                text = "${getFlagEmoji(LocalContext.current, code)} ",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun UserProfileCardPreview() {
    GenteStranaTheme(appTheme = AppTheme.LIGHT) {
        UserProfileCard(
            user = User(
                username = "Maria",
                rawBirthTimestamp = 725846400000L,
                topics = listOf("salute mentale"),
                spokenLanguages = listOf("it", "en")
            ),
            onClick = {}
        )
    }
}
