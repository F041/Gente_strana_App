// File: Gentestrana\app\src\main\java\com\gentestrana\MainActivity.kt

package com.gentestrana

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.gentestrana.ui.theme.GenteStranaTheme
import android.content.Intent
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.gentestrana.screens.AppTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController // Importa NavHostController
import com.gentestrana.ui.theme.LocalAppTheme
import com.gentestrana.utils.forceTokenRefreshIfNeeded
import com.google.firebase.auth.FirebaseAuth
import com.gentestrana.utils.updateUserLastActive
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings

class MainActivity : ComponentActivity() {

    // --- INIZIO NUOVA PARTE ---
    private var navController: NavHostController? = null // Riferimento al NavController
    // --- FINE NUOVA PARTE ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 10L else 3600L
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        val defaultMap = mapOf(
            "daily_message_limit" to 300L,
            "message_rate_limit_window_seconds" to 50L,
            "message_rate_limit_max_messages" to 4L,
            "onboarding_autism_test_url" to "https://embrace-autism.com/asrs-v1-1/#test",
            "onboarding_adhd_test_url" to "https://psychology-tools.com/test/adult-adhd-self-report-scale",
            "registration_email_validation_regex" to "^[A-Za-z0-9._%+-]+@(?:gmail\\.com|outlook\\.com|yahoo\\.com|icloud\\.com|protonmail\\.com|live\\.com|hotmail\\.it|yahoo\\.it)$"
        )
        remoteConfig.setDefaultsAsync(defaultMap)
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    Log.d("RemoteConfig", "Config params updated: $updated")
                } else {
                    Log.w("RemoteConfig", "Fetch failed")
                }
            }

        // Rimosso il blocco di gestione Intent da qui, lo mettiamo in una funzione separata
        // e lo chiamiamo sia in onCreate che in onNewIntent.

        setContent {
            val context = LocalContext.current
            val sharedPreferences = remember {
                context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            }
            // val authState by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) } // Non sembra usato direttamente qui
            val appThemeKey = "app_theme"

            var appTheme by remember {
                mutableStateOf(
                    when (sharedPreferences.getString(appThemeKey, "SYSTEM")) {
                        "LIGHT" -> AppTheme.LIGHT
                        "DARK" -> AppTheme.DARK
                        else -> AppTheme.SYSTEM
                    }
                )
            }

            val onThemeChange: (AppTheme) -> Unit = remember {
                { newTheme: AppTheme ->
                    appTheme = newTheme
                    sharedPreferences.edit()
                        .putString(appThemeKey, newTheme.name)
                        .apply()
                    Log.d("MainActivity", "Tema cambiato tramite callback onThemeChange a: $newTheme")
                }
            }

            // Ricorda il NavController e assegnalo alla variabile di istanza
            val rememberedNavController = rememberNavController()
            navController = rememberedNavController // Assegna al NavController della classe

            val onVerifyEmailScreenNavigation: () -> Unit = remember {
                {
                    // Usa la variabile di istanza navController ---
                    navController?.navigate("verifyEmail")
                    Log.d("MainActivity", "Navigating to VerifyEmailScreen")
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
                            // Passa il rememberedNavController ---
                            navController = rememberedNavController,
                            onThemeChange = onThemeChange,
                            isOnboardingCompleted = ::isOnboardingCompleted,
                            onVerifyEmailScreenNavigation = onVerifyEmailScreenNavigation
                        )
                    }
                }
            }
        }
        // --- INIZIO NUOVA PARTE ---
        // Gestisci l'intent iniziale dopo che setContent è stato chiamato
        // e navController è potenzialmente inizializzato.
        intent?.let { handleIntent(it) }
        // --- FINE NUOVA PARTE ---

        forceTokenRefreshIfNeeded(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Quando l'attività riceve un nuovo intent mentre è già in esecuzione
        // (es. l'utente tocca una notifica mentre l'app è in background),
        // gestisci il nuovo intent.
        setIntent(intent) // Aggiorna l'intent dell'attività
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val data = intent.data

        Log.d("MainActivity", "handleIntent: action=$action, data=$data")

        // Controlla se l'intent proviene da una notifica FCM che abbiamo gestito
        if (intent.hasExtra("notification_data")) {
            @Suppress("UNCHECKED_CAST")
            val notificationData = intent.getSerializableExtra("notification_data") as? HashMap<String, String>
            Log.d("MainActivity", "Dati dalla notifica: $notificationData")

            val chatId = notificationData?.get("chatId") // Estrai il chatId

            if (!chatId.isNullOrEmpty()) {
                Log.d("MainActivity", "Trovato chatId dalla notifica: $chatId. Navigazione a chat/$chatId")
                // Assicurati che il navController sia inizializzato
                // Potrebbe essere necessario un piccolo delay o un LaunchedEffect
                // se la navigazione avviene troppo presto.
                // Per ora proviamo direttamente:
                navController?.let {
                    // Naviga alla chat specifica
                    // È importante assicurarsi che il NavHost e le sue destinazioni siano già
                    // state composte prima di tentare la navigazione.
                    // Se la mainTabs non è la startDestination principale dell'app,
                    // potrebbe essere necessario navigare prima a "main"
                    // e poi a "chat/{chatId}".

                    // Assumendo che 'main' (che contiene 'mainTabs') sia la route principale dopo login/onboarding
                    if (FirebaseAuth.getInstance().currentUser != null && isOnboardingCompleted()) {
                        it.navigate("main") {
                            // Opzionale: pulisci la backstack se necessario
                            popUpTo(it.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                        it.navigate("chat/$chatId") {
                            launchSingleTop = true // Evita di impilare la stessa chat più volte
                        }
                    } else {
                        Log.w("MainActivity", "Utente non loggato o onboarding non completato. Navigazione alla chat $chatId saltata.")
                        // Potresti voler navigare alla schermata di login/onboarding qui
                    }
                } ?: Log.e("MainActivity", "navController è null in handleIntent, impossibile navigare alla chat.")
            } else {
                Log.d("MainActivity", "chatId non trovato nei dati della notifica.")
            }
        }
        // Gestione del deep link per la verifica email (codice che avevi già)
        else if (action == Intent.ACTION_VIEW && data != null) {
            if (data.scheme == "gentestrana" && data.host == "verifyemail") {
                val token = data.getQueryParameter("token")
                Log.d("DeepLink", "Token di verifica da deep link: $token")
                // Qui dovresti avere la logica per usare il token e navigare
                // alla schermata appropriata (es. login o direttamente main se la verifica va a buon fine)
                // Per ora, se il token è per la verifica, potresti voler navigare a "verifyEmail"
                // o direttamente a "login" se la verifica avviene altrove.
                // navController?.navigate("login") // Esempio
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUserLastActive()
    }

    private fun isOnboardingCompleted(): Boolean {
        val sharedPreferences = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("onboarding_completed", false)
    }
}