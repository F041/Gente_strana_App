package com.gentestrana.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.gentestrana.chat.ChatMessage
import kotlinx.coroutines.tasks.await
import androidx.compose.material3.Text
import com.gentestrana.chat.MessageRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.ui.res.stringResource
import com.gentestrana.R
import com.gentestrana.chat.ChatRepository
import com.gentestrana.chat.DateSeparatorRow
import com.gentestrana.utils.getDateSeparator
import com.google.firebase.firestore.DocumentChange
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(docId: String, navController: NavController) {
    // Log.d("ChatScreen", "docId: $docId")  // Log per debug
    val db = Firebase.firestore
    val currentUser = Firebase.auth.currentUser
    val currentUserId = currentUser?.uid
    var showDeleteDialog by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<String?>(null) }

    // Stato per il nome del destinatario
    var recipientName by remember { mutableStateOf("Nome destinatario") }

    LaunchedEffect(docId, currentUserId) {
        if (currentUserId != null) {
            try {
                val repository = ChatRepository()

                // 1. Ottieni i partecipanti alla chat
                val chatDoc = db.collection("chats").document(docId).get().await()
                val participants = chatDoc.get("participants") as? List<String> ?: emptyList()

                // 2. Identifica l'altro utente
                val otherUserId = participants.firstOrNull { it != currentUserId }

                if (otherUserId != null) {
                    // 3. Marca i messaggi come DELIVERED
                    repository.markMessagesAsDelivered(docId, currentUserId)


                    // 6. Aggiorna l'UI in tempo reale
                    db.collection("chats/$docId/messages")
                        .whereEqualTo("sender", otherUserId)
                        .addSnapshotListener { snapshot, _ ->
                            snapshot?.documentChanges?.forEach { change ->
                                if (change.type == DocumentChange.Type.MODIFIED) {
                                    val newStatus = change.document.getString("status")
                                    Log.d("StatusUpdate", "Nuovo stato: $newStatus")
                                }
                            }
                        }
                }
            } catch (e: Exception) {
                Log.e("ChatScreen", "Errore aggiornamento stato messaggi", e)
            }
        }
    }

    // Recupero ottimizzato del nome del destinatario
    LaunchedEffect(docId, currentUserId) {
        if (currentUserId != null) {
            try {
                val chatDocSnapshot = db.collection("chats").document(docId).get().await()
                val participants = chatDocSnapshot.get("participants") as? List<String> ?: emptyList()
                val otherUserId = participants.firstOrNull { it != currentUserId && it.isNotBlank() }
                if (otherUserId != null) {
                    val userDoc = db.collection("users").document(otherUserId).get().await()
                    recipientName = userDoc.getString("username") ?: "Sconosciuto"
                }
                // bestiale sto pezzo
                if (otherUserId != null) {
                    // Controlla se il current user è il destinatario
                    val isRecipient = participants.any {
                        it == currentUserId && it != otherUserId
                    }

                    if (isRecipient) {
                        val repository = ChatRepository()
                        repository.markMessagesAsRead(
                            chatId = docId,
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e("ChatScreen", "Errore nel recupero del destinatario: ${e.message}")
            }
        }
    }


    // Stato per la lista dei messaggi
// In ChatScreen.kt
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

                // Forza il refresh dello stato della chat
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

    // Log per debug dei messaggi caricati
    LaunchedEffect(messagesState.value) {
        Log.d("ChatScreen", "Messaggi caricati: ${messagesState.value}")
    }

    // Stato per l'input
    var messageText by remember { mutableStateOf("") }

    // Crea un LazyListState per gestire lo scroll
    val listState = rememberLazyListState()

    // Effetto per scrollare all'ultimo messaggio quando la lista cambia
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
                // LazyColumn con i messaggi e scroll automatico
                LazyColumn(
                    state = listState, // il tuo LazyListState per lo scroll automatico
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp), // space from borders of the screen
                verticalArrangement = Arrangement.spacedBy(4.dp) // spazio fra messaggi, verticale
                ) {
                    if (currentUser != null) {
                        // Itera sui messaggi con forEachIndexed per avere l'indice
                        messagesState.value.forEachIndexed { index, message ->
                            // Ottieni il separatore di data per il messaggio corrente
                            val currentSeparator = getDateSeparator(message.timestamp)
                            // Ottieni il separatore di data del messaggio precedente (se esiste)
                            val previousSeparator = if (index > 0) getDateSeparator(messagesState.value[index - 1].timestamp) else ""
                            // Se è il primo messaggio o se il separatore cambia, aggiungi una riga separatrice
                            if (index == 0 || currentSeparator != previousSeparator) {
                                item {
                                    DateSeparatorRow(dateText = currentSeparator)
                                }
                            }
                            // Determina se il messaggio è il primo del blocco per mostrare l'avatar
                            val previousMessage = messagesState.value.getOrNull(index - 1)
                            val isFirstInBlock = previousMessage == null || previousMessage.sender != message.sender
                            // Mostra il messaggio
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
                                    } else null // Passa null per i messaggi altrui
                                )
                            }
                        }
                    }
                }

                // Sezione input
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
                        placeholder = { Text(stringResource(R.string.write_message))}
                    )
                    Button(
                        onClick = {
                            if (messageText.isNotBlank() && currentUser != null)
                            {
                                sendMessage(docId, currentUser.uid, messageText)
                                messageText = ""
                            }
                        }
                    ) {
                        Icon( // Sostituisci Text con Icon
                            imageVector = Icons.AutoMirrored.Filled.Send, // Usa l'icona Send (Filled)
                            contentDescription = "Invia messaggio" // Descrizione per accessibilità
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
                                    deleteMessage(docId, id)
                                }
                                messageToDelete = null
                            }
                        ) {
                            Text("Conferma")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                messageToDelete = null
                            }
                        ) {
                            Text("Annulla")
                        }
                    }
                )
            }
        }

    )
}

fun sendMessage(chatId: String, sender: String, message: String) {
    val db = Firebase.firestore
    val messageData = hashMapOf(
        "sender" to sender,
        "message" to message,
        "timestamp" to Timestamp.now(),
        "status" to "SENT"  // Vediamo se così ste regole funzionano...
    )
    // Log dei campi inviati
    Log.d("SEND_DEBUG", "Campi inviati: ${messageData.keys.joinToString()}")

    db.collection("chats")
        .document(chatId)
        .collection("messages")
        .add(messageData)
        .addOnFailureListener { e ->
            Log.e("ChatScreen", "Errore durante l'invio del messaggio", e)
        }
}

fun deleteMessage(chatId: String, messageId: String) {
    val db = Firebase.firestore
    db.collection("chats")
        .document(chatId)
        .collection("messages")
        .document(messageId)
        .delete()
        .addOnSuccessListener {
            // To delete with unit test
            Log.d("ChatScreen", "Messaggio eliminato con successo")
        }
        .addOnFailureListener { e ->
            // To delete with unit test
            Log.e("ChatScreen", "Errore eliminazione messaggio", e)
        }
}


