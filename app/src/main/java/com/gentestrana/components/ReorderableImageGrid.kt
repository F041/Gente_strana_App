//package com.gentestrana.components
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.unit.dp
//import kotlin.math.roundToInt
//
///**
// * Composable che dispone gli item in una griglia riordinabile tramite drag & drop.
// *
// * @param initialItems Lista iniziale di ImageItem.
// * @param columns Numero di colonne della griglia (default 3).
// * @param cellSize Dimensione in dp di ogni cella (default 100dp).
// * @param spacing Spaziatura tra le celle (default 16dp).
// * @param onReorder Callback chiamato con la lista aggiornata dopo un riordino.
// */
//@Composable
//fun ReorderableImageGrid(
//    initialItems: List<ImageItem>,
//    columns: Int = 3,
//    cellSize: Int = 100,
//    spacing: Int = 16,
//    onReorder: (List<ImageItem>) -> Unit
//) {
//    // Manteniamo la lista degli item in uno stato mutabile
//    var items by remember { mutableStateOf(initialItems) }
//    // Stato per tracciare quale item è in drag e il suo offset
//    var draggingIndex by remember { mutableStateOf<Int?>(null) }
//    var dragOffset by remember { mutableStateOf(Offset.Zero) }
//
//    // Calcola le dimensioni in pixel per cella e spaziatura
//    val density = LocalDensity.current
//    val cellSizePx = with(density) { cellSize.dp.toPx() }
//    val spacingPx = with(density) { spacing.dp.toPx() }
//    val cellTotalPx = cellSizePx + spacingPx
//
//    // Funzione per calcolare il nuovo indice in base all'offset trascinato
//    fun calculateNewIndex(originalIndex: Int, offset: Offset): Int {
//        val originalRow = originalIndex / columns
//        val originalCol = originalIndex % columns
//
//        // Coordinate originali dell'item
//        val originalX = originalCol * cellTotalPx
//        val originalY = originalRow * cellTotalPx
//
//        // Nuove coordinate considerando l'offset
//        val newX = originalX + offset.x
//        val newY = originalY + offset.y
//
//        // Calcola nuova colonna e riga (arrotondando)
//        val newCol = (newX / cellTotalPx).roundToInt().coerceIn(0, columns - 1)
//        val newRow = (newY / cellTotalPx).roundToInt().coerceAtLeast(0)
//        val newIndex = newRow * columns + newCol
//        return newIndex.coerceAtMost(items.size - 1)
//    }
//
//    // Layout della griglia: raggruppiamo gli item in righe
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(spacing.dp),
//        verticalArrangement = Arrangement.spacedBy(spacing.dp)
//    ) {
//        // Raggruppa la lista in righe da 'columns' elementi
//        val rows = items.chunked(columns)
//        rows.forEachIndexed { rowIndex, rowItems ->
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.spacedBy(spacing.dp)
//            ) {
//                rowItems.forEachIndexed { colIndex, item ->
//                    val index = rowIndex * columns + colIndex
//                    // Per l'item in drag, gestiamo i callback
//                    DraggableGridImageCell(
//                        item = item,
//                        onMove = { offset ->
//                            // Se non è già in drag, setta l'indice
//                            if (draggingIndex == null) draggingIndex = index
//                            dragOffset = offset
//                        },
//                        onDragEnd = {
//                            // Se l'item trascinato ha uno spostamento, calcola il nuovo indice
//                            draggingIndex?.let { originalIndex ->
//                                val targetIndex = calculateNewIndex(originalIndex, dragOffset)
//                                if (targetIndex != originalIndex) {
//                                    val mutableList = items.toMutableList()
//                                    // Rimuovi l'item dalla posizione originale
//                                    val draggedItem = mutableList.removeAt(originalIndex)
//                                    // Inseriscilo nella nuova posizione
//                                    mutableList.add(targetIndex, draggedItem)
//                                    items = mutableList
//                                    onReorder(items)
//                                }
//                            }
//                            draggingIndex = null
//                            dragOffset = Offset.Zero
//                        }
//                    )
//                }
//                // Se l'ultima riga ha meno di 'columns' elementi, aggiungi spazi vuoti
//                if (rowItems.size < columns) {
//                    repeat(columns - rowItems.size) {
//                        Spacer(modifier = Modifier.size(cellSize.dp))
//                    }
//                }
//            }
//        }
//    }
//}
