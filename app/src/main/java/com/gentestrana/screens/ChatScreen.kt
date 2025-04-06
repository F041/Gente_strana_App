package com.gentestrana.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gentestrana.chat.MessageRow
import com.gentestrana.chat.DateSeparatorRow
import com.gentestrana.utils.getDateSeparator
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.stringResource
import com.gentestrana.R
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import com.gentestrana.components.GenericLoadingScreen // Mantenuto per il loader
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gentestrana.ui_controller.ChatViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import com.gentestrana.ui_controller.SendMessageEvent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity


/*
ChatScreen fa varie cose:
- quando si apre la chat, scrolla automaticamente all'ultimo messaggio
- quando scrollo in alto e scrivo un messagio, scrolla automaticamente all'ultimo messaggio
- quando scrollo in basso e elimino un messaggio, scrolla automaticamente all'ultimo messaggio
- anche quando l'altro partecipante alla chat manda un messaggio, scrolla automaticamente all'ultimo messaggio
- mostra il primo messaggio senza scrollare in alto
 */


// Factory per il ViewModel
class ChatViewModelFactory(private val chatId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(chatId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(docId: String, navController: NavController) {
    val viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(docId))

    // Raccogli stati dal ViewModel
    val currentUserId by viewModel.currentUserId.collectAsState()
    val recipientName by viewModel.recipientName.collectAsState()
    val recipientDocId by viewModel.recipientDocId.collectAsState()
    val messages by viewModel.messages.collectAsState() // Stato dei messaggi dal ViewModel
    val isLoadingOlderMessages by viewModel.isLoadingOlderMessages.collectAsState() // Stato caricamento dal ViewModel
    val hasMoreMessages by viewModel.hasMoreMessages.collectAsState() // Stato "ci sono altri messaggi?"

    // Stati locali per UI
    var showDeleteDialog by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<String?>(null) }
    var messageText by remember { mutableStateOf("") }

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope() // Mantenuto per l'eliminazione
    val initialScrollDone = remember { mutableStateOf(false) }
    val textFieldIsFocused = remember { mutableStateOf(false) }
    val imeInsets = WindowInsets.ime
    val isKeyboardVisible = imeInsets.getBottom(LocalDensity.current) > 0

    LaunchedEffect(isKeyboardVisible) {
        if (!isKeyboardVisible && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
            Log.d("ChatScreen_Scroll", "Scrolled to last message because keyboard is closed.")
        }
    }

    // Gestione Eventi di Invio dal ViewModel
    LaunchedEffect(key1 = Unit) {
        viewModel.sendMessageEvent.collectLatest { event ->
            when (event) {
                is SendMessageEvent.Success -> {
                    Log.d("ChatScreen", "Message sent successfully (event received).")
                    // Aggiungiamo un breve delay per permettere l'aggiornamento della lista dei messaggi
                    delay(200) // da 500 versione non elegante
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                        Log.d("ChatScreen_Scroll", "Forced scroll to last message after sending message.")
                    }
                }
                is SendMessageEvent.Error -> {
                    val errorMessage = event.message
                    Log.e("ChatScreen", "Error sending message (event received): $errorMessage")
                    val toastMessage = when {
                        errorMessage.contains("Daily message limit exceeded") -> "❌📨➡⏳💤"
                        errorMessage.contains("Rate limit exceeded") -> "🚫📨➡⏳💤 "
                        else -> "Errore nell'invio del messaggio."
                    }
                    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

// Effetto per scrollare in fondo quando arrivano nuovi messaggi o all'apertura
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val lastIndex = messages.size - 1
            if (!initialScrollDone.value) {
                // PRIMO SCROLL: Vai subito in fondo senza animazione
                listState.scrollToItem(lastIndex)
                initialScrollDone.value = true
                Log.d("ChatScreen_Scroll", "Initial scroll to index: $lastIndex, message size: ${messages.size}")
            } else {
                val lastMessage = messages.lastOrNull()
                // Ora scrolla se:
                // - L'ultimo messaggio è inviato da me
                // - Oppure il campo di input è focalizzato
                // - Oppure l'utente è già vicino al fondo
                if (lastMessage?.sender == currentUserId ||
                    textFieldIsFocused.value ||
                    listState.firstVisibleItemIndex > lastIndex - 5) {
                    listState.animateScrollToItem(lastIndex)
                    Log.d("ChatScreenScroll", "Animated scroll to index: $lastIndex")
                } else {
                    Log.d("ChatScreenScroll", "Scroll skipped (user reading older messages)")
                }
            }
        }
    }

    LaunchedEffect(textFieldIsFocused.value) {
        if (textFieldIsFocused.value && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
            Log.d("ChatScreen_Scroll", "Scrolled to last message due to input focus.")
        }
    }

    // Effetto per caricare messaggi più vecchi quando si raggiunge l'inizio della lista
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstVisibleIndex ->
                // Carica se siamo al primo item, non stiamo già caricando e ci sono altri messaggi
                if (firstVisibleIndex == 0 && !isLoadingOlderMessages && hasMoreMessages) {
                    viewModel.loadOlderMessages()
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val currentRecipientName = recipientName
                    val currentRecipientDocId = recipientDocId
                    if (currentRecipientName != null) {
                        Row(
                            modifier = Modifier
                                .clickable {
                                    if (!currentRecipientDocId.isNullOrEmpty()) {
                                        navController.navigate("userProfile/$currentRecipientDocId")
                                    }
                                }
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currentRecipientName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Visualizza profilo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    reverseLayout = false, // Ora non più necessario invertire il layout
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    // Mostra il loader in cima se stiamo caricando messaggi vecchi
                    if (isLoadingOlderMessages) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    // Usa la lista di messaggi dal ViewModel
                    val currentUid = currentUserId
                    if (currentUid != null) {
                        itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                            val currentSeparator = getDateSeparator(message.timestamp)
                            // Controlla il messaggio precedente nella lista corrente (non invertita)
                            val previousSeparator = if (index > 0) getDateSeparator(messages[index - 1].timestamp) else ""
                            if (index == 0 || currentSeparator != previousSeparator) {
                                DateSeparatorRow(dateText = currentSeparator)
                            }
                            val previousMessage = messages.getOrNull(index - 1)
                            val isFirstInBlock = previousMessage == null || previousMessage.sender != message.sender
                            MessageRow(
                                chatMessage = message,
                                currentUserId = currentUid,
                                showAvatar = isFirstInBlock,
                                onDelete = if (message.sender == currentUid) {
                                    {
                                        messageToDelete = message.id
                                        showDeleteDialog = true
                                    }
                                } else null
                            )
                        }
                    }
                }
                // Input field e bottone di invio
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { state ->
                                // Aggiornamento dello stato del focus
                                textFieldIsFocused.value = state.isFocused
                                if (state.isFocused) {
                                    scope.launch {
                                        delay(200)
                                        if (messages.isNotEmpty()) {
                                            listState.animateScrollToItem(messages.size - 1)
                                        }
                                    }
                                }
                            },
                        placeholder = { Text(stringResource(R.string.write_message)) },
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            val messageToSend = messageText.trim()
                            if (messageToSend.isNotEmpty()) {
                                messageText = ""
                                viewModel.sendMessage(messageToSend, context)
                            }
                        })
                    )

                    Button(
                        onClick = {
                            val messageToSend = messageText.trim()
                            if (messageToSend.isNotEmpty()) {
                                messageText = ""
                                viewModel.sendMessage(messageToSend, context)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.send_message)
                        )
                    }
                }
            }
            // Dialog per eliminazione messaggio
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteDialog = false
                        messageToDelete = null
                    },
                    title = { Text("Elimina messaggio") },
                    text = { Text("Vuoi eliminare definitivamente questo messaggio?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                messageToDelete?.let { id ->
                                    viewModel.deleteMessage(id)
                                }
                                messageToDelete = null
                            }
                        ) {
                            Text(text = stringResource(id = R.string.confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                messageToDelete = null
                            }
                        ) {
                            Text(text = stringResource(id = R.string.cancel))
                        }
                    }
                )
            }
        }
    )
}