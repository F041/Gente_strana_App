package com.gentestrana.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.gentestrana.R
import com.gentestrana.users.User
import com.gentestrana.utils.computeAgeFromTimestamp
import com.gentestrana.utils.getFlagEmoji
import com.gentestrana.utils.getLanguageName
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth


private const val DEFAULT_PROFILE_IMAGE_URL = "https://icons.veryicon.com/png/o/system/ali-mom-icon-library/random-user.png"

@Composable
fun ProfileContent(
    user: User,
    padding: PaddingValues,
    navController: NavHostController,
    onStartChat: () -> Unit,
    showChatButton: Boolean,
    onWriteRecommendationClick: (() -> Unit)? = null,
    onEditMemoryClick: ((com.gentestrana.users.Memory) -> Unit)? = null
) {
    val currentUser = FirebaseAuth.getInstance().currentUser

    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
    ) {
        // Nome ed età
        val age = computeAgeFromTimestamp(user.birthTimestamp)
        val nameAndAge = if (age > 0) "${user.username}, $age" else user.username
        Text(
            text = nameAndAge,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))
        // 8 perché senno vedo uno spazio aggiuntivo
        // per niente elegante ma pazienza

        // Immagine profilo
        val photoClickModifier = if (user.profilePicUrl.isNotEmpty()) {
            Modifier.clickable {
                navController.currentBackStackEntry?.savedStateHandle?.set("imageUrls", user.profilePicUrl)
                navController.currentBackStackEntry?.savedStateHandle?.set("startIndex", 0)
                navController.navigate("profile_pictures_screen")
            }
        } else Modifier

        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .align(Alignment.CenterHorizontally)
                .then(photoClickModifier),
            contentAlignment = Alignment.Center
        ) {
            val imageUrlToLoad: String = if (user.profilePicUrl.isEmpty()) {
                DEFAULT_PROFILE_IMAGE_URL
            } else {
                user.profilePicUrl.first()
            }

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrlToLoad)
                    .placeholder(R.drawable.random_user)
                    .error(R.drawable.random_user)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (user.profilePicUrl.size > 1) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "1/${user.profilePicUrl.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (user.profilePicUrl.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                user.profilePicUrl.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (index == 0) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                            .clickable {
                                navController.currentBackStackEntry?.savedStateHandle?.set("imageUrls", user.profilePicUrl)
                                navController.currentBackStackEntry?.savedStateHandle?.set("startIndex", index)
                                navController.navigate("profile_pictures_screen")
                            }
                    )
                }
            }
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
                Spacer(modifier = Modifier.height(16.dp))

                // Display Batteria Sociale
                val batteryText = when (user.socialBatteryStatus) {
                    "MEDIUM" -> stringResource(R.string.social_battery_medium)
                    "LOW" -> stringResource(R.string.social_battery_low)
                    "RECHARGING" -> stringResource(R.string.social_battery_recharging)
                    "FOCUS_ONLY" -> stringResource(R.string.social_battery_focus)
                    else -> stringResource(R.string.social_battery_high)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🔋 " + stringResource(R.string.social_battery_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = batteryText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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

        // Sezione Memorie (Stile Tandem)
        MemoriesSection(
            memories = user.memories,
            targetUserName = user.username,
            onWriteClick = if (currentUser?.uid != user.docId) onWriteRecommendationClick else null,
            onEditMemory = onEditMemoryClick
        )

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


