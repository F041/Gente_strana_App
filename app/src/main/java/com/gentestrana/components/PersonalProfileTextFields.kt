@file:OptIn(ExperimentalMaterial3Api::class)
package com.gentestrana.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gentestrana.R
import com.gentestrana.ui.theme.commonProfileBoxModifier
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.ui.platform.LocalContext
import com.gentestrana.utils.removeSpaces
import com.gentestrana.components.EditButton


// Composable per un campo di testo standard
@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String? = null,
    minLength: Int = 0,  // proprietà per la lunghezza minima
    isError: Boolean = false,
    errorMessage: String? = null,
    maxLength: Int? = null,
    removeSpaces: Boolean = false, // <-- nuovo parametro
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    var isFocused by remember { mutableStateOf(false) }
    // Stato per tracciare se l'utente ha interagito con il campo
    var hasInteracted by remember { mutableStateOf(false) }
    val focusModifier = Modifier.onFocusChanged { state ->
        isFocused = state.isFocused
    }

    // Calcola se c'è un errore
    val showError = value.isBlank() || (minLength > 0 && value.length < minLength)
    // Messaggio d'errore da visualizzare
    val displayErrorMessage = when {
        value.isBlank() -> errorMessage ?: "Questo campo non può essere vuoto"
        minLength > 0 && value.length < minLength -> errorMessage ?: "Inserisci almeno $minLength caratteri"
        else -> null
    }

    // Forza il limite di caratteri se specificato e rimuove spazi se richiesto
    val newOnValueChange: (String) -> Unit = { newText ->
        if (!hasInteracted) {
            hasInteracted = true
        }
        // Usa la funzione utility se removeSpaces è true
        val processedText = if (removeSpaces) {
            removeSpaces(newText)
        } else {
            newText
        }
        if (maxLength != null) {
            if (processedText.length <= maxLength) {
                onValueChange(processedText)
            } else {
                onValueChange(processedText.substring(0, maxLength))
            }
        } else {
            onValueChange(processedText)
        }
    }

    // Vibrazione solo se l'utente ha interagito e c'è un errore
    val context = LocalContext.current
    if (showError && hasInteracted) {
        LaunchedEffect(showError, hasInteracted) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(200)
                }
            }
        }
    }

    Column(modifier = modifier.then(focusModifier)) {
        OutlinedTextField(
            value = value,
            onValueChange = newOnValueChange,
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium, // Migliore leggibilità
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            placeholder = placeholder?.let {
                {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) // Placeholder più chiaro
                    )
                }
            },
            isError = showError,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error
            )
        )
        if (maxLength != null) {
            Text(
                text = "${value.length} / $maxLength",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .padding(horizontal = 16.dp),
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                color = if (value.length > maxLength) MaterialTheme.colorScheme.error else Color.Gray
            )
        }
        if (showError && displayErrorMessage != null) {
            Text(
                text = displayErrorMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 16.dp, end = 16.dp),
                // Allineato al campo di testo
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                // Testo in grassetto per maggiore evidenza
                )
            )
        }
    }
}

// Composable per la bio (campo multilinea in un box con stile uniforme)
// I valori di default (titolo, placeholder, etichette) sono letti direttamente da stringResource
@Composable
fun ProfileBioBox(
    initialContent: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(id = R.string.bio_title),
    placeholder: String = stringResource(id = R.string.bio_placeholder),
    maxLength: Int = 800,
    editLabel: String = stringResource(id = R.string.edit),
    saveLabel: String = stringResource(id = R.string.save)
) {
    var isEditing by remember { mutableStateOf(false) }
    var localText by remember { mutableStateOf(initialContent) }

    // Sincronizza il contenuto locale con quello esterno
    LaunchedEffect(initialContent) {
        localText = initialContent
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
        if (isEditing) {
            OutlinedTextField(
                value = localText,
                onValueChange = { newText ->
                    if (newText.length <= maxLength) {
                        localText = newText
                    }
                },
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                maxLines = 5,
                textStyle = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${localText.length} / $maxLength",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (localText.length > maxLength) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        } else {
            val displayText = if (localText.isEmpty()) placeholder else localText
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (localText.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        EditButton(
            isEditing = isEditing,
            onClick = {
                if (isEditing) onValueChange(localText)
                isEditing = !isEditing
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
