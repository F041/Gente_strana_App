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
import androidx.compose.ui.window.Dialog
import com.gentestrana.R
import com.gentestrana.utils.getCountriesList
import com.gentestrana.ui.theme.LocalDimensions


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
    var languageSearchQuery by remember { mutableStateOf("") }
    val dimensions = LocalDimensions.current

    // Questo Dialog personalizzato non userà AlertDialog, ma avremo un layout custom.
    Dialog(onDismissRequest = onDismiss) {
        // Superficie con forma e stile personalizzabili
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = dimensions.dialogElevation, // Usa il valore dal tema
            color = MaterialTheme.colorScheme.surface,
        ) {
            // Aggiungiamo padding e imePadding per adattarci quando la tastiera compare
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensions.dialogPadding)
                    .imePadding()  // <-- Chiave per far “salire” il contenuto quando compare la tastiera
            ) {
                // Titolo
                Text(
                    text = stringResource(R.string.filter_by).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Scelta del filtro (ALL, LANGUAGE, LOCATION)
                FilterType.entries
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

                // Se il filtro è LANGUAGE, mostriamo subito la barra di ricerca e la lista
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
                    val filteredLanguages = if (languageSearchQuery.isBlank()) {
                        supportedLanguages
                    } else {
                        supportedLanguages.filter { lang ->
                            lang.lowercase().contains(languageSearchQuery.lowercase())
                        }
                    }
                    // Limitiamo l’altezza della lista
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp)
                    ) {
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
                }

                // Se il filtro è LOCATION, mostriamo un TextField per la location
                if (tempFilterType == FilterType.LOCATION) {
                    Spacer(modifier = Modifier.height(16.dp))
                    // Stato per il testo di ricerca della location
                    var locationSearchQuery by remember { mutableStateOf("") }

                    OutlinedTextField(
                        value = locationSearchQuery,
                        onValueChange = { locationSearchQuery = it },
                        label = { Text(stringResource(R.string.enter_location)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Ottieni la lista dei paesi dalla utility (CountriesUtils.kt)
                    val countries = getCountriesList()
                    val filteredCountries = if (locationSearchQuery.isBlank())
                        countries
                    else
                        countries.filter { it.lowercase().contains(locationSearchQuery.lowercase()) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp) // altezza fissa per la lista
                    ) {
                        LazyColumn {
                            items(filteredCountries) { country ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = country == tempLocation,
                                            onClick = { tempLocation = country }
                                        )
                                        .padding(8.dp)
                                ) {
                                    RadioButton(
                                        selected = country == tempLocation,
                                        onClick = null
                                    )
                                    Text(
                                        text = country,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Spaziatura finale
                Spacer(modifier = Modifier.height(16.dp))

                // Bottoni di azione (conferma e annulla)
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
                    Row {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel).uppercase())
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
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
                        ) {
                            Text(stringResource(R.string.apply).uppercase())
                        }
                    }
                }
            }
        }
    }
}
