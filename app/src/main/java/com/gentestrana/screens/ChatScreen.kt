package com.gentestrana.screens

import android.util.Log
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
import com.gentestrana.chat.ChatMessage
import com.gentestrana.chat.MessageRow
import com.gentestrana.chat.DateSeparatorRow
import com.gentestrana.utils.getDateSeparator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.gentestrana.chat.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.stringResource
import com.gentestrana.R
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import com.gentestrana.components.GenericLoadingScreen
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.distinctUntilChanged

//TODO: gestire casi dove non si può mandare messaggio per mancanza linea internet
// o altri motivi. Creare dialog per reinvio?

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(docId: String, navController: NavController) {
    val db = Firebase.firestore
    val currentUser = Firebase.auth.currentUser
    val currentUserId = currentUser?.uid
    var showDeleteDialog by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<String?>(null) }
    var recipientName by remember { mutableStateOf<String?>(null) }
    var recipientDocId by remember { mutableStateOf("") }
    var messagesLimit by remember { mutableStateOf(20) }

    // Blocco unico per recuperare il documento della chat, aggiornare lo stato e ottenere il nome destinatario
    LaunchedEffect(docId, currentUserId) {
        if (currentUserId != null) {
            try {
                val chatDocSnapshot = db.collection("chats").document(docId).get().await()
                val participants = chatDocSnapshot.get("participants") as? List<String> ?: emptyList()
                val otherUserId = participants.firstOrNull { it != currentUserId && it.isNotBlank() }
                if (otherUserId != null) {
                    recipientDocId = otherUserId
                    val userDoc = db.collection("users").document(otherUserId).get().await()
                    recipientName = userDoc.getString("username") ?: "Sconosciuto"

                    val repository = ChatRepository()
                    // Aggiorna lo stato dei messaggi: li marca come DELIVERED e READ
                    repository.markMessagesAsDelivered(docId, currentUserId)
                    repository.markMessagesAsRead(docId)
                }
            } catch (e: Exception) {
                Log.e("ChatScreen", "Errore nel recupero e aggiornamento dati della chat: ${e.message}")
            }
        }
    }

    // Stato per i messaggi
    val messagesState = produceState<List<ChatMessage>>(initialValue = emptyList(), key1 = messagesLimit) {
        db.collection("chats")
            .document(docId)
            .collection("messages")
            // Ordina in modo discendente per avere i messaggi più recenti
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(messagesLimit.toLong())
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatScreen", "Error fetching messages: ${e.message}")
                    return@addSnapshotListener
                }
                // Mappa i documenti in oggetti ChatMessage e inverte la lista
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("ChatScreen", "Error parsing message ${doc.id}", e)
                        null
                    }
                }?.reversed() ?: emptyList()
                value = messages
            }
    }

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var forceScrollToBottom by remember { mutableStateOf(false) }

    var isLoadingOlderMessages by remember { mutableStateOf(false) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstVisibleIndex ->
                if (firstVisibleIndex == 0 && messagesState.value.size >= messagesLimit && !isLoadingOlderMessages) {
                    isLoadingOlderMessages = true  // Mostra il loader

                    kotlinx.coroutines.delay(300)  // Attendi un po' per mostrare il loader

                    messagesLimit += 20  // Carica altri messaggi

                    snapshotFlow { messagesState.value.size }
                        .distinctUntilChanged()
                        .collect {
                            isLoadingOlderMessages = false  // Nasconde il loader
                        }
                }
            }
    }


    // Effetto per lo scroll iniziale (una sola volta)
    var initialLoadDone by remember { mutableStateOf(false) }
    LaunchedEffect(messagesState.value) {
        if (!initialLoadDone && messagesState.value.isNotEmpty()) {
            kotlinx.coroutines.delay(150) // Delay per l'aggiornamento del layout
            listState.animateScrollToItem(messagesState.value.size - 1)
            initialLoadDone = true
        }
    }

    // Effetto per forzare lo scroll alla fine al momento dell'invio di un nuovo messaggio

    LaunchedEffect(forceScrollToBottom) {
        if (forceScrollToBottom && messagesState.value.isNotEmpty()) {
            kotlinx.coroutines.delay(150) // Aspetta l'aggiornamento del layout
            listState.animateScrollToItem(messagesState.value.size - 1)
            forceScrollToBottom = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (recipientName != null) {
                        Row(
                            modifier = Modifier
                                .clickable {
                                    if (recipientDocId.isNotEmpty()) {
                                        navController.navigate("userProfile/$recipientDocId")
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
                                text = recipientName!!,
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
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = 8.dp // Spazio per l'input
                        // inizialmente 80
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                    // Mostra il loader se stiamo caricando messaggi vecchi
                    if (isLoadingOlderMessages) {
                        item {
                            GenericLoadingScreen(
                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                            )
                        }
                    }

                    if (currentUser != null) {
                        itemsIndexed(messagesState.value, key = { index, message -> message.id }) { index, message ->
                            // Mostra la data se serve
                            val currentSeparator = getDateSeparator(message.timestamp)
                            val previousSeparator = if (index > 0) getDateSeparator(messagesState.value[index - 1].timestamp) else ""
                            if (index == 0 || currentSeparator != previousSeparator) {
                                // Il costruttore DSL consente di chiamare direttamente composable
                                DateSeparatorRow(dateText = currentSeparator)
                            }
                            // Mostra il messaggio
                            val previousMessage = messagesState.value.getOrNull(index - 1)
                            val isFirstInBlock = previousMessage == null || previousMessage.sender != message.sender
                            MessageRow(
                                chatMessage = message,
                                currentUserId = currentUser.uid,
                                showAvatar = isFirstInBlock,
                                onDelete = if (message.sender == currentUser.uid) {
                                    {
                                        messageToDelete = message.id
                                        showDeleteDialog = true
                                    }
                                } else null
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.write_message)) }, // String resource
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send), // Aggiungiamo keyboardOptions
                        keyboardActions = KeyboardActions(onSend = { // Aggiungiamo keyboardActions
                            if (messageText.isNotBlank() && currentUserId != null) {
                                Log.d("ChatScreen", "Testo del messaggio prima dell'invio con Enter: '$messageText'")
                                val messageToSend = messageText
                                messageText = ""
                                forceScrollToBottom = true
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val repository = ChatRepository()
                                        repository.sendMessage(docId, currentUserId, messageToSend)
                                    } catch (e: Exception) {
                                        Log.e("ChatScreen", "Errore nell'invio del messaggio con Enter", e)
                                    }
                                }
                            }
                        })
                    )
                    val uiScope = rememberCoroutineScope()
                    // da dove viene? A cosa serviva? Posso rimuoverlo?

                    Button(
                        onClick = {
                            // lasciamo vuoto onClick per ora
                            if (messageText.isNotBlank() && currentUserId != null) {
                                Log.d("ChatScreen", "Testo del messaggio prima dell'invio con Bottone: '$messageText'")
                                val messageToSend = messageText
                                messageText = ""
                                forceScrollToBottom = true

                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val repository = ChatRepository()
                                        repository.sendMessage(docId, currentUserId, messageToSend)
                                    } catch (e: Exception) {
                                        Log.e("ChatScreen", "Errore nell'invio del messaggio con Bottone", e)
                                    }
                                }
                            }
                        }
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
                    title = { Text("Elimina messaggio") },
                    text = { Text("Vuoi eliminare definitivamente questo messaggio?") },
                    //TODO: Stringabili
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                messageToDelete?.let { id ->
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            val repository = ChatRepository()
                                            repository.deleteMessage(docId, id)
                                        } catch (e: Exception) {
                                            Log.e("ChatScreen", "Errore nell'eliminazione del messaggio", e)
                                        }
                                    }
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