package com.gentestrana.components

data class FilterState(
    val searchQuery: String = "",
    val filterType: FilterType = FilterType.ALL
)

enum class FilterType {
    ALL,
    LANGUAGE,
    LOCATION,
    FUTURE_ONE,  // Placeholder per un filtro futuro
    FUTURE_TWO   // Placeholder per un filtro futuro
}