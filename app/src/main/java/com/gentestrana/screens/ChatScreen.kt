package com.gentestrana.screens

import android.util.Log
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(docId: String, navController: NavController) {
    val db = Firebase.firestore
    val currentUser = Firebase.auth.currentUser
    val currentUserId = currentUser?.uid
    var showDeleteDialog by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<String?>(null) }
    var recipientName by remember { mutableStateOf("Nome destinatario") }

    // Blocco unico per recuperare il documento della chat, aggiornare lo stato e ottenere il nome destinatario
    LaunchedEffect(docId, currentUserId) {
        if (currentUserId != null) {
            try {
                val chatDocSnapshot = db.collection("chats").document(docId).get().await()
                val participants = chatDocSnapshot.get("participants") as? List<String> ?: emptyList()
                val otherUserId = participants.firstOrNull { it != currentUserId && it.isNotBlank() }
                if (otherUserId != null) {
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
    val messagesState = produceState<List<ChatMessage>>(initialValue = emptyList()) {
        db.collection("chats")
            .document(docId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatScreen", "Error fetching messages: ${e.message}")
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("ChatScreen", "Error parsing message ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                value = messages
            }
    }

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messagesState.value.size) {
        if (messagesState.value.isNotEmpty()) {
            listState.animateScrollToItem(messagesState.value.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipientName) },
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
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (currentUser != null) {
                        messagesState.value.forEachIndexed { index, message ->
                            val currentSeparator = getDateSeparator(message.timestamp)
                            val previousSeparator = if (index > 0) getDateSeparator(messagesState.value[index - 1].timestamp) else ""
                            if (index == 0 || currentSeparator != previousSeparator) {
                                item {
                                    DateSeparatorRow(dateText = currentSeparator)
                                }
                            }
                            val previousMessage = messagesState.value.getOrNull(index - 1)
                            val isFirstInBlock = previousMessage == null || previousMessage.sender != message.sender
                            item {
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
                        placeholder = { Text("Scrivi un messaggio") }
                    )
                    Button(
                        onClick = {
                            if (messageText.isNotBlank() && currentUser != null) {
                                Log.d("ChatScreen", "Testo del messaggio prima dell'invio: '$messageText'")
                                val messageToSend = messageText // Catturiamo il valore attuale
                                messageText = "" // Ora resettiamo il campo
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val repository = ChatRepository()
                                        repository.sendMessage(docId, currentUser.uid, messageToSend)
                                    } catch (e: Exception) {
                                        Log.e("ChatScreen", "Errore nell'invio del messaggio", e)
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Invia messaggio"
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
