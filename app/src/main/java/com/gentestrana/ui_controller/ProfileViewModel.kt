package com.gentestrana.ui_controller

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.gentestrana.users.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.gentestrana.utils.uploadProfileImage
import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


class ProfileViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val uid = auth.currentUser?.uid ?: ""
    private val firestore = Firebase.firestore

    // Stati per il profilo
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    private val _bio = MutableStateFlow("")
    val bio: StateFlow<String> = _bio

    private val _topicsText = MutableStateFlow("")
    val topicsText: StateFlow<String> = _topicsText

    private val _profilePicUrl = MutableStateFlow("res/drawable/random_user.webp")
    val profilePicUrl: StateFlow<String> = _profilePicUrl

    private val _spokenLanguages = mutableStateOf("")
    val spokenLanguages: State<String> get() = _spokenLanguages

    // Altri stati (birthTimestamp, sex, spokenLanguages, location, ecc.) possono essere aggiunti qui

    init {
        // Carica i dati dell'utente da Firestore
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                user?.let {
                    _username.value = it.username
                    _bio.value = it.bio
                    _topicsText.value = it.topics.joinToString(", ")
                    _profilePicUrl.value = it.profilePicUrl.firstOrNull() ?: "res/drawable/random_user.webp"
                    // Aggiorna gli altri stati se necessario
                }
            }
            .addOnFailureListener { e ->
                // Gestione dell'errore: si potrebbe usare un ulteriore StateFlow per gli errori
            }
    }

    fun updateProfile(
        updatedUsername: String,
        updatedBio: String,
        updatedTopics: String,
        updatedProfilePicUrl: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val updatedData = mapOf(
            "username" to updatedUsername,
            "bio" to updatedBio,
            "topics" to updatedTopics.split(",").map { it.trim() },
            "profilePicUrl" to listOf(updatedProfilePicUrl)
        )

        firestore.collection("users").document(uid)
            .update(updatedData) // causa del permission denied che affligge da 5 giorni?
            .addOnSuccessListener {
                _username.value = updatedUsername
                _bio.value = updatedBio
                _topicsText.value = updatedTopics
                _profilePicUrl.value = updatedProfilePicUrl
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Errore sconosciuto")
            }
    }


    // Funzione per gestire l'upload dell'immagine (semplificata, la funzione uploadProfileImage esistente può essere integrata qui)
    fun uploadNewProfileImage(newImageUri: android.net.Uri, onComplete: (String) -> Unit) {
        // Qui chiameremo la funzione uploadProfileImage passando uid e newImageUri
        // E aggiorneremo _profilePicUrl al termine
        uploadProfileImage(uid, newImageUri) { imageUrl ->
            if (imageUrl.isNotEmpty()) {
                _profilePicUrl.value = imageUrl
            }
            onComplete(imageUrl)
        }
    }

    fun setUsername(newUsername: String) {
        _username.value = newUsername
    }

    fun setTopics(newTopics: String) {
        _topicsText.value = newTopics
    }

    fun setBio(newBio: String) {
        _bio.value = newBio
    }

    // Carica i dati iniziali dall'utente
    fun loadUserData() {
        viewModelScope.launch {
            try {
//                println("1. Inizio caricamento lingue...")
                val userId = auth.currentUser?.uid ?: return@launch.also {
                    println("2. Utente non loggato")
                }
//                println("3. User ID: $userId")
                val userDoc = db.collection("users").document(userId).get().await()
//                println("4. Documento ottenuto: ${userDoc.exists()}")
                val languagesList = userDoc.get("spokenLanguages") as? List<String> ?: emptyList()
                val languages = languagesList.joinToString(",")
//                println("5. Lingue trovate: $languages")
                _spokenLanguages.value = languages
//                println("6. Stato aggiornato: ${_spokenLanguages.value}")
            } catch (e: Exception) {
                println("ERRORE durante il caricamento: ${e.message}")
            }
        }
    }

    // Salva le lingue su Firestore
    fun setSpokenLanguages(languages: String) {
        _spokenLanguages.value = languages
        viewModelScope.launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                db.collection("users").document(userId)
                    .update("spokenLanguages", languages.split(","))
                    .await()
            } catch (e: Exception) {
//                println("Lingue caricate: ${_spokenLanguages.value}")
//                println("Lingue salvate: $languages")
            }
        }
    }
}
