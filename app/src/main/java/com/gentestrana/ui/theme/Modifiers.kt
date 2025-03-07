package com.gentestrana.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun commonProfileBoxModifier(
    cornerRadius: Dp = 12.dp,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    minHeight: Dp = 120.dp, // Modificato da 150dp, perchè?
    paddingValue: Dp = 16.dp
): Modifier {
    return Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(cornerRadius))
        .background(backgroundColor)
        .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
        .padding(paddingValue)
        .heightIn(min = minHeight)
}