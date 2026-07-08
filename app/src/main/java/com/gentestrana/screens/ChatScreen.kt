package com.gentestrana.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gentestrana.R
import com.gentestrana.chat.ChatMessage
import com.gentestrana.chat.DateSeparatorRow
import com.gentestrana.chat.MessageRow
import com.gentestrana.ui_controller.ChatViewModel
import com.gentestrana.ui_controller.SendMessageEvent
import com.gentestrana.utils.IcebreakerUtils
import com.gentestrana.utils.getDateSeparator
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

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

    val currentUserId by viewModel.currentUserId.collectAsState()
    val recipientName by viewModel.recipientName.collectAsState()
    val currentUserName by remember { mutableStateOf(Firebase.auth.currentUser?.displayName ?: "Tu") }
    val recipientDocId by viewModel.recipientDocId.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isLoadingOlderMessages by viewModel.isLoadingOlderMessages.collectAsState()
    val hasMoreMessages by viewModel.hasMoreMessages.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<String?>(null) }
    var messageText by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }

    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Stato per gli icebreaker: calcolato solo se la chat è vuota
    val icebreakerQuestions = remember {
        mutableStateOf<List<String>>(emptyList())
    }
    // Aggiorna gli icebreaker quando i messaggi cambiano (solo se vuoti)
    LaunchedEffect(messages.size, recipientName) {
        if (messages.isEmpty() && recipientName != null) {
            icebreakerQuestions.value = IcebreakerUtils.getRandomIcebreakers(context, 3)
        }
    }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val initialScrollDone = remember { mutableStateOf(false) }
    val textFieldIsFocused = remember { mutableStateOf(false) }
    val imeInsets = WindowInsets.ime
    val isKeyboardVisible = imeInsets.getBottom(LocalDensity.current) > 0

    LaunchedEffect(isKeyboardVisible) {
        if (!isKeyboardVisible && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(key1 = Unit) {
        viewModel.sendMessageEvent.collectLatest { event ->
            when (event) {
                is SendMessageEvent.Success -> {
                    delay(200)
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }
                is SendMessageEvent.Error -> {
                    val errorMessage = event.message
                    val toastMessage = when {
                        errorMessage.contains("Daily message limit exceeded") -> "â ŒðŸ“¨âž¡â ³ðŸ’¤"
                        errorMessage.contains("Rate limit exceeded") -> "ðŸš«ðŸ“¨âž¡â ³ðŸ’¤ "
                        else -> "Errore nell'invio del messaggio."
                    }
                    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val lastIndex = messages.size - 1
            if (!initialScrollDone.value) {
                listState.scrollToItem(lastIndex)
                initialScrollDone.value = true
            } else {
                val lastMessage = messages.lastOrNull()
                if (lastMessage?.sender == currentUserId ||
                    textFieldIsFocused.value ||
                    listState.firstVisibleItemIndex > lastIndex - 5) {
                    listState.animateScrollToItem(lastIndex)
                }
            }
        }
    }

    LaunchedEffect(textFieldIsFocused.value) {
        if (textFieldIsFocused.value && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstVisibleIndex ->
                if (firstVisibleIndex == 0 && !isLoadingOlderMessages && hasMoreMessages) {
                    viewModel.loadOlderMessages()
                }
            }
    }

    val onSend = {
        val messageToSend = messageText.trim()
        if (messageToSend.isNotEmpty()) {
            // Chiamiamo la nuova funzione del ViewModel, passando anche `replyingToMessage`
            viewModel.sendMessage(messageToSend, context, replyingToMessage)

            // Eseguiamo le pulizie!
            messageText = ""           // Svuota il campo di testo
            replyingToMessage = null   // Fa sparire l'anteprima della risposta. PROBLEMA RISOLTO!
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
                    reverseLayout = false,
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    if (isLoadingOlderMessages) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    val currentUid = currentUserId
                    if (currentUid != null) {
                        itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                            val currentSeparator = getDateSeparator(message.timestamp)
                            val previousSeparator = if (index > 0) getDateSeparator(messages[index - 1].timestamp) else ""
                            if (index == 0 || currentSeparator != previousSeparator) {
                                DateSeparatorRow(dateText = currentSeparator)
                            }
                            val previousMessage = messages.getOrNull(index - 1)
                            val isFirstInBlock = previousMessage == null || previousMessage.sender != message.sender
                            MessageRow(
                                chatMessage = message,
                                currentUserId = currentUid,
                                currentUserName = currentUserName, // Passa il nome dell'utente corrente
                                recipientName = recipientName ?: "", // Passa il nome del destinatario
                                showAvatar = isFirstInBlock,
                                onDelete = if (message.sender == currentUid) {
                                    {
                                        messageToDelete = message.id
                                        showDeleteDialog = true
                                    }
                                } else null,
                                onReplySwipe = if (message.sender != currentUid) {
                                    { replyingToMessage = message }
                                } else null
                            )
                        }
                    }
                }

                if (replyingToMessage != null) {
                    ReplyPreview(
                        // Passiamo il nome del destinatario
                        recipientName = recipientName ?: replyingToMessage!!.sender,
                        message = replyingToMessage!!,
                        onCancelReply = { replyingToMessage = null }
                    )
                }

                // Icebreaker chips: mostrati SOLO quando la chat è vuota
                if (messages.isEmpty() && icebreakerQuestions.value.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.icebreaker_header),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        icebreakerQuestions.value.forEach { question ->
                            SuggestionChip(
                                onClick = {
                                    messageText = question
                                },
                                label = {
                                    Text(
                                        text = question,
                                        maxLines = 2,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

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
                        keyboardOptions = KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(onSend = { onSend() })
                    )

                    Button(
                        // ---> INIZIO MODIFICA 3: Chiama la nuova funzione onSend <---
                        onClick = { onSend() },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.send_message)
                        )
                    }
                }
            }
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteDialog = false
                        messageToDelete = null
                    },
                    title = { Text(stringResource(id = R.string.delete_message)) },
                    text = { Text(stringResource(id = R.string.delete_message_confirm)) },
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
                            Text(text = stringResource(id = R.string.confirm).uppercase())
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                messageToDelete = null
                            }
                        ) {
                            Text(text = stringResource(id = R.string.cancel).uppercase())
                        }
                    }
                )
            }
        }
    )
}

@Composable
fun ReplyPreview(
    recipientName: String,
    message: ChatMessage,
    onCancelReply: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 4.dp) // Leggero padding
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
            )
            .padding(start = 8.dp), // Padding interno per distanziare la linea verticale
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Linea verticale colorata (stile WhatsApp)
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp) // Altezza fissa per la linea
                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(2.dp))
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Contenuto del messaggio in anteprima
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = recipientName, // Usa il nome passato
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = message.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Bottone per annullare la risposta
        IconButton(onClick = onCancelReply) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel Reply"
            )
        }
    }
}