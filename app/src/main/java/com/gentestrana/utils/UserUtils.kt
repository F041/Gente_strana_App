package com.gentestrana.utils

import java.util.*

fun computeAgeFromTimestamp(timestamp: Long): Int {
    if (timestamp <= 0) return 0
    val dob = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    return today.get(Calendar.YEAR) - dob.get(Calendar.YEAR).let { year ->
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) year - 1 else year
    }
}