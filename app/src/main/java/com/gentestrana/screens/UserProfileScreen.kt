@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.gentestrana.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.compose.ui.res.stringResource
import com.gentestrana.R
import com.gentestrana.users.User
import com.gentestrana.utils.computeAgeFromTimestamp
import kotlinx.coroutines.launch // Import per CoroutineScope
import com.gentestrana.users.*
import com.google.firebase.auth.FirebaseAuth

@Composable
fun UserProfileScreen(
    docId: String,
    navController: NavHostController
) {
    val context = LocalContext.current
    val firestore = Firebase.firestore

    val userState = produceState<User?>(initialValue = null) {
        firestore.collection("users").document(docId).get()
            .addOnSuccessListener { document ->
                value = document.toObject(User::class.java)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    if (userState.value == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val user = userState.value!!
    val descriptionItems = user.description
    val scrollState = rememberLazyListState()
    var currentDescriptionIndex by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(scrollState.firstVisibleItemScrollOffset) {
        val firstVisible = scrollState.firstVisibleItemIndex
        if (firstVisible != currentDescriptionIndex) {
            currentDescriptionIndex = firstVisible
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.user_profile)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Nome ed età
            Text(
                text = "${user.username}, ${computeAgeFromTimestamp(user.birthTimestamp)}",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            // Galleria immagini
            UserPicsGallery(
                imageUrls = user.profilePicUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                imageSize = 140
            )

            // Descrizione swipeabile
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentDescriptionIndex > 0) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                scrollState.animateScrollToItem(currentDescriptionIndex - 1)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = stringResource(R.string.previous),
                                modifier = Modifier
                                    .rotate(180f)
                                    .padding(8.dp)
                            )
                        }
                    }

                    LazyRow(
                        state = scrollState,
                        flingBehavior = rememberSnapFlingBehavior(lazyListState = scrollState),
                        modifier = Modifier.width(200.dp)
                    ) {
                        items(descriptionItems.size) { index ->
                            Text(
                                text = descriptionItems[index],
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    if (currentDescriptionIndex < descriptionItems.lastIndex) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                scrollState.animateScrollToItem(currentDescriptionIndex + 1)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = stringResource(R.string.next),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

            // Sezione informazioni
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = user.bio,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.languages_spoken),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                user.spokenLanguages.forEach { code ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "${getFlagEmoji(code)} ${getLanguageName(code)}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val otherUserId = user.docId
                    val chatId = if (currentUserId < otherUserId) {
                        "$currentUserId-$otherUserId"
                    } else {
                        "$otherUserId-$currentUserId"
                    }

                    val firestore = Firebase.firestore
                    val chatDocRef = firestore.collection("chats").document(chatId)

                    // Check if the chat document exists
                    chatDocRef.get()
                        .addOnSuccessListener { document ->
                            if (!document.exists()) {
                                // Log for document creation
                                println("Creating new chat document: $chatId")
                                val initialChatData = hashMapOf(
                                    "participants" to listOf(currentUserId, otherUserId),
                                    "createdAt" to com.google.firebase.Timestamp.now()
                                )
                                chatDocRef.set(initialChatData)
                                    .addOnSuccessListener {
                                        println("Chat document created successfully!")
                                        navController.navigate("chat/$chatId")
                                    }
                                    .addOnFailureListener { e ->
                                        println("Error creating chat document: ${e.message}")
                                        e.printStackTrace()
                                    }
                            } else {
                                println("Chat document already exists, navigating to chat!")
                                navController.navigate("chat/$chatId")
                            }
                        }
                        .addOnFailureListener { e ->
                            println("Error fetching chat document: ${e.message}")
                            e.printStackTrace()
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(stringResource(R.string.chat_with_user, user.username))
            }


        }
    }
}

fun getFlagEmoji(code: String): String = when (code.lowercase()) {
    "it" -> "🇮🇹"
    "en" -> "🇬🇧"
    "es" -> "🇪🇸"
    "fr" -> "🇫🇷"
    "de" -> "🇩🇪"
    else -> "🌍"
}

fun getLanguageName(code: String): String = when (code.lowercase()) {
    "it" -> "Italiano"
    "en" -> "English"
    "es" -> "Español"
    "fr" -> "Français"
    "de" -> "Deutsch"
    else -> code
}