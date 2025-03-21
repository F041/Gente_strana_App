package com.gentestrana.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gentestrana.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// TODO: possiamo farla scomparire se l'utente scrolla?
// TODO: se faccio una ricerca e clicco sul primo user card e torno indietro
//  il testo della ricerca scompare. Come salvarlo?

fun CompactSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onFilterClicked: () -> Unit
) {
    // 1) Ottenere dimensioni schermo
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val desiredHeight = screenHeight * 0.07f

    // 2) Definire un shape unico (bordo e contenitore)
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
                modifier = Modifier.size(20.dp),
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
        // font settings ricerca, altrimenti non si comporta
        // come placeholder
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(desiredHeight)
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
