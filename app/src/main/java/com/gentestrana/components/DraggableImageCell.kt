package com.gentestrana.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

/**
 * Dati relativi a un singolo elemento della griglia.
 * Puoi aggiungere campi extra se necessario (es: didTapDelete).
 */
data class ImageItem(
    val id: String,    // identificativo unico
    val url: String
)

/**
 * Mostra una singola cella con l'immagine, senza logica di drag & drop.
 * (Implementeremo il drag più avanti.)
 */
@Composable
fun GridImageCell(
    item: ImageItem,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(item.url),
            contentDescription = null, // immagine profilo
            modifier = Modifier.size(100.dp),
            contentScale = ContentScale.Crop
        )
    }
}


