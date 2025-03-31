package com.gentestrana.screens

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
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
import com.gentestrana.components.ProfileContent
import com.gentestrana.users.User
import com.gentestrana.users.UserPicsGallery
import com.gentestrana.users.UserRepository
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.WarningAmber
import com.gentestrana.users.ReportReason
import com.google.firebase.auth.FirebaseAuth


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    docId: String,
    navController: NavHostController,) {
    val userRepository = remember { UserRepository() }
    val chatRepository = remember { ChatRepository() }
    var showGallery by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Stato per il motivo selezionato e per eventuali commenti aggiuntivi
    var selectedReportReason by remember { mutableStateOf(ReportReason.CONTENUTI_INAPPROPRIATI) }
    var additionalComments by remember { mutableStateOf("") }
    var showBlockDialog by remember { mutableStateOf(false) }

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
                title = { Text(stringResource(R.string.user_profile)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    // Mostra i pulsanti SOLO se non è il profilo personale
                    if (docId != currentUserId) {
                        IconButton(onClick = { showReportDialog = true }) {
                            Icon(Icons.Filled.WarningAmber, contentDescription = "Segnala utente")
                        }
                        IconButton(onClick = { showBlockDialog = true }) {
                            Icon(Icons.Filled.Block, contentDescription = "Blocca utente")
                        }
                    }
                }
            )
        }
    ) { padding ->
        BoxWithOverlay(
            showGallery = showGallery,
            images = user.profilePicUrl,
            onCloseGallery = { showGallery = false }
        )
        {
            Log.d("UserProfileScreen", "docId: $docId, currentUserId: $currentUserId")

            ProfileContent(
                user = user,
                padding = padding,
                //scrollState = scrollState,
                onProfileImageClick = { showGallery = true },
                navController = navController,
                onStartChat = if (docId != currentUserId) {
                    { // Lambda per avviare la chat (solo se NON è il profilo personale)
                        coroutineScope.launch {
                            try {
                                val chatId = chatRepository.createNewChat(user)
                                navController.navigate("chat/$chatId")
                            } catch (e: Exception) {
                                // Gestisci l'errore (es. mostra un Snackbar)
                            }
                        }
                    }
                } else {
                    {} // Lambda vuota: NON fare nulla se è il profilo personale
                },
                showChatButton = docId != currentUserId
            )
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text(stringResource(R.string.report_user_dialog_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.report_user_dialog_select_reason))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedReportReason == ReportReason.CONTENUTI_INAPPROPRIATI,
                            onClick = { selectedReportReason = ReportReason.CONTENUTI_INAPPROPRIATI }
                        )
                        Text(stringResource(R.string.report_reason_inappropriate_content))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedReportReason == ReportReason.COMPORTAMENTO_OFFENSIVO,
                            onClick = { selectedReportReason = ReportReason.COMPORTAMENTO_OFFENSIVO }
                        )
                        Text(stringResource(R.string.report_reason_offensive_behavior))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedReportReason == ReportReason.ALTRO,
                            onClick = { selectedReportReason = ReportReason.ALTRO }
                        )
                        Text(stringResource(R.string.report_reason_other))
                    }
                    if (selectedReportReason == ReportReason.ALTRO) {
                        OutlinedTextField(
                            value = additionalComments,
                            onValueChange = { additionalComments = it },
                            label = { Text(stringResource(R.string.report_reason_other_comments_label)) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Chiama il report passando l'ID dell'utente corrente (user.docId) o quello visualizzato
                        userRepository.reportUser(
                            reportedUserId = user.docId,
                            reason = selectedReportReason,
                            additionalComments = additionalComments,
                            onSuccess = {
                                showReportDialog = false
                                // Eventuale feedback all'utente (es. Snackbar)
                            },
                            onFailure = {
                                // Mostra un messaggio d'errore
                            }
                        )
                    }
                ) {
                    Text(stringResource(R.string.report_user_dialog_report_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text(stringResource(R.string.block_user_dialog_title)) },
            text = { Text(stringResource(R.string.block_user_dialog_text)) },
            confirmButton = {
                Button(
                    onClick = {
                        // Inserisci qui la logica per bloccare l'utente
                        showBlockDialog = false
                    }
                ) {
                    Text(stringResource(R.string.block_user_dialog_block_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Blocca Utente") }, // stringabile
            text = { Text("Sei sicuro di voler bloccare questo utente?") }, // stringabile
            confirmButton = {
                Button(
                    onClick = {
                        // TODO: Inserisci qui la logica per bloccare l'utente
                        showBlockDialog = false
                    }
                ) {
                    Text("Blocca")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
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
//TODO: rimovibile e usare Generic etc

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
                imageSize = 300,
            )
        }
    }
}