package com.gentestrana.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.staticCompositionLocalOf

data class Dimensions(
    val smallPadding: Dp,
    val mediumPadding: Dp,
    val largePadding: Dp,
    val buttonHeight: Dp,
    val iconSize: Dp,
    val dialogPadding: Dp,
    val dialogElevation: Dp
)

val MobileDimensions = Dimensions(
    smallPadding = 4.dp,
    mediumPadding = 8.dp,
    largePadding = 16.dp,
    buttonHeight = 48.dp,
    iconSize = 24.dp,
    dialogPadding = 16.dp,       // Padding per i dialog
    dialogElevation = 4.dp      // Elevazione ridotta
)

val TabletDimensions = Dimensions(
    smallPadding = 8.dp,
    mediumPadding = 16.dp,
    largePadding = 24.dp,
    buttonHeight = 56.dp,
    iconSize = 32.dp,
    dialogPadding = 24.dp,      // Un valore adeguato per tablet
    dialogElevation = 6.dp      // Puoi decidere se variare anche qui
)

val LocalDimensions = staticCompositionLocalOf<Dimensions> { error("No dimensions provided") }
