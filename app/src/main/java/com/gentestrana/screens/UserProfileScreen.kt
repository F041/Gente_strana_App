package com.gentestrana.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gentestrana.R
import com.gentestrana.chat.ChatRepository
import com.gentestrana.users.User
import com.gentestrana.users.UserPicsGallery
import com.gentestrana.users.UserRepository
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    docId: String,
    navController: NavHostController,

) {
    val userRepository = remember { UserRepository() }
    val chatRepository = remember { ChatRepository() } // <-- Aggiungi questa linea
    var showGallery by remember { mutableStateOf(false) }
    var showAddImageDialog by remember { mutableStateOf(false) }
    var newImageUrl by remember { mutableStateOf("") }

    // Stato per gestire il caricamento delle immagini
    val userState = produceState<User?>(initialValue = null) {
        userRepository.getUser(docId,
            onSuccess = { user -> value = user },
            onFailure = { /* Gestisci l'errore */ }
        )
    }

    if (userState.value == null) {
        CircularProgressCentered()
        return
    }

    val user = userState.value!!
    val descriptionItems = user.topics
    val scrollState = rememberLazyListState()
    var currentDescriptionIndex by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(scrollState.firstVisibleItemScrollOffset) {
        val firstVisible = scrollState.firstVisibleItemIndex
        if (firstVisible != currentDescriptionIndex) currentDescriptionIndex = firstVisible
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profilo") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        BoxWithOverlay(
            showGallery = showGallery,
            images = user.profilePicUrl,
            onCloseGallery = { showGallery = false }
        ) {
            ProfileContent(
                user = user,
                padding = padding,
                descriptionItems = descriptionItems,
                scrollState = scrollState,
                currentDescriptionIndex = currentDescriptionIndex,
                coroutineScope = coroutineScope,
                onProfileImageClick = { showGallery = true },
                navController = navController, // Passa navController
                onStartChat = { // Lambda per avviare la chat
                    coroutineScope.launch {
                        try {
                            val chatId = chatRepository.createNewChat(user)
                            navController.navigate("chat/$chatId")
                        } catch (e: Exception) {
                            // Gestisci l'errore (es. mostra un Snackbar)
                        }
                    }
                }
            )
        }
    }

    if (showAddImageDialog) {
        AlertDialog(
            onDismissRequest = { showAddImageDialog = false },
            title = { Text("Aggiungi nuova immagine") },
            text = {
                Column {
                    TextField(
                        value = newImageUrl,
                        onValueChange = { newImageUrl = it },
                        label = { Text("URL immagine") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        userRepository.addProfileImage(
                            docId = docId,
                            newImageUrl = newImageUrl,
                            onSuccess = {
                                showAddImageDialog = false
                                newImageUrl = ""
                            },
                            onFailure = { /* Gestisci errore */ }
                        )
                    }
                ) {
                    Text("Salva")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddImageDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
private fun CircularProgressCentered() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun BoxWithOverlay(
    showGallery: Boolean,
    images: List<String>,
    onCloseGallery: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        content()
        if (showGallery) {
            GalleryOverlay(images, onCloseGallery)
        }
    }
}

@Composable
private fun GalleryOverlay(images: List<String>, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(stringResource(R.string.photos), style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, stringResource(R.string.close))
                }
            }
            Spacer(Modifier.height(16.dp))
            UserPicsGallery(
                imageUrls = images,
                modifier = Modifier.fillMaxSize(),
                imageSize = 300
            )
        }
    }
}