package com.gentestrana.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.gentestrana.R
import com.gentestrana.components.GenericLoadingScreen
import com.gentestrana.components.ProfileCompletionFields
import com.gentestrana.users.UserRepository
import com.gentestrana.utils.missingRequiredProfileFields
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig

val userRepository = UserRepository()

private const val REMOTE_CONFIG_EMAIL_REGEX_KEY = "registration_email_validation_regex"

// FIX: aggiunti domini molto diffusi (hotmail.com, outlook.it, libero.it, ecc.).
// Il controllo viene fatto ignorando maiuscole/minuscole (vedi buildEmailRegex).
private const val DEFAULT_EMAIL_REGEX =
    "^[A-Za-z0-9._%+-]+@(?:gmail\\.com|googlemail\\.com|outlook\\.com|outlook\\.it|hotmail\\.com|hotmail\\.it|" +
        "live\\.com|live\\.it|msn\\.com|yahoo\\.com|yahoo\\.it|icloud\\.com|me\\.com|mac\\.com|" +
        "protonmail\\.com|proton\\.me|libero\\.it|virgilio\\.it|tiscali\\.it|alice\\.it|tim\\.it|" +
        "fastwebnet\\.it|aol\\.com|gmx\\.com|gmx\\.it)$"

/** Lunghezza minima richiesta da Firebase Auth. */
private const val MIN_PASSWORD_LENGTH = 6

/**
 * FIX: una regex scritta male in Remote Config non deve far crashare la schermata.
 * In quel caso si torna alla regex di default.
 */
private fun buildEmailRegex(pattern: String): Regex {
    val source = pattern.ifBlank { DEFAULT_EMAIL_REGEX }
    return runCatching { source.toRegex(RegexOption.IGNORE_CASE) }
        .getOrElse { e ->
            Log.w("RegistrationScreen", "Regex email di Remote Config non valida, uso quella di default: ${e.message}")
            DEFAULT_EMAIL_REGEX.toRegex(RegexOption.IGNORE_CASE)
        }
}

@Composable
fun RegistrationScreen(
    onRegistrationSuccess: () -> Unit,
    onVerifyEmailScreenNavigation: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var sex by remember { mutableStateOf("Undefined") }
    var rawBirthTimestamp by remember { mutableStateOf(0L) }
    var bio by remember { mutableStateOf("") }
    var topics by remember { mutableStateOf<List<String>>(emptyList()) }
    // Diventa true quando l'utente prova a registrarsi senza aver compilato i campi obbligatori.
    // Serve per mostrare gli errori in rosso solo dopo il primo tentativo.
    var showValidationErrors by remember { mutableStateOf(false) }

    // Campi obbligatori ancora mancanti (bio, almeno 1 topic, data di nascita).
    // Funzione pura e testata: vedi ProfileValidationTest.
    val missingFields = missingRequiredProfileFields(bio, topics, rawBirthTimestamp)

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // FIX: Remote Config viene davvero aggiornato (fetchAndActivate) e la regex
    // si ricostruisce quando arriva il nuovo valore.
    val remoteConfig = remember { Firebase.remoteConfig }
    var emailRegexString by remember {
        mutableStateOf(remoteConfig.getString(REMOTE_CONFIG_EMAIL_REGEX_KEY))
    }
    LaunchedEffect(Unit) {
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            emailRegexString = remoteConfig.getString(REMOTE_CONFIG_EMAIL_REGEX_KEY)
        }
    }
    val emailRegexPattern = remember(emailRegexString) { buildEmailRegex(emailRegexString) }

    // FIX: email ripulita da spazi e in minuscolo; l'errore e' calcolato a ogni
    // ricomposizione, quindi resta corretto anche se la regex cambia.
    val normalizedEmail = email.trim().lowercase()
    val isEmailError = email.isNotBlank() && !normalizedEmail.matches(emailRegexPattern)
    val isConfirmPasswordError = confirmPassword.isNotEmpty() && confirmPassword != password

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            errorMessage = null
        }
    }

    val passwordTransformation =
        if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
    val passwordToggle: @Composable () -> Unit = {
        IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(
                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = stringResource(
                    if (passwordVisible) R.string.registration_hide_password
                    else R.string.registration_show_password
                )
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Campo email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.registration_email_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.registration_email_placeholder)) },
                    isError = isEmailError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )
                if (isEmailError) {
                    Text(
                        text = stringResource(R.string.registration_email_error_message),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Campo password — con pulsante mostra/nascondi
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.registration_password_label)) },
                    placeholder = { Text(stringResource(R.string.registration_password_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = passwordTransformation,
                    trailingIcon = passwordToggle,
                    isError = password.isNotEmpty() && password.length < MIN_PASSWORD_LENGTH,
                    supportingText = {
                        Text(stringResource(R.string.registration_password_min_length, MIN_PASSWORD_LENGTH))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Campo conferma password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.registration_confirm_password_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = passwordTransformation,
                    isError = isConfirmPasswordError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    )
                )
                if (isConfirmPasswordError) {
                    Text(
                        text = stringResource(R.string.registration_passwords_dont_match),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Campo username
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.registration_firstname_label)) },
                    placeholder = { Text(stringResource(R.string.registration_firstname_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Selettore sesso
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (sex == "M") MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                else Color.Transparent
                            )
                            .padding(8.dp)
                    ) {
                        IconButton(onClick = { sex = "M" }) {
                            Icon(
                                imageVector = Icons.Filled.Male,
                                contentDescription = stringResource(R.string.sex_male_description),
                                tint = if (sex == "M") MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                        Text(stringResource(R.string.sex_male))
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (sex == "F") MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                else Color.Transparent
                            )
                            .padding(8.dp)
                    ) {
                        IconButton(onClick = { sex = "F" }) {
                            Icon(
                                imageVector = Icons.Filled.Female,
                                contentDescription = stringResource(R.string.sex_female_description),
                                tint = if (sex == "F") MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                        Text(stringResource(R.string.sex_female))
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (sex == "Undefined") MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                else Color.Transparent
                            )
                            .padding(8.dp)
                    ) {
                        IconButton(onClick = { sex = "Undefined" }) {
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

                ProfileCompletionFields(
                    context = context,
                    bio = bio,
                    onBioChange = { bio = it },
                    topics = topics,
                    onTopicsChange = { topics = it },
                    birthTimestamp = rawBirthTimestamp,
                    onBirthTimestampChange = { rawBirthTimestamp = it },
                    missingFields = if (showValidationErrors) missingFields else emptyList()
                )

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
            }

            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    if (normalizedEmail.isBlank() || password.isBlank() || username.isBlank()) {
                        errorMessage = context.getString(R.string.login_fill_fields)
                        return@Button
                    }
                    if (isEmailError) {
                        errorMessage = context.getString(R.string.registration_email_error_message)
                        return@Button
                    }
                    // Campi obbligatori: bio, almeno un topic e data di nascita.
                    if (missingFields.isNotEmpty()) {
                        showValidationErrors = true
                        errorMessage = context.getString(R.string.profile_complete_required_error)
                        return@Button
                    }
                    // FIX: controlli sulla password prima di chiamare Firebase.
                    if (password.length < MIN_PASSWORD_LENGTH) {
                        errorMessage = context.getString(R.string.registration_password_min_length, MIN_PASSWORD_LENGTH)
                        return@Button
                    }
                    if (confirmPassword != password) {
                        errorMessage = context.getString(R.string.registration_passwords_dont_match)
                        return@Button
                    }
                    isLoading = true
                    userRepository.registerUserAndUploadImage(
                        email = normalizedEmail,
                        password = password,
                        username = username.trim(),
                        sex = sex,
                        bio = bio,
                        topics = topics,
                        rawBirthTimestamp = rawBirthTimestamp,
                        selectedImageUri = selectedImageUri,
                        context = context,
                        onSuccess = {
                            isLoading = false
                            onVerifyEmailScreenNavigation()
                        },
                        onFailure = { error ->
                            isLoading = false
                            errorMessage = error ?: context.getString(R.string.registration_error_generic)
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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