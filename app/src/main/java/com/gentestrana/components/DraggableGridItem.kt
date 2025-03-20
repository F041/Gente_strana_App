package com.gentestrana.components


import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * Cella draggabile.
 *
 * @param item L'oggetto ImageItem da mostrare.
 * @param onMove Callback chiamato durante il drag, con l'offset corrente.
 * @param onDragEnd Callback chiamato al termine del drag.
 * @param modifier Modifier aggiuntivo.
 */
@Composable
fun DraggableGridImageCell(
    item: ImageItem,
    onMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Stato per tracciare l'offset corrente durante il drag
    var offset by remember { mutableStateOf(Offset.Zero) }

    // 🎯 Animiamo l'offset invece di applicarlo istantaneamente
    val animatedOffset by animateOffsetAsState(
        targetValue = offset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, // Effetto "rimbalzante"
            stiffness = Spring.StiffnessLow // Movimento fluido
        )
    )

    Box(
        modifier = modifier
            .offset { IntOffset(animatedOffset.x.roundToInt(), animatedOffset.y.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume() // Evita conflitti con altri gesti
                        offset += dragAmount // Modifica il target dell'animazione
                        onMove(offset) // Notifica il movimento
                    },
                    onDragEnd = {
                        onDragEnd() // Callback di fine drag
                        offset = Offset.Zero // L'animazione riporta dolcemente l'elemento alla posizione iniziale
                    }
                )
            }
    ) {
        // Mostra la cella dell'immagine
        GridImageCell(item = item)
    }
}
