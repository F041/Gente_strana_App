package com.gentestrana.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gentestrana.R
import com.gentestrana.components.ProfilePhotoCarousel

@Composable
fun ProfilePicturesScreen(
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
) {
    println("DEBUG - Numero immagini: ${imageUrls.size}") // Controlla in Logcat
    // Puoi aggiungere altri elementi (es. un titolo o un pulsante di back) se necessario
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.photos),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Qui chiamiamo il ProfilePhotoCarousel
        ProfilePhotoCarousel(
            imageUrls = imageUrls,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)

        )

    }
}
