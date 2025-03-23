package com.gentestrana.components

data class FilterState(
    val searchQuery: String = "",
    val filterType: FilterType = FilterType.ALL,
    val selectedLanguage: String = "",  // Nuovo campo
    val selectedLocation: String = ""   // Nuovo campo
)

enum class FilterType {
    ALL,
    LANGUAGE,
    LOCATION,
    FUTURE_ONE,
    FUTURE_TWO
}