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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
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
import androidx.compose.ui.unit.sp
import com.gentestrana.utils.ActiveChatTracker
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
import com.gentestrana.users.UserRepository
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
    val recipientBio by viewModel.recipientBio.collectAsState()
    val recipientTopics by viewModel.recipientTopics.collectAsState()
    val recipientAutopilotEnabled by viewModel.recipientAutopilotEnabled.collectAsState()
    val recipientSocialBatteryStatus by viewModel.recipientSocialBatteryStatus.collectAsState()
    val recipientLastActive by viewModel.recipientLastActive.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isLoadingOlderMessages by viewModel.isLoadingOlderMessages.collectAsState()
    val hasMoreMessages by viewModel.hasMoreMessages.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<String?>(null) }
    var messageText by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }

    val context = LocalContext.current
    val listState = rememberLazyListState()

    DisposableEffect(docId) {
        ActiveChatTracker.activeChatId = docId
        ActiveChatTracker.clearNotificationForChat(context, docId)
        onDispose {
            if (ActiveChatTracker.activeChatId == docId) {
                ActiveChatTracker.activeChatId = null
            }
        }
    }

    // Stato per gli icebreaker: calcolato solo se la chat è vuota e l'autopilot del destinatario è attivo
    val icebreakerQuestions = remember {
        mutableStateOf<List<String>>(emptyList())
    }
    var isIcebreakerLoading by remember { mutableStateOf(false) }

    // Aggiorna gli icebreaker quando i messaggi cambiano o arrivano bio/topics/autopilot (solo se vuoti)
    LaunchedEffect(messages.size, recipientName, recipientBio, recipientTopics, recipientAutopilotEnabled) {
        if (messages.isEmpty() && recipientName != null) {
            if (recipientAutopilotEnabled) {
                // Calcola subito gli icebreaker senza ritardi artificiali
                isIcebreakerLoading = true
                icebreakerQuestions.value = IcebreakerUtils.matchIcebreakers(
                    bio = recipientBio,
                    topics = recipientTopics,
                    context = context,
                    count = 3
                )
                isIcebreakerLoading = false
            } else {
                isIcebreakerLoading = false
                icebreakerQuestions.value = emptyList()
            }
        }
    }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val initialScrollDone = remember { mutableStateOf(false) }
    val textFieldIsFocused = remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0

    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible && messages.isNotEmpty()) {
            delay(150) // Aspetta che la tastiera completi l'animazione di apertura
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

    var showMetricsDialog by remember { mutableStateOf(false) }
    var showWriteRecommendationDialog by remember { mutableStateOf(false) }
    var recommendationText by remember { mutableStateOf("") }
    var myExistingMemoryInChat by remember { mutableStateOf<com.gentestrana.users.Memory?>(null) }
    val userRepository = remember { UserRepository() }

    LaunchedEffect(recipientDocId) {
        if (!recipientDocId.isNullOrEmpty()) {
            userRepository.hasUserWrittenMemory(recipientDocId!!) { existing ->
                myExistingMemoryInChat = existing
            }
        }
    }

    if (showMetricsDialog) {
        val metrics by viewModel.conversationMetrics.collectAsState()
        val name = recipientName ?: "L'utente"
        AlertDialog(
            onDismissRequest = { showMetricsDialog = false },
            title = { Text("📊 Metriche Conversazione") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Messaggi inviati da $name: ${metrics.recipientMessageCount}")
                    Text("• Anzianità conversazione: ${metrics.conversationAgeText} (dal ${metrics.startDateText})")
                    Text("• Infodump Index: ~${metrics.avgWordsPerMessage} parole per messaggio")
                    if (metrics.isMemoryUnlocked) {
                        Text(
                            text = "✨ Trigger Memorie: SBLOCCATO (soglia di 50 messaggi raggiunta!)",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "🔒 Trigger Memorie: in corso (${metrics.recipientMessageCount}/50 messaggi)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (metrics.isMemoryUnlocked) {
                        Button(onClick = {
                            showMetricsDialog = false
                            if (myExistingMemoryInChat != null) {
                                recommendationText = myExistingMemoryInChat!!.text
                            } else {
                                recommendationText = ""
                            }
                            showWriteRecommendationDialog = true
                        }) {
                            Text(if (myExistingMemoryInChat != null) "✏️ Modifica memoria" else "✍️ Scrivi memoria")
                        }
                    }
                    TextButton(onClick = { showMetricsDialog = false }) {
                        Text("OK")
                    }
                }
            }
        )
    }

    if (showWriteRecommendationDialog) {
        AlertDialog(
            onDismissRequest = { showWriteRecommendationDialog = false },
            title = { Text(if (myExistingMemoryInChat != null) "✏️ Modifica la tua memoria per ${recipientName ?: "L'utente"}" else "✍️ Scrivi una memoria per ${recipientName ?: "L'utente"}") },
            text = {
                OutlinedTextField(
                    value = recommendationText,
                    onValueChange = { recommendationText = it },
                    placeholder = { Text("Condividi la tua esperienza positiva...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = recipientDocId
                        if (!targetId.isNullOrEmpty() && recommendationText.isNotBlank()) {
                            userRepository.addMemory(
                                targetUserId = targetId,
                                text = recommendationText.trim(),
                                chatId = docId,
                                onSuccess = {
                                    showWriteRecommendationDialog = false
                                    recommendationText = ""
                                    userRepository.hasUserWrittenMemory(targetId) { existing ->
                                        myExistingMemoryInChat = existing
                                    }
                                    Toast.makeText(context, "Memoria salvata con successo! 🎉", Toast.LENGTH_SHORT).show()
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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    val currentRecipientName = recipientName
                    val currentRecipientDocId = recipientDocId
                    if (currentRecipientName != null) {
                        Column(
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
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val batteryIcon = when (recipientSocialBatteryStatus) {
                                    "MEDIUM" -> " 🟡"
                                    "LOW" -> " 🪫"
                                    "RECHARGING" -> " ⚡"
                                    "FOCUS_ONLY" -> " 🎯"
                                    else -> ""
                                }
                                Text(
                                    text = currentRecipientName + batteryIcon,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = TextDecoration.Underline
                                    )
                                )
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Visualizza profilo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            val lastSeenStr = remember(recipientLastActive) {
                                com.gentestrana.utils.formatLastSeen(recipientLastActive)
                            }
                            if (lastSeenStr != null) {
                                val isOnlineNow = lastSeenStr.contains("ora") || lastSeenStr.contains("now")
                                Text(
                                    text = lastSeenStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOnlineNow) androidx.compose.ui.graphics.Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isOnlineNow) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                },
                actions = {
                    val metrics by viewModel.conversationMetrics.collectAsState()
                    if (metrics.recipientMessageCount > 0 || metrics.conversationAgeText.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (metrics.isMemoryUnlocked)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { showMetricsDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (metrics.isMemoryUnlocked) "✉️ ${metrics.recipientMessageCount} ✨" else "✉️ ${metrics.recipientMessageCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (metrics.avgWordsPerMessage > 0) {
                                    Text(
                                        text = "📊 ${metrics.avgWordsPerMessage}w",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (metrics.conversationAgeText.isNotEmpty()) {
                                    Text(
                                        text = "⏳ ${metrics.conversationAgeText}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Pulsante Chiamata
                    val mySentCount = messages.count { it.sender == currentUserId }
                    val otherSentCount = metrics.recipientMessageCount
                    val isCallUnlocked = mySentCount >= 5 && otherSentCount >= 5

                    var showCallExplanationDialog by remember { mutableStateOf(false) }
                    var callExplanationText by remember { mutableStateOf("") }

                    IconButton(
                        onClick = {
                            if (isCallUnlocked) {
                                navController.navigate("call/$docId/${recipientName ?: "Utente"}")
                            } else {
                                val myMissing = (5 - mySentCount).coerceAtLeast(0)
                                val otherMissing = (5 - otherSentCount).coerceAtLeast(0)
                                callExplanationText = "Le chiamate si sbloccano quando avete scritto almeno 5 messaggi ciascuno.\n" +
                                    when {
                                        myMissing > 0 && otherMissing > 0 -> "A te ne mancano $myMissing, a ${recipientName ?: "L'utente"} ne mancano $otherMissing."
                                        myMissing > 0 -> "A te ne mancano $myMissing."
                                        else -> "A ${recipientName ?: "L'utente"} ne mancano $otherMissing."
                                    }
                                showCallExplanationDialog = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Chiamata",
                            tint = if (isCallUnlocked)
                                androidx.compose.ui.graphics.Color(0xFF2E7D32)
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }

                    if (showCallExplanationDialog) {
                        AlertDialog(
                            onDismissRequest = { showCallExplanationDialog = false },
                            title = { Text("📞 Chiamate") },
                            text = { Text(callExplanationText) },
                            confirmButton = {
                                TextButton(onClick = { showCallExplanationDialog = false }) {
                                    Text("OK")
                                }
                            }
                        )
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                // Banner Batteria Sociale se non ad alta carica
                val socialBatteryDisclaimer = when (recipientSocialBatteryStatus) {
                    "LOW" -> stringResource(R.string.social_battery_low_disclaimer, recipientName ?: "")
                    "RECHARGING" -> stringResource(R.string.social_battery_recharging_disclaimer, recipientName ?: "")
                    "FOCUS_ONLY" -> stringResource(R.string.social_battery_focus_disclaimer, recipientName ?: "")
                    else -> null
                }

                if (socialBatteryDisclaimer != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = when (recipientSocialBatteryStatus) {
                                    "LOW" -> "🪫"
                                    "RECHARGING" -> "⚡"
                                    "FOCUS_ONLY" -> "🎯"
                                    else -> "🔋"
                                },
                                fontSize = 18.sp
                            )
                            Text(
                                text = socialBatteryDisclaimer,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

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
                                } else null,
                                onToggleReaction = { messageId, emoji ->
                                    viewModel.toggleReaction(messageId, emoji)
                                }
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

                // Icebreaker chips: mostrati SOLO quando la chat è vuota E il destinatario ha l'Autopilot attivo
                // Tre stati: loading (shimmer), pronto (chip con fade-in), nascosto
                if (messages.isEmpty() && recipientAutopilotEnabled && (isIcebreakerLoading || icebreakerQuestions.value.isNotEmpty())) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isIcebreakerLoading) {
                            // Stato di caricamento: loader + testo
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.icebreaker_loading),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Quando le domande sono pronte, appaiono con fade-in
                        AnimatedVisibility(
                            visible = icebreakerQuestions.value.isNotEmpty(),
                            enter = fadeIn()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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