package com.gentestrana.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gentestrana.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tos_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp) // Padding interno
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.tos_last_updated),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            TosSection(titleRes = R.string.tos_introduction_header, textRes = R.string.tos_introduction_text)
            TosSection(titleRes = R.string.tos_eligibility_header, textRes = R.string.tos_eligibility_text)
            TosSection(titleRes = R.string.tos_account_header, textRes = R.string.tos_account_text)
            TosSection(titleRes = R.string.tos_conduct_header, textRes = R.string.tos_conduct_text)
            TosSection(titleRes = R.string.tos_user_content_header, textRes = R.string.tos_user_content_text)
            TosSection(titleRes = R.string.tos_our_rights_header, textRes = R.string.tos_our_rights_text)
            TosSection(titleRes = R.string.tos_disclaimer_header, textRes = R.string.tos_disclaimer_text)
            TosSection(titleRes = R.string.tos_app_ip_header, textRes = R.string.tos_app_ip_text)
            TosSection(titleRes = R.string.tos_privacy_header, textRes = R.string.tos_privacy_text)
            TosSection(titleRes = R.string.tos_changes_header, textRes = R.string.tos_changes_text)
            TosSection(titleRes = R.string.tos_termination_header, textRes = R.string.tos_termination_text)
            TosSection(titleRes = R.string.tos_law_header, textRes = R.string.tos_law_text)
            TosSection(titleRes = R.string.tos_contact_header, textRes = R.string.tos_contact_text)

            Spacer(modifier = Modifier.height(16.dp)) // Spazio alla fine
        }
    }
}

@Composable
private fun TosSection(titleRes: Int, textRes: Int) {
    val textContent = stringResource(textRes)
    // Rimuoviamo il check isList qui, lo gestirà il nuovo LinkifiedText
    // val isList = textContent.contains("\n") // NON PIÙ NECESSARIO QUI

    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        // Titolo (invariato)
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // ---> USA DIRETTAMENTE IL NUOVO LINKIFIED TEXT <---
        LinkifiedText(
            text = textContent,
            // Passa lo stile che vuoi per il testo normale
            style = MaterialTheme.typography.bodyLarge
        )
        // ---> FINE MODIFICA <---

    }
}

@Composable
fun LinkifiedText(text: String, modifier: Modifier = Modifier, style: TextStyle = LocalTextStyle.current) {
    println("gentestrana Testo ricevuto: $text")
    val urlRegex = remember { Regex("""(https?://[^\s]+)|<a\s+href="([^"]+)">([^<]+)</a>""", RegexOption.IGNORE_CASE) }
    val uriHandler = LocalUriHandler.current


    val matches = urlRegex.findAll(text).toList()
    matches.forEach { match ->
        println("Match trovato: ${match.value}")
    }

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        urlRegex.findAll(text).forEach { matchResult ->
            val match = matchResult.value
            val startIndex = matchResult.range.first
            val endIndex = matchResult.range.last + 1

            // Aggiungi il testo prima del match
            append(text.substring(lastIndex, startIndex))

            // Estrai URL e testo visualizzato
            val (url, displayText) = if (match.startsWith("<a")) {
                // È un tag <a>
                val href = matchResult.groupValues[2] // Gruppo 2 contiene l'URL
                val linkText = matchResult.groupValues[3] // Gruppo 3 contiene il testo del link
                href to linkText
            } else {
                // È un URL semplice
                match to match // Mostra l'URL stesso come testo
            }

            // Aggiungi il testo del link con annotazione
            pushStringAnnotation(tag = "URL", annotation = url)
            withStyle(style = SpanStyle(
                color = MaterialTheme.colorScheme.primary, // Colore link
                textDecoration = TextDecoration.Underline // Sottolinea link
            )) {
                append(displayText)
            }
            pop() // Rimuovi l'annotazione

            lastIndex = endIndex
        }
        // Aggiungi il testo rimanente dopo l'ultimo match
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }

    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = style.copy(color = MaterialTheme.colorScheme.onSurface), // Colore testo base
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset + 1)
                .firstOrNull()?.let { annotation ->
                    try {
                        uriHandler.openUri(annotation.item)
                    } catch (e: Exception) {
                        // Puoi gestire l'eccezione se necessario
                    }
                }
        }
    )
}
