package com.gentestrana.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gentestrana.R
import com.gentestrana.utils.RequiredProfileField

/**
 * Campi obbligatori del profilo: bio, almeno un topic e data di nascita.
 *
 * Componente condiviso tra:
 * - RegistrationScreen (creazione account con email)
 * - CompleteProfileScreen (completamento per login Google e utenti gia registrati)
 *
 * I topics vengono aggiornati SUBITO a ogni aggiunta/rimozione (onTopicsChange),
 * a differenza di ProfileTopicsList che salva solo uscendo dalla modalita modifica.
 * Questo evita che l'utente aggiunga un topic e il form lo consideri ancora mancante.
 *
 * @param missingFields campi obbligatori mancanti, evidenziati in rosso (da
 *        [com.gentestrana.utils.missingRequiredProfileFields]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileCompletionFields(
    context: Context,
    bio: String,
    onBioChange: (String) -> Unit,
    topics: List<String>,
    onTopicsChange: (List<String>) -> Unit,
    birthTimestamp: Long,
    onBirthTimestampChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    missingFields: List<RequiredProfileField> = emptyList()
) {
    var newTopic by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        // ---- BIO ----
        Text(
            text = stringResource(R.string.profile_bio_label).uppercase(),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = bio,
            onValueChange = { if (it.length <= BioMaxLength) onBioChange(it) },
            placeholder = { Text(stringResource(R.string.bio_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            isError = missingFields.contains(RequiredProfileField.BIO),
            minLines = 3
        )
        if (missingFields.contains(RequiredProfileField.BIO)) {
            RequiredFieldError()
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- TOPICS ----
        Text(
            text = stringResource(R.string.profile_topics_label).uppercase(),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (topics.isEmpty()) {
            Text(
                text = stringResource(R.string.profile_topics_empty_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                topics.forEach { topic ->
                    Chip(
                        text = topic,
                        onDelete = { onTopicsChange(topics - topic) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = newTopic,
                onValueChange = { if (it.length <= TopicMaxLength) newTopic = it },
                placeholder = { Text(stringResource(R.string.topics_placeholder)) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                val candidate = newTopic.trim()
                if (candidate.isNotBlank() && topics.none { it.equals(candidate, ignoreCase = true) }) {
                    onTopicsChange(topics + candidate)
                    newTopic = ""
                }
            }) {
                Text(stringResource(R.string.add))
            }
        }

        if (missingFields.contains(RequiredProfileField.TOPICS)) {
            RequiredFieldError()
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---- DATA DI NASCITA ----
        Text(
            text = stringResource(R.string.profile_birthdate_label).uppercase(),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        DateOfBirthPicker(
            context = context,
            birthTimestamp = birthTimestamp,
            onDateSelected = onBirthTimestampChange
        )
        if (missingFields.contains(RequiredProfileField.BIRTH_DATE)) {
            RequiredFieldError()
        }
    }
}

@Composable
private fun RequiredFieldError() {
    Text(
        text = stringResource(R.string.profile_field_required),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
    )
}

private const val BioMaxLength = 500
private const val TopicMaxLength = 40