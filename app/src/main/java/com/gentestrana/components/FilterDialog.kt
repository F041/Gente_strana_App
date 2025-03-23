package com.gentestrana.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gentestrana.R
// Aggiungi in cima al file
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.items

@Composable
fun FilterDialog(
    currentFilter: FilterType,
    currentLanguage: String,
    currentLocation: String,
    supportedLanguages: List<String>,
    onFilterSelected: (FilterType, String) -> Unit,
    onDismiss: () -> Unit
) {
    var tempLanguage by remember { mutableStateOf(currentLanguage) }
    var tempLocation by remember { mutableStateOf(currentLocation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_by)) },
        text = {
            Column {
                when(currentFilter) {
                    FilterType.LANGUAGE -> {
                        Text("Seleziona lingua:", modifier = Modifier.padding(bottom = 8.dp))
                        LazyColumn {
                            items(supportedLanguages) { lang -> // Usa items() direttamente con la lista
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = lang == tempLanguage,
                                            onClick = { tempLanguage = lang }
                                        )
                                        .padding(8.dp)
                                ) {
                                    RadioButton(
                                        selected = lang == tempLanguage,
                                        onClick = null // Gestito dal selectable
                                    )
                                    Text(
                                        text = lang,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    FilterType.LOCATION -> {
                        TextField(
                            value = tempLocation,
                            onValueChange = { tempLocation = it },
                            label = { Text("Inserisci località") }
                        )
                    }
                    else -> {
                        FilterType.values().filter { it != FilterType.FUTURE_ONE && it != FilterType.FUTURE_TWO }
                            .forEach { filter ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = filter == currentFilter,
                                            onClick = {
                                                onFilterSelected(
                                                    filter,
                                                    when(filter) {
                                                        FilterType.LANGUAGE -> tempLanguage
                                                        FilterType.LOCATION -> tempLocation
                                                        else -> ""
                                                    }
                                                )
                                            }
                                        )
                                        .padding(16.dp)
                                ) {
                                    RadioButton(
                                        selected = filter == currentFilter,
                                        onClick = null
                                    )
                                    Text(
                                        text = when(filter) {
                                            FilterType.ALL -> stringResource(R.string.all_users)
                                            FilterType.LANGUAGE -> stringResource(R.string.language)
                                            FilterType.LOCATION -> stringResource(R.string.location)
                                            else -> ""
                                        },
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }
                            }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onFilterSelected(currentFilter,
                        if(currentFilter == FilterType.LANGUAGE) tempLanguage
                        else tempLocation
                    )
                    onDismiss()
                }
            ) { Text("Applica") }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        // Resetta TUTTI i filtri
                        onFilterSelected(FilterType.ALL, "")
                        tempLanguage = ""
                        tempLocation = ""
                    }
                ) {
                    Text(stringResource(R.string.reset))
                }

                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}