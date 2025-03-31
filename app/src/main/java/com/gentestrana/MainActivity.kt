package com.gentestrana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.gentestrana.ui.theme.GenteStranaTheme
import android.content.Intent
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.gentestrana.screens.AppTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.ContextCompat
import com.gentestrana.ui.theme.LocalAppTheme
import com.gentestrana.utils.forceTokenRefreshIfNeeded
import com.google.firebase.auth.FirebaseAuth
import com.gentestrana.utils.updateUserLastActive
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()

        // Gestione Deep Link all'avvio
        val intent = intent // Intent che ha avviato MainActivity
        val action = intent?.action
        val data = intent?.data

        if (action == Intent.ACTION_VIEW && data != null) {
            // Se l'app è stata avviata tramite un Intent ACTION_VIEW (Deep Link)
            if (data.scheme == "gentestrana" && data.host == "verifyemail") {
                // È un deep link per la verifica email!
                val token = data.getQueryParameter("token") // Estrai il token (se presente)
                Log.d("DeepLink", "Token di verifica: $token") // Log di debug
                // TODO: Inviare il token al backend per la verifica email
                // e NAVIGARE alla schermata di successo/login
            }
        }

        setContent {
            // Ottieni il contesto
            val context = LocalContext.current
            // SharedPreferences per leggere il tema salvato
            val sharedPreferences = remember {
                context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            }
            val authState by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
            val appThemeKey = "app_theme"



            // Leggi il tema salvato da SharedPreferences (come stato ricordato!)
            var appTheme by remember {
                mutableStateOf(
                    when (sharedPreferences.getString(appThemeKey, "SYSTEM")) {
                        "LIGHT" -> AppTheme.LIGHT
                        "DARK" -> AppTheme.DARK
                        else -> AppTheme.SYSTEM
                    }
                )
            }


            // Crea la callback onThemeChange per SettingsScreen - con tipo ESPLICITO!**
            val onThemeChange: (AppTheme) -> Unit = remember { // ✅ Tipo funzione callback SPECIFICATO ESPLICITAMENTE: (AppTheme) -> Unit
                { newTheme: AppTheme ->
                    // Funzione lambda per la callback (INALTERATO)
                    appTheme = newTheme
                    // Salva anche la preferenza in SharedPreferences (OPZIONALE, ma consigliato per coerenza)
                    sharedPreferences.edit()
                        .putString(appThemeKey, newTheme.name)
                        .apply()
                    Log.d("MainActivity", "Tema cambiato tramite callback onThemeChange a: $newTheme")
                }
            }

            // Ottieni NavController
            val navController = rememberNavController()
            // di nuovo?

            val onVerifyEmailScreenNavigation: () -> Unit = remember {
                {
                    navController.navigate("verifyEmail")
                    Log.d("MainActivity", "Navigating to VerifyEmailScreen") // Log di debug
                }
            }

            CompositionLocalProvider(
                LocalAppTheme provides appTheme
            ) {
                GenteStranaTheme(
                    appTheme = appTheme,
                    dynamicColor = false
                ) {
                    Surface {
                        AppNavHost(
                            navController = navController,
                            onThemeChange = onThemeChange,
                            isOnboardingCompleted = ::isOnboardingCompleted,
                            onVerifyEmailScreenNavigation = onVerifyEmailScreenNavigation
                        )
                    }
                }
            }
        }
        forceTokenRefreshIfNeeded(this)
    }

    private fun requestNotificationPermission() {
        // Controllo: Android 13+ richiede runtime permission per le notifiche
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Richiede il permesso tramite il launcher
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }


    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Permesso notifiche concesso")
        } else {
            Log.w("MainActivity", "Permesso notifiche negato")
            // Puoi mostrare un messaggio all'utente per spiegare l'importanza del permesso
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Permesso notifiche concesso")
            } else {
                Log.w("MainActivity", "Permesso notifiche negato")
                // Qui puoi, ad esempio, mostrare un messaggio all'utente per spiegare perché il permesso è necessario
            }
        }
    }


    override fun onUserInteraction() {
        super.onUserInteraction()
        updateUserLastActive()
    }

    private fun isOnboardingCompleted(): Boolean {
        val sharedPreferences = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("onboarding_completed", false)
    // "false" è il valore di default (onboarding non completato)
    }
}