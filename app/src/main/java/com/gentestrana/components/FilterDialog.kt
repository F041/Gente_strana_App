package com.gentestrana.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gentestrana.R

@Composable
fun FilterDialog(
    currentFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_by)) },
        text = {
            Column {
                FilterType.values().filter { it != FilterType.FUTURE_ONE && it != FilterType.FUTURE_TWO }
                    .forEach { filter ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = filter == currentFilter,
                                    onClick = { onFilterSelected(filter) }
                                )
                                .padding(16.dp)
                        ) {
                            RadioButton(
                                selected = filter == currentFilter,
                                onClick = { onFilterSelected(filter) }
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}