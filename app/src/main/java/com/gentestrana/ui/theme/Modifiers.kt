package com.gentestrana.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gentestrana.ui.theme.NeuroBackground
import com.gentestrana.ui.theme.NeuroSecondary

val commonProfileBoxModifier = Modifier
    .fillMaxWidth()
    .clip(RoundedCornerShape(12.dp))
    .background(NeuroBackground)
    .border(1.dp, NeuroSecondary, RoundedCornerShape(12.dp))
    .padding(16.dp)
    .heightIn(min = 150.dp) // Altezza minima uniforme per entrambi i box
