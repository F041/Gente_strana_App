package com.gentestrana.screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.gentestrana.R
import com.gentestrana.users.UserRepository
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

@Composable
fun GoogleLoginScreen(
    onLoginSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val userRepository = UserRepository()
    val oneTapClient: SignInClient = Identity.getSignInClient(context)

    // Nuova richiesta di autenticazione Google
    val signInRequest = BeginSignInRequest.builder()
        .setGoogleIdTokenRequestOptions(
            BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                .setSupported(true)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(false)
                // Mostra sempre il selettore Google
                .build()
        )
        .build()

    // Launcher per gestire il risultato del login
    val launcher = rememberLauncherForActivityResult(StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
            val idToken = credential.googleIdToken

            if (idToken != null) {
                userRepository.signInWithGoogle(
                    idToken = idToken,
                    onSuccess = {
                        val firebaseUser = FirebaseAuth.getInstance().currentUser
                        if (firebaseUser != null) {
                            userRepository.getUser(
                                docId = firebaseUser.uid,
                                onSuccess = { user ->
                                    if (user.isAdmin) {
                                        FirebaseMessaging.getInstance().subscribeToTopic("adminReports")
                                            .addOnCompleteListener { subscribeTask ->
                                                if (subscribeTask.isSuccessful) {
                                                    Log.d("FCM", "Iscritto a adminReports")
                                                } else {
                                                    Log.e("FCM", "Errore iscrizione topic")
                                                }
                                            }
                                    }
                                    onLoginSuccess()
                                },
                                onFailure = { error ->
                                    Toast.makeText(context, "Errore nel recupero dati utente: $error", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess()
                                }
                            )
                        } else {
                            onError("Utente non trovato dopo il login.")
                        }
                    },
                    onFailure = { error ->
                        onError(error ?: "Authentication failed")
                    }
                )
            } else {
                onError("Google sign-in failed: No ID token")
            }
        } else {
            onError("Google sign-in canceled")
        }
    }

    // UI: Pulsante di login con Google
    Button(onClick = {
        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener { result ->
                launcher.launch(IntentSenderRequest.Builder(result.pendingIntent).build())
            }
            .addOnFailureListener { e ->
                onError("Google sign-in failed: ${e.localizedMessage}")
            }
    }) {
        Text(stringResource(R.string.sign_in_with_google_button))
    }
}
