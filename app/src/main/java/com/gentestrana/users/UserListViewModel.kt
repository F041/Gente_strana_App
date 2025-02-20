package com.gentestrana.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserListViewModel : ViewModel() {
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users = _users.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            try {
                Firebase.firestore.collection("users")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            _error.value = "Connection error: ${error.message}"
                            return@addSnapshotListener
                        }
                        _users.value = snapshot?.documents?.mapNotNull {
                            it.toObject(User::class.java)
                        } ?: emptyList()
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            }
        }
    }
}