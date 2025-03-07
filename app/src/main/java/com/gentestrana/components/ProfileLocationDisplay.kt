package com.gentestrana.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gentestrana.R

@Composable
fun ProfileLocationDisplay(locationName: String?) {
    // <-- Riceve locationName come parametro
    Spacer(modifier = Modifier.height(8.dp))
    // <-- Rimesso qui lo Spacer che c'era prima

    locationName?.let {
        // Usa locationName (il parametro)
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.your_location).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            )
            Text(
                text = it, // Usa 'it' che è locationName
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            )
        }
    }
}