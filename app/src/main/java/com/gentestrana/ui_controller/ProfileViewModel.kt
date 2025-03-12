package com.gentestrana.ui_controller

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.gentestrana.users.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.gentestrana.utils.uploadMainProfileImage
import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.gentestrana.utils.normalizeUrl
import com.gentestrana.utils.uploadMultipleImages
import com.google.firebase.firestore.FieldValue


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

    private val _profilePicUrl = MutableStateFlow<List<String>>(emptyList())
    val profilePicUrl: StateFlow<List<String>> = _profilePicUrl

    private val _spokenLanguages = mutableStateOf("")
    val spokenLanguages: State<String> get() = _spokenLanguages

    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location


    init {
        // Carica i dati dell'utente da Firestore
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                user?.let {
                    _username.value = it.username
                    _bio.value = it.bio
                    _topicsText.value = it.topics.joinToString(", ")
                    _profilePicUrl.value = it.profilePicUrl
                    _location.value = it.location ?: ""
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
        updatedProfilePicUrl: List<String>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val updatedData = mapOf(
            "username" to updatedUsername,
            "bio" to updatedBio,
            "topics" to updatedTopics.split(",").map { it.trim() },
            "profilePicUrl" to updatedProfilePicUrl,
            "location" to _location.value
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

    fun setProfilePicOrder(newOrder: List<String>) {
        _profilePicUrl.value = newOrder
    }

    fun deleteProfileImage(index: Int) {
        val currentList = _profilePicUrl.value.toMutableList()
        if (index in currentList.indices) {
            // ✅ Controlla se l'indice è valido prima di rimuovere
            // altrimenti l'app crasha
            // ma esattamente come risolve il problema?
            currentList.removeAt(index)
            _profilePicUrl.value = currentList
        }
    }


    // Funzione per gestire l'upload dell'immagine (semplificata, la funzione uploadProfileImage esistente può essere integrata qui)
    fun uploadNewProfileImage(newImageUri: android.net.Uri, onComplete: (String) -> Unit) {
        viewModelScope.launch { // <--- **INIZIA *DIRETTAMENTE* CON viewModelScope.launch**
            // Usa uploadMultipleImages e passa una lista con UN SOLO URI - **CORRETTO!**
            val imageUrls = uploadMultipleImages(uid, listOf(newImageUri)) // uploadMultipleImages restituisce una LISTA di URL
            val imageUrl = imageUrls.firstOrNull() ?: "" // Prendi il PRIMO URL dalla lista (o stringa vuota se lista vuota)

            if (imageUrl.isNotEmpty()) { // Usa imageUrl (SINGOLO URL) per i controlli successivi - **CORRETTO!**
                viewModelScope.launch {
                    // 1. Recupera la lista attuale da Firestore - **CORRETTO!**
                    val userDoc = firestore.collection("users").document(uid).get().await()
                    val currentProfilePicUrls = userDoc.get("profilePicUrl") as? List<String> ?: emptyList()

                    val normalizedImageUrl = imageUrl.normalizeUrl() // Normalizza imageUrl - **CORRETTO!**
                    val normalizedCurrentUrls = currentProfilePicUrls.map { it.normalizeUrl() } // Normalizza TUTTI gli URL nella lista - **CORRETTO!**

                    // Verifica se l'URL NORMALIZZATO è DUPLICATO - **CORRETTO!**
                    if (normalizedCurrentUrls.contains(normalizedImageUrl)) { // Usa la lista NORMALIZZATA e URL NORMALIZZATO - **CORRETTO!**
                        onComplete("") // Segnala "upload fallito" (stringa vuota) - **CORRETTO!**
                        return@launch // ESCE dalla coroutine SENZA fare l'upload - **CORRETTO!**
                    }

                    // Verifica se il limite è già raggiunto PRIMA di aggiungere - **CORRETTO!**
                    if (currentProfilePicUrls.size >= 3) { // - **CORRETTO!**
                        onComplete("") // Segnala "upload fallito" (stringa vuota) - **CORRETTO!**
                        return@launch // ESCE dalla coroutine SENZA fare l'upload - **CORRETTO!**
                    }
                    // 2. Crea una nuova lista AGGIUNGENDO il nuovo imageUrl - **CORRETTO!**

                    // 3. Aggiorna Firestore con la NUOVA lista - **CORRETTO!**
                    firestore.collection("users").document(uid)
                        .update("profilePicUrl", FieldValue.arrayUnion(imageUrl))
                        .await()

                    // 4. Aggiorna _profilePicUrl (StateFlow) con la lista AGGIORNATA
                    val updatedUserDoc = firestore.collection("users").document(uid).get().await()
                    _profilePicUrl.value = updatedUserDoc.get("profilePicUrl") as? List<String> ?:emptyList()
                }
            }
            onComplete(imageUrl) // Restituisce il SINGOLO imageUrl (o stringa vuota) - **CORRETTO!**
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

    fun setLocation(newLocation: String) {
        _location.value = newLocation
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
