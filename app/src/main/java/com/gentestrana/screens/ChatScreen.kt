package com.gentestrana.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import kotlinx.coroutines.launch
import android.text.format.DateUtils


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(docId: String, navController: NavController) {
    Log.d("ChatScreen", "docId: $docId")  // <-- Aggiungi questo
    val db = Firebase.firestore
    val currentUser = Firebase.auth.currentUser
    val currentUserId = currentUser?.uid

    // Stato per il nome del destinatario
    var recipientName by remember { mutableStateOf("Unknown") }

    // Recupera il nome del destinatario
    LaunchedEffect(docId, currentUserId) {
        if (currentUserId != null) {
            val chatDocSnapshot = db.collection("chats").document(docId).get().await()
            val participants = chatDocSnapshot.get("participants") as? List<String> ?: emptyList()
            val otherUserId = participants.firstOrNull { it != currentUserId && it.isNotBlank() }
            if (otherUserId != null) {
                val userDoc = db.collection("users").document(otherUserId).get().await()
                recipientName = userDoc.getString("username") ?: "Unknown"
            }
        }
    }

    // Stato per la lista dei messaggi
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
                value = snapshot?.documents?.mapNotNull {
                    it.toObject(ChatMessage::class.java)
                } ?: emptyList()
            }
    }

    // 🔸 Qui il LaunchedEffect per fare il log dei messaggi
    LaunchedEffect(messagesState.value) {
        Log.d("ChatScreen", "Messaggi caricati: ${messagesState.value}")
    }

    // Stato per l'input
    var messageText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipientName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                // LazyColumn con i messaggi
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (currentUser != null) {
                        items(messagesState.value) { message ->
                            MessageRow(chatMessage = message, currentUserId = currentUser.uid)
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
                        placeholder = { Text("Type your message") }
                    )
                    Button(
                        onClick = {
                            if (messageText.isNotBlank() && currentUser != null) {
                                sendMessage(docId, currentUser.uid, messageText)
                                messageText = ""
                            }
                        }
                    ) {
                        Text("Send")
                    }
                }
            }
        }
    )
}



// Caching globale per gli URL delle foto profilo (evita richieste ripetute a Firestore)
private val profilePicCache = mutableMapOf<String, String>()

@Composable
fun MessageRow(chatMessage: ChatMessage, currentUserId: String) {
    val isSentByCurrentUser = chatMessage.sender == currentUserId
    val coroutineScope = rememberCoroutineScope()
    val profilePicUrl = remember { mutableStateOf("") }

    // Recupera l'avatar (con caching, come già implementato)
    LaunchedEffect(chatMessage.sender) {
        if (profilePicCache.containsKey(chatMessage.sender)) {
            profilePicUrl.value = profilePicCache[chatMessage.sender]!!
        } else {
            coroutineScope.launch {
                try {
                    val userDoc = Firebase.firestore.collection("users")
                        .document(chatMessage.sender)
                        .get()
                        .await()

                    val picList = userDoc.get("profilePicUrl") as? List<String>
                    val url = picList?.firstOrNull()
                        ?: "https://icons.veryicon.com/png/o/system/ali-mom-icon-library/random-user.png"

                    profilePicCache[chatMessage.sender] = url
                    profilePicUrl.value = url
                } catch (e: Exception) {
                    profilePicUrl.value = "https://icons.veryicon.com/png/o/system/ali-mom-icon-library/random-user.png"
                }
            }
        }
    }

    // Calcola il timestamp relativo
    val relativeTime = DateUtils.getRelativeTimeSpanString(
        chatMessage.timestamp.toDate().time,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isSentByCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isSentByCurrentUser) {
            AsyncImage(
                model = profilePicUrl.value,
                contentDescription = "Sender avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Bolla del messaggio con Column per messaggio e timestamp
        Box(
            modifier = Modifier
                .background(
                    color = if (isSentByCurrentUser)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = chatMessage.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSentByCurrentUser)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = relativeTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        if (isSentByCurrentUser) {
            Spacer(modifier = Modifier.width(8.dp))
            AsyncImage(
                model = profilePicUrl.value,
                contentDescription = "Sender avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        }
    }
}


fun sendMessage(chatId: String, sender: String, message: String) {
    val messageData = ChatMessage(
        sender = sender,
        message = message,
        timestamp = Timestamp.now()
    )

    Firebase.firestore.collection("chats")
        .document(chatId)
        .collection("messages")
        .add(messageData)
        .addOnSuccessListener {
            Log.i("sendMessage", "Message sent successfully.")
        }
        .addOnFailureListener { e ->
            Log.e("sendMessage", "Failed to send message: ${e.message}")
        }
}

