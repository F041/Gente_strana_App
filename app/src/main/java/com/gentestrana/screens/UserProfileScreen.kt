package com.gentestrana.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
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
import kotlinx.coroutines.tasks.await
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.ui.platform.LocalContext
import com.gentestrana.users.ReportReason
import com.google.firebase.auth.FirebaseAuth


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    docId: String,
    navController: NavHostController,) {
    val userRepository = remember { UserRepository() }
    val chatRepository = remember { ChatRepository() }
    var showReportDialog by remember { mutableStateOf(false) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Stato per il motivo selezionato e per eventuali commenti aggiuntivi
    var selectedReportReason by remember { mutableStateOf(ReportReason.CONTENUTI_INAPPROPRIATI) }
    var additionalComments by remember { mutableStateOf("") }
    var isBlocked by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showUnblockDialog by remember { mutableStateOf(false)}
    var showWriteRecommendationDialog by remember { mutableStateOf(false) }
    var recommendationText by remember { mutableStateOf("") }
    var isMemoryUnlockedForProfile by remember { mutableStateOf(false) }
    var memoriesList by remember { mutableStateOf<List<com.gentestrana.users.Memory>>(emptyList()) }
    var editingMemory by remember { mutableStateOf<com.gentestrana.users.Memory?>(null) }
    var editText by remember { mutableStateOf("") }

    fun refreshMemories() {
        userRepository.getMemories(
            targetUserId = docId,
            onSuccess = { recs -> memoriesList = recs },
            onFailure = { /* ignore */ }
        )
    }

    LaunchedEffect(docId) {
        refreshMemories()
    }

        // Stato per gestire il caricamento delle immagini
    val userState = produceState<User?>(initialValue = null) {
        userRepository.getUser(docId,
            onSuccess = { user -> value = user },
            onFailure = { /* Gestisci l'errore */ }
        )
    }

    LaunchedEffect(docId, currentUserId, userState.value) {
        val loadedUser = userState.value
        if (currentUserId != null && docId != currentUserId && loadedUser != null) {
            userRepository.isUserBlocked(docId) { result ->
                isBlocked = result
                Log.d("UserProfileScreen", "Utente $docId è bloccato? $result")
            }
            try {
                val chatId = chatRepository.createNewChat(loadedUser)
                val countSnapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .count()
                    .get(com.google.firebase.firestore.AggregateSource.SERVER)
                    .await()
                isMemoryUnlockedForProfile = countSnapshot.count >= 50L
            } catch (e: Exception) {
                isMemoryUnlockedForProfile = false
            }
        } else {
            isBlocked = false
            isMemoryUnlockedForProfile = false
        }
    }

    if (userState.value == null) {
        CircularProgressCentered()
        return
    }

    val user = userState.value!!
    val scrollState = rememberLazyListState()
    var currentDescriptionIndex by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current // Ottieni il contesto

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
                        IconButton(onClick = {
                            if (isBlocked) {
                                showUnblockDialog = true // Mostra dialog SBLOCCO
                            } else {
                                showBlockDialog = true // Mostra dialog BLOCCO
                            }
                        }) {
                            if (isBlocked) {
                                // Se è bloccato, mostra icona per Sbloccare
                                Icon(
                                    Icons.Filled.LockOpen, // Usa un'icona per "Sblocca"
                                    contentDescription = "Sblocca utente", // Cambia descrizione
                                    tint = MaterialTheme.colorScheme.primary // Colore opzionale
                                )
                            } else {
                                // Se non è bloccato, mostra icona per Bloccare
                                Icon(
                                    Icons.Filled.Block,
                                    contentDescription = "Blocca utente",
                                    tint = MaterialTheme.colorScheme.error // Colore rosso per azione "negativa"
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        {
            Log.d("UserProfileScreen", "docId: $docId, currentUserId: $currentUserId")

            val hasUserAlreadyWrittenMemory = memoriesList.any { it.authorId == currentUserId }
            val displayUser = user.copy(memories = memoriesList)
            ProfileContent(
                user = displayUser,
                padding = padding,
                //scrollState = scrollState,
                navController = navController,
                onStartChat = if (docId != currentUserId && !isBlocked)  {
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
                showChatButton = (docId != currentUserId && !isBlocked),
                onWriteRecommendationClick = if (isMemoryUnlockedForProfile && !hasUserAlreadyWrittenMemory) {
                    { showWriteRecommendationDialog = true }
                } else {
                    null
                },
                onEditMemoryClick = { mem ->
                    editingMemory = mem
                    editText = mem.text
                }
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
                        coroutineScope.launch {
                        // Chiama il report passando l'ID dell'utente corrente (user.docId) o quello visualizzato
                        userRepository.reportUser(
                            reportedUserId = user.docId,
                            reason = selectedReportReason,
                            additionalComments = additionalComments,
                            onSuccess = {
                                showReportDialog = false
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.report_sent_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onFailure = {
                                showReportDialog = false
                                Toast.makeText(
                                    context,
                                    "Errore durante l'invio della segnalazione",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
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
                        coroutineScope.launch {
                            userRepository.blockUser(
                                blockedUserId = user.docId,
                                onSuccess = {
                                    isBlocked = true // Aggiorna lo stato locale
                                    showBlockDialog = false
                                    Toast.makeText(context, "Utente bloccato.", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { errorMsg ->
                                    showBlockDialog = false
                                    Toast.makeText(context, "Errore blocco: ${errorMsg ?: "sconosciuto"}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    // Stile opzionale per il bottone di conferma blocco
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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

    if (showUnblockDialog) {
        AlertDialog(
            onDismissRequest = { showUnblockDialog = false },
            title = { Text(stringResource(R.string.unblock_user_dialog_title)) },
            text = { Text(stringResource(R.string.unblock_user_dialog_text)) },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            userRepository.unblockUser(
                                unblockedUserId = user.docId,
                                onSuccess = {
                                    isBlocked = false // Aggiorna lo stato locale
                                    showUnblockDialog = false
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.unblock_user_success_toast),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onFailure = { errorMsg ->
                                    showUnblockDialog = false
                                    Toast.makeText(context, "Errore sblocco: ${errorMsg ?: "sconosciuto"}", Toast.LENGTH_LONG).show() // Feedback errore
                                }
                            )
                        }
                    }
                ) {
                    Text(stringResource(R.string.unblock_user_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnblockDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showWriteRecommendationDialog) {
        AlertDialog(
            onDismissRequest = { showWriteRecommendationDialog = false },
            title = { Text("✍️ Scrivi una memoria per ${user.username}") },
            text = {
                OutlinedTextField(
                    value = recommendationText,
                    onValueChange = { recommendationText = it },
                    placeholder = { Text("Condividi la tua esperienza positiva con ${user.username}...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (recommendationText.isNotBlank()) {
                            userRepository.addMemory(
                                targetUserId = user.docId,
                                text = recommendationText.trim(),
                                onSuccess = {
                                    showWriteRecommendationDialog = false
                                    recommendationText = ""
                                    refreshMemories()
                                    Toast.makeText(context, "Memoria pubblicata con successo! 🎉", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { err ->
                                    Toast.makeText(context, "Errore: $err", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                ) {
                    Text("Pubblica")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWriteRecommendationDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    if (editingMemory != null) {
        val targetMem = editingMemory!!
        AlertDialog(
            onDismissRequest = { editingMemory = null },
            title = { Text("✏️ Modifica la tua memoria per ${user.username}") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    placeholder = { Text("Modifica il testo...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editText.isNotBlank()) {
                            userRepository.updateMemory(
                                memoryId = targetMem.id,
                                targetUserId = user.docId,
                                newText = editText.trim(),
                                onSuccess = {
                                    editingMemory = null
                                    refreshMemories()
                                    Toast.makeText(context, "Memoria aggiornata con successo! 🎉", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { err ->
                                    Toast.makeText(context, "Errore: $err", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                ) {
                    Text("Aggiorna")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMemory = null }) {
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
