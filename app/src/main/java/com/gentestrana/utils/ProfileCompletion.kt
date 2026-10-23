package com.gentestrana.utils

import com.gentestrana.users.User

data class ProfileCompletionStatus(
    val completedCount: Int,
    val totalCount: Int = 7,
    val percentage: Int,
    val missingItems: List<String>
)

fun calculateProfileCompletion(user: User?): ProfileCompletionStatus {
    if (user == null) {
        return ProfileCompletionStatus(0, 7, 0, emptyList())
    }

    val missing = mutableListOf<String>()
    var count = 0

    // 1. Photo
    val hasValidPhoto = user.profilePicUrl.any {
        it.isNotBlank() && !it.contains("veryicon.com") && !it.contains("random-user")
    }
    if (hasValidPhoto) count++ else missing.add("la foto profilo")

    // 2. Bio
    if (user.bio.trim().isNotEmpty()) count++ else missing.add("la bio")

    // 3. Topics
    val hasTopics = user.topics.any { it.trim().isNotEmpty() }
    if (hasTopics) count++ else missing.add("gli argomenti di interesse")

    // 4. Date of birth
    if (user.birthTimestamp > 0L) count++ else missing.add("la data di nascita")

    // 5. Spoken languages
    val hasLanguages = user.spokenLanguages.any { it.trim().isNotEmpty() }
    if (hasLanguages) count++ else missing.add("le lingue parlate")

    // 6. Location
    if (user.location.trim().isNotEmpty()) count++ else missing.add("la località")

    // 7. Social battery status
    val hasExplicitBattery = user.socialBatteryStatus.isNotEmpty()
    if (hasExplicitBattery) count++ else missing.add("la batteria sociale")

    val percentage = Math.round((count.toFloat() / 7f) * 100f)

    return ProfileCompletionStatus(
        completedCount = count,
        totalCount = 7,
        percentage = percentage,
        missingItems = missing
    )
}
