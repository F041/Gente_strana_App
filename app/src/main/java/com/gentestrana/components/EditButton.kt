package com.gentestrana.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gentestrana.ui.theme.NeuroSecondary

@Composable
fun EditButton(
    isEditing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Se vuoi internazionalizzare, passa stringResource al posto di "edit"/"save"
    editLabel: String = "edit",
    saveLabel: String = "save"
) {
    val label = if (isEditing) saveLabel else editLabel
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = NeuroSecondary, // Sfondo pulsante
            contentColor = Color.White       // Colore del testo
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(imageVector = Icons.Default.Edit, contentDescription = label)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label)
    }
}
