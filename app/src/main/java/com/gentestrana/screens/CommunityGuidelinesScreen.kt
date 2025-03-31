package com.gentestrana.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.gentestrana.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityGuidelinesScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                // Titolo già usa stringResource, va bene
                title = { Text(stringResource(R.string.community_guidelines)) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            GuidelineSectionTitle(stringResource(R.string.guidelines_intro_title))
            GuidelineText(stringResource(R.string.guidelines_intro_text))

            Spacer(modifier = Modifier.height(16.dp))

            GuidelineSectionTitle(stringResource(R.string.guidelines_respect_title))
            GuidelineText(stringResource(R.string.guidelines_respect_text))

            Spacer(modifier = Modifier.height(16.dp))

            GuidelineSectionTitle(stringResource(R.string.guidelines_communication_title))
            GuidelineText(stringResource(R.string.guidelines_communication_text))

            Spacer(modifier = Modifier.height(16.dp))

            GuidelineSectionTitle(stringResource(R.string.guidelines_disagreements_title))
            GuidelineText(stringResource(R.string.guidelines_disagreements_text))

            Spacer(modifier = Modifier.height(16.dp))

            GuidelineSectionTitle(stringResource(R.string.guidelines_not_accepted_title))
            GuidelineText(stringResource(R.string.guidelines_not_accepted_text))

            Spacer(modifier = Modifier.height(16.dp))

            GuidelineSectionTitle(stringResource(R.string.guidelines_reporting_title))
            GuidelineText(stringResource(R.string.guidelines_reporting_text))

            Spacer(modifier = Modifier.height(16.dp))

            GuidelineSectionTitle(stringResource(R.string.guidelines_selfcare_title))
            GuidelineText(stringResource(R.string.guidelines_selfcare_text))

            Spacer(modifier = Modifier.height(24.dp))

            // Testo finale
            Text(
                text = stringResource(R.string.guidelines_closing_text),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Helper per i titoli - ora accetta una stringa già risolta
@Composable
private fun GuidelineSectionTitle(text: String) {
    Text(
        text = text, // Usa direttamente la stringa passata
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

// Helper per il testo - ora accetta una stringa già risolta
@Composable
private fun GuidelineText(text: String) {
    Text(
        text = text, // Usa direttamente la stringa passata
        style = MaterialTheme.typography.bodyLarge
    )
}