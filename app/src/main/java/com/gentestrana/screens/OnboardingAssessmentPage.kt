package com.gentestrana.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.pager.PagerState
import com.gentestrana.R
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingAssessmentPageContent(
    pagerState: PagerState,
    context: Context,
    onNextPageEnabledChanged: (Boolean) -> Unit
) {
    val sharedPreferences = remember {
        context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
    }
    val coroutineScope = rememberCoroutineScope()
    var isConsentChecked by remember { mutableStateOf(false) }
    var autismScoreText by remember { mutableStateOf("") }
    var adhdScoreText by remember { mutableStateOf("") }
    var hasDiagnosis by remember { mutableStateOf(false) }
    val autismTestLink = stringResource(R.string.onboarding_screen3_autism_test_link)
    val adhdTestLink = stringResource(R.string.onboarding_screen3_adhd_test_link)
    val scoreErrorKey = "score_error_occurred"
    var isScoreError by remember {
        mutableStateOf(sharedPreferences.getBoolean(scoreErrorKey, false))
    }
    val scrollState = rememberScrollState()
    var showADHDError by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.onboarding_screen3_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboarding_screen3_description),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Column(
            horizontalAlignment = Alignment.Start,
            // Allinea a sinistra le label e i campi
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.onboarding_screen3_gdpr_notice),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Start
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isConsentChecked,
                    onCheckedChange = { isChecked ->
                        isConsentChecked = isChecked
                        // Abilita pagina succ solo se ha ANCHE diagnosi
                        // Altrimenti l'abilitazione avverrà tramite il bottone "Verify"
                        onNextPageEnabledChanged(hasDiagnosis && isChecked)
                    }
                )
                Text(
                    text = stringResource(R.string.onboarding_screen3_checkbox_consent),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

        // Checkbox "Ho già una diagnosi" e testo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Checkbox(
                checked = hasDiagnosis,
                onCheckedChange = { isChecked ->
                    hasDiagnosis = isChecked
                    // Abilita pagina succ solo se ha diagnosi E ha ANCHE dato consenso
                    onNextPageEnabledChanged(isChecked && isConsentChecked)
                }
            )
            Text(
                text = stringResource(R.string.onboarding_screen3_diagnosis_checkbox),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))



            // INIZIO SEZIONE CONDIZIONALE
            if (!hasDiagnosis) {
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    // Link ai test
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(autismTestLink))
                            context.startActivity(intent)
                        }
                            .hoverable(interactionSource = remember { MutableInteractionSource() })
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_screen3_autism),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = TextDecoration.Underline,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp)) // Spazio tra testo e icona
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Launch,
                            contentDescription = stringResource(R.string.open_external_link), // Stringa per accessibilità
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp) // Dimensione icona
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(adhdTestLink))
                            context.startActivity(intent)
                        }
                            .hoverable(interactionSource = remember { MutableInteractionSource() })
                ) {
                        Text(
                            text = stringResource(R.string.onboarding_screen3_adhd),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = TextDecoration.Underline,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Launch,
                            contentDescription = stringResource(R.string.open_external_link),
                            // --- MODIFICA: Cambiato colore a secondary ---
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Sezione punteggio e verifica (solo se il consenso è dato)
                    if (isConsentChecked) {
                        Column(
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_screen3_score),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedTextField(
                                value = autismScoreText,
                                onValueChange = { newText ->
                                    if (newText.isEmpty() || newText.matches(Regex("^\\d+\$"))) {
                                        autismScoreText = newText
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                enabled = !isScoreError,
                                modifier = Modifier.fillMaxWidth(0.8f),
                                colors = TextFieldDefaults.colors( // <-- Colori CORRETTAMENTE qui
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(
                                        alpha = 0.12f
                                    )
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = stringResource(R.string.onboarding_screen3_score_adhd), // String resource da aggiungere
                                style = MaterialTheme.typography.bodyMedium
                            )
                            OutlinedTextField(
                                value = adhdScoreText,
                                isError = showADHDError,
                                supportingText = {
                                    if (showADHDError) {
                                        Text(
                                            text = stringResource(R.string.adhd_score_error),
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )                                  }
                                },
                                onValueChange = { newText ->
                                    when {
                                        // Permette campo vuoto (per backspace)
                                        newText.isEmpty() -> adhdScoreText = newText
                                        // Accetta solo numeri da 1 a 6
                                        newText.matches(Regex("^[1-6]\$")) -> {
                                            if (newText.toInt() in 1..6) {
                                                adhdScoreText = newText
                                            }
                                        }
                                        // Blocca input non valido
                                        else -> showADHDError = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .padding(bottom = 4.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // <-- Tastiera numerica
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.12f)
                                ),
                                singleLine = true
                            )
                            Text(
                                text = stringResource(R.string.onboarding_screen3_score_adhd_label),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 0.9
                                ),
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .padding(top = 2.dp, bottom = 4.dp)
                                    .offset(y = (-16).dp)
                                // non elegante
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isScoreError) {
                                Text(
                                    text = stringResource(R.string.onboarding_screen3_score_threshold_both),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                // Padding sotto l'errore
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    val autismScore = autismScoreText.toIntOrNull()
                                    val adhdScore = adhdScoreText.toIntOrNull()
                                    // Verifica se ALMENO uno dei punteggi è valido
                                    val isValid =
                                        (autismScore != null && autismScore >= 64) || (adhdScore != null && adhdScore >= 4)

                                    if (isValid) {
                                        Log.d(
                                            "OnboardingScreen",
                                            "Punteggio valido. Pagina succ abilitata."
                                        )
                                        isScoreError = false // Resetta l'errore se valido
                                        sharedPreferences.edit().remove(scoreErrorKey)
                                            .apply() // Rimuovi errore salvato
                                        onNextPageEnabledChanged(true) // Abilita SOLO se valido
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    } else {
                                        Log.d("OnboardingScreen", "Nessun punteggio valido.")
                                        isScoreError = true // Imposta errore
                                        sharedPreferences.edit().putBoolean(scoreErrorKey, true)
                                            .apply()
                                        onNextPageEnabledChanged(false) // Disabilita pagina succ
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(), // Usa fillMaxWidth dentro la Column ridotta
                                enabled = !isScoreError // Disabilitato se c'è errore
                            ) {
                                // Testo del bottone cambiato
                                Text(stringResource(R.string.verify_score_button).uppercase())
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                        }
                    }
                }
            }
            else if (isConsentChecked) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(0.8f).align(Alignment.CenterHorizontally) // Centra il bottone
                ) {
                    Text(stringResource(R.string.next).uppercase())
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}