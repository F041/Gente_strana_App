package com.gentestrana.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gentestrana.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/**
 * Schermata che gestisce il reset della password tramite oobCode
 * ricevuto dal deep link della email di reset.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    oobCode: String,
    navController: NavController
) {
    val context = LocalContext.current
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var operationResult by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reset_password_screen_title)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!isSuccess) {
                Text(
                    text = stringResource(R.string.reset_password_instruction),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.new_password_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.confirm_new_password_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (newPassword.length < 6) {
                            operationResult = context.getString(R.string.error_password_min_length)
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            operationResult = context.getString(R.string.error_passwords_dont_match)
                            return@Button
                        }

                        isLoading = true
                        operationResult = null

                        Log.d("ResetPassword", "Tentativo reset con oobCode: $oobCode")

                        Firebase.auth.confirmPasswordReset(oobCode, newPassword)
                            .addOnSuccessListener {
                                isLoading = false
                                isSuccess = true
                                operationResult = context.getString(R.string.reset_password_success)
                                Log.d("ResetPassword", "Reset password completato con successo")
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                val msg = when {
                                    e.message?.contains("INVALID_OOB_CODE") == true ->
                                        context.getString(R.string.reset_password_link_invalid)
                                    e.message?.contains("EXPIRED_OOB_CODE") == true ->
                                        context.getString(R.string.reset_password_link_expired)
                                    else -> context.getString(R.string.error_prefix, e.localizedMessage)
                                }
                                operationResult = msg
                                Log.e("ResetPassword", "Errore reset password", e)
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && newPassword.isNotBlank() && confirmPassword.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.reset_password_button))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            operationResult?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSuccess)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = if (isSuccess)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                if (isSuccess) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            navController.navigate("auth") {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.go_to_login))
                    }
                }
            }
        }
    }
}