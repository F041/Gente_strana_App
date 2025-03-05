package com.gentestrana.components

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gentestrana.R
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DateOfBirthPicker(
    context: Context,
    birthTimestamp: Long,
    onDateSelected: (Long) -> Unit
) {
    val calendar = remember { Calendar.getInstance() }
    if (birthTimestamp > 0L) {
        calendar.timeInMillis = birthTimestamp
    }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val newCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                }
                onDateSelected(newCalendar.timeInMillis)
                showDialog = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Button(
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        val displayText = if (birthTimestamp > 0L) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            // Cambiare con stringa?
            sdf.format(Date(birthTimestamp))
        } else {
            stringResource(R.string.set_birthdate)
        }
        Text(text = displayText, color = MaterialTheme.colorScheme.onSurface)
    }
}
