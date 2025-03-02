package com.gentestrana.utils

import java.util.*

fun computeAgeFromTimestamp(timestamp: Long): Int {
    if (timestamp <= 0) return 0

    // Usa UTC per entrambi i Calendar
    val utc = TimeZone.getTimeZone("UTC")
    val dob = Calendar.getInstance(utc).apply { timeInMillis = timestamp }
    val today = Calendar.getInstance(utc)

    val hasBirthdayPassed = today.get(Calendar.DAY_OF_YEAR) >= dob.get(Calendar.DAY_OF_YEAR)
    return today.get(Calendar.YEAR) - dob.get(Calendar.YEAR) - if (hasBirthdayPassed) 0 else 1
}