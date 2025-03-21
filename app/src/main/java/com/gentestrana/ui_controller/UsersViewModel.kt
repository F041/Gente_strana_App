package com.gentestrana.ui_controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.gentestrana.components.FilterState

class UsersViewModel : ViewModel() {

    var filterState by mutableStateOf(FilterState())
        private set

    fun updateSearchQuery(newQuery: String) {
        filterState = filterState.copy(searchQuery = newQuery)
    }
}