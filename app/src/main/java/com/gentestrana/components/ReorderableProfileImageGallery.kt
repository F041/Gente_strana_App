package com.gentestrana.components

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gentestrana.R
import com.gentestrana.ui.theme.LocalDimensions
import kotlin.math.roundToInt

// Rimosso DEFAULT_PROFILE_IMAGE_URL perché usiamo il drawable di fallback direttamente in AsyncImage

private fun <T> List<T>.chunkedBy(count: Int): List<List<T>> = this.chunked(count)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReorderableProfileImageGridWithAdd(
    images: List<String>,
    maxImages: Int = 3,
    isUploading: Boolean,
    onImageOrderChanged: (List<String>) -> Unit,
    onDeleteImage: (String) -> Unit,
    onAddImage: () -> Unit
) {
    var imageList by remember { mutableStateOf(images.toMutableList()) }
    val context = LocalContext.current
    val dimensions = LocalDimensions.current

    LaunchedEffect(images) {
        imageList = images.toMutableList()
    }

    val displayList = remember(imageList) {
        val listCopy = imageList.toMutableList()
        if (listCopy.size < maxImages) listCopy.add("ADD_CELL")
        listCopy
    }

    // Non raggruppiamo più in righe, useremo una griglia flessibile.
    // val rows = displayList.chunkedBy(3) // <-- RIMOSSO

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    // Usiamo BoxWithConstraints per ottenere la larghezza totale disponibile per la griglia
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(dimensions.largePadding)) {
        val gridWidthPx = with(density) { maxWidth.toPx() }
        val spacingPx = with(density) { dimensions.mediumPadding.toPx() }
        // La larghezza di ogni cella è (larghezza totale - 2 spazi) / 3
        val cellWidthPx = (gridWidthPx - (2 * spacingPx)) / 3f

        fun calculateNewIndex(originalIndex: Int, offset: Offset): Int {
            val originalCol = originalIndex % 3
            val originalRow = originalIndex / 3

            val originalX = originalCol * (cellWidthPx + spacingPx)
            val originalY = originalRow * (cellWidthPx + spacingPx)

            val newX = originalX + offset.x
            val newY = originalY + offset.y

            val newCol = (newX / (cellWidthPx + spacingPx)).roundToInt().coerceIn(0, 2)
            val newRow = (newY / (cellWidthPx + spacingPx)).roundToInt().coerceAtLeast(0)

            val newIndex = (newRow * 3) + newCol
            return newIndex.coerceAtMost(imageList.size - 1)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimensions.mediumPadding)
        ) {
            // Unica Row che si adatta e va a capo automaticamente
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensions.mediumPadding),
                maxItemsInEachRow = 3 // Specifichiamo che vogliamo 3 elementi per riga
            ) {
                displayList.forEachIndexed { index, item ->

                    // Modifier comune per tutte le celle, ora è dinamico
                    val cellModifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f) // Mantiene le celle quadrate
                        .clip(MaterialTheme.shapes.medium)

                    if (item == "ADD_CELL") {
                        Box(
                            modifier = cellModifier
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
                        Box(
                            modifier = cellModifier
                                .then(if (draggingIndex == index) Modifier.offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) } else Modifier)
                                .then(if (index == 0) Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary)) else Modifier)
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { if (draggingIndex == null) { draggingIndex = index } },
                                        onDrag = { change, dragAmount ->
                                            if (draggingIndex == index) {
                                                change.consume()
                                                dragOffset += dragAmount
                                            }
                                        },
                                        onDragEnd = {
                                            draggingIndex?.let { originalIndex ->
                                                val targetIndex = calculateNewIndex(originalIndex, dragOffset).coerceIn(0, imageList.size -1)
                                                if (targetIndex != originalIndex) {
                                                    val mutable = imageList.toMutableList()
                                                    val draggedItem = mutable.removeAt(originalIndex)
                                                    mutable.add(targetIndex.coerceAtMost(mutable.size), draggedItem)
                                                    imageList = mutable
                                                    onImageOrderChanged(mutable)
                                                }
                                            }
                                            draggingIndex = null
                                            dragOffset = Offset.Zero
                                        },
                                        onDragCancel = {
                                            draggingIndex = null
                                            dragOffset = Offset.Zero
                                        }
                                    )
                                },
                            contentAlignment = Alignment.TopEnd
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item)
                                    .placeholder(R.drawable.random_user) // Placeholder locale
                                    .error(R.drawable.random_user) // Fallback locale
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile Image $index",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = {
                                    // Usa indexOf su imageList invece dell'indice di displayList
                                    // perché displayList può includere "ADD_CELL" e divergere da imageList
                                    val realIndex = imageList.indexOf(item)
                                    if (realIndex >= 0) {
                                        val removedItem = imageList.removeAt(realIndex)
                                        onImageOrderChanged(imageList.toList()) // Notifica la nuova lista
                                        onDeleteImage(removedItem)
                                    } else {
                                        Log.e("DeleteImage", "URL '$item' non trovato in imageList, size: ${imageList.size}")
                                    }
                                },
                                modifier = Modifier.size(24.dp) // Leggermente più grande per essere più facile da toccare
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

            Text(
                text = stringResource(R.string.profile_image_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensions.smallPadding),
                textAlign = TextAlign.Center
            )
        }
    }
}