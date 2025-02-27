package com.gentestrana.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gentestrana.R
import com.gentestrana.utils.getFlagEmoji
import com.gentestrana.utils.getLanguageName
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.setValue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileLanguagesField(
    selectedLanguages: String,
    onLanguagesChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    val currentSelected = remember { mutableStateListOf<String>() }

    // Aggiorna la lista quando cambiano le lingue esterne
    LaunchedEffect(selectedLanguages) {
        currentSelected.clear()
        currentSelected.addAll(selectedLanguages.split(",").filter { it.isNotBlank() })
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.languages_spoken))
        }

        if (currentSelected.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.selected_languages),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    currentSelected.forEach { code ->
                        Text(
                            text = "${getFlagEmoji(context, code)} ${getLanguageName(context, code)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (showDialog) {
            var searchQuery by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onLanguagesChanged(currentSelected.joinToString(","))
                            showDialog = false
                        }
                    ) { Text(stringResource(R.string.save)) }
                },
                title = {
                    Text(
                        text = stringResource(R.string.select_languages),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column {
                        // Barra di ricerca con icona e hint
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Cerca"
                                )
                            },
                            placeholder = {
                                Text(stringResource(R.string.search_hint))
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )

                        // Lista filtrata delle lingue
                        val filteredLanguages = context.resources.getStringArray(R.array.supported_language_codes)
                            .filter { code ->
                                val languageName = getLanguageName(context, code).lowercase()
                                languageName.contains(searchQuery.lowercase()) || code.lowercase().contains(searchQuery.lowercase())
                            }

                        LazyColumn {
                            items(filteredLanguages) { code ->
                                val isSelected = code in currentSelected
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSelected) currentSelected.remove(code)
                                            else currentSelected.add(code)
                                        }
                                        .padding(8.dp)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null
                                    )
                                    Text(
                                        text = "${getFlagEmoji(context, code)} ${getLanguageName(context, code)}",
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}