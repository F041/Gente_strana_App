@file:OptIn(ExperimentalMaterial3Api::class)

package com.gentestrana.screens

import android.app.DatePickerDialog
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.gentestrana.utils.uploadProfileImage
import com.gentestrana.users.User
import java.text.SimpleDateFormat
import java.util.*

/**
 * Schermata del profilo personale.
 *
 * Utilizza il modello [User] per recuperare e aggiornare i dati dell'utente.
 *
 * Campi principali:
 * - **username**: nome utente.
 * - **bio**: breve descrizione personale.
 * - **description**: lista di argomenti (gestita in UI come stringa separata da virgola).
 * - **profilePicUrl**: lista di URL per le immagini del profilo. Il primo elemento è l'immagine principale.
 * - **birthTimestamp**: timestamp della data di nascita; la UI visualizza la data formattata ("dd/MM/yyyy") e la aggiorna tramite DatePicker.
 * - **sex**: valore ammesso "M", "F" o "Undefined" (la validazione è gestita in UI).
 * - **spokenLanguages**: lingue parlate, gestite come stringa separata da virgola.
 * - **location**: paese, ottenuto ad esempio dal GPS al primo utilizzo.
 */
@Composable
fun PersonalProfileScreen(
    userProfilePicUrl: List<String>,
    navController: NavHostController
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: return
    val firestore = Firebase.firestore

    // Recupera i dati utente da Firestore
    val userState = produceState<User?>(initialValue = null) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                value = document.toObject(User::class.java)
            }
            .addOnFailureListener { e ->
                Log.e("PersonalProfileScreen", "Error fetching user data", e)
            }
    }

    // Mostra un loader se i dati non sono ancora caricati
    if (userState.value == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Stati modificabili per i campi del profilo
    var username by remember { mutableStateOf("") }
    var birthDateText by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var profilePicUrlState by remember { mutableStateOf(
        if (userProfilePicUrl.isNotEmpty()) userProfilePicUrl.first()
        else "https://icons.veryicon.com/png/o/system/ali-mom-icon-library/random-user.png"
    ) }
    var birthTimestamp by remember { mutableStateOf(0L) }  // Timestamp della data di nascita
    var sex by remember { mutableStateOf("Undefined") }
    var spokenLanguagesText by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var newImageUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher per la selezione di una nuova immagine
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        newImageUri = uri
    }

    // Popola gli stati dai dati recuperati
    LaunchedEffect(userState.value) {
        userState.value?.let { user ->
            username = user.username
            bio = user.bio
            descriptionText = user.description.joinToString(", ")
            profilePicUrlState = user.profilePicUrl.firstOrNull()
                ?: "https://icons.veryicon.com/png/o/system/ali-mom-icon-library/random-user.png"
            // Usa la proprietà birthTimeMillis
            birthTimestamp = user.birthTimestamp
            birthDateText = if (birthTimestamp != 0L) {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                sdf.format(Date(birthTimestamp))
            } else ""
            sex = user.sex
            spokenLanguagesText = user.spokenLanguages.joinToString(", ")
            location = user.location
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personal Profile") },
                actions = {
                    IconButton(onClick = {
                        auth.signOut()
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            // Visualizza l'immagine principale del profilo
            Image(
                painter = rememberAsyncImagePainter(newImageUri ?: profilePicUrlState),
                contentDescription = "Profile Picture",
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Bottone per selezionare una nuova immagine
            Button(onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()) {
                Text("Change Profile Picture")
            }
            if (newImageUri != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            isUploading = true
                            uploadProfileImage(uid, newImageUri!!) { imageUrl ->
                                if (imageUrl.isNotEmpty()) {
                                    firestore.collection("users").document(uid)
                                        .update("profilePicUrl", listOf(imageUrl))
                                        .addOnSuccessListener {
                                            profilePicUrlState = imageUrl
                                            newImageUri = null
                                            isUploading = false
                                            Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
                                            val user = FirebaseAuth.getInstance().currentUser
                                            val profileUpdates = userProfileChangeRequest {
                                                photoUri = Uri.parse(imageUrl)
                                            }
                                            user?.updateProfile(profileUpdates)
                                        }
                                        .addOnFailureListener { e ->
                                            isUploading = false
                                            Toast.makeText(context, "Failed to update picture", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    isUploading = false
                                    Toast.makeText(context, "Image upload failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUploading
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Upload New Picture")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Campo per Username
            ProfileTextField(value = username, onValueChange = { username = it }, label = "Username")
            Spacer(modifier = Modifier.height(8.dp))
            // Campo per Bio
            ProfileTextField(value = bio, onValueChange = { bio = it }, label = "Bio")
            Spacer(modifier = Modifier.height(8.dp))
            // Campo per Description (argomenti), separati da virgola
            ProfileTextField(value = descriptionText, onValueChange = { descriptionText = it }, label = "Description (comma separated)")
            Spacer(modifier = Modifier.height(8.dp))
            // Campo per la data di nascita tramite DatePickerDialog
            DateOfBirthPicker(
                birthTimestamp = birthTimestamp,
                onDateSelected = { newTimestamp ->
                    birthTimestamp = newTimestamp
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Campo per Sex
            ProfileTextField(value = sex, onValueChange = { newSex ->
                if (newSex == "M" || newSex == "F" || newSex == "Undefined") {
                    sex = newSex
                }
            }, label = "Sex (M, F, Undefined)")
            Spacer(modifier = Modifier.height(8.dp))
            // Campo per Lingue parlate, come stringa separata da virgola
            ProfileTextField(value = spokenLanguagesText, onValueChange = { spokenLanguagesText = it }, label = "Spoken Languages (comma separated)")
            Spacer(modifier = Modifier.height(8.dp))
            // Campo per Location (nazione)
            ProfileTextField(value = location, onValueChange = { location = it }, label = "Location (Country)")
            Spacer(modifier = Modifier.height(16.dp))
            // Bottone per salvare gli aggiornamenti del profilo
            Button(
                onClick = {
                    if (username.isBlank()) {
                        Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val updatedData = mapOf(
                        "username" to username,
                        "bio" to bio,
                        "description" to descriptionText.split(",").map { it.trim() },
                        "profilePicUrl" to listOf(profilePicUrlState),
                        // Salva il campo come Timestamp, non come Long
                        "rawBirthTimestamp" to com.google.firebase.Timestamp(Date(birthTimestamp)),
                        "sex" to sex,
                        "spokenLanguages" to spokenLanguagesText.split(",").map { it.trim() },
                        "location" to location
                    )
                    firestore.collection("users").document(uid)
                        .set(updatedData, SetOptions.merge())
                        .addOnSuccessListener {
                            val user = FirebaseAuth.getInstance().currentUser
                            val profileUpdates = userProfileChangeRequest {
                                displayName = username
                                photoUri = Uri.parse(profilePicUrlState)
                            }
                            user?.updateProfile(profileUpdates)
                                ?.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Firebase profile update failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Profile")
            }
        }
    }
}

/**
 * Funzione helper per creare un OutlinedTextField a tutta larghezza.
 */
@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Composable per selezionare la data di nascita tramite DatePickerDialog.
 */
@Composable
fun DateOfBirthPicker(
    birthTimestamp: Long,
    onDateSelected: (Long) -> Unit
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    if (birthTimestamp > 0L) {
        calendar.timeInMillis = birthTimestamp
    }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val newCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                }
                onDateSelected(newCalendar.timeInMillis)
                showDialog = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Button(
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        val displayText = if (birthTimestamp > 0L) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.format(Date(birthTimestamp))
        } else {
            "Set Date of Birth"
        }
        Text(text = displayText)
    }
}
