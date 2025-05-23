@file:OptIn(ExperimentalMaterial3Api::class)

package com.gentestrana.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.gentestrana.R
import com.google.firebase.auth.FirebaseAuth
import com.gentestrana.ui_controller.ProfileViewModel
import com.gentestrana.users.User
import com.gentestrana.components.DateOfBirthPicker
import com.gentestrana.components.GenericLoadingScreen
import com.gentestrana.components.PersonalProfilePreviewCard
import com.gentestrana.components.ProfileBioBox
import com.gentestrana.components.ProfileLanguagesField
import com.gentestrana.components.ProfileLocationDisplay
import com.gentestrana.components.ProfileTextField
import com.gentestrana.components.ProfileTopicsList
import com.gentestrana.components.ReorderableProfileImageGridWithAdd
import com.gentestrana.utils.LocationUtils
import com.gentestrana.utils.OperationResult
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalProfileScreen(
    navController: NavHostController
) {
    // Otteniamo il ViewModel (situato nella cartella ui_controller)
    val profileViewModel: ProfileViewModel = viewModel()
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    // val uid = auth.currentUser?.uid ?: return


    // Stati osservati dal ViewModel
    val username by profileViewModel.username.collectAsState()
    val bio by profileViewModel.bio.collectAsState()
    val topicsText by profileViewModel.topicsText.collectAsState()
    val profilePicUrl by profileViewModel.profilePicUrl.collectAsState()

    val birthTimestamp by profileViewModel.birthTimestamp
    val topicsList = profileViewModel.topicsText.collectAsState().value
        .split(",") // Divide la stringa in base alla virgola
        .map { it.trim() } // Rimuove spazi extra
        .filter { it.isNotEmpty() } // Evita elementi vuoti

    val spokenLanguages by profileViewModel.spokenLanguages
    var uploadLimitExceeded by remember { mutableStateOf(false) }

    // Altri stati locali per campi non ancora gestiti nel ViewModel
    var isUploading by remember { mutableStateOf(false) }
    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLocationLoading by remember { mutableStateOf(false) }
    val currentLocation = profileViewModel.location.collectAsState().value
    val coroutineScope = rememberCoroutineScope()

    // **INIZIO FUNZIONE getLocation **
    fun getLocation(context: Context) {
        isLocationLoading = true
        // **USA DIRETTAMENTE requestCurrentLocationName (CHE GESTISCE GIA' GPS E NETWORK)**
        LocationUtils.requestCurrentLocationName(context) { locationResult -> // Chiama DIRETTAMENTE requestCurrentLocationName
            isLocationLoading =
                false // <-- NASCONDI ProgressIndicator (FINE localizzazione - successo o errore)
            when (locationResult) {
                is OperationResult.Success -> {
                    profileViewModel.setLocation(locationResult.data) // <-- Chiama setLocation QUI, DENTRO Success
                }

                is OperationResult.Error -> {
                    Toast.makeText(
                        context,
                        "Errore localizzazione: ${locationResult.message}",
                        Toast.LENGTH_LONG
                    ).show() // Mostra errore (se ENTRAMBI i provider falliscono)
                    Log.w(
                        "PersonalProfileScreen",
                        "Timeout localizzazione (Network e GPS): ${locationResult.message}"
                    ) // Log WARNING invece di ERRORE
                }
            }
        }
    }

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
        if (uri != null) {
            isUploading = true  // Attiva l'indicatore di caricamento
            profileViewModel.uploadNewProfileImage(uri, context) { result ->
                isUploading = false  // Disattiva l'indicatore in ogni caso
                when {
                    result == "DUPLICATE" -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.duplicate_image),
                            Toast.LENGTH_SHORT
                        ).show()
                        uploadLimitExceeded = false
                    }

                    result == "LIMIT_EXCEEDED" -> {
                        // Imposta lo stato per mostrare il messaggio con la clessidra
                        uploadLimitExceeded = true
                    }

                    result.isEmpty() -> {
                        Toast.makeText(
                            context, "❌",
                            Toast.LENGTH_SHORT
                        ).show()
                        uploadLimitExceeded = false
                    }

                    else -> {
                        Toast.makeText(context, "👌", Toast.LENGTH_SHORT).show()
                        uploadLimitExceeded = false
                    }
                }
            }
        } else {
            isUploading = false
        }
    }

    LaunchedEffect(Unit) {
        profileViewModel.loadUserData()
    }

    val user by profileViewModel.userState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.personal_profile)) },
                actions = {

                    IconButton(onClick = {
                        val videoUrl = "https://youtube.com/shorts/hD9KP_bgxuE"
                        // TikTok darebbe problemi in Italia, richiederebbe Login
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.QuestionMark,
                            contentDescription = "Guida su come completare il profilo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Icona anteprima profilo
                    IconButton(onClick = {
                        val currentUserId = auth.currentUser?.uid
                        if (currentUserId != null) {
                            navController.navigate("userProfile/${currentUserId}")
                        } else {
                            Toast.makeText(context, "Utente non loggato", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.RemoveRedEye,
                            contentDescription = "Profilo (anteprima)",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

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
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
//                .padding(16.dp) fonte della discordia rispetto ProfileContent
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Zona gallery immagini profilo
                Box {
                    key(profilePicUrl) {
                        ReorderableProfileImageGridWithAdd(
                            images = profilePicUrl,
                            maxImages = 3,
                            isUploading = isUploading,
                            onImageOrderChanged = { newOrder ->
                                profileViewModel.setProfilePicOrder(newOrder)
                            },
                            onDeleteImage = { imageUrl ->
                                coroutineScope.launch {
                                    profileViewModel.deleteProfileImage(imageUrl)
                                }
                            },
                            onAddImage = {
                                imagePickerLauncher.launch("image/*")
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (uploadLimitExceeded) {
                    Text(
                        text = "⏰⏳",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                // Gli space si potrebbero reintrodurre per coerenza ed eliminarli in ProfileImageSection

                // Campo per Username
                ProfileTextField(
                    value = username,
                    onValueChange = { profileViewModel.setUsername(it) },
                    label = stringResource(id = R.string.first_name),
                    placeholder = stringResource(id = R.string.name_placeholder),
                    minLength = 2,
                    maxLength = 13,
                    errorMessage = "❌",
                    removeSpaces = true,
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Campo per Topics (ex description)
                ProfileTopicsList(
                    title = stringResource(id = R.string.topics_title),
                    topics = topicsList,
                    placeholder = stringResource(id = R.string.topics_placeholder),
                    newTopicMaxLength = 200,
                    onValueChange = { updatedTopics ->
                        profileViewModel.setTopics(updatedTopics.joinToString(", "))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp) // Allinea al padding di ProfileContent
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo bio
                ProfileBioBox(
                    initialContent = bio,
                    onValueChange = { updatedBio -> profileViewModel.setBio(updatedBio) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                DateOfBirthPicker(
                    context = LocalContext.current,
                    birthTimestamp = birthTimestamp ?: 0L, // Rimosso .value
                    onDateSelected = { newTimestamp ->
                        profileViewModel.setBirthTimestamp(newTimestamp)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bottone "Ottieni Località" - getLocation CHIAMATA DOPO LA DICHIARAZIONE
                Button(
                    onClick = {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            getLocation(context)
                        } else {
                            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(stringResource(R.string.get_location))
                }

                Spacer(modifier = Modifier.height(8.dp))


                // SEZIONE ProgressIndicator
                if (isLocationLoading) {
                    // Mostra ProgressIndicator SOLO se isLocationLoading è true
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally, // Centra orizzontalmente
                        modifier = Modifier.padding(vertical = 8.dp) // Padding verticale
                    ) {
                        user?.let {
                            PersonalProfilePreviewCard(
                                user = it,
                                modifier = Modifier.padding(bottom = 16.dp) // Spazio sotto l'anteprima
                            )
                        }

                        GenericLoadingScreen(modifier = Modifier.size(24.dp)) // ProgressIndicator
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\uD83D\uDD0D \uD83D\uDCCD...", // stringabile
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) // Stile testo di stato
                        )
                    }
                }

                // Mostra posizione
                ProfileLocationDisplay(locationName = currentLocation)

                Spacer(modifier = Modifier.height(16.dp))

                ProfileLanguagesField(
                    selectedLanguages = spokenLanguages,
                    onLanguagesChanged = { newLanguages ->
                        profileViewModel.setSpokenLanguages(newLanguages)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
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
                            updatedBirthTimestamp = birthTimestamp,
                            onSuccess = {
                                Toast.makeText(context, "✅", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { error ->
                                Toast.makeText(context, "⛔: $error", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                {
                    Text(text = stringResource(R.string.save_profile))
                }
            }
        })
}