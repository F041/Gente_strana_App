package com.gentestrana.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gentestrana.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun VerifyEmailScreen() {
    val snackbarHostState = remember { SnackbarHostState() }
    var isResendingEmail by remember { mutableStateOf(false) }

    val verificationResentMessage = stringResource(R.string.verification_email_resent_success) // STRINGA OTTENUTA QUI

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Email,
                contentDescription = stringResource(R.string.email_verification_icon_description),
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.verify_email_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.verify_email_instructions),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    isResendingEmail = true
                    userRepository.sendVerificationEmail(
                        onSuccess = {
                            isResendingEmail = false
                            CoroutineScope(Dispatchers.Main).launch {
                                snackbarHostState.showSnackbar(
                                    message = verificationResentMessage,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        },
                        onFailure = { error ->
                            isResendingEmail = false
                            CoroutineScope(Dispatchers.Main).launch {
                                snackbarHostState.showSnackbar(
                                    message = error,
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    )
                },
                enabled = !isResendingEmail
            ) {
                if (isResendingEmail) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(stringResource(R.string.resend_verification_email_button))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.verify_email_check_spam),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }
    }
}