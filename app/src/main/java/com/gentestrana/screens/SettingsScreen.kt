package com.gentestrana.screens

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gentestrana.R
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import com.gentestrana.users.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class AppTheme { SYSTEM, LIGHT, DARK }

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

    // Ottenimento informazioni versione app
    val packageInfo = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    val versionName = packageInfo?.versionName ?: "N/A"
    val versionCode = if (packageInfo != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toString()
        }
    } else {
        "N/A"
    }

    // SharedPreferences
    val sharedPreferences = remember {
        context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
    }
    val pushNotificationsKey = "push_notifications_enabled"
    val appThemeKey = "app_theme"

    // Stato Notifiche Push
    val pushNotificationsEnabled = remember {
        mutableStateOf(sharedPreferences.getBoolean(pushNotificationsKey, true))
    }

    // Stato Tema App
    val selectedTheme = remember {
        mutableStateOf(
            when (sharedPreferences.getString(appThemeKey, "SYSTEM")) {
                "LIGHT" -> AppTheme.LIGHT
                "DARK" -> AppTheme.DARK
                else -> AppTheme.SYSTEM
            }
        )
    }

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) }
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
                text = stringResource(R.string.account_section),
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.change_password)) },
                modifier = Modifier.clickable { navController.navigate("changePassword") }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.blocked_users_navigation_title)) },
                modifier = Modifier.clickable { navController.navigate("blockedUsers") }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.delete_account)) },
                colors = ListItemDefaults.colors(headlineColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.clickable { showDeleteConfirmationDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.notifications_section),
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.push_notifications)) },
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
            // Opzioni Tema
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedTheme.value == AppTheme.SYSTEM,
                    onClick = {
                        val newTheme = AppTheme.SYSTEM
                        selectedTheme.value = newTheme
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
                        sharedPreferences.edit()
                            .putString(appThemeKey, "DARK")
                            .apply()
                        onThemeChange(newTheme)
                    }
                )
                Text(stringResource(R.string.dark_theme))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.info_section),
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.privacy_policy)) }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.terms_of_service)) },
                modifier = Modifier.clickable { navController.navigate("termsOfService") }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.community_guidelines)) },
                modifier = Modifier.clickable { navController.navigate("communityGuidelines") }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.app_version)) },
                trailingContent = {
                    Text("Versione: $versionName (Code: $versionCode)")
                }
            )
        }
        if (showDeleteConfirmationDialog) {
            AlertDialog(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(22.dp)),
                onDismissRequest = { showDeleteConfirmationDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.delete_account_dialog_title),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                text = { Text(stringResource(R.string.delete_account_dialog_message)) },
                confirmButton = {
                    Button(
                        onClick = {
                            CoroutineScope(Dispatchers.Main).launch {
                                userRepository.deleteUserAccount(
                                    onSuccess = {
                                        Toast.makeText(
                                            context,
                                            "Account eliminato",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        auth.signOut()
                                        rootNavController.navigate("auth") {
                                            popUpTo(rootNavController.graph.id) {
                                                inclusive = true
                                            }
                                            launchSingleTop = true
                                            restoreState = false
                                        }
                                    },
                                    onFailure = { errorMessage ->
                                        Toast.makeText(
                                            context,
                                            "Errore: ${errorMessage ?: "Sconosciuto"}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        showDeleteConfirmationDialog = false
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.delete_account_dialog_confirm_button))
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