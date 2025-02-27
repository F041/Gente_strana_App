package com.gentestrana.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.gentestrana.R
import com.gentestrana.users.UserRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

// come portare nome su google account come Username-firstname?
@Composable
fun GoogleLoginScreen(
    onLoginSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val userRepository = UserRepository()
    // Configura Google Sign-In:
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()
    val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    // Launcher per gestire il risultato del flusso Google Sign-In.
    val launcher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // Recupera l'account Google e il relativo ID token.
                val account: GoogleSignInAccount = task.getResult(Exception::class.java)!!
                userRepository.signInWithGoogle(
                    idToken = account.idToken ?: "",
                    onSuccess = onLoginSuccess,
                    onFailure = { error -> onError(error ?: "Authentication failed") }
                )
            } catch (e: Exception) {
                onError("Google sign in failed: ${e.localizedMessage}")
            }
        } else {
            onError("Google sign in canceled")
        }
    }

    // UI: Pulsante per avviare il flusso di Google Sign-In.
    Button(onClick = {
        val signInIntent: Intent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }) {
        Text("Sign in with Google")
        // stringabile
    }
}
