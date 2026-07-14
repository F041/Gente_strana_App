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
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.gentestrana.R
import com.gentestrana.components.DateOfBirthPicker
import com.gentestrana.components.GenericLoadingScreen
import com.gentestrana.users.UserRepository
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig

val userRepository = UserRepository()

private const val REMOTE_CONFIG_EMAIL_REGEX_KEY = "registration_email_validation_regex"
private const val DEFAULT_EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@(?:gmail\\.com|outlook\\.com|yahoo\\.com|icloud\\.com|protonmail\\.com|live\\.com|hotmail\\.it|yahoo\\.it)$"

@Composable
fun RegistrationScreen(
    onRegistrationSuccess: () -> Unit,
    onVerifyEmailScreenNavigation: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isEmailError by remember { mutableStateOf(false) }
    var sex by remember { mutableStateOf("Undefined") }
    var rawBirthTimestamp by remember { mutableStateOf(0L) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    val remoteConfig = Firebase.remoteConfig
    val emailValidationRegexString = remoteConfig.getString(REMOTE_CONFIG_EMAIL_REGEX_KEY)
    val actualEmailRegexString = emailValidationRegexString.ifBlank { DEFAULT_EMAIL_REGEX }
    val emailRegexPattern = remember(actualEmailRegexString) { actualEmailRegexString.toRegex() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            errorMessage = null
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
                    onValueChange = {
                        email = it
                        isEmailError = !it.matches(emailRegexPattern)
                    },
                    label = { Text(stringResource(R.string.registration_email_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.registration_email_placeholder)) },
                    isError = isEmailError,
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

                // Campo password — oscurata con PasswordVisualTransformation
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.registration_password_label)) },
                    placeholder = { Text(stringResource(R.string.registration_password_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Campo username
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.registration_firstname_label)) },
                    placeholder = { Text(stringResource(R.string.registration_firstname_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
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

                DateOfBirthPicker(
                    context = context,
                    birthTimestamp = rawBirthTimestamp,
                    onDateSelected = { rawBirthTimestamp = it }
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
                    if (isEmailError || email.isBlank() || password.isBlank() || username.isBlank()) {
                        errorMessage = context.getString(R.string.login_fill_fields)
                        if (isEmailError) errorMessage = context.getString(R.string.registration_email_error_message)
                        return@Button
                    }
                    isLoading = true
                    userRepository.registerUserAndUploadImage(
                        email = email,
                        password = password,
                        username = username,
                        sex = sex,
                        bio = "",
                        rawBirthTimestamp = rawBirthTimestamp,
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