package com.gentestrana.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gentestrana.R
import com.gentestrana.components.CompactSearchBar
import com.gentestrana.components.FilterDialog
import com.gentestrana.components.FilterState
import com.gentestrana.components.FilterType
import com.gentestrana.components.GenericLoadingScreen
import com.gentestrana.ui.theme.LocalDimensions
import com.gentestrana.ui_controller.UserListViewModel
import com.gentestrana.users.UserProfileCard
import com.gentestrana.utils.computeUserSimilarity
import com.gentestrana.utils.getCountryIsoFromName
import com.gentestrana.utils.getLanguageName
import com.google.firebase.auth.FirebaseAuth

@Composable
fun UsersListScreen(navController: NavHostController) {
    val viewModel: UserListViewModel = viewModel()
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasError by viewModel.hasError.collectAsState()
    val hasMoreData by viewModel.hasMoreData.collectAsState()

    val context = LocalContext.current
    var filterState by remember { mutableStateOf(FilterState()) }
    var showFilterDialog by remember { mutableStateOf(false) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val dimensions = LocalDimensions.current
    val currentUserProfile by viewModel.currentUserProfile.collectAsState()


    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    Scaffold { paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues)
            .padding(dimensions.smallPadding) // medium non mi piace
            // prima non avevo padding
        ) {
            // Barra di ricerca con filtro
            CompactSearchBar(
                query = filterState.searchQuery,
                onQueryChanged = { newValue -> filterState = filterState.copy(searchQuery = newValue) },
                onFilterClicked = { showFilterDialog = true }
            )

            if (showFilterDialog) {
                FilterDialog(
                    currentFilter = filterState.filterType,
                    currentLanguage = filterState.selectedLanguage,
                    currentLocation = filterState.selectedLocation,
                    supportedLanguages = context.resources.getStringArray(R.array.supported_language_codes)
                        .map { code -> getLanguageName(context, code) },
                    onFilterSelected = { newType, newValue ->
                        filterState = when (newType) {
                            FilterType.LANGUAGE -> filterState.copy(filterType = newType, selectedLanguage = newValue, searchQuery = "")
                            FilterType.LOCATION -> filterState.copy(filterType = newType, selectedLocation = newValue, searchQuery = "")
                            FilterType.SIMILARITY -> filterState.copy(filterType = newType, selectedLanguage = "", selectedLocation = "", searchQuery = "") // Resetta language, location e search
                            FilterType.ALL -> filterState.copy(filterType = newType, selectedLanguage = "", selectedLocation = "", searchQuery = "") // Resetta tutto tranne il tipo
                            else -> filterState.copy(filterType = newType)
                        }
                        showFilterDialog = false
                    },
                    onDismiss = { showFilterDialog = false }
                )
            }

            val filteredUsers by remember(users, filterState, currentUserProfile) {
                derivedStateOf {
                    // Usa il currentUserProfile raccolto dallo StateFlow
                    val profileForSimilarity = currentUserProfile

                    // Se il filtro è SIMILARITY ma il profilo non è ancora caricato,
                    // temporaneamente non filtrare (o mostra una lista vuota/loading).
                    // Qui scegliamo di non filtrare finché il profilo non è pronto.
                    if (filterState.filterType == FilterType.SIMILARITY && profileForSimilarity == null) {
                        Log.d("UserListScreen", "Filtro similarità attivo ma currentUserProfile è null. Lista vuota temporaneamente.")
                        emptyList() // Restituisce lista vuota finché il profilo non è caricato
                    } else {
                        users.filter { user ->
                            // Condizione per escludere il profilo corrente
                            if (currentUserId != null && user.docId == currentUserId) return@filter false

                            // Condizione 1: query di ricerca (se presente, almeno 2 caratteri)
                            val searchMatches = if (filterState.searchQuery.length >= 2) {
                                val query = filterState.searchQuery.lowercase()
                                user.username.lowercase().contains(query) ||
                                        user.topics.any { it.lowercase().contains(query) }
                            } else true

                            // Condizione 2: filtro avanzato
                            val filterMatches = when (filterState.filterType) {
                                FilterType.ALL -> true
                                FilterType.LANGUAGE -> {
                                    // Logica filtro lingua (invariata)
                                    if (filterState.selectedLanguage.isBlank()) true
                                    else user.spokenLanguages.any { langCode ->
                                        getLanguageName(context, langCode).trim().lowercase() ==
                                                filterState.selectedLanguage.trim().lowercase()
                                    }
                                }
                                FilterType.LOCATION -> {
                                    // 1. Ottieni l'ISO code del paese inserito nel filtro (invariato)
                                    val filterCountryIso = getCountryIsoFromName(filterState.selectedLocation) ?: ""
                                    // 2. Estrai il potenziale nome del paese dalla stringa location dell'utente
                                    //    Prende la parte dopo l'ultima virgola (o tutta la stringa se non c'è virgola)
                                    //    e rimuove spazi bianchi iniziali/finali.
                                    val potentialUserCountryName = user.location.substringAfterLast(',', user.location).trim()
                                    // 3. Ottieni l'ISO code del paese estratto dalla location dell'utente
                                    val userCountryIso = getCountryIsoFromName(potentialUserCountryName) ?: ""
                                    // 4. Confronta gli ISO code (assicurati che non siano vuoti)
                                    userCountryIso.isNotBlank() && filterCountryIso.isNotBlank() && userCountryIso == filterCountryIso
                                }
                                FilterType.SIMILARITY -> {
                                    // Logica filtro similarità (USA profileForSimilarity)
                                    if (profileForSimilarity != null) {
                                        val similarity = computeUserSimilarity(profileForSimilarity, user)
                                        Log.d("SIMILARITY_FILTER", "User: ${user.username}, Similarity with ${profileForSimilarity.username}: $similarity")
                                        similarity >= 0.3
                                    // TODO: Soglia di similarità per remote config
                                    //  o configurabile internamente?
                                    } else {
                                        // Se il profilo corrente non è caricato, non applicare il filtro
                                        // (questo caso è gestito all'inizio del derivedStateOf,
                                        // ma lo teniamo qui per sicurezza)
                                        true
                                    }
                                }
                                else -> true
                            }

                            // L'utente viene incluso se soddisfa entrambe le condizioni (AND)
                            searchMatches && filterMatches

                        }.let { filtered ->
                            // Ordinamento finale (incluso quello per similarità)
                            when (filterState.filterType) {
                                FilterType.SIMILARITY -> {
                                    if (profileForSimilarity != null) {
                                        // Ordina per similarità decrescente
                                        filtered.sortedByDescending { user ->
                                            computeUserSimilarity(profileForSimilarity, user)
                                        }
                                    } else {
                                        // Se il profilo non c'è, ordina per lastActive (fallback)
                                        filtered.sortedByDescending { it.lastActive?.toDate()?.time ?: 0L }
                                    }
                                }
                                else -> {
                                    // Ordina per lastActive per gli altri filtri
                                    filtered.sortedByDescending { it.lastActive?.toDate()?.time ?: 0L }
                                }
                            }
                        }
                    }
                }
            }


            // Contatore utenti trovati
            Text(
                text = if (filterState.filterType == FilterType.SIMILARITY && currentUserProfile == null) {
                    stringResource(R.string.loading_default)
                // Messaggio mentre carica il profilo per similarità
                } else if (filterState.searchQuery.length == 1) {
                    "\uD83D\uDD0D️2\uFE0F⃣"
                // Icona se la ricerca è di un solo carattere
                } else {
                    "${filteredUsers.size} ${stringResource(R.string.users_found)}" // Conteggio normale
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            // Lista utenti con caricamento progressivo
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(filteredUsers) { index, user ->
                    UserProfileCard(
                        user = user,
                        onClick = { navController.navigate("userProfile/${user.docId}") }
                    )
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                    // Caricamento progressivo (invariato)
                    if (index == filteredUsers.lastIndex && hasMoreData && !isLoading) {
                        LaunchedEffect(Unit) {
                            Log.d("UserListScreen", "Caricamento prossima pagina...") // Log per debug
                            viewModel.loadUsers()
                        }
                    }
                }

                // Indicatore di caricamento e errore (invariati)
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GenericLoadingScreen(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                if (hasError) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical=16.dp), // Aggiunto padding
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(stringResource(R.string.connection_error)) // Usa string resource
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadUsers() }) {
                                Text(stringResource(R.string.retry_button)) // Usa string resource
                            }
                        }
                    }
                }
            }
        }
    }
}