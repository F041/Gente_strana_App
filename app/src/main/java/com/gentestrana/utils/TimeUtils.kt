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

fun formatLastSeen(lastActive: Timestamp?, isItalian: Boolean = true): String? {
    if (lastActive == null) return null

    val nowMs = System.currentTimeMillis()
    val lastActiveMs = lastActive.toDate().time
    val diffSec = (nowMs - lastActiveMs) / 1000

    if (diffSec in 0..390) {
        return if (isItalian) "online ora" else "online now"
    }

    val calLast = Calendar.getInstance().apply { timeInMillis = lastActiveMs }
    val calToday = Calendar.getInstance()
    val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    val isSameYear = calLast.get(Calendar.YEAR) == calToday.get(Calendar.YEAR)
    val isToday = isSameYear && calLast.get(Calendar.DAY_OF_YEAR) == calToday.get(Calendar.DAY_OF_YEAR)
    val isYesterday = isSameYear && calLast.get(Calendar.DAY_OF_YEAR) == calYesterday.get(Calendar.DAY_OF_YEAR)

    if (isToday) {
        return if (isItalian) "online oggi" else "online today"
    }
    if (isYesterday) {
        return if (isItalian) "online ieri" else "online yesterday"
    }

    val dayOfMonth = calLast.get(Calendar.DAY_OF_MONTH)
    if (isItalian) {
        val article = if (dayOfMonth == 1 || dayOfMonth == 8 || dayOfMonth == 11) "l'" else "il "
        val pattern = if (isSameYear) "d MMM" else "d MMM yyyy"
        val formattedDate = SimpleDateFormat(pattern, Locale.ITALIAN).format(calLast.time)
        return "online $article$formattedDate"
    } else {
        val pattern = if (isSameYear) "MMM d" else "MMM d, yyyy"
        val formattedDate = SimpleDateFormat(pattern, Locale.ENGLISH).format(calLast.time)
        return "online on $formattedDate"
    }
}
