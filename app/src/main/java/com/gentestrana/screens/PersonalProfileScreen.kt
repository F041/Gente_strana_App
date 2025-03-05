@file:OptIn(ExperimentalMaterial3Api::class)
package com.gentestrana.screens

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.gentestrana.R
import com.google.firebase.auth.FirebaseAuth
import com.gentestrana.ui_controller.ProfileViewModel
import com.gentestrana.users.User
import com.gentestrana.components.DateOfBirthPicker
import com.gentestrana.components.ProfileBioBox
import com.gentestrana.components.ProfileLanguagesField
import com.gentestrana.components.ProfileTextField
import com.gentestrana.components.ProfileTopicsList
import com.gentestrana.utils.LocationUtils
import com.gentestrana.utils.OperationResult


/**
 * Schermata del profilo personale.
 *
 * Utilizza il modello [User] per recuperare e aggiornare i dati dell'utente.
 *
 * Campi principali:
 * - **profilePicUrl**: lista di per le immagini del profilo. Il primo elemento è l'immagine principale.
 * - **username**: nome utente.
 * - **topics**: lista di argomenti (gestita in UI come stringa separata da virgola).
 * - **bio**: breve descrizione personale.
 *
 * - **birthTimestamp**: timestamp della data di nascita; la UI visualizza la data formattata ("dd/MM/yyyy") e la aggiorna tramite DatePicker.
 * - **spokenLanguages**: lingue parlate, gestite come stringa separata da virgola.
 * - **location**: paese, ottenuto ad esempio dal GPS al primo utilizzo.
 */

@Composable
fun PersonalProfileScreen(
    navController: NavHostController
) {
    // Otteniamo il ViewModel (situato nella cartella ui_controller)
    val profileViewModel: ProfileViewModel = viewModel()
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: return

    // Stati osservati dal ViewModel
    val username by profileViewModel.username.collectAsState()
    val bio by profileViewModel.bio.collectAsState()
    val topicsText by profileViewModel.topicsText.collectAsState()
    val profilePicUrl by profileViewModel.profilePicUrl.collectAsState()
    var birthTimestamp by remember { mutableStateOf(0L) }
    val topicsList = profileViewModel.topicsText.collectAsState().value
        .split(",") // Divide la stringa in base alla virgola
        .map { it.trim() } // Rimuove spazi extra
        .filter { it.isNotEmpty() } // Evita elementi vuoti

    val spokenLanguages by profileViewModel.spokenLanguages // Nessun .collectAsState()

    // Altri stati locali per campi non ancora gestiti nel ViewModel
    var isUploading by remember { mutableStateOf(false) }
    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLocationLoading by remember { mutableStateOf(false) }

    // **INIZIO FUNZIONE getLocation **
    fun getLocation(context: Context) {
        isLocationLoading = true
        // **USA DIRETTAMENTE requestCurrentLocationName (CHE GESTISCE GIA' GPS E NETWORK)**
        // **USA DIRETTAMENTE requestCurrentLocationName (CHE GESTISCE GIA' GPS E NETWORK)**
        LocationUtils.requestCurrentLocationName(context) { locationResult -> // Chiama DIRETTAMENTE requestCurrentLocationName
            isLocationLoading = false // <-- NASCONDI ProgressIndicator (FINE localizzazione - successo o errore)
            when (locationResult) {
                is OperationResult.Success -> {
                    // **CORREZIONE: Chiama setLocation DENTRO il case Success**
                    profileViewModel.setLocation(locationResult.data) // <-- Chiama setLocation QUI, DENTRO Success
                    // location = locationResult.data // NON serve più (location locale non usata)
                }
                is OperationResult.Error -> {
                    Toast.makeText(context, "Errore localizzazione: ${locationResult.message}", Toast.LENGTH_LONG).show() // Mostra errore (se ENTRAMBI i provider falliscono)
                    Log.w("PersonalProfileScreen", "Timeout localizzazione (Network e GPS): ${locationResult.message}") // Log WARNING invece di ERRORE
                }
            }
        }
    }
    // **FINE FUNZIONE getLocation (LOCAL FUNCTION)**

    // Launcher per la richiesta dei permessi di localizzazione
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permesso concesso, ora possiamo ottenere la posizione
            getLocation(context)
        } else {
            // Permesso negato, gestisci la situazione (es. mostra un messaggio)
            Toast.makeText(context, "Permesso di localizzazione negato", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher per selezionare una nuova immagine dalla galleria
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        newImageUri = uri
    }

    LaunchedEffect(Unit) {
        profileViewModel.loadUserData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.personal_profile))},
                actions = {
                    // Pulsante di logout
                    IconButton(onClick = {
                        auth.signOut()
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            // Mostra l'immagine del profilo
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f) // <--- Usa una frazione di fillMaxWidth, es. 0.5f (50% della larghezza)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .align(Alignment.CenterHorizontally) // Centra il Box orizzontalmente (opzionale)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(newImageUri ?: profilePicUrl),
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(), // Mantieni fillMaxSize per riempire il Box (ora più piccolo)
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Bottone per scegliere una nuova immagine dalla galleria
            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.change_profile_picture))
            }
            if (newImageUri != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        isUploading = true
                        newImageUri?.let { uri ->
                            profileViewModel.uploadNewProfileImage(uri) { downloadUrl ->
                                // Se downloadUrl è vuoto, il ViewModel imposterà il fallback
                                isUploading = false
                                newImageUri = null
                                Toast.makeText(
                                    context, // Assuming 'context' is available in your scope
                                    context.getString(R.string.profile_picture_updated), // Use getString to get resource
                                    Toast.LENGTH_SHORT
                                ).show()
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
                        Text(stringResource(id = R.string.upload_new_profile_picture))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Campo per Username
            ProfileTextField(
                value = username,
                onValueChange = { profileViewModel.setUsername(it) },
                label = stringResource(id = R.string.first_name),
                placeholder = stringResource(id = R.string.name_placeholder),
                minLength=2,
                maxLength = 13,
                errorMessage = "Too short", //stringabile
                removeSpaces = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Campo per Topics (ex description)
            ProfileTopicsList(
                title = stringResource(id = R.string.topics_title),
                topics = topicsList, // Ora è una lista di stringhe, non più una singola stringa
                placeholder = stringResource(id = R.string.topics_placeholder),
                newTopicMaxLength = 200,
                onValueChange = { updatedTopics ->
                    // Convertiamo la lista aggiornata in una stringa prima di inviarla al ViewModel
                    profileViewModel.setTopics(updatedTopics.joinToString(", "))
                }
            )

            // Campo per Bio
            ProfileBioBox(
                initialContent = bio,
                onValueChange = { updatedBio -> profileViewModel.setBio(updatedBio) },
                modifier = Modifier.padding(16.dp),
                maxLength = 800,
            )

            Spacer(modifier = Modifier.height(8.dp))

            DateOfBirthPicker(
                context = LocalContext.current,
                birthTimestamp = birthTimestamp,
                onDateSelected = { newTimestamp ->
                    birthTimestamp = newTimestamp
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bottone "Ottieni Località" - getLocation CHIAMATA DOPO LA DICHIARAZIONE
            Button(
                onClick = {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        getLocation(context)
                    } else {
                        locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.get_location))
            }

            Spacer(modifier = Modifier.height(8.dp))


            // **SEZIONE ProgressIndicator e TESTO STATO (AGGIUNTA)**
            if (isLocationLoading) { // Mostra ProgressIndicator SOLO se isLocationLoading è true
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally, // Centra orizzontalmente
                    modifier = Modifier.padding(vertical = 8.dp) // Padding verticale
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp)) // ProgressIndicator
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ricerca della posizione...", // Testo di stato (stringabile) - OPZIONALE
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) // Stile testo di stato
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // **SEZIONE UI PER MOSTRARE LA LOCALITÀ
            val currentLocation = profileViewModel.location.collectAsState().value // <-- OTTIENI location DAL ViewModel
            currentLocation?.let { locationName -> // Usa currentLocation (dal ViewModel)
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(
                        text = stringResource(R.string.your_location).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = locationName, // Mostra currentLocation (dal ViewModel)
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            ProfileLanguagesField(
                selectedLanguages = spokenLanguages,
                onLanguagesChanged = { newLanguages ->
                    profileViewModel.setSpokenLanguages(newLanguages)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bottone per salvare gli aggiornamenti del profilo
            Button(
                onClick = {
                    profileViewModel.updateProfile(
                        updatedUsername = username,
                        updatedBio = bio,
                        updatedTopics = topicsText,
                        updatedProfilePicUrl = profilePicUrl,
                        onSuccess = {
                            Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { error ->
                            Toast.makeText(context, "Update failed: $error", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            {
                Text(text = stringResource(R.string.save_profile))
            }
        }
    }
}