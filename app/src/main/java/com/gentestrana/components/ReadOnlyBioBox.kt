package com.gentestrana.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gentestrana.R
import com.gentestrana.utils.TranslationHelper
import com.gentestrana.ui.theme.commonProfileBoxModifier
import java.util.Locale

@Composable
fun ReadOnlyBioBox(
    bioText: String,
    modifier: Modifier = Modifier,
    title: String = stringResource(id = R.string.bio_title),
    placeholder: String = stringResource(id = R.string.bio_placeholder)
) {
    var translatedText by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            // Applica la clip per gli angoli arrotondati
            .border( // Applica il bordo
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                // Usa lo stesso colore del bordo di commonProfileBoxModifier
                shape = RoundedCornerShape(12.dp)
            // Applica la stessa forma del bordo
            )
            .padding(16.dp) // Applica il padding interno
            .animateContentSize()
    ) {
        // Titolo
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Testo principale
        val displayText = if (bioText.isEmpty()) placeholder else bioText
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (bioText.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        // Pulsante di traduzione
        Row(
            modifier = Modifier
                .fillMaxWidth()
//                .padding(top = 2.dp)
            ,
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = {
                    if (translatedText == null) {
                        isTranslating = true
                        TranslationHelper.translateText(
                            text = bioText,
                            targetLanguageCode = Locale.getDefault().language,
                            onSuccess = { result ->
                                translatedText = result
                                isTranslating = false
                            },
                            onFailure = {
                                isTranslating = false
                            }
                        )
                    } else {
                        translatedText = null // Collassa la traduzione
                    }
                },
                modifier = Modifier
                    .size(32.dp)
                    .padding(0.dp),
                enabled = !isTranslating
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = "Traduci bio",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Sezione traduzione (solo se presente)
        translatedText?.let {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}