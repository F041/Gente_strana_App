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
                            else -> filterState.copy(filterType = newType)
                        }
                        showFilterDialog = false
                    },
                    onDismiss = { showFilterDialog = false }
                )
            }

            val filteredUsers by remember {
                derivedStateOf {
                    users.filter { user ->
                        // Condizione per escludere il profilo corrente
                        if (currentUserId != null && user.docId == currentUserId) return@filter false

                        // Condizione 1: query di ricerca (se presente, almeno 2 caratteri)
                        val searchMatches = if (filterState.searchQuery.length >= 2) {
                            val query = filterState.searchQuery.lowercase()
                            user.username.lowercase().contains(query) ||
                                    user.topics.any { it.lowercase().contains(query) }
                        } else true

                        // Condizione 2: filtro avanzato per lingua/locazione
                        val filterMatches = when (filterState.filterType) {
                            FilterType.ALL -> true
                            FilterType.LANGUAGE -> {
                                // Se il filtro per lingua è impostato, verifica la condizione
                                if (filterState.selectedLanguage.isBlank()) true
                                else user.spokenLanguages.any { langCode ->
                                    getLanguageName(context, langCode).trim().lowercase() ==
                                            filterState.selectedLanguage.trim().lowercase()
                                }
                            }
                            FilterType.LOCATION -> {
                                // Supponendo di aver normalizzato come descritto, oppure usando direttamente il confronto
                                val userCountryIso = getCountryIsoFromName(user.location) ?: ""
                                val filterCountryIso = getCountryIsoFromName(filterState.selectedLocation) ?: ""
                                userCountryIso == filterCountryIso
                            }
                            else -> true
                        }

                        // L'utente viene incluso se soddisfa entrambe le condizioni (AND)
                        searchMatches && filterMatches


                }.sortedByDescending { it.lastActive?.toDate()?.time ?: 0L }
                }
            }


            // Contatore utenti trovati
            Text(
                text = if (filterState.searchQuery.length in 1..1) "\uD83D\uDD0D2\uFE0F⃣"
                else "${filteredUsers.size} ${stringResource(R.string.users_found)}",
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

                    if (index == filteredUsers.lastIndex && hasMoreData) {
                        LaunchedEffect(index) { viewModel.loadUsers() }
                    }
                }

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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Connection error. Please try again.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadUsers() }) {
                                Text("Riprova")
                            }
                        }
                    }
                }
            }
        }
    }
}


