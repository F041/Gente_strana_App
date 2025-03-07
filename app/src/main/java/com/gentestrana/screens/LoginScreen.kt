package com.gentestrana.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.gentestrana.R
import com.gentestrana.users.UserRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.messaging

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
                    Toast.makeText(context, context.getString(R.string.login_fill_fields), Toast.LENGTH_SHORT).show();                   return@Button
                }

                isLoading = true // Start loading
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            Toast.makeText(context, context.getString(R.string.login_successful), Toast.LENGTH_SHORT).show()
                            task.result?.user?.let { firebaseUser ->
                                // Recupera i dettagli dell'utente da Firestore per verificare se è admin
                                UserRepository().getUser(
                                    docId = firebaseUser.uid,
                                    onSuccess = { user ->
                                        // Se l'utente è admin, iscrivilo al topic "adminReports"
                                        if (user.isAdmin) {
                                            Firebase.messaging.subscribeToTopic("adminReports")
                                                .addOnCompleteListener { subscribeTask ->
                                                    if (subscribeTask.isSuccessful) {
                                                        Log.d("FCM", "Iscritto con successo alle notifiche adminReports")
                                                    } else {
                                                        Log.e("FCM", "Errore nell'iscrizione al topic")
                                                    }
                                                }
                                        }
                                        // Prosegui con il login
                                        onLoginSuccess(firebaseUser)
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(context, "Errore nel recupero dei dati utente: $error", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess(firebaseUser)
                                    }
                                )
                            }
                        } else {
                            Toast.makeText(context, context.getString(R.string.login_failed, task.exception?.message), Toast.LENGTH_SHORT).show()
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
            Text(context.getString(R.string.register_prompt))
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