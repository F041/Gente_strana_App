package com.gentestrana


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseUser

@Composable
fun HomeScreen(user: FirebaseUser?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),  // ✅ Fixed padding issue
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome, ${user?.email ?: "User"}!")
    }
}
