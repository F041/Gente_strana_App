package com.gentestrana.screens

import android.content.Context
import android.util.Log
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gentestrana.R

enum class AppTheme { SYSTEM, LIGHT, DARK, /*SPECIAL*/ }
// SPECIAL servirà per dislessia probabilmente

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun SettingsScreen(
    navController: NavController,
    onThemeChange: (AppTheme) -> Unit
) {
    val context = LocalContext.current

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
            when (sharedPreferences.getString(appThemeKey, "SYSTEM")) { // "SYSTEM" è il default come stringa
                "LIGHT" -> AppTheme.LIGHT
                "DARK" -> AppTheme.DARK
                else -> AppTheme.SYSTEM
            // SYSTEM come default se non trovato o valore non valido
            }
        )
    }

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
                headlineContent = { Text(stringResource(R.string.delete_account)) }, // Usa string resource
                colors = ListItemDefaults.colors(headlineColor = MaterialTheme.colorScheme.error)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.notifications_section), // Usa string resource per "Notifiche"
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
                text = stringResource(R.string.theme_section), // Usa string resource per "Tema"
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider()
            // Opzioni Tema con RadioButton
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedTheme.value == AppTheme.SYSTEM,
                    onClick = {
                        val newTheme = AppTheme.SYSTEM // ✅ Ottieni il NUOVO tema selezionato
                        selectedTheme.value = newTheme // ✅ Aggiorna lo stato LOCALE (per UI SettingsScreen)
                        Log.d("SettingsScreen", "RadioButton SYSTEM cliccato, selectedTheme.value ora: ${selectedTheme.value}")
                        selectedTheme.value = AppTheme.SYSTEM // ✅ MODIFICA STATO selectedTheme!
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
                        Log.d("SettingsScreen", "RadioButton SYSTEM cliccato, selectedTheme.value ora: ${selectedTheme.value}")
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
                        Log.d("SettingsScreen", "RadioButton SYSTEM cliccato, selectedTheme.value ora: ${selectedTheme.value}")
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
                text = stringResource(R.string.info_section), // Usa string resource per "Informazioni"
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
    }
}