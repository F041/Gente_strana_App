// File: \Gentestrana\app\src\main\java\com\gentestrana\screens\LoginScreen.kt

package com.gentestrana.screens

import android.Manifest
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
    var isLoading by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Log.d("LoginScreen", "Permesso notifiche concesso dopo il login.")
            } else {
                Log.w("LoginScreen", "Permesso notifiche negato dopo il login.")
            }
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                onLoginSuccess(user)
            }
        }
    )

    fun handleLoginFlow(user: FirebaseUser) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onLoginSuccess(user)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Campo email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Campo password — oscurata con PasswordVisualTransformation
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, context.getString(R.string.login_fill_fields), Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true

                UserRepository().loginAndCheckEmail(
                    email = email,
                    password = password,
                    onVerified = {
                        val firebaseUser = FirebaseAuth.getInstance().currentUser
                        if (firebaseUser != null) {
                            UserRepository().getUser(
                                docId = firebaseUser.uid,
                                onSuccess = { user ->
                                    if (user.isAdmin) {
                                        Firebase.messaging.subscribeToTopic("adminReports")
                                            .addOnCompleteListener { /* ... */ }
                                    }
                                    isLoading = false
                                    handleLoginFlow(firebaseUser)
                                },
                                onFailure = { error ->
                                    isLoading = false
                                    Toast.makeText(context, "Errore nel recupero dei dati utente: $error", Toast.LENGTH_SHORT).show()
                                    handleLoginFlow(firebaseUser)
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
                        user?.sendEmailVerification()?.addOnSuccessListener {
                            Toast.makeText(context, "Email di verifica inviata nuovamente.", Toast.LENGTH_SHORT).show()
                        }?.addOnFailureListener { e ->
                            Toast.makeText(context, "Errore nel rinvio dell'email: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        navController.navigate("verifyEmail")
                    },
                    onFailure = { error ->
                        isLoading = false
                        Toast.makeText(context, "Errore di login: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Login")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToRegistration) {
            Text(context.getString(R.string.register_prompt))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Or sign in with Google:")
        Spacer(modifier = Modifier.height(8.dp))
        GoogleLoginScreen(
            onLoginSuccess = {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    handleLoginFlow(user)
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