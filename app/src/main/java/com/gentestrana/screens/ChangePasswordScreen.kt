package com.gentestrana.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gentestrana.R
import com.gentestrana.components.GenericLoadingScreen
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavController) {
    val context = LocalContext.current
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.change_password)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!isSuccess) {
                Text(
                    stringResource(R.string.change_password_instructions),
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text(stringResource(R.string.current_password_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.new_password_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmNewPassword,
                    onValueChange = { confirmNewPassword = it },
                    label = { Text(stringResource(R.string.confirm_new_password_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (currentPassword.isBlank() || newPassword.isBlank() || confirmNewPassword.isBlank()) {
                            message = context.getString(R.string.error_fill_all_fields)
                            return@Button
                        }
                        if (newPassword.length < 6) {
                            message = context.getString(R.string.error_password_min_length)
                            return@Button
                        }
                        if (newPassword != confirmNewPassword) {
                            message = context.getString(R.string.error_passwords_dont_match)
                            return@Button
                        }

                        isLoading = true
                        message = ""

                        val user = FirebaseAuth.getInstance().currentUser ?: run {
                            isLoading = false
                            message = context.getString(R.string.error_user_not_authenticated)
                            return@Button
                        }
                        val email = user.email ?: run {
                            isLoading = false
                            message = context.getString(R.string.error_email_not_found)
                            return@Button
                        }

                        val credential = EmailAuthProvider.getCredential(email, currentPassword)
                        user.reauthenticate(credential)
                            .addOnSuccessListener {
                                user.updatePassword(newPassword)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        isSuccess = true
                                        message = context.getString(R.string.change_password_success)
                                        Log.d("ChangePassword", "Password cambiata con successo")
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        message = context.getString(R.string.error_prefix, e.localizedMessage)
                                        Log.e("ChangePassword", "Errore updatePassword", e)
                                    }
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                message = when {
                                    e.message?.contains("ERROR_WRONG_PASSWORD") == true ->
                                        context.getString(R.string.error_wrong_password)
                                    else -> context.getString(R.string.error_prefix, e.localizedMessage)
                                }
                                Log.e("ChangePassword", "Errore reauthentication", e)
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        GenericLoadingScreen(color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(stringResource(R.string.change_password_button))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (message.isNotEmpty()) {
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
                            MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }

                if (isSuccess) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.back_to_settings))
                    }
                }
            }
        }
    }
}