package com.gentestrana.components


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentestrana.R
import com.gentestrana.components.EditButton
import com.gentestrana.ui.theme.commonProfileBoxModifier

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileTopicsList(
    title: String,
    topics: List<String>,
    placeholder: String,
    newTopicMaxLength: Int,
    onValueChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var newTopicText by remember { mutableStateOf("") }

    // Gestione della lista dei topics in locale
    val localTopics = remember { mutableStateListOf<String>().apply { addAll(topics) } }

    // Sincronizza la lista locale se topics cambia
    LaunchedEffect(topics) {
        localTopics.clear()
        localTopics.addAll(topics)
    }

    Column(
        modifier = modifier.then(commonProfileBoxModifier())
    ) {
        // Titolo aggiornato con bodyMedium bold
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                localTopics.forEachIndexed { index, topic ->
                    Chip(
                        text = topic,
                        onDelete = if (isEditing) {
                            { localTopics.removeAt(index) }
                        } else null
                    )
                }
            }
        }

        if (isEditing) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = newTopicText,
                onValueChange = {
                    if (it.length <= newTopicMaxLength) newTopicText = it
                },
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    if (newTopicText.isNotBlank()) {
                        localTopics.add(newTopicText.trim())
                        newTopicText = ""
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = stringResource(id = R.string.add))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        EditButton(
            isEditing = isEditing,
            onClick = {
                if (isEditing) onValueChange(localTopics.toList())
                isEditing = !isEditing
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun Chip(
    text: String,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text = text, fontSize = 14.sp)
            if (onDelete != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

