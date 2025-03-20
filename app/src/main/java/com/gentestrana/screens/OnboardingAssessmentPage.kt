package com.gentestrana.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.pager.PagerState // Import PagerState
import com.gentestrana.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingAssessmentPageContent(
    pagerState: PagerState,
    context: Context,
    onNextPageEnabledChanged: (Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isConsentChecked by remember { mutableStateOf(false) }
    var scoreText by remember { mutableStateOf("") }
    var isScoreError by remember { mutableStateOf(false) }
    var hasDiagnosis by remember { mutableStateOf(false) }
    val autismTestLink = stringResource(R.string.onboarding_screen3_autism_test_link)
    val adhdTestLink = stringResource(R.string.onboarding_screen3_adhd_test_link)


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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

        // Checkbox "Ho già una diagnosi" e testo
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = hasDiagnosis,
                onCheckedChange = { hasDiagnosis = it
                    onNextPageEnabledChanged(it)
                }
            )
            Text(
                text = stringResource(R.string.onboarding_screen3_diagnosis_checkbox),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            horizontalAlignment = Alignment.Start
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
                    onCheckedChange = { isConsentChecked = it }
                )
                Text(
                    text = stringResource(R.string.onboarding_screen3_checkbox_consent),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // INIZIO SEZIONE CONDIZIONALE
            if (!hasDiagnosis) {
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    // Link ai test
                    Text(
                        text = stringResource(R.string.onboarding_screen3_autism),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = TextDecoration.Underline,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(autismTestLink))
                            context.startActivity(intent)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.onboarding_screen3_adhd),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = TextDecoration.Underline,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(adhdTestLink))
                            context.startActivity(intent)
                        }
                    )
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
                                value = scoreText,
                                onValueChange = { newText ->
                                    scoreText = newText
                                    Log.d("OnboardingScreen", "Punteggio inserito: $scoreText")
                                },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Questo controllo del punteggio è specifico per il test sull'autismo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .padding(bottom = 8.dp)
                            )

                            Button(
                                onClick = {
                                    val score = scoreText.toIntOrNull()
                                    if (score != null && score >= 64) {
                                        Log.d("OnboardingScreen", "Punteggio valido: $score. Pagina successiva abilitata.")
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                        onNextPageEnabledChanged(true)
                                    } else {
                                        Log.d("OnboardingScreen", "Punteggio non valido o inferiore a 64: $score")
                                        isScoreError = true
                                        onNextPageEnabledChanged(false)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                Text(text = "Verifica punteggio")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isScoreError) {
                                Text(
                                    text = stringResource(R.string.onboarding_screen3_score_threshold),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                )
                            }
                        }
                    }
                }

//                if(hasDiagnosis)
//                {
//
//                }

            }
        }
    }
}