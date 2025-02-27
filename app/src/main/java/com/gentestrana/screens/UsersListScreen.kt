package com.gentestrana.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.gentestrana.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
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

    val usersState = produceState<List<User>>(initialValue = emptyList()) {
        Firebase.firestore.collection("users")
            .get()
            .addOnSuccessListener { result ->
                hasError.value = false
                value = result.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(docId = doc.id)
                }
            }
            .addOnFailureListener {
                hasError.value = true
            }
    }

    val filteredUsers = usersState.value
        .filter { user ->
            user.username.contains(filterState.value.searchQuery, ignoreCase = true) ||
                    user.topics.any { it.contains(filterState.value.searchQuery, ignoreCase = true) }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NeuroSurface,
                    titleContentColor = NeuroPrimary
                ),
                title = {
                    Column {
                        TextField(
                            value = filterState.value.searchQuery,
                            onValueChange = {
                                filterState.value = filterState.value.copy(searchQuery = it)
                            },
                            placeholder = { Text(stringResource(R.string.users_search_bar))},
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                color = NeuroPrimary
                            )
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .background(NeuroBackground)
        ) {
            if (hasError.value) {
                item { ErrorMessage("Connection error. Please try again.") }
            } else if (filteredUsers.isEmpty()) {
                item { ErrorMessage("No matches found\nTry different keywords") }
            } else {
                items(filteredUsers) { user ->
                    UserProfileCard(
                        user = user,
                        onClick = { navController.navigate("userProfile/${user.docId}") }
                    )
                    Divider(
                        color = NeuroAccent.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorMessage(text: String) {
    Text(
        text = text,
        color = NeuroSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        lineHeight = 24.sp
    )
}