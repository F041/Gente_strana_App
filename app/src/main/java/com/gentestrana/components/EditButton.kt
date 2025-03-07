package com.gentestrana.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gentestrana.R


@Composable
fun EditButton(
    isEditing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = if (isEditing) stringResource(R.string.save) else stringResource(R.string.edit)

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Icon(imageVector = Icons.Default.Edit, contentDescription = label)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label)
    }
}


@Composable
fun CancelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.cancel))
    }
}