package com.gentestrana.users

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@Composable
fun ServiceProfileCard(
    service: Service,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Immagine del servizio
            Image(
                painter = rememberAsyncImagePainter(
                    service.profilePicUrl.firstOrNull() ?: "res/drawable/random_user.webp"
                ),
                contentDescription = "Service Image",
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            // Dettagli del servizio
            Column {
                Text(text = service.username, style = MaterialTheme.typography.titleMedium)
                if (service.topics.isNotEmpty()) {
                    Text(text = service.topics.first(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
