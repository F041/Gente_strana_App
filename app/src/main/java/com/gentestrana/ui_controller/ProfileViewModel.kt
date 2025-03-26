package com.gentestrana.ui_controller

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.gentestrana.users.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope
import com.gentestrana.utils.convertImageUriToWebP
import com.gentestrana.utils.deleteProfileImageFromStorage
import com.gentestrana.utils.generateMD5HashFromUri
import com.gentestrana.utils.isValidImageUri
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.gentestrana.utils.normalizeUrl
import com.gentestrana.utils.saveByteArrayToTempFile
import com.gentestrana.utils.uploadMultipleImages
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.UUID




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

    // Stato per l'utente corrente
    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState.asStateFlow()
    // Versione StateFlow "calda" per l'utente
    val liveUserState: StateFlow<User?> = _userState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // Mantiene lo stato attivo per 5 secondi dopo l'ultima sottoscrizione
        initialValue = null
    )


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


    suspend fun deleteProfileImage(imageUrlToDelete: String) {
        Log.d("ProfileViewModel", "deleteProfileImage STARTED - imageUrlToDelete: $imageUrlToDelete")

        // 1. Recupera la lista attuale da Firestore (RE-FETCH per sicurezza)
        val userDoc = firestore.collection("users").document(uid).get().await()
        val currentProfilePicUrls = userDoc.get("profilePicUrl") as? List<String> ?: emptyList()

        // 🚩 LOG AGGIUNTO: Log della lista *RAW* recuperata da Firestore, PRIMA della normalizzazione
        Log.d("ProfileViewModel", "currentProfilePicUrls (RAW from Firestore):") // 🚩 INIZIO LOG LISTA RAW
        currentProfilePicUrls.forEachIndexed { index, rawUrl ->
            Log.d("ProfileViewModel", "  [$index]: $rawUrl") // 🚩 LOG DI OGNI URL RAW
        }
        Log.d("ProfileViewModel", "currentProfilePicUrls (RAW from Firestore) END") // 🚩 FINE LOG LISTA RAW


        // Normalizza imageUrlToDelete
        val normalizedImageUrlToDelete = imageUrlToDelete.normalizeUrl()
        Log.d("ProfileViewModel", "normalizedImageUrlToDelete: $normalizedImageUrlToDelete")

        // Normalizza TUTTI gli URL nella currentProfilePicUrls per il confronto
        val normalizedCurrentList = currentProfilePicUrls.map { it.normalizeUrl() }

        // LOG della LISTA NORMALIZZATA COMPLETA (GIA' PRESENTE)
        Log.d("ProfileViewModel", "normalizedCurrentList:")
        normalizedCurrentList.forEachIndexed { index, normalizedUrl ->
            Log.d("ProfileViewModel", "  [$index]: $normalizedUrl")
        }
        Log.d("ProfileViewModel", "normalizedCurrentList END")


        // Trova l'indice dell'imageUrl NORMALIZZATO nella lista NORMALIZZATA
        val index = normalizedCurrentList.indexOf(normalizedImageUrlToDelete)

        if (index != -1) {
            // ✅ Usa currentProfilePicUrls (lista da Firestore) e crea una COPIA MUTABILE
            val mutableCurrentList = currentProfilePicUrls.toMutableList()

            // Usa l'indice TROVATO (che si riferisce alla lista NORMALIZZATA)
            // per rimuovere l'elemento dalla lista MUTABILE (mutableCurrentList)
            if (index in mutableCurrentList.indices) { // Condizione ancora ridondante, ma la lasciamo per ora
                mutableCurrentList.removeAt(index)
                _profilePicUrl.value = mutableCurrentList // Aggiorna StateFlow con la lista MUTABILE MODIFICATA
            }
            deleteProfileImageFromStorage(imageUrlToDelete) { isDeletionSuccessful -> // Usa imageUrlToDelete ORIGINALE per l'eliminazione da Storage
                if (isDeletionSuccessful) {
                    Log.d("ProfileViewModel", "Immagine eliminata con successo da Storage: $imageUrlToDelete")
                    viewModelScope.launch {
                        try {
                            firestore.collection("users").document(uid)
                                .update("profilePicUrl", mutableCurrentList) // Aggiorna Firestore con la lista MUTABILE MODIFICATA
                                .await()
                            Log.d("ProfileViewModel", "Firestore aggiornato con la lista immagini modificata.")
                        } catch (e: Exception) {
                            Log.e("ProfileViewModel", "Errore nell'aggiornamento di Firestore dopo eliminazione immagine: ${e.message}")
                        }
                    }
                } else {
                    Log.e("ProfileViewModel", "Errore nell'eliminazione dell'immagine da Storage: $imageUrlToDelete")
                }
            }
        } else {
            Log.w("ProfileViewModel", "imageUrlToDelete NON TROVATO (dopo normalizzazione) nella lista _profilePicUrl: $normalizedImageUrlToDelete")
        }
        Log.d("ProfileViewModel", "deleteProfileImage ENDED - imageUrlToDelete: $imageUrlToDelete")
    }

    // Funzione per gestire l'upload dell'immagine

    fun uploadNewProfileImage(newImageUri: Uri, context: Context, onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Validazione formato immagine
                if (!isValidImageUri(context, newImageUri)) {
                    withContext(Dispatchers.Main) {
                        onComplete("INVALID_FORMAT")
                    }
                    return@launch
                }

                // Conversione a WebP
                val webpData = convertImageUriToWebP(context, newImageUri) ?: run {
                    withContext(Dispatchers.Main) { onComplete("") }
                    return@launch
                }

                // Salvataggio file temporaneo
                val tempFileName = "temp_${UUID.randomUUID()}.webp"
                val tempFileUri = saveByteArrayToTempFile(context, webpData, tempFileName)
                    ?: run {
                        withContext(Dispatchers.Main) { onComplete("") }
                        return@launch
                    }

                // Generazione hash MD5
                val newHash = generateMD5HashFromUri(context, tempFileUri)
                    ?: run {
                        withContext(Dispatchers.Main) { onComplete("") }
                        return@launch
                    }

                // Controllo duplicati
                val duplicateFound = withContext(Dispatchers.Default) {
                    _profilePicUrl.value.any { it.contains(newHash, true) }
                }

                if (duplicateFound) {
                    withContext(Dispatchers.Main) {
                        onComplete("DUPLICATE")
                    }
                    return@launch
                }

                // Upload immagine (funzione suspend chiamata in coroutine)
                val imageUrls = uploadMultipleImages(uid, listOf(tempFileUri), context)
                val newUrl = imageUrls.firstOrNull()?.first ?: ""

                // Aggiornamento Firestore

                if (newUrl.isNotEmpty()) {
                    // Aggiornamento Firestore con transazione
                    firestore.runTransaction { transaction ->
                        val userDoc = transaction.get(firestore.collection("users").document(uid))
                        val currentUrls = userDoc.get("profilePicUrl") as? List<String> ?: emptyList()
                        val updatedUrls = currentUrls + newUrl
                        transaction.update(userDoc.reference, "profilePicUrl", updatedUrls)
                    }.await()

                    // Aggiorna lo stato locale
                    _profilePicUrl.value = _profilePicUrl.value + newUrl
                }

                // Notifica il completamento
                withContext(Dispatchers.Main) {
                    onComplete(newUrl)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete("")
                }
            }
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
