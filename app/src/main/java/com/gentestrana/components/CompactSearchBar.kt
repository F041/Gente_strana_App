package com.gentestrana.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gentestrana.R
import com.gentestrana.ui.theme.LocalDimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onFilterClicked: () -> Unit
) {

    // 1. Rimosso il calcolo dell'altezza basato sullo schermo.

    // 2. Recupera solo le dimensioni dinamiche dal tema, come già facevi.
    val dimensions = LocalDimensions.current

    // 3. Il resto rimane quasi identico.
    val roundedShape = RoundedCornerShape(8.dp)

    TextField(
        value = query,
        onValueChange = onQueryChanged,
        singleLine = true,
        maxLines = 1,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp), // Lasciamo questa misura fissa per ora, va bene per le icone
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            IconButton(onClick = onFilterClicked) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "users_filter",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        placeholder = {
            Text(
                text = stringResource(R.string.users_search_bar),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        textStyle = MaterialTheme.typography.bodySmall,
        // 4. La modifica chiave è qui, nel Modifier.
        modifier = Modifier
            .fillMaxWidth()
            // Utilizziamo le dimensioni dinamiche per il padding, come già facevi.
            .padding(horizontal = dimensions.mediumPadding, vertical = dimensions.smallPadding)
            // RIMOSSA la riga .height(desiredHeight).
            // Ora l'altezza si adatterà automaticamente al contenuto (testo e icone).
            .clip(roundedShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, roundedShape),
        colors = TextFieldDefaults.textFieldColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent
        ),
        shape = roundedShape
    )

}