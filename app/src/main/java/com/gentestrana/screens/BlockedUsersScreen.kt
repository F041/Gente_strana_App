package com.gentestrana.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gentestrana.R
import com.gentestrana.components.BlockedUserListItem
import com.gentestrana.components.GenericLoadingScreen // Importa il loader generico
import com.gentestrana.ui_controller.BlockedUsersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(
    navController: NavController, // NavController per tornare indietro
    viewModel: BlockedUsersViewModel = viewModel() // Inietta il ViewModel
) {
    val blockedUsers by viewModel.blockedUsers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.blocked_users_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { // Azione per tornare indietro
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // --- CASO 1: Caricamento in corso ---
                isLoading -> {
                    GenericLoadingScreen(modifier = Modifier.align(Alignment.Center))
                }
                // --- CASO 2: Errore ---
                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = error!!, // Mostra il messaggio di errore
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.retryListenerRegistration()  }) { // Bottone Riprova
                            Text(stringResource(R.string.retry_button))
                        }
                    }
                }
                // --- CASO 3: Lista vuota ---
                blockedUsers.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_blocked_users),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                // --- CASO 4: Lista utenti bloccati ---
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp) // Aggiunge un po' di spazio sopra/sotto
                    ) {
                        items(
                            items = blockedUsers,
                            key = { user -> user.docId } // Chiave univoca per ogni elemento
                        ) { user ->
                            BlockedUserListItem(
                                user = user,
                                onUnblockClick = { userId ->
                                    viewModel.unblockUser(userId) // Chiama la funzione del ViewModel
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) // Separatore
                        }
                    }
                }
            }
        }
    }
}