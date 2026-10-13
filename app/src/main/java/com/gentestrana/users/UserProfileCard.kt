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
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
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
