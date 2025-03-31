package com.gentestrana.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gentestrana.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

@Composable
fun VerifyEmailScreen(
    navController: NavHostController // Aggiunto NavController come parametro
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var isResendingEmail by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var isEmailVerified by remember { mutableStateOf(false) }

    val verificationResentMessage = stringResource(R.string.verification_email_resent_success)

    // Polling per verificare lo stato dell'email ogni 3 secondi
    LaunchedEffect(Unit) { // Unit significa che LaunchedEffect verrà eseguito solo una volta all'avvio
        while (true) {
            userRepository.checkEmailVerificationStatus(
                onVerified = {
                    isEmailVerified = true
                    navController.navigate("main") { // "main" è la route per MainTabsScreen in NavGraph
                        popUpTo("verifyEmail") { inclusive = true } // Rimuovi VerifyEmailScreen dalla back stack
                    }
                },
                onNotVerified = {
                    isEmailVerified = false // Anche se dovrebbe essere già false all'inizio
                }
            )
            delay(2400) // Attendi 2,4 secondi prima del prossimo controllo
        }
    }


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
            if (!isEmailVerified) { // Mostra il titolo e le istruzioni solo se l'email NON è verificata
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
            } else { // Altrimenti, mostra un messaggio di successo
                Text(
                    stringResource(R.string.email_verified_success_message),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }


            Spacer(modifier = Modifier.height(24.dp))
            if (!isEmailVerified) {
                // Mostra il pulsante di "Reinvia email" solo se l'email NON è verificata
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
                Button(
                    // Mostra il pulsante "Apri App Email" solo se l'email NON è verificata
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text(stringResource(R.string.open_email_app_button))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    // Mostra il testo "Controlla spam" solo se l'email NON è verificata
                    stringResource(R.string.verify_email_check_spam),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}