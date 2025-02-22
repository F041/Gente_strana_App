package com.gentestrana.components

data class FilterState(
    val searchQuery: String = "",
    val filterType: FilterType = FilterType.ALL
)

enum class FilterType { ALL }