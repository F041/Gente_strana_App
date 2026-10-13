package com.gentestrana.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

fun formatTimestamp(timestamp: Timestamp): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}

fun getDateSeparator(timestamp: Timestamp): String {
    val messageDate = timestamp.toDate()
    val calendarMessage = Calendar.getInstance().apply {
        time = messageDate
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val yesterday = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return when (calendarMessage.timeInMillis) {
        today.timeInMillis -> "Oggi"
        yesterday.timeInMillis -> "Ieri"
        else -> {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.format(messageDate)
        }
    }
}

fun formatConversationAge(startTimestamp: Timestamp): String {
    val now = System.currentTimeMillis()
    val startMs = startTimestamp.toDate().time
    val diffMs = maxOf(0L, now - startMs)
    val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
    return when {
        diffDays < 1 -> "Oggi"
        diffDays < 30 -> "${diffDays}gg"
        diffDays < 365 -> "${diffDays / 30}m"
        else -> "${diffDays / 365}a"
    }
}
