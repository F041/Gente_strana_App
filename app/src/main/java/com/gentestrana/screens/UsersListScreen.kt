package com.gentestrana.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gentestrana.R
import com.gentestrana.components.CompactSearchBar
import com.gentestrana.users.User
import com.gentestrana.components.FilterState
import com.gentestrana.components.GenericLoadingScreen
import com.gentestrana.users.UserProfileCard
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun UsersListScreen(navController: NavHostController) {
    var filterState by remember { mutableStateOf(FilterState()) }
    val hasError = remember { mutableStateOf(false) }
    val isLoading = remember { mutableStateOf(false) }
    val users = remember { mutableStateListOf<User>() }
    var lastDocumentSnapshot by remember { mutableStateOf<DocumentSnapshot?>(null) }
    val hasMoreData = remember { mutableStateOf(true) }
    val pageSize = 10

    fun onFilterClicked() {
        // TODO: apri un dialog, naviga a una schermata di filtro, ecc.
    }

    fun loadUsers() {
        if (!hasMoreData.value || isLoading.value) return
        isLoading.value = true

        var query = Firebase.firestore.collection("users").limit(pageSize.toLong())
        lastDocumentSnapshot?.let { query = query.startAfter(it) }

        query.get()
            .addOnSuccessListener { result ->
                hasError.value = false
                if (result.documents.isNotEmpty()) {
                    lastDocumentSnapshot = result.documents.last()
                    users.addAll(result.documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(docId = doc.id)
                    })
                    if (result.documents.size < pageSize) hasMoreData.value = false
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

    // Effettua il primo caricamento quando il composable viene creato
    LaunchedEffect(Unit) {
        loadUsers()
    }

    // Filtra gli utenti in base alla query di ricerca
    val filteredUsers = users.filter { user ->
        user.username.contains(filterState.searchQuery, ignoreCase = true) ||
                user.topics.any { it.contains(filterState.searchQuery, ignoreCase = true) }
    }
    // TODO: mettere almeno due lettere per fare ricerca

    Scaffold { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {

            // Barra di ricerca con filtro
            CompactSearchBar(
                query = filterState.searchQuery,
                onQueryChanged = { newValue ->
                    filterState = filterState.copy(searchQuery = newValue)
                },
                onFilterClicked = { onFilterClicked() }
            )

            // Contatore utenti trovati
            Text(
                text = "${filteredUsers.size} ${stringResource(R.string.users_found)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            // Lista utenti con caricamento progressivo
            LazyColumn(
                modifier = Modifier.fillMaxSize()
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

                    // Caricamento dei dati quando si raggiunge l'ultimo elemento
                    if (index == filteredUsers.lastIndex && hasMoreData.value) {
                        LaunchedEffect(index) { loadUsers() }
                    }
                }

                // Spinner di caricamento
                if (isLoading.value) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GenericLoadingScreen(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                // Messaggio di errore
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



