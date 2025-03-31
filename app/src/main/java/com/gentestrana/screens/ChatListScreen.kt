package com.gentestrana.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.gentestrana.components.ChatList
import com.gentestrana.ui_controller.ChatListViewModel
import com.gentestrana.ui.theme.LocalDimensions
import com.gentestrana.R
// lasciamo che n'si sa mai

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    navController: NavController,
    viewModel: ChatListViewModel = viewModel()
// Utilizza viewModel() invece di hiltViewModel()
// hiltViewModel() dava problemi, non la prima volta...
) {
    val dimensions = LocalDimensions.current
    // Osserva lo stato delle chat dal ViewModel
    val chats by viewModel.chats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("chat") })
                 //stringabile ma non ha molto senso
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(dimensions.smallPadding)
            ) {
                ChatList(chats = chats, onChatClick = { chat ->
                    navController.navigate("chat/${chat.id}")
                })
            }
        }
    )
}
