package com.gentestrana.ui_controller

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentestrana.users.User
import com.gentestrana.users.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserListViewModel : ViewModel() {
    // Lista degli utenti
    private val _rawUsers = MutableStateFlow<List<User>>(emptyList())
    // **RINOMINATO** da _users a _rawUsers

    // Stato per il caricamento ed errori
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> get() = _hasError

    // Variabili per il paging
    private var lastDocumentSnapshot: DocumentSnapshot? = null
    private val _hasMoreData = MutableStateFlow(true)
    val hasMoreData: StateFlow<Boolean> get() = _hasMoreData

    private val pageSize = 10

    private val _currentUserProfile = MutableStateFlow<User?>(null)
    val currentUserProfile: StateFlow<User?> get() = _currentUserProfile

    private val userRepository = UserRepository() // Aggiungiamo un'istanza del repository
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private val firestore: FirebaseFirestore = Firebase.firestore

    private val _blockedUserIds = MutableStateFlow<Set<String>>(emptySet())
    // Usiamo un Set per ricerche più efficienti (contains)

    // Combina _rawUsers e _blockedUserIds per creare la lista filtrata finale
    val users: StateFlow<List<User>> = combine(_rawUsers, _blockedUserIds) { rawUsers, blockedIds ->
        rawUsers.filterNot { user ->
            blockedIds.contains(user.docId) // Filtra se l'ID utente è nel Set dei bloccati
        }
    }.stateIn( // Usa stateIn qui
        scope = viewModelScope, // Specifica lo scope del ViewModel
        started = SharingStarted.WhileSubscribed(5000), // Policy per mantenere attivo il flow
        initialValue = emptyList() // Fornisci un valore iniziale
    )


    private var blockedUsersListenerRegistration: ListenerRegistration? = null

    init {
        loadUsers()
        loadCurrentUserProfile()
        registerBlockedUsersListener()
    }

    private fun registerBlockedUsersListener() {
        if (currentUserId == null) {
            Log.w("UserListVM", "Impossibile registrare il listener: Utente non loggato.")
            _blockedUserIds.value = emptySet() // Assicura stato vuoto
            return
        }
        // Rimuovi listener precedente se esiste (sicurezza)
        blockedUsersListenerRegistration?.remove()

        Log.d("UserListVM_Listener", "Registrazione listener per blockedUsers di $currentUserId")

        val userDocRef = firestore.collection("users").document(currentUserId)

        blockedUsersListenerRegistration = userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("UserListVM_Listener", "Errore nell'ascolto di blockedUsers: ${error.message}")
                _blockedUserIds.value = emptySet() // Imposta a vuoto in caso di errore
                _hasError.value = true // Segnala errore generico (opzionale)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val blockedList = snapshot.get("blockedUsers") as? List<String> ?: emptyList()
                val newBlockedSet = blockedList.toSet()
                if (newBlockedSet != _blockedUserIds.value) { // Aggiorna solo se è cambiato
                    _blockedUserIds.value = newBlockedSet
                    Log.d("UserListVM_Listener", "Listener blockedUsers aggiornato: ${newBlockedSet.size} utenti.")
                } else {
                    // Log.d("UserListVM_Listener", "Listener blockedUsers: Nessun cambiamento rilevato.")
                }
            } else {
                Log.w("UserListVM_Listener", "Documento utente $currentUserId non trovato o snapshot nullo.")
                _blockedUserIds.value = emptySet() // Documento non trovato
            }
        }
    }

    // Funzione per caricare gli utenti
    fun loadUsers() {
        // Il controllo !isLoading è già gestito dal chiamante (UsersListScreen)
        // ma aggiungiamo un controllo qui per sicurezza se venisse chiamato direttamente
        if (!_hasMoreData.value || _isLoading.value) return

        _isLoading.value = true
        _hasError.value = false // Reset errore all'inizio del caricamento
        viewModelScope.launch {
            try {
                val query = Firebase.firestore.collection("users")
                    .orderBy("lastActive", com.google.firebase.firestore.Query.Direction.DESCENDING) // Ordina per lastActive
                    .let { baseQuery ->
                        lastDocumentSnapshot?.let { baseQuery.startAfter(it) } ?: baseQuery
                    }
                    .limit(pageSize.toLong())

                val result = query.get().await()

                if (result.documents.isNotEmpty()) {
                    lastDocumentSnapshot = result.documents.last()
                    val newUsers = result.documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(docId = doc.id)
                    }

                    // Escludi subito l'utente corrente dalla lista raw
                    _rawUsers.value = _rawUsers.value + newUsers.filterNot { it.docId == currentUserId }

                    _hasMoreData.value = result.documents.size >= pageSize
                    Log.d("UserListVM", "Caricati ${newUsers.size} nuovi utenti raw. HasMoreData: ${_hasMoreData.value}")

                } else {
                    _hasMoreData.value = false
                    Log.d("UserListVM", "Nessun altro utente raw da caricare. HasMoreData: false")
                }

            } catch (e: Exception) {
                Log.e("UserListVM", "Errore caricamento utenti raw: ${e.message}")
                _hasError.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("UserListVM", "ViewModel cleared. Removing blockedUsers listener.")
        blockedUsersListenerRegistration?.remove() // Rimuovi il listener!
        blockedUsersListenerRegistration = null
    }

    private fun loadCurrentUserProfile() {
        if (currentUserId == null) {
            Log.w("UserListViewModel", "Impossibile caricare il profilo: Utente non loggato.")
            _currentUserProfile.value = null // Assicurati sia null se non c'è utente
            return
        }
        viewModelScope.launch {
            try {
                userRepository.getUser(
                    docId = currentUserId,
                    onSuccess = { user ->
                        _currentUserProfile.value = user
                        Log.d("UserListViewModel", "Profilo utente corrente caricato: ${user.username}")
                    },
                    onFailure = { error ->
                        Log.e("UserListViewModel", "Errore caricamento profilo utente corrente", error)
                        _currentUserProfile.value = null // Imposta a null in caso di errore
                    }
                )
            } catch (e: Exception) {
                Log.e("UserListViewModel", "Eccezione durante il caricamento del profilo utente corrente", e)
                _currentUserProfile.value = null
            }
        }
    }
}
