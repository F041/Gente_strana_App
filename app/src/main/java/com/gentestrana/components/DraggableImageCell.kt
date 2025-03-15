package com.gentestrana.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import org.burnoutcrew.reorderable.*






//@Composable
//fun DraggableImageCell(
//    imageUrl: String,
//    state: ReorderableItem,
//    onDelete: (String) -> Unit
//) {
//    Box(
//        modifier = Modifier
//            .size(100.dp)
//            .clip(RoundedCornerShape(8.dp))
//            .background(MaterialTheme.colorScheme.surfaceVariant)
//            .then(state.dragModifier), // Applica il modificatore per il drag & drop
//        contentAlignment = Alignment.Center
//    ) {
//        Image(
//            painter = rememberAsyncImagePainter(imageUrl),
//            contentDescription = "Profile Image",
//            modifier = Modifier.fillMaxSize(),
//            contentScale = ContentScale.Crop
//        )
//
//        // Pulsante per eliminare l'immagine
//        IconButton(
//            onClick = { onDelete(imageUrl) },
//            modifier = Modifier.align(Alignment.TopEnd)
//        ) {
//            Icon(
//                imageVector = Icons.Default.Close,
//                contentDescription = "Elimina immagine",
//                tint = MaterialTheme.colorScheme.error
//            )
//        }
//    }
//}
