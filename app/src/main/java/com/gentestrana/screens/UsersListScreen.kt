package com.gentestrana.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gentestrana.R
import com.gentestrana.users.User
import com.gentestrana.components.FilterState
import com.gentestrana.users.UserProfileCard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersListScreen(navController: NavHostController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val profilePicUrls = remember(currentUser) {
        when (val url = currentUser?.photoUrl?.toString()) {
            null -> listOf(R.drawable.random_user)
            else -> listOf(url)
        }
    }
    val filterState = remember { mutableStateOf(FilterState()) }
    val hasError = remember { mutableStateOf(false) }
    val isLoading = remember { mutableStateOf(false) }

    // Stato per il caricamento paginato
    val users = remember { mutableStateListOf<User>() }
    var lastDocumentSnapshot by remember { mutableStateOf<DocumentSnapshot?>(null) }
    val hasMoreData = remember { mutableStateOf(true) }
    val pageSize = 10
    // numbers of users per page, some use 20

    // Funzione per caricare la pagina successiva
    fun loadUsers() {
        if (!hasMoreData.value || isLoading.value) return
        isLoading.value = true
        var query = Firebase.firestore.collection("users").limit(pageSize.toLong())
        lastDocumentSnapshot?.let {
            query = query.startAfter(it)
        }
        query.get()
            .addOnSuccessListener { result ->
                hasError.value = false
                if (result.documents.isNotEmpty()) {
                    lastDocumentSnapshot = result.documents.last()
                    users.addAll(result.documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(docId = doc.id)
                    })
                    if (result.documents.size < pageSize) {
                        hasMoreData.value = false
                    }
                } else {
                    hasMoreData.value = false
                }
                isLoading.value = false
            }
            .addOnFailureListener {
                hasError.value = true
                isLoading.value = false
            }
    }

    // Caricamento iniziale
    LaunchedEffect(Unit) {
        loadUsers()
    }

    // Filtro sugli utenti già caricati
    val filteredUsers = users.filter { user ->
        user.username.contains(filterState.value.searchQuery, ignoreCase = true) ||
                user.topics.any { it.contains(filterState.value.searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Column {
                        TextField(
                            value = filterState.value.searchQuery,
                            onValueChange = {
                                filterState.value = filterState.value.copy(searchQuery = it)
                            },
                            placeholder = { Text(stringResource(R.string.users_search_bar)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues)
        ) {
            itemsIndexed(filteredUsers) { index, user ->
                UserProfileCard(
                    user = user,
                    onClick = { navController.navigate("userProfile/${user.docId}") }
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                // Carica la pagina successiva se si sta visualizzando l'ultimo elemento
                if (index == filteredUsers.lastIndex && hasMoreData.value) {
                    LaunchedEffect(key1 = Unit) { loadUsers() }
                }
            }
            if (isLoading.value) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            if (hasError.value) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ErrorMessage("Connection error. Please try again.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { loadUsers() }) {
                            Text("Riprova")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorMessage(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        lineHeight = 24.sp
    )
}

