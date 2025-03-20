// File: app/src/main/java/com/gentestrana/ServicesScreen.kt
package com.gentestrana.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gentestrana.users.User
import com.gentestrana.users.UserProfileCard

@Composable
fun ServicesScreen() {
    // Definiti manualmente, in futuro a mano tramite fb?
    val services = listOf(
        User(
            docId = "1",
            username = "STATiCalmo",
            rawBirthTimestamp = 1672531200000L, // 1 gennaio 2023 => 2 anni (calcolato)
            topics = listOf("studio di statistica per aziende ed organizzazioni"),
            profilePicUrl = listOf("https://staticalmo.com/wp-content/uploads/2023/04/cropped-Logo_STATiCalmo_-transformed-1-120x117.png"),
            spokenLanguages = emptyList()
        ),
        User(
            docId = "2",
            username = "Auticon",
            rawBirthTimestamp = 1420070400000L, // 1 gennaio 2011
            topics = listOf("consulenza IT che assume neurodiversi"),
            profilePicUrl = listOf("https://www.laboratoriolinc.it/wp-content/uploads/2020/11/Logo-auticon-1024x1024-1.png"),
            spokenLanguages = emptyList()
        ),
        User(
            docId = "3",
            username = "Specialisterne",
            rawBirthTimestamp = 1420070400000L, // 1 gennaio 2015 => 10 anni
            topics = listOf("azienda internazionale danese che assume e fa assumere in altre aziende persone con autismo e Asperger, facendo svolgere loro attività come test del software, controllo di qualità e data entry, particolarmente adatte alle loro caratteristiche di buona memoria, attenzione ai dettagli, concentrazione e attitudine a svolgere azioni ripetitive."),
            profilePicUrl = listOf("https://d1byvvo791gp2e.cloudfront.net/public/assets/media/images/000/632/598/images/size_550x415_LogoSpec.png?1449765967"),
            spokenLanguages = emptyList()
        )

    )

    // Layout principale con Scaffold e LazyColumn per elencare le card
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header per la schermata dei servizi
            Text(
                text = "Servizi",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )

            // Lista dei servizi con le card utente
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                itemsIndexed(services) { index, service ->
                    UserProfileCard(
                        user = service,
                        onClick = { /* Azione da definire al click della card */ }
                    )
                  HorizontalDivider(thickness = 1.dp)
                }
            }
        }
    }
}
