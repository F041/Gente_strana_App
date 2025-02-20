package com.gentestrana.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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

@Composable
fun RegistrationScreen(onRegistrationSuccess: () -> Unit) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    isLoading = true
                    registerUserAndUploadImage(
                        email = email,
                        password = password,
                        username = username,
                        bio = bio,
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
    bio: String,
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
                "bio" to bio,
                "profilePicUrl" to "",
                "age" to 0,
                "sex" to ""
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
