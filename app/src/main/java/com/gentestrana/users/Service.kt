package com.gentestrana.users

data class Service(
    val docId: String,
    val username: String,
    val rawBirthTimestamp: Any? = null,
    val topics: List<String> = emptyList(),
    val profilePicUrl: List<String> = listOf("res/drawable/random_user.webp"),
    val spokenLanguages: List<String> = emptyList()
)
