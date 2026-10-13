package com.gentestrana.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gentestrana.R
import com.gentestrana.components.ChatList
import com.gentestrana.ui.theme.LocalDimensions
import com.gentestrana.ui_controller.ChatFilter
import com.gentestrana.ui_controller.ChatListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    navController: NavController,
    viewModel: ChatListViewModel = viewModel()
) {
    val dimensions = LocalDimensions.current
    val chats by viewModel.chats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            placeholder = {
                                Text(
                                    "Cerca per nome o messaggio...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(stringResource(R.string.chat))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (isSearchActive) {
                            isSearchActive = false
                            viewModel.searchQuery.value = ""
                        } else {
                            isSearchActive = true
                        }
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Cerca chat"
                        )
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
                // Filtri stile Tandem: Tutti, Senza risposta, Non letti
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensions.smallPadding, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedFilter == ChatFilter.ALL,
                        onClick = { viewModel.selectedFilter.value = ChatFilter.ALL },
                        label = { Text("Tutti") }
                    )
                    FilterChip(
                        selected = selectedFilter == ChatFilter.UNANSWERED,
                        onClick = { viewModel.selectedFilter.value = ChatFilter.UNANSWERED },
                        label = { Text("Senza risposta") }
                    )
                    FilterChip(
                        selected = selectedFilter == ChatFilter.UNREAD,
                        onClick = { viewModel.selectedFilter.value = ChatFilter.UNREAD },
                        label = { Text("Non letti") }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensions.smallPadding)
                ) {
                    ChatList(
                        chats = chats,
                        onChatClick = { chat ->
                            navController.navigate("chat/${chat.id}")
                        },
                        onChatPin = { chat -> viewModel.togglePin(chat) },
                        onChatDelete = { chat -> viewModel.deleteChat(chat) }
                    )
                }
            }
        }
    )
}
