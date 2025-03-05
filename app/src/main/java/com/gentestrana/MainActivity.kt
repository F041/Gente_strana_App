package com.gentestrana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.gentestrana.ui.theme.GenteStranaTheme
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.gentestrana.screens.AppTheme
import androidx.compose.runtime.CompositionLocalProvider
import com.gentestrana.ui.theme.LocalAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Ottieni il contesto
            val context = LocalContext.current
            // SharedPreferences per leggere il tema salvato
            val sharedPreferences = remember {
                context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            }
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
            Log.d("MainActivity", "Valore di appTheme prima di CompositionLocalProvider: $appTheme")

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

            CompositionLocalProvider(
                LocalAppTheme provides appTheme
            ) {
                GenteStranaTheme(
                    appTheme = appTheme,
                    dynamicColor = false
                ) {
                    Surface {
                        val navController = rememberNavController()
                        AppNavHost(navController = navController,
                            onThemeChange = onThemeChange,
                            isOnboardingCompleted = ::isOnboardingCompleted)
                    }
                }
            }
        }
    }
    private fun isOnboardingCompleted(): Boolean {
        val sharedPreferences = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("onboarding_completed", false) // "false" è il valore di default (onboarding non completato)
    }
}