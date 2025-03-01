package com.gentestrana.components


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentestrana.R
import com.gentestrana.ui.theme.NeuroSecondary
import com.gentestrana.ui.theme.commonProfileBoxModifier

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileTopicsList(
    title: String,
    topics: List<String>,
    placeholder: String,
    newTopicMaxLength: Int,
    modifier: Modifier = Modifier,
    onValueChange: (List<String>) -> Unit,
    editLabel: String = stringResource(R.string.edit),
    saveLabel: String = stringResource(R.string.save)
) {
    var isEditing by remember { mutableStateOf(false) }
    var localTopics by remember { mutableStateOf(topics) }
    var newTopicText by remember { mutableStateOf("") }

    // Sincronizza la lista locale se quella esterna cambia (es. da Firebase)
    LaunchedEffect(topics) {
        localTopics = topics
    }

    Column(
        modifier = modifier.then(commonProfileBoxModifier)
    ) {
        // Titolo del box
        Text(
            text = title.uppercase(),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Incapsula il FlowRow in un Box che forza fillMaxWidth()
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
                            {
                                localTopics = localTopics.toMutableList().also { it.removeAt(index) }
                            }
                        } else null
                    )
                }
            }
        }

        if (isEditing) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = newTopicText,
                onValueChange = { newText ->
                    if (newText.length <= newTopicMaxLength) {
                        newTopicText = newText
                    }
                },
                placeholder = { Text(placeholder) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    if (newTopicText.isNotBlank()) {
                        localTopics = localTopics + newTopicText.trim()
                        newTopicText = ""
                    }
                },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeuroSecondary,
                    contentColor = Color.White
                )
            ) {
                Text(text = stringResource(id = R.string.add))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Bottone di modifica / salvataggio uniforme
        EditButton(
            isEditing = isEditing,
            onClick = {
                if (isEditing) {
                    onValueChange(localTopics)
                }
                isEditing = !isEditing
            },
            editLabel = editLabel,
            saveLabel = saveLabel
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

