package com.gentestrana.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.gentestrana.R
import com.gentestrana.users.UserRepository
import androidx.compose.foundation.shape.RoundedCornerShape
import com.gentestrana.components.GenericLoadingScreen

val userRepository = UserRepository()
const val emailRegex = "^[A-Za-z0-9._%+-]+@(?:gmail\\.com|outlook\\.com|yahoo\\.com|icloud\\.com|protonmail\\.com|live\\.com|hotmail\\.it|yahoo\\.it)$"

@Composable
fun RegistrationScreen(
    onRegistrationSuccess: () -> Unit,
    onVerifyEmailScreenNavigation: () -> Unit)
{
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isEmailError by remember { mutableStateOf(false) }
    var sex by remember { mutableStateOf("Undefined") }
    // Stato per il sesso selezionato

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // Mostra lo snackbar quando errorMessage viene impostato
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            errorMessage = null // Reset dell'errore dopo aver mostrato lo Snackbar
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    isEmailError = !it.matches(emailRegex.toRegex())
                },
                label = { Text(stringResource(R.string.registration_email_label)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.registration_email_placeholder)) },

                isError = isEmailError
                )
            if (isEmailError) { // MOSTRA UN MESSAGGIO DI ERRORE SOTTO IL CAMPO (OPZIONALE)
                Text(
                    text = stringResource(R.string.registration_email_error_message), // CREA QUESTA STRINGA IN strings.xml
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.registration_password_label)) },
                placeholder = { Text(stringResource(R.string.registration_password_placeholder)) }, // cambiare con string
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.registration_firstname_label)) },// cambiare con string
                placeholder = { Text(stringResource(R.string.registration_firstname_placeholder)) }, // cambiare con string
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icona per Maschio
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp)) // Bordo arrotondato opzionale
                        .background(if (sex == "M") MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) else Color.Transparent) // Sfondo colorato se selezionato
                        .padding(8.dp) // Padding interno per lo sfondo
                ) {
                    IconButton(onClick = {
                        sex = "M"
                        Log.d("RegistrationScreen", "Sesso selezionato: Maschio") // Aggiunto Log
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Male,
                            contentDescription = stringResource(R.string.sex_male_description),
                            tint = if (sex == "M") MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    Text(stringResource(R.string.sex_male))
                }

                // Icona per Femmina
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp)) // Bordo arrotondato opzionale
                        .background(if (sex == "F") MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) else Color.Transparent) // Sfondo colorato se selezionato
                        .padding(8.dp) // Padding interno per lo sfondo
                ) {
                    IconButton(onClick = {
                        sex = "F"
                        Log.d("RegistrationScreen", "Sesso selezionato: Femmina") // Aggiunto Log
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Female,
                            contentDescription = stringResource(R.string.sex_female_description),
                            tint = if (sex == "F") MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    Text(stringResource(R.string.sex_female))
                }

                // Icona per Non definito
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp)) // Bordo arrotondato opzionale
                        .background(if (sex == "Undefined") MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) else Color.Transparent) // Sfondo colorato se selezionato
                        .padding(8.dp) // Padding interno per lo sfondo
                ) {
                    IconButton(onClick = {
                        sex = "Undefined"
                        Log.d("RegistrationScreen", "Sesso selezionato: Non definito") // Aggiunto Log
                    }) {
                        Icon(
                            imageVector = Icons.Filled.QuestionMark,
                            contentDescription = stringResource(R.string.sex_undefined_description),
                            tint = if (sex == "Undefined") MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    Text(stringResource(R.string.sex_undefined))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.registration_select_profile_picture_button))
            }

            selectedImageUri?.let { uri ->
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = "Selected Profile Picture",
                    modifier = Modifier
                        .size(100.dp)
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    isLoading = true
                    userRepository.registerUserAndUploadImage(
                        email = email,
                        password = password,
                        username = username,
                        sex = sex,
                        bio = "",
                        selectedImageUri = selectedImageUri,
                        context = context,
                        onSuccess = {
                            isLoading = false
                            onVerifyEmailScreenNavigation()
                        },
                        onFailure = { error ->
                            isLoading = false
                            errorMessage = error
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    GenericLoadingScreen(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.registration_register_button))
                }
            }
        }
    }
}