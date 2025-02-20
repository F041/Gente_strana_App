package com.gentestrana.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentestrana.ui.theme.NeuroAccent
import com.gentestrana.ui.theme.NeuroPrimary

@Composable
fun FilterChip(
    text: String,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {}
) {
    Surface(
        color = if (isSelected) NeuroAccent.copy(alpha = 0.4f) else NeuroAccent.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.clickable { onSelect() }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (isSelected) Color.White else NeuroPrimary,
            fontSize = 14.sp
        )
    }
}
