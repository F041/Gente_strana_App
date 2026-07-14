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
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.UUID




class ProfileViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    // FIX BUG: L'uid NON viene più salvato al construction time come stringa vuota.
    // Usiamo una funzione per ottenerlo sempre fresco da auth.currentUser.
    private fun getUid(): String? = auth.currentUser?.uid
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

    private val _birthTimestamp = mutableStateOf<Long?>(null)
    val birthTimestamp: State<Long?> = _birthTimestamp

    fun setBirthTimestamp(newTimestamp: Long?) {
        _birthTimestamp.value = newTimestamp
        val uid = getUid() ?: return // FIX: uid reattivo
        newTimestamp?.let {
            val seconds = it / 1000
            val nanoseconds = (it % 1000) * 1_000_000
            firestore.collection("users").document(uid)
                .update("rawBirthTimestamp", com.google.firebase.Timestamp(seconds, nanoseconds.toInt()))
                .addOnSuccessListener {
//                    Log.d("ProfileViewModel", "Birth timestamp aggiornato su Firestore: $newTimestamp")
                }
                .addOnFailureListener { e ->
//                    Log.e("ProfileViewModel", "Errore aggiornando birth timestamp: ${e.localizedMessage}")
                }
        }
    }


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
        loadUserData()
    }


    fun loadUserData() {
        viewModelScope.launch {
            try {
                val uid = getUid()
                if (uid == null) {
                    Log.e("ProfileViewModel", "loadUserData: Utente non loggato")
                    return@launch
                }
                val userDoc = db.collection("users").document(uid).get().await()
                if (!userDoc.exists()) {
                    Log.w("ProfileViewModel", "loadUserData: Documento non trovato per $uid")
                    return@launch
                }
                
                // Carica username
                _username.value = userDoc.getString("username") ?: ""
                // Carica bio
                _bio.value = userDoc.getString("bio") ?: ""
                // Carica topics
                val topicsList = userDoc.get("topics") as? List<String> ?: emptyList()
                _topicsText.value = topicsList.joinToString(", ")
                // Carica profilePicUrl
                _profilePicUrl.value = userDoc.get("profilePicUrl") as? List<String> ?: emptyList()
                // Carica location
                _location.value = userDoc.getString("location") ?: ""
                // Carica spokenLanguages
                val languagesList = userDoc.get("spokenLanguages") as? List<String> ?: emptyList()
                _spokenLanguages.value = languagesList.joinToString(",")
                // Carica birthTimestamp
                val rawBirthTimestamp = userDoc.get("rawBirthTimestamp")
                if (rawBirthTimestamp is com.google.firebase.Timestamp) {
                    _birthTimestamp.value = rawBirthTimestamp.toDate().time
                }
                
                // Aggiorna userState
                val user = userDoc.toObject(User::class.java)?.copy(docId = userDoc.id)
                _userState.value = user
                
                Log.d("ProfileViewModel", "loadUserData completato per $uid")
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "loadUserData: Errore: ${e.message}", e)
            }
        }
    }


    fun updateProfile(
        updatedUsername: String,
        updatedBio: String,
        updatedTopics: String,
        updatedProfilePicUrl: List<String>,
        updatedBirthTimestamp: Long?, // Accetta valori null
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onFailure("User not authenticated")
            return
        }

        val updates = hashMapOf<String, Any>().apply {
            put("username", updatedUsername)
            put("bio", updatedBio)
            put("topics", updatedTopics.split(",").map { it.trim() })
            put("profilePicUrl", updatedProfilePicUrl)

            updatedBirthTimestamp?.let { ts ->
                // Conversione corretta a Firestore Timestamp
                val seconds = ts / 1000
                val nanoseconds = (ts % 1000) * 1_000_000
                // Converti millisecondi a nanosecondi ma perché?
                put("rawBirthTimestamp", com.google.firebase.Timestamp(seconds, nanoseconds.toInt()))
            } ?: run {
                // Se null, rimuovi il campo (opzionale)
                put("rawBirthTimestamp", FieldValue.delete())
            }
        }

        Firebase.firestore.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onFailure("Firestore error: ${e.localizedMessage}")
            }
    }

    fun setProfilePicOrder(newOrder: List<String>) {
        _profilePicUrl.value = newOrder
    }


    suspend fun deleteProfileImage(imageUrlToDelete: String) {
        val uid = getUid() ?: return // FIX: uid reattivo
        
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
            if (index in mutableCurrentList.indices) {
                mutableCurrentList.removeAt(index)
                _profilePicUrl.value = mutableCurrentList // Aggiorna StateFlow con la lista MUTABILE MODIFICATA
            }

            // --- INIZIO MODIFICA IMPORTANTE ---
            // Controlliamo se l'URL è di Firebase Storage prima di tentare la cancellazione da Storage
            val isFirebaseStorageUrl = imageUrlToDelete.contains("firebasestorage.googleapis.com") || imageUrlToDelete.startsWith("gs://")

            if (isFirebaseStorageUrl) {
                Log.d("ProfileViewModel", "L'URL '$imageUrlToDelete' sembra essere di Firebase Storage. Procedo con l'eliminazione da Storage.")
                deleteProfileImageFromStorage(imageUrlToDelete) { isDeletionSuccessful -> // Usa imageUrlToDelete ORIGINALE per l'eliminazione da Storage
                    if (isDeletionSuccessful) {
                        Log.d("ProfileViewModel", "Immagine eliminata con successo da Storage: $imageUrlToDelete")
                        // L'aggiornamento di Firestore avviene comunque dopo, quindi qui non serve fare altro
                        // se non loggare il successo dell'eliminazione da Storage.
                    } else {
                        Log.e("ProfileViewModel", "Errore nell'eliminazione dell'immagine da Storage: $imageUrlToDelete")
                        // Anche se l'eliminazione da Storage fallisce, l'URL è già stato rimosso
                        // dalla lista locale e verrà rimosso da Firestore.
                        // Potresti voler gestire questo errore in modo più specifico se necessario.
                    }
                    // L'aggiornamento di Firestore avverrà comunque fuori da questo blocco if/else
                }
            } else {
                Log.d("ProfileViewModel", "L'URL '$imageUrlToDelete' NON sembra essere di Firebase Storage (es. Google Auth). Salto l'eliminazione da Storage.")
                // Se non è un URL di Firebase Storage, non facciamo nulla per l'eliminazione da Storage.
                // L'URL è già stato rimosso dalla lista locale (_profilePicUrl.value)
                // e l'aggiornamento di Firestore avverrà comunque.
            }
            // --- FINE MODIFICA IMPORTANTE ---

            // L'aggiornamento di Firestore avviene qui, INDIPENDENTEMENTE dal fatto che l'immagine
            // sia stata eliminata da Storage o meno (perché l'URL deve essere rimosso dalla lista in Firestore)
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
            Log.w("ProfileViewModel", "imageUrlToDelete NON TROVATO (dopo normalizzazione) nella lista _profilePicUrl: $normalizedImageUrlToDelete")
        }
        Log.d("ProfileViewModel", "deleteProfileImage ENDED - imageUrlToDelete: $imageUrlToDelete")
    }

    // Funzione per gestire l'upload dell'immagine

    fun uploadNewProfileImage(newImageUri: Uri, context: Context, onComplete: (String) -> Unit) {
        val uid = getUid() ?: run {
            Log.e("ProfileViewModel", "uploadNewProfileImage: Utente non loggato")
            return
        }
        
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
                    // Aggiornamento Firestore con transazione.
                    // La transazione restituisce la lista aggiornata per garantire
                    // che lo stato locale sia identico a ciò che è stato scritto in Firestore,
                    // evitando race condition con deleteProfileImage() che modifica
                    // concorrentemente _profilePicUrl.value e Firestore.
                    val updatedUrlsFromTransaction = firestore.runTransaction { transaction ->
                        val userDoc = transaction.get(firestore.collection("users").document(uid))
                        val currentUrls = userDoc.get("profilePicUrl") as? List<String> ?: emptyList()
                        val updatedUrls = currentUrls + newUrl
                        transaction.update(userDoc.reference, "profilePicUrl", updatedUrls)
                        updatedUrls // Restituisce la lista aggiornata dalla transazione
                    }.await()

                    // Aggiorna lo stato locale col risultato DALLA TRANSAZIONE FIRESTORE,
                    // non con _profilePicUrl.value + newUrl (che potrebbe leggere
                    // uno stato obsoleto se deleteProfileImage ha modificato
                    // _profilePicUrl.value nel frattempo).
                    _profilePicUrl.value = updatedUrlsFromTransaction
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
        val uid = getUid() ?: return // FIX: uid reattivo
        val topicsList = newTopics.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        firestore.collection("users").document(uid)
            .update("topics", topicsList)
            .addOnSuccessListener {
//                Log.d("ProfileViewModel", "Topics aggiornati su Firestore: $topicsList")
            }
            .addOnFailureListener { e ->
//                Log.e("ProfileViewModel", "Errore aggiornando topics: ${e.localizedMessage}")
            }
    }

    fun setBio(newBio: String) {
        _bio.value = newBio
        val uid = getUid() ?: return // FIX: uid reattivo
        firestore.collection("users").document(uid)
            .update("bio", newBio)
            .addOnSuccessListener {
//                Log.d("ProfileViewModel", "Bio aggiornato su Firestore: $newBio")
            }
            .addOnFailureListener { e ->
//                Log.e("ProfileViewModel", "Errore aggiornando bio: ${e.localizedMessage}")
            }
    }

    fun setLocation(newLocation: String) {
        _location.value = newLocation
        val uid = getUid() ?: return // FIX: uid reattivo
        firestore.collection("users").document(uid)
            .update("location", newLocation)
            .addOnSuccessListener {
//                Log.d("ProfileViewModel", "Location aggiornata su Firestore: $newLocation")
            }
            .addOnFailureListener { e ->
//                Log.e("ProfileViewModel", "Errore aggiornando location: ${e.localizedMessage}")
            }
    }

    // Salva le lingue su Firestore
    fun setSpokenLanguages(languages: String) {
        _spokenLanguages.value = languages
        val uid = getUid() ?: return // FIX: uid reattivo
        viewModelScope.launch {
            try {
                db.collection("users").document(uid)
                    .update("spokenLanguages", languages.split(","))
                    .await()
            } catch (e: Exception) {
//                println("Lingue caricate: ${_spokenLanguages.value}")
//                println("Lingue salvate: $languages")
            }
        }
    }
}