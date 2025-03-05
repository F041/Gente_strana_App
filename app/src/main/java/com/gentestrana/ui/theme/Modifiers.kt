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
import androidx.compose.ui.unit.dp

@Composable // 👈 Aggiungi l'annotazione @Composable
fun commonProfileBoxModifier(): Modifier {
    return Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant) // ✅ Ora MaterialTheme.colorScheme è OK qui dentro
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        .padding(16.dp)
        .heightIn(min = 150.dp)
}
