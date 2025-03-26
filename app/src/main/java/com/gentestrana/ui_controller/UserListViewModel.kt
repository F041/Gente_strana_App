package com.gentestrana.ui_controller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentestrana.users.User
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserListViewModel : ViewModel() {
    // Lista degli utenti
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> get() = _users

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

    // Funzione per caricare gli utenti
    fun loadUsers() {
        if (!_hasMoreData.value || _isLoading.value) return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val query = Firebase.firestore.collection("users")
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
                    _users.value = _users.value + newUsers
                    if (result.documents.size < pageSize) {
                        _hasMoreData.value = false
                    }
                } else {
                    _hasMoreData.value = false
                }
                _hasError.value = false
            } catch (e: Exception) {
                _hasError.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }
}
