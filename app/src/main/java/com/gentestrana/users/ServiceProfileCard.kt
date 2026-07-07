package com.gentestrana.users

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.gentestrana.R

@Composable
fun ServiceProfileCard(
    service: Service,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
                    ImageRequest.Builder(context)
                        .data(service.profilePicUrl.firstOrNull()?.takeIf { it.isNotBlank() } ?: R.drawable.random_user)
                        .placeholder(R.drawable.random_user)
                        .error(R.drawable.random_user)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = "Service Image",
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Crop
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
