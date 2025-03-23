package com.gentestrana.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gentestrana.R
import com.gentestrana.components.CompactSearchBar
import com.gentestrana.components.FilterDialog
import com.gentestrana.users.User
import com.gentestrana.components.FilterState
import com.gentestrana.components.FilterType
import com.gentestrana.components.GenericLoadingScreen
import com.gentestrana.users.UserProfileCard
import com.gentestrana.utils.getLanguageName
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth

@Composable
fun UsersListScreen(navController: NavHostController) {
    var filterState by remember { mutableStateOf(FilterState()) }
    val hasError = remember { mutableStateOf(false) }
    val isLoading = remember { mutableStateOf(false) }
    val users = remember { mutableStateListOf<User>() }
    var lastDocumentSnapshot by remember { mutableStateOf<DocumentSnapshot?>(null) }
    val hasMoreData = remember { mutableStateOf(true) }
    val pageSize = 10
    var showEmojiHint by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(filterState.searchQuery) {
        showEmojiHint = filterState.searchQuery.isNotEmpty() && filterState.searchQuery.length < 2
    }

    fun onFilterClicked() {
        showFilterDialog = true
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
            if (showFilterDialog) {
                FilterDialog(
                    currentFilter = filterState.filterType,
                    currentLanguage = filterState.selectedLanguage,
                    currentLocation = filterState.selectedLocation,
                    supportedLanguages = context.resources.getStringArray(R.array.supported_language_codes)
                        .map { code -> getLanguageName(context, code) },
                    onFilterSelected = { newType, newValue ->
                        filterState = when(newType) {
                            FilterType.LANGUAGE -> filterState.copy(
                                filterType = newType,
                                selectedLanguage = newValue,
                                searchQuery = ""  // Resetta la ricerca testuale
                            )
                            FilterType.LOCATION -> filterState.copy(
                                filterType = newType,
                                selectedLocation = newValue,
                                searchQuery = ""  // Resetta la ricerca testuale
                            )
                            else -> filterState.copy(filterType = newType)
                        }
                        showFilterDialog = false
                    },
                    onDismiss = { showFilterDialog = false }
                )
            }
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            // Unico filtro che applica sia la ricerca che il filtro in base a FilterType
            val filteredUsers = remember(users, filterState) {
                // Se non ci sono utenti o il caricamento è in corso, mostra tutto
                if (users.isEmpty()) {
                    users
                } else {
                    users.filter { user ->
                        // Filtro per escludere l'utente corrente
                        if (user.docId == currentUserId) return@filter false

                        // Applica i filtri in base al tipo selezionato
                        when (filterState.filterType) {
                            FilterType.ALL -> {
                                // Se c'è una query di ricerca, applica il filtro
                                if (filterState.searchQuery.length >= 2) {
                                    val query = filterState.searchQuery.lowercase()
                                    user.username.lowercase().contains(query) ||
                                            user.topics.any { it.lowercase().contains(query) }
                                } else {
                                    // Se non c'è query, mostra tutti
                                    true
                                }
                            }

                            FilterType.LANGUAGE -> {
                                // Filtra per lingua selezionata
                                user.spokenLanguages.any { langCode ->
                                    getLanguageName(context, langCode).equals(
                                        filterState.selectedLanguage,
                                        ignoreCase = true
                                    )
                                }
                            }

                            FilterType.LOCATION -> {
                                // Filtra per posizione selezionata
                                user.location?.equals(filterState.selectedLocation, ignoreCase = true) ?: false
                            }

                            else -> true
                        }
                    }
                        .sortedByDescending { it.lastActive?.toDate()?.time ?: 0L }
                }
            }


            // Contatore utenti trovati (usa la lista ordinata)
            Text(
                text = if (showEmojiHint) "\uD83D\uDD0D2\uFE0F⃣" else "${filteredUsers.size} ${stringResource(R.string.users_found)}",
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


// TODO: spostabile altrove
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