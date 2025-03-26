// File: FilterDialog.kt
package com.gentestrana.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.gentestrana.R

@Composable
fun FilterDialog(
    currentFilter: FilterType,
    currentLanguage: String,
    currentLocation: String,
    supportedLanguages: List<String>,
    onFilterSelected: (FilterType, String) -> Unit,
    onDismiss: () -> Unit
) {
    // Stati temporanei per la scelta del filtro e dei valori
    var tempFilterType by remember { mutableStateOf(currentFilter) }
    var tempLanguage by remember { mutableStateOf(currentLanguage) }
    var tempLocation by remember { mutableStateOf(currentLocation) }
    // Stato per il testo di ricerca delle lingue
    var languageSearchQuery by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.filter_by).uppercase()) },
        text = {
            Column(modifier = Modifier
                .fillMaxWidth()
            ) {
                // Sezione per scegliere il tipo di filtro (ALL, LANGUAGE, LOCATION)
                // (La riga "choose_filter" è stata rimossa come richiesto)
                FilterType.values()
                    .filter { it != FilterType.FUTURE_ONE && it != FilterType.FUTURE_TWO }
                    .forEach { filter ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = filter == tempFilterType,
                                    onClick = { tempFilterType = filter }
                                )
                                .padding(8.dp)
                        ) {
                            RadioButton(
                                selected = filter == tempFilterType,
                                onClick = null
                            )
                            Text(
                                text = when (filter) {
                                    FilterType.ALL -> stringResource(R.string.all_users).uppercase()
                                    FilterType.LANGUAGE -> stringResource(R.string.language).uppercase()
                                    FilterType.LOCATION -> stringResource(R.string.location).uppercase()
                                    else -> ""
                                },
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                // Se il filtro selezionato è LANGUAGE, mostra subito la ricerca e la lista delle lingue
                if (tempFilterType == FilterType.LANGUAGE) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = languageSearchQuery,
                        onValueChange = { languageSearchQuery = it },
                        label = { Text(stringResource(R.string.search_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.select_languages),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    val filteredLanguages = if (languageSearchQuery.isBlank())
                        supportedLanguages
                    else
                        supportedLanguages.filter { lang ->
                            lang.lowercase().contains(languageSearchQuery.lowercase())
                        }
                    LazyColumn {
                        items(filteredLanguages) { lang ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = lang == tempLanguage,
                                        onClick = { tempLanguage = lang }
                                    )
                                    .padding(8.dp)
                            ) {
                                RadioButton(
                                    selected = lang == tempLanguage,
                                    onClick = null
                                )
                                Text(
                                    text = lang,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
                // Se il filtro selezionato è LOCATION, mostra subito il campo di testo per la location
                if (tempFilterType == FilterType.LOCATION) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = tempLocation,
                        onValueChange = { tempLocation = it },
                        label = { Text(stringResource(id = R.string.enter_location)) },
                        singleLine = true,  // Aggiunto per evitare il ridimensionamento
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Usa tempFilterType per aggiornare il filtro globale
                    onFilterSelected(
                        tempFilterType,
                        when (tempFilterType) {
                            FilterType.LANGUAGE -> tempLanguage
                            FilterType.LOCATION -> tempLocation
                            else -> ""
                        }
                    )
                    onDismiss()
                }
            ) { Text(stringResource(R.string.apply).uppercase()) }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        onFilterSelected(FilterType.ALL, "")
                        tempFilterType = FilterType.ALL
                        tempLanguage = ""
                        tempLocation = ""
                    }
                ) {
                    Text(stringResource(R.string.reset).uppercase())
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel).uppercase())
                }
            }
        }
    )
}
