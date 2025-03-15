package com.gentestrana.components

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.gentestrana.components.GenericLoadingScreen

/**
 * Raggruppa una lista in righe da count elementi.
 */
private fun <T> List<T>.chunkedBy(count: Int): List<List<T>> = this.chunked(count)

/**
 * Griglia statica di immagini di profilo con:
 * - Riordino tramite frecce (testuali "←" e "→").
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
    var imageList = remember { mutableStateOf(images.toMutableList()) }
    LaunchedEffect(images) {
        imageList.value = images.toMutableList()
    }

    // Se il numero di immagini è inferiore al massimo, aggiungi un marker "ADD_CELL"
    val displayList = remember(imageList.value) {
        val listCopy = imageList.value.toMutableList()
        if (listCopy.size < maxImages) listCopy.add("ADD_CELL")
        listCopy
    }

    // Raggruppa la lista in righe da 3 elementi
    val rows = displayList.chunkedBy(3)

    // Calcola l'altezza della griglia in base al numero di righe
    val cellSize = 100.dp
    val verticalSpacing = 16.dp
    val extraPadding = 32.dp
    val rowCount = if (displayList.size % 3 == 0) displayList.size / 3 else (displayList.size / 3 + 1)
    val gridHeight = (cellSize * rowCount) + (if (rowCount > 0) verticalSpacing * (rowCount - 1) else 0.dp) + extraPadding

    // Utilizza una Column statica che avrà altezza esattamente pari a gridHeight
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        // Box per immagine esistente con pulsante di eliminazione e frecce per riordino
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .clip(MaterialTheme.shapes.medium)
                                .then(
                                    if (flatIndex == 0)
                                        Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary))
                                    else Modifier
                                ),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(item),
                                contentDescription = "Profile Image $flatIndex",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = {
                                    val mutable = imageList.value.toMutableList()
                                    Log.d("DeleteImage", "Elimina immagine CLICKED, flatIndex: $flatIndex, listSize: ${mutable.size}, imageListSize: ${imageList.value.size}, imageUrl: $item")

                                    // 🚩 LOG AGGIUNTO: Log dell'URL PRIMA della chiamata a onDeleteImage
                                    Log.d("DeleteImage_UI_URL", "URL PRIMA di onDeleteImage: $item")

                                    mutable.removeAt(flatIndex)
                                    imageList.value = mutable
                                    Log.d("DeleteImage", "Immagine ELIMINATA, NUOVA listSize: ${mutable.size}, NUOVA imageListSize: ${imageList.value.size}")
                                    onImageOrderChanged(mutable)
                                    onDeleteImage(item)   // <-- CHIAMA CALLBACK **DOPO** aver aggiornato imageList.value e passa imageUrl
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
                            // Controlli di riordino: freccette a sinistra e a destra, in basso
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                if (flatIndex > 0) {
                                    Text(
                                        text = "←",
                                        modifier = Modifier.clickable {
                                            val mutable = imageList.value.toMutableList()
                                            val temp = mutable[flatIndex - 1]
                                            mutable[flatIndex - 1] = mutable[flatIndex]
                                            mutable[flatIndex] = temp
                                            imageList.value = mutable
                                            onImageOrderChanged(mutable)
                                        }
                                    )
                                } else {
                                    Text(text = " ")
                                }
                                if (flatIndex < imageList.value.size - 1 && flatIndex < maxImages - 1) {
                                    Text(
                                        text = "→",
                                        modifier = Modifier.clickable {
                                            val mutable = imageList.value.toMutableList()
                                            val temp = mutable[flatIndex + 1]
                                            mutable[flatIndex + 1] = mutable[flatIndex]
                                            mutable[flatIndex] = temp
                                            imageList.value = mutable
                                            onImageOrderChanged(mutable)
                                        }
                                    )
                                } else {
                                    Text(text = " ")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
