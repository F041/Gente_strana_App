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
import com.google.firebase.Timestamp
import java.util.Date
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.gentestrana.users.Service
import com.gentestrana.users.ServiceProfileCard

@Composable
fun ServicesScreen() {
    // Definiti manualmente, in futuro a mano tramite fb?
    val context = LocalContext.current
    val services = listOf(
        Service(
            docId = "1",
            username = "STATiCalmo",
            rawBirthTimestamp = Timestamp(Date(1672531200000L)), // 1 gennaio 2023 => 2 anni (calcolato)
            topics = listOf("studio di statistica per aziende ed organizzazioni"),
            profilePicUrl = listOf("https://staticalmo.com/wp-content/uploads/2023/04/cropped-Logo_STATiCalmo_-transformed-1-120x117.png"),
            spokenLanguages = emptyList()
        ),
        Service(
            docId = "2",
            username = "Auticon",
            rawBirthTimestamp = Timestamp(Date(1293840000000L)), // 1 gennaio 2011
            topics = listOf("consulenza IT che assume neurodiversi"),
            profilePicUrl = listOf("https://www.laboratoriolinc.it/wp-content/uploads/2020/11/Logo-auticon-1024x1024-1.png"),
            spokenLanguages = emptyList()
        ),
        Service(
            docId = "3",
            username = "Specialisterne",
            rawBirthTimestamp = Timestamp(Date(1104537600000L)),
            topics = listOf("azienda internazionale danese che assume e fa assumere in altre aziende persone con autismo e Asperger, facendo svolgere loro attività come test del software, controllo di qualità e data entry, particolarmente adatte alle loro caratteristiche di buona memoria, attenzione ai dettagli, concentrazione e attitudine a svolgere azioni ripetitive."),
            profilePicUrl = listOf("https://d1byvvo791gp2e.cloudfront.net/public/assets/media/images/000/632/598/images/size_550x415_LogoSpec.png?1449765967"),
            spokenLanguages = emptyList()
        )

    )

    val randomizedServices = services.shuffled()

    // Layout principale con Scaffold e LazyColumn per elencare le card
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = "Servizi",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                itemsIndexed(randomizedServices) { index, service ->
                    ServiceProfileCard(
                        service = service,
                        onClick = {
                            // Quando l'utente tocca la card, apriamo il sito web associato
                            val url = getServiceWebsite(service)
                            openWebsite(context, url)
                        }
                    )
                    HorizontalDivider(thickness = 1.dp)
                }
            }
        }
    }
}


// Funzione per ottenere l'URL del sito web del servizio
fun getServiceWebsite(service: Service): String = when(service.docId) {
    "1" -> "https://staticalmo.com"
    "2" -> "https://www.auticon.com"
    "3" -> "https://specialisterne.com/"
    else -> ""
}

fun openWebsite(context: Context, url: String) {
    if (url.isNotBlank()) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}

