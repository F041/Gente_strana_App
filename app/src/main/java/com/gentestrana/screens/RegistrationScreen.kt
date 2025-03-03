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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.gentestrana.utils.uploadProfileImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.gentestrana.R
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun RegistrationScreen(onRegistrationSuccess: () -> Unit) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var sex by remember { mutableStateOf("Undefined") } // Stato per il sesso selezionato


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
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("usa la tua migliore mail") }, // cambiare con string
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                placeholder = { Text("password forte ma non banale") }, // cambiare con string
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("First name") },// cambiare con string
                placeholder = { Text("Marco, Giovanna, solo il PRIMO nome") }, // cambiare con string
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
                            contentDescription = stringResource(R.string.sex_male),
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
                            contentDescription = stringResource(R.string.sex_female),
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
                            contentDescription = stringResource(R.string.sex_undefined),
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
                Text("Select Profile Picture")
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
                    registerUserAndUploadImage(
                        email = email,
                        password = password,
                        username = username,
                        sex = sex, // Passa la variabile sex
                        selectedImageUri = selectedImageUri,
                        context = context,
                        onSuccess = {
                            isLoading = false
                            onRegistrationSuccess()
                        },
                        onFailure = { error ->
                            isLoading = false
                            errorMessage = error // Imposta il messaggio di errore
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Register")
                }
            }
        }
    }
}

fun registerUserAndUploadImage(
    email: String,
    password: String,
    username: String,
    sex: String, // Aggiunto parametro sex
    selectedImageUri: Uri?,
    context: android.content.Context,
    onSuccess: () -> Unit,
    onFailure: (String?) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = Firebase.firestore

    auth.createUserWithEmailAndPassword(email, password)
        .addOnSuccessListener { authResult ->
            val uid = authResult.user?.uid ?: run {
                onFailure("User ID is null")
                return@addOnSuccessListener
            }

            val userData = mapOf(
                "username" to username,
                "profilePicUrl" to "",
                "age" to 0,
                "sex" to sex // Usa la variabile sex passata come parametro
            )

            firestore.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener {
                    if (selectedImageUri != null) {
                        uploadProfileImage(uid, selectedImageUri) { imageUrl ->
                            if (imageUrl.isNotEmpty()) {
                                updateProfileWithImage(uid, username, imageUrl, onSuccess, onFailure)
                            } else {
                                onFailure("Image upload failed")
                            }
                        }
                    } else {
                        updateProfileWithoutImage(username, onSuccess, onFailure)
                    }
                }
                .addOnFailureListener { e ->
                    onFailure(e.message)
                }
        }
        .addOnFailureListener { e ->
            onFailure(e.message)
        }
}

fun updateProfileWithImage(
    uid: String,
    username: String,
    imageUrl: String,
    onSuccess: () -> Unit,
    onFailure: (String?) -> Unit
) {
    val firestore = Firebase.firestore
    firestore.collection("users").document(uid)
        .update("profilePicUrl", imageUrl)
        .addOnSuccessListener {
            val user = FirebaseAuth.getInstance().currentUser
            val profileUpdates = userProfileChangeRequest {
                displayName = username
                photoUri = Uri.parse(imageUrl)
            }
            user?.updateProfile(profileUpdates)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onSuccess()
                    } else {
                        onFailure(task.exception?.message)
                    }
                }
        }
        .addOnFailureListener { e ->
            onFailure(e.message)
        }
}

fun updateProfileWithoutImage(
    username: String,
    onSuccess: () -> Unit,
    onFailure: (String?) -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    val profileUpdates = userProfileChangeRequest {
        displayName = username
    }
    user?.updateProfile(profileUpdates)
        ?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess()
            } else {
                onFailure(task.exception?.message)
            }
        }
}