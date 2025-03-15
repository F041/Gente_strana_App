package com.gentestrana.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import kotlin.math.roundToInt

// TODO: A CHE SERVIVA? ELIMINARE?
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ProfileImageGallery(
    images: List<String>,
    onImageOrderChanged: (List<String>) -> Unit,
    onDeleteImage: (Int) -> Unit
) {
    // Creiamo una copia mutabile delle immagini
    var imageList by remember { mutableStateOf(images.toMutableList()) }
    LaunchedEffect(images) {
        imageList = images.toMutableList()
    }

    // Dati per il layout
    val cellSize = 100.dp
    val verticalSpacing = 16.dp
    val extraPadding = 32.dp  // Padding extra per sicurezza
    // Calcola il numero di righe necessarie
    val rowCount = if (imageList.size % 3 == 0) imageList.size / 3 else (imageList.size / 3 + 1)
    val gridHeight = (cellSize * rowCount) + (if (rowCount > 0) verticalSpacing * (rowCount - 1) else 0.dp) + extraPadding

    // Limita la griglia ad una frazione dello schermo (es. 40%)
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val finalGridHeight = if (gridHeight < screenHeight * 0.4f) gridHeight else screenHeight * 0.4f

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .height(finalGridHeight)
            .padding(PaddingValues(16.dp)),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(imageList) { index, imageUrl ->
            DraggableImageItem(
                index = index,
                imageUrl = imageUrl,
                listSize = imageList.size, // Passa la dimensione corrente della lista
                cellSize = cellSize,
                isPrimary = index == 0,
                onMove = { from, to ->
                    val mutableList = imageList.toMutableList()
                    val item = mutableList.removeAt(from)
                    mutableList.add(to, item)
                    imageList = mutableList
                    onImageOrderChanged(imageList)
                },
                onDelete = {
                    val mutableList = imageList.toMutableList()
                    mutableList.removeAt(index)
                    imageList = mutableList
                    onImageOrderChanged(imageList)
                    onDeleteImage(index)
                }
            )
        }
    }
}

// TODO: completamente da rifare, dragga sull'asse Y, completamente non
// intuivo
@Composable
fun DraggableImageItem(
    index: Int,
    imageUrl: String,
    listSize: Int,
    cellSize: Dp,
    isPrimary: Boolean,
    onMove: (from: Int, to: Int) -> Unit,
    onDelete: () -> Unit
) {
    // Stato per gestire l'offset durante il drag
    var offsetY by remember { mutableStateOf(0f) }
    // Utilizza LocalDensity per convertire Dp in pixel
    val density = LocalDensity.current
    val threshold = with(density) { cellSize.toPx() / 2 }

    Box(
        modifier = Modifier
            .size(cellSize)
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (isPrimary) Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary))
                else Modifier
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetY > threshold && index < listSize - 1) {
                            // Sposta l'elemento in giù (nuovo indice = index + 1)
                            onMove(index, index + 1)
                        } else if (offsetY < -threshold && index > 0) {
                            // Sposta l'elemento in su (nuovo indice = index - 1)
                            onMove(index, index - 1)
                        }
                        offsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consumeAllChanges()
                        offsetY += dragAmount.y
                    }
                )
            },
        contentAlignment = Alignment.TopEnd
    ) {
        Image(
            painter = rememberAsyncImagePainter(imageUrl),
            contentDescription = "Profile Image $index",
            modifier = Modifier.fillMaxSize()
        )
        // Icona di eliminazione in alto a destra usando IconButton e Icons.Default.Close
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Elimina immagine",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}