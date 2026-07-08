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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.gentestrana.screens.AppTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import com.gentestrana.ui.theme.LocalAppTheme
import com.gentestrana.utils.forceTokenRefreshIfNeeded
import com.google.firebase.auth.FirebaseAuth
import com.gentestrana.utils.updateUserLastActive
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings

class MainActivity : ComponentActivity() {

    private var navController: NavHostController? = null

    // Stato reactive per la navigazione pendente dalle notifiche chat.
    private val pendingChatId = mutableStateOf<String?>(null)

    // Stato reactive per gestire il deep link di reset password (oobCode).
    private val pendingOobCode = mutableStateOf<String?>(null)

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

        setContent {
            val context = LocalContext.current
            val sharedPreferences = remember {
                context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            }
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

            val rememberedNavController = rememberNavController()
            navController = rememberedNavController

            val onVerifyEmailScreenNavigation: () -> Unit = remember {
                {
                    navController?.navigate("verifyEmail")
                    Log.d("MainActivity", "Navigating to VerifyEmailScreen")
                }
            }

            // ===== GESTIONE NOTIFICA: Navigazione alla chat =====
            val currentPendingChatId = pendingChatId.value
            LaunchedEffect(currentPendingChatId) {
                if (currentPendingChatId != null) {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser != null && isOnboardingCompleted()) {
                        Log.d("MainActivity", "Navigazione pendente verso la chat: $currentPendingChatId")
                        rememberedNavController.navigate("chat/$currentPendingChatId") {
                            launchSingleTop = true
                        }
                    } else {
                        Log.w("MainActivity", "Utente non loggato o onboarding non completato. Chat navigazione rimandata.")
                    }
                    pendingChatId.value = null
                }
            }

            // ===== GESTIONE DEEP LINK: Reset Password =====
            // Quando l'utente clicca il link di reset password nell'email,
            // viene generato un oobCode. handleIntent() imposta pendingOobCode.value
            // che questa LaunchedEffect osserva per navigare alla schermata di reset.
            val currentPendingOobCode = pendingOobCode.value
            LaunchedEffect(currentPendingOobCode) {
                if (currentPendingOobCode != null) {
                    Log.d("MainActivity", "Reset password con oobCode: $currentPendingOobCode")
                    rememberedNavController.navigate("resetPassword/$currentPendingOobCode") {
                        launchSingleTop = true
                        popUpTo(0) { inclusive = true } // Pulisce tutto, parte da zero
                    }
                    pendingOobCode.value = null
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
                            navController = rememberedNavController,
                            onThemeChange = onThemeChange,
                            isOnboardingCompleted = ::isOnboardingCompleted,
                            onVerifyEmailScreenNavigation = onVerifyEmailScreenNavigation
                        )
                    }
                }
            }
        }

        // Gestisci l'intent iniziale DOPO che setContent è stato chiamato.
        intent?.let { handleIntent(it) }

        forceTokenRefreshIfNeeded(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent?.let { handleIntent(it) }
    }

    /**
     * Estrae il chatId dall'Intent indipendentemente dalla provenienza.
     *
     * Ci sono DUE possibili origini per l'Intent di una notifica tap:
     *
     * 1. [FOREGROUND] Notifica creata manualmente da showNotification() in MyFirebaseMessagingService.
     *    In questo caso i dati sono in un Serializable extra chiamato "notification_data".
     *
     * 2. [BACKGROUND - LA PIÙ COMUNE] Notifica creata automaticamente dal sistema FCM.
     *    Quando l'app è in background, onMessageReceived() NON viene chiamato (per messaggi
     *    notification+data). Il sistema crea la notifica dalla parte "notification" del payload
     *    e, al tap, mette la parte "data" COME EXTRAS DIRETTI sull'Intent di lancio.
     *    Quindi chatId, messageId sono extras direttamente sull'Intent, NON annidati.
     *
     * Bisogna controllare ENTRAMBI i casi.
     */
    private fun extractChatIdFromIntent(intent: Intent): String? {
        // CASO 1: Notifica creata manualmente (app in foreground)
        // I dati sono in un HashMap<String,String> sotto la chiave "notification_data"
        @Suppress("DEPRECATION")
        if (intent.hasExtra("notification_data")) {
            val notificationData = intent.getSerializableExtra("notification_data") as? HashMap<*, *>
            if (notificationData != null) {
                val chatId = notificationData["chatId"] as? String
                if (!chatId.isNullOrEmpty()) {
                    Log.d("MainActivity", "chatId estratto da notification_data: $chatId")
                    return chatId
                }
            }
        }

        // CASO 2: Notifica creata dal sistema FCM (app in background)
        // chatId è un extra diretto dell'Intent, perché FCM system mette
        // i campi del data payload come extras direttamente sull'Intent
        if (intent.hasExtra("chatId")) {
            val chatId = intent.getStringExtra("chatId")
            if (!chatId.isNullOrEmpty()) {
                Log.d("MainActivity", "chatId estratto come direct extra dall'Intent: $chatId")
                return chatId
            }
        }

        Log.d("MainActivity", "chatId non trovato nell'Intent")
        return null
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val data = intent.data

        Log.d("MainActivity", "handleIntent: action=$action, data=$data")

        // Estrai chatId dall'Intent, indipendentemente dalla provenienza
        val chatId = extractChatIdFromIntent(intent)

        if (chatId != null) {
            Log.d("MainActivity", "handleIntent: chatId=$chatId. Imposto navigazione pendente.")
            pendingChatId.value = chatId
        }

        // Gestione del deep link: intercetta link da email Firebase
        // (reset password o verifica email) e naviga allo screen appropriato.
        if (action == Intent.ACTION_VIEW && data != null) {
            if (data.scheme == "gentestrana" && data.host == "verifyemail") {
                // Firebase ActionCodeSettings redirige qui.
                // I parametri possono essere:
                //   - ?oobCode=...&mode=resetPassword    (reset password)
                //   - ?oobCode=...&mode=verifyEmail      (verifica email)
                //   - ?token=...                          (verifica email legacy)
                val oobCode = data.getQueryParameter("oobCode")
                val mode = data.getQueryParameter("mode")

                if (!oobCode.isNullOrBlank()) {
                    if (mode == "resetPassword" || mode == null) {
                        // resetPassword o redirect senza mode (da emailVerificationRedirect)
                        Log.d("DeepLink", "Reset password deep link ricevuto con oobCode: $oobCode")
                        pendingOobCode.value = oobCode
                    } else if (mode == "verifyEmail") {
                        // Verifica email tramite oobCode
                        Log.d("DeepLink", "Verifica email deep link con oobCode: $oobCode")
                        val token = data.getQueryParameter("token")
                        Log.d("DeepLink", "Token di verifica da deep link: $token")
                    }
                } else {
                    val token = data.getQueryParameter("token")
                    Log.d("DeepLink", "Deep link generico a verifyemail, token: $token")
                }
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