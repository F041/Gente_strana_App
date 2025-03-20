package com.gentestrana.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gentestrana.R
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.gentestrana.users.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class AppTheme { SYSTEM, LIGHT, DARK, /*SPECIAL*/ }
// SPECIAL servirà per dislessia probabilmente

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun SettingsScreen(
    rootNavController: NavController,
    navController: NavController,
    onThemeChange: (AppTheme) -> Unit,
    auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val context = LocalContext.current
    val userRepository = remember { UserRepository() }

    // SharedPreferences
    val sharedPreferences = remember {
        context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
    }
    val pushNotificationsKey = "push_notifications_enabled"
    val appThemeKey = "app_theme" // Chiave per salvare il tema

    // Stato Notifiche Push (già esistente)
    val pushNotificationsEnabled = remember {
        mutableStateOf(sharedPreferences.getBoolean(pushNotificationsKey, true))
    }

    // Stato Tema App, caricato da SharedPreferences o valore di default (SYSTEM)
    val selectedTheme = remember {
        mutableStateOf(
            when (sharedPreferences.getString(
                appThemeKey,
                "SYSTEM"
            )) { // "SYSTEM" è il default come stringa
                "LIGHT" -> AppTheme.LIGHT
                "DARK" -> AppTheme.DARK
                else -> AppTheme.SYSTEM
                // SYSTEM come default se non trovato o valore non valido
            }
        )
    }

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) } // Usa string resource per "Impostazioni"
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.account_section), // Usa string resource per "Account"
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.change_password)) }, // Usa string resource
                modifier = Modifier.clickable { navController.navigate("changePassword") }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.delete_account)) },
                colors = ListItemDefaults.colors(headlineColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.clickable {
                    showDeleteConfirmationDialog = true
                    // Mostra il dialog quando si clicca su "Elimina Account"
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.notifications_section),
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.push_notifications)) }, // Usa string resource
                trailingContent = {
                    Switch(
                        checked = pushNotificationsEnabled.value,
                        onCheckedChange = { isChecked ->
                            pushNotificationsEnabled.value = isChecked
                            sharedPreferences.edit()
                                .putBoolean(pushNotificationsKey, isChecked)
                                .apply()
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.theme_section),
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider()
            // Opzioni Tema con RadioButton
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedTheme.value == AppTheme.SYSTEM,
                    onClick = {
                        val newTheme = AppTheme.SYSTEM
                        selectedTheme.value = newTheme
                        Log.d(
                            "SettingsScreen",
                            "RadioButton SYSTEM cliccato, selectedTheme.value ora: ${selectedTheme.value}"
                        )
                        selectedTheme.value = AppTheme.SYSTEM
                        sharedPreferences.edit()
                            .putString(appThemeKey, "SYSTEM")
                            .apply()
                        onThemeChange(newTheme)
                    }
                )
                Text(stringResource(R.string.system_theme))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedTheme.value == AppTheme.LIGHT,
                    onClick = {
                        val newTheme = AppTheme.LIGHT
                        selectedTheme.value = newTheme
                        Log.d(
                            "SettingsScreen",
                            "RadioButton SYSTEM cliccato, selectedTheme.value ora: ${selectedTheme.value}"
                        )
                        selectedTheme.value = AppTheme.LIGHT
                        sharedPreferences.edit()
                            .putString(appThemeKey, "LIGHT")
                            .apply()
                        onThemeChange(newTheme)
                    }
                )
                Text(stringResource(R.string.light_theme))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedTheme.value == AppTheme.DARK,
                    onClick = {
                        val newTheme = AppTheme.DARK
                        selectedTheme.value = newTheme
                        Log.d(
                            "SettingsScreen",
                            "RadioButton SYSTEM cliccato, selectedTheme.value ora: ${selectedTheme.value}"
                        )
                        selectedTheme.value = AppTheme.DARK
                        sharedPreferences.edit()
                            .putString(appThemeKey, "DARK")
                            .apply()
                        onThemeChange(newTheme)
                    }
                )
                Text(stringResource(R.string.dark_theme))
            }

            // OPZIONE TEMA SPECIALE (COMMENTATA PER ORA)
            // Row(verticalAlignment = Alignment.CenterVertically) {
            //     RadioButton(
            //         selected = selectedTheme.value == AppTheme.SPECIAL, // Dovremmo aggiungere SPECIAL all'enum AppTheme
            //         onClick = {
            //             selectedTheme.value = AppTheme.SPECIAL
            //             sharedPreferences.edit()
            //                 .putString(appThemeKey, "SPECIAL") // Dovremmo aggiungere "SPECIAL" alle opzioni salvate
            //                 .apply()

            //         }
            //     )
            //     Text(stringResource(R.string.special_theme)) // Dovremmo aggiungere string resource per "Speciale"
            // }


            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.info_section),
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.privacy_policy)) } // Usa string resource
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.terms_of_service)) }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.app_version)) },
                trailingContent = { Text("1.0") }
            )
        }
        if (showDeleteConfirmationDialog) {
            AlertDialog(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(22.dp)),
                // i .dp differiscono, non proprio elegante
                // Bordo rosso DIRETTO sull'AlertDialog
                onDismissRequest = { showDeleteConfirmationDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.delete_account_confirmation_title),
                        color = MaterialTheme.colorScheme.error // Titolo in rosso
                    )
                },
                text = { Text(stringResource(R.string.delete_account_confirmation_message)) },
                confirmButton = {
                    Button(
                        onClick = {
                            // Chiamata a deleteUserAccount
                            CoroutineScope(Dispatchers.Main).launch {
                                // Lancia la coroutine nel contesto UI
                                userRepository.deleteUserAccount(
                                    onSuccess = {
                                        // Account eliminato con successo
                                        Toast.makeText(
                                            context,
                                            "🚮 ✔ ",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        // 2. Log per debug
                                        Log.d("SettingsScreen", "Account eliminato, eseguendo signOut...")

                                        // 3. Logout da Firebase
                                        auth.signOut()


                                        // 4. Pulisci dati locali (se hai una funzione clearUserData)

                                        // 5. Navigazione con reset completo
                                        rootNavController.navigate("auth") {
                                            popUpTo(rootNavController.graph.id) {
                                                inclusive = true
                                            }
                                            launchSingleTop = true
                                            restoreState = false
                                        }
                                    },
                                    onFailure = { errorMessage ->
                                        // Errore durante l'eliminazione
                                        Toast.makeText(
                                            context,
                                            "Errore eliminazione account: ${errorMessage ?: "Sconosciuto"}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        showDeleteConfirmationDialog =
                                            false
                                    // Chiudi il dialog in caso di errore
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.delete_account_confirm_button))
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showDeleteConfirmationDialog = false }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}