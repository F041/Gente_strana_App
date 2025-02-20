package com.gentestrana.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

@Composable
fun LoginScreen(
    onLoginSuccess: (FirebaseUser) -> Unit,
    onNavigateToRegistration: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) } // Track loading state

    // Layout principale per il login
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Campo email
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Campo password
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Pulsante per il login via email
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true // Start loading
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        isLoading = false // Stop loading
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                            task.result?.user?.let { onLoginSuccess(it) }
                        } else {
                            Toast.makeText(
                                context,
                                "Authentication failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading // Disable button while loading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Login")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Pulsante per navigare alla schermata di registrazione
        TextButton(onClick = onNavigateToRegistration) {
            Text("Don't have an account? Register")
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Sezione per il login con Google
        Text("Or sign in with Google:")
        Spacer(modifier = Modifier.height(8.dp))
        GoogleLoginScreen(
            onLoginSuccess = {
                // Dopo il login con Google, recupera l'utente corrente e passa alla callback
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    onLoginSuccess(user)
                } else {
                    Toast.makeText(context, "Google authentication failed.", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        )
    }
}