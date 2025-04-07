package com.gentestrana.ui_controller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentestrana.users.User
import com.gentestrana.users.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class BlockedUsersViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    // Stato per la lista degli utenti bloccati (oggetti User completi)
    private val _blockedUsers = MutableStateFlow<List<User>>(emptyList())
    val blockedUsers: StateFlow<List<User>> = _blockedUsers.asStateFlow()

    // Stato per indicare il caricamento
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Stato per eventuali messaggi di errore
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Istanza Firestore e ID utente corrente
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Variabile per il listener
    private var blockedUsersListenerRegistration: ListenerRegistration? = null

    init {
        registerBlockedUsersListener()
    }

    private fun registerBlockedUsersListener() {
        if (currentUserId == null) {
            Log.w("BlockedUsersVM", "Impossibile registrare il listener: Utente non loggato.")
            _blockedUsers.value = emptyList()
            _isLoading.value = false
            return
        }
        // Rimuovi listener precedente se esiste
        blockedUsersListenerRegistration?.remove()

        Log.d("BlockedUsersVM_Listener", "Registrazione listener per blockedUsers di $currentUserId")
        _isLoading.value = true // Inizia il caricamento
        _error.value = null // Resetta errore

        val userDocRef = firestore.collection("users").document(currentUserId)

        blockedUsersListenerRegistration = userDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("BlockedUsersVM_Listener", "Errore nell'ascolto di blockedUsers: ${error.message}")
                _error.value = "Errore nel caricamento: ${error.localizedMessage}"
                _blockedUsers.value = emptyList()
                _isLoading.value = false
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val blockedIds = snapshot.get("blockedUsers") as? List<String> ?: emptyList()
                Log.d("BlockedUsersVM_Listener", "Ricevuti ${blockedIds.size} ID bloccati.")
                // Se la lista di ID è cambiata o è la prima volta, ricarica i dettagli utente
                // Possiamo confrontare i Set per efficienza
                if (blockedIds.toSet() != _blockedUsers.value.map { it.docId }.toSet() || _blockedUsers.value.isEmpty()) {
                    fetchBlockedUserDetails(blockedIds)
                } else {
                    // Lista ID invariata, non serve ricaricare dettagli, ferma loading se era attivo
                    _isLoading.value = false
                }
            } else {
                Log.w("BlockedUsersVM_Listener", "Documento utente $currentUserId non trovato o snapshot nullo.")
                _blockedUsers.value = emptyList() // Nessun utente bloccato se il doc non esiste
                _isLoading.value = false
            }
        }
    }

    private fun fetchBlockedUserDetails(blockedIds: List<String>) {
        if (blockedIds.isEmpty()) {
            _blockedUsers.value = emptyList()
            _isLoading.value = false
            Log.d("BlockedUsersVM", "Nessun utente bloccato da caricare.")
            return
        }

        viewModelScope.launch {
            _error.value = null // Resetta errore specifico del fetch dettagli
            Log.d("BlockedUsersVM", "Avvio fetch dettagli per ${blockedIds.size} utenti bloccati.")
            try {
                val users = mutableListOf<User>()
                blockedIds.forEach { blockedUserId ->
                    try {
                        val userDoc = firestore.collection("users").document(blockedUserId).get().await()
                        userDoc.toObject(User::class.java)?.let { user ->
                            users.add(user.copy(docId = userDoc.id)) // Importante copiare docId
                        } ?: Log.w("BlockedUsersVM", "Impossibile convertire documento per utente bloccato: $blockedUserId")
                    } catch (fetchError: Exception) {
//                        Log.e("BlockedUsersVM", "Errore fetch dettaglio utente bloccato $blockedUserId: ${fetchError.message}")
                        // Decidi se continuare o fermarti. Per ora continuiamo.
                    }
                }
                _blockedUsers.value = users // Aggiorna la lista con gli oggetti User completi
                Log.d("BlockedUsersVM", "Dettagli caricati per ${users.size} utenti bloccati.")
            } catch (e: Exception) {
                // Errore generale durante il fetch dei dettagli
                Log.e("BlockedUsersVM", "Errore generale fetch dettagli utenti bloccati: ${e.message}")
                _error.value = "Errore nel caricare i dettagli degli utenti bloccati."
                // _blockedUsers.value = emptyList() // Potresti voler svuotare la lista qui
            } finally {
                _isLoading.value = false // Termina il caricamento (anche in caso di errore parziale)
            }
        }
    }

    fun unblockUser(userIdToUnblock: String) {
        // Non impostiamo isLoading qui, lo gestirà il listener
        _error.value = null // Resetta errore prima di tentare
        viewModelScope.launch {
            userRepository.unblockUser(
                unblockedUserId = userIdToUnblock,
                onSuccess = {
                    Log.d("BlockedUsersVM", "Utente $userIdToUnblock sbloccato con successo (Firestore update OK).")

                },
                onFailure = { errorMsg ->
                    _error.value = "Errore sbloccando l'utente: ${errorMsg ?: "sconosciuto"}"
                    Log.e("BlockedUsersVM", "Errore sbloccando utente $userIdToUnblock: $errorMsg")
                }
            )
        }
    }

    fun retryListenerRegistration() {
//        Log.d("BlockedUsersVM", "Retry button clicked. Tentativo di registrare nuovamente il listener.")
        // Questa funzione già gestisce la rimozione del listener precedente.
        registerBlockedUsersListener()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("BlockedUsersVM", "ViewModel cleared. Removing blockedUsers listener.")
        blockedUsersListenerRegistration?.remove() // Rimuovi il listener!
        blockedUsersListenerRegistration = null
    }
}