package com.gentestrana.components

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gentestrana.R
import kotlin.math.roundToInt
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage

private const val DEFAULT_PROFILE_IMAGE_URL = "https://icons.veryicon.com/png/o/system/ali-mom-icon-library/random-user.png"


/**
 * Raggruppa una lista in righe da [count] elementi.
 */
private fun <T> List<T>.chunkedBy(count: Int): List<List<T>> = this.chunked(count)

/**
 * Griglia di immagini di profilo con:
 * - Drag & drop manuale per riordinare (invece delle frecce).
 * - Cella "ADD" per aggiungere nuove immagini se non si raggiunge il limite.
 * - Le immagini vengono mostrate con crop.
 * - Nella cella "ADD", se isUploading è true, viene mostrato GenericLoadingScreen.
 *
 * @param images Lista di URL delle immagini.
 * @param maxImages Numero massimo di immagini consentite (default 3).
 * @param isUploading Stato di caricamento: se true, nella cella "ADD" viene mostrato l’indicatore.
 * @param onImageOrderChanged Callback per aggiornare l’ordine delle immagini.
 * @param onDeleteImage Callback per eliminare un’immagine (passa l’URL dell’immagine eliminata).
 * @param onAddImage Callback chiamata quando l’utente clicca sul box “+” per aggiungere una nuova immagine.
 */
@Composable
fun ReorderableProfileImageGridWithAdd(
    images: List<String>,
    maxImages: Int = 3,
    isUploading: Boolean,
    onImageOrderChanged: (List<String>) -> Unit,
    onDeleteImage: (String) -> Unit,
    onAddImage: () -> Unit
) {
    // Mantieni una copia mutabile delle immagini
    var imageList by remember { mutableStateOf(images.toMutableList()) }
    val context = LocalContext.current

    // Aggiorna imageList quando "images" cambia da fuori
    LaunchedEffect(images) {
        imageList = images.toMutableList()
    }

    // Se il numero di immagini è inferiore al massimo, aggiungi un marker "ADD_CELL"
    val displayList = remember(imageList) {
        val listCopy = imageList.toMutableList()
        if (listCopy.size < maxImages) listCopy.add("ADD_CELL")
        listCopy
    }

    // Raggruppa la lista in righe da 3 elementi
    val rows = displayList.chunkedBy(3)

    // Calcola l'altezza della griglia in base al numero di righe
    val cellSize = 100.dp
    val verticalSpacing = 16.dp
//    val extraPadding = 32.dp
//    val rowCount = if (displayList.size % 3 == 0) displayList.size / 3 else (displayList.size / 3 + 1)
//    val gridHeight = (cellSize * rowCount) +
//            (if (rowCount > 0) verticalSpacing * (rowCount - 1) else 0.dp) +
//            extraPadding

    // Drag & drop states
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // Calcolo dimensioni in px per interpretare l’offset
    val density = LocalDensity.current
    val cellSizePx = with(density) { cellSize.toPx() }
    val spacingPx = with(density) { 16.dp.toPx() }  // lo spacing orizzontale/verticale tra le celle
    val cellTotalPx = cellSizePx + spacingPx

    // Funzione per calcolare il nuovo indice in base all'offset
    fun calculateNewIndex(originalIndex: Int, offset: Offset): Int {
        val originalRow = originalIndex / 3
        val originalCol = originalIndex % 3

        // Coordinate originali in pixel
        val originalX = originalCol * cellTotalPx
        val originalY = originalRow * cellTotalPx

        // Nuove coordinate considerando l'offset
        val newX = originalX + offset.x
        val newY = originalY + offset.y

        // Calcola nuova colonna e riga (arrotondando)
        val newCol = (newX / cellTotalPx).roundToInt().coerceIn(0, 2)
        val newRow = (newY / cellTotalPx).roundToInt().coerceAtLeast(0)

        // Converte (row,col) in un indice di displayList
        val newIndex = (newRow * 3) + newCol
        return newIndex.coerceAtMost(imageList.size - 1)
    }

    // Layout a colonna statica con altezza pari a gridHeight
    Column(
        modifier = Modifier
            .fillMaxWidth()
//            .height(gridHeight)
            // non mostrava text
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        rows.forEachIndexed { rowIndex, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                rowItems.forEachIndexed { colIndex, item ->
                    val flatIndex = rowIndex * 3 + colIndex
                    if (item == "ADD_CELL") {
                        // Box per aggiungere una nuova immagine
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onAddImage() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploading) {
                                GenericLoadingScreen(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "+",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Box per immagine esistente con drag & drop manuale
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                // Se questo item è in drag, applichiamo l'offset
                                .then(
                                    if (draggingIndex == flatIndex)
                                        Modifier.offset {
                                            IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt())
                                        }
                                    else Modifier
                                )
                                .clip(MaterialTheme.shapes.medium)
                                // Aggiunge il bordo se flatIndex == 0 (come prima)
                                .then(
                                    if (flatIndex == 0)
                                        Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary))
                                    else Modifier
                                )
                                // Rilevamento gesture di drag
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            // Se non c'è già un item in drag, imposta l'indice
                                            if (draggingIndex == null) {
                                                draggingIndex = flatIndex
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            // Solo se stiamo effettivamente trascinando questo item
                                            if (draggingIndex == flatIndex) {
                                                dragOffset += dragAmount
                                            }
                                        },
                                        onDragEnd = {
                                            // Calcola la nuova posizione
                                            draggingIndex?.let { originalIndex ->
                                                val targetIndex = calculateNewIndex(originalIndex, dragOffset)
                                                    .coerceIn(0, imageList.size -1)
                                                if (targetIndex != originalIndex) {
                                                    val mutable = imageList.toMutableList()
                                                    val draggedItem = mutable.removeAt(originalIndex)
                                                    val finalTargetIndex = targetIndex.coerceAtMost(mutable.size)
                                                    mutable.add(finalTargetIndex, draggedItem)
                                                    imageList = mutable
                                                    onImageOrderChanged(mutable)
                                                }
                                            }
                                            // Reset
                                            draggingIndex = null
                                            dragOffset = Offset.Zero
                                        },
                                        onDragCancel = {
                                            draggingIndex = null
                                            dragOffset = Offset.Zero
                                        }
                                    )
                                },
                            contentAlignment = Alignment.TopCenter
                        ) {
                            // Immagine
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item) // URL dell'immagine
                                    .placeholder(R.drawable.random_user)
                                    .error(R.drawable.random_user)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile Image $flatIndex",
                                modifier = Modifier
                                    .fillMaxSize() // AsyncImage riempie il Box
                                    .aspectRatio(1f), // Mantiene le proporzioni quadrate
                                contentScale = ContentScale.Crop // Applica il crop
                            )
                            // Pulsante di eliminazione
                            IconButton(
                                onClick = {
                                    val mutable = imageList.toMutableList()
                                    Log.d("DeleteImage", "Elimina immagine CLICKED, flatIndex: $flatIndex, listSize: ${mutable.size}")
                                    if(flatIndex >= 0 && flatIndex < mutable.size) { // Aggiungi controllo indice
                                        val removedItem = mutable.removeAt(flatIndex) // Prendi l'item rimosso
                                        imageList = mutable
                                        onImageOrderChanged(mutable) // Notifica il nuovo ordine
                                        onDeleteImage(removedItem) // Notifica l'URL cancellato
                                    } else {
                                        Log.e("DeleteImage", "Indice non valido per la cancellazione: $flatIndex, size: ${mutable.size}")
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Elimina immagine",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.profile_image_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}
