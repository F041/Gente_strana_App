import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import kotlin.math.roundToInt

@Composable
fun DraggableImageCell(
    imageUrl: String,
    index: Int,
    onMove: (from: Int, to: Int) -> Unit,
    onDelete: () -> Unit,
    isPrimary: Boolean
) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    val threshold = 30f  // Soglia in pixel per lo swap

    // Animazione dell'elevazione per dare feedback visivo durante il drag
    val elevation by animateDpAsState(targetValue = if (isDragging) 16.dp else 0.dp)

    Box(
        modifier = Modifier
            .size(100.dp)
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .shadow(elevation)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDrag = { change, dragAmount ->
                        change.consumeAllChanges()
                        offset += dragAmount
                    },
                    onDragCancel = {
                        offset = Offset.Zero
                        isDragging = false
                    },
                    onDragEnd = {
                        if (offset.x > threshold) {
                            onMove(index, index + 1)
                        } else if (offset.x < -threshold) {
                            onMove(index, index - 1)
                        }
                        // Potresti aggiungere logica verticale se necessario.
                        offset = Offset.Zero
                        isDragging = false
                    }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Image(
            painter = rememberAsyncImagePainter(imageUrl),
            contentDescription = "Profile Image $index",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (isPrimary) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary))
            )
        }
        IconButton(
            onClick = onDelete,
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
