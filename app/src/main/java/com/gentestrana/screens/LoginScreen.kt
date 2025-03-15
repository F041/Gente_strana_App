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
import androidx.navigation.NavHostController
import com.gentestrana.R
import com.gentestrana.users.UserRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.messaging.messaging


@Composable
fun LoginScreen(
    onLoginSuccess: (FirebaseUser) -> Unit,
    onNavigateToRegistration: () -> Unit,
    navController: NavHostController
) {
    val context = LocalContext.current

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
                    Toast.makeText(context, context.getString(R.string.login_fill_fields), Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true // Avvia il caricamento

                // Utilizziamo il metodo loginAndCheckEmail di UserRepository
                UserRepository().loginAndCheckEmail(
                    email = email,
                    password = password,
                    onVerified = {
                        // Se l'email è verificata, procediamo recuperando i dettagli utente
                        val firebaseUser = FirebaseAuth.getInstance().currentUser
                        if (firebaseUser != null) {
                            UserRepository().getUser(
                                docId = firebaseUser.uid,
                                onSuccess = { user ->
                                    // Se l'utente è admin, sottoscrivilo al topic "adminReports"
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
                                    isLoading = false
                                    // Procedi con il login completo
                                    onLoginSuccess(firebaseUser)
                                },
                                onFailure = { error ->
                                    isLoading = false
                                    Toast.makeText(context, "Errore nel recupero dei dati utente: $error", Toast.LENGTH_SHORT).show()
                                    // Anche in caso di errore, passiamo l'utente per continuare
                                    onLoginSuccess(firebaseUser)
                                }
                            )
                        } else {
                            isLoading = false
                            Toast.makeText(context, context.getString(R.string.login_failed, "Utente non trovato"), Toast.LENGTH_SHORT).show()
                        }
                    },
                    onNotVerified = {
                        isLoading = false
                        Toast.makeText(context, "Email non verificata. Controlla la tua casella email.", Toast.LENGTH_SHORT).show()

                        val user = FirebaseAuth.getInstance().currentUser
                        user?.sendEmailVerification()
                            ?.addOnSuccessListener {
                                Toast.makeText(context, "Email di verifica inviata nuovamente.", Toast.LENGTH_SHORT).show()
                            }
                            ?.addOnFailureListener { e ->
                                Toast.makeText(context, "Errore nel rinvio dell'email: ${e.message}", Toast.LENGTH_SHORT).show()
                            }

                        navController.navigate("verifyEmail")
                    },
                    onFailure = { error ->  // ✅ Aggiunto parametro onFailure
                        isLoading = false
                        Toast.makeText(context, "Errore di login: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading // Disabilita il pulsante durante il caricamento
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                // TODO: cambiabile con GenericLoadingScreen?
            } else {
                Text("Login") // TODO: stringabile
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