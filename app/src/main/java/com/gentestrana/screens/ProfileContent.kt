package com.gentestrana.screens

import android.util.Log
import com.gentestrana.chat.ChatRepository
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileContent(
    user: User,
    padding: PaddingValues,
    descriptionItems: List<String>,
    scrollState: LazyListState,
    currentDescriptionIndex: Int,
    coroutineScope: CoroutineScope,
    onProfileImageClick: () -> Unit,
    navController: NavHostController, // Parametro per la navigazione
    onStartChat: () -> Unit // <-- Parametro aggiunto qui
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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

        // Immagine profilo centrale
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

        // Descrizione swipeabile
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentDescriptionIndex > 0) {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            scrollState.animateScrollToItem(currentDescriptionIndex - 1)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = stringResource(R.string.previous),
                            modifier = Modifier
                                .rotate(180f)
                                .padding(8.dp)
                        )
                    }
                }

                LazyRow(
                    state = scrollState,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = scrollState),
                    modifier = Modifier.width(200.dp)
                ) {
                    items(descriptionItems.size) { index ->
                        Text(
                            text = descriptionItems[index],
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                if (currentDescriptionIndex < descriptionItems.lastIndex) {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            scrollState.animateScrollToItem(currentDescriptionIndex + 1)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = stringResource(R.string.next),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        // Sezione lingue
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = user.bio,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.languages_spoken),
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
                        text = "${getFlagEmoji(context, code)} ${getLanguageName(context, code)}", // Passa il context!
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        // Pulsante "Chatta"
        Button(
            onClick = onStartChat, // <-- Usa il parametro onStartChat
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.chat_with_user, user.username))
        }
    }
}