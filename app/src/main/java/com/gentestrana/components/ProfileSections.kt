package com.gentestrana.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration

import com.gentestrana.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter

@Composable
fun ProfilePicturesScreen(
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
    onAddImage: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Titolo centrato
        Text(
            text = "LE TUE FOTO",  // In seguito potrai sostituirlo con stringResource(R.string.profile_pictures_title)
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Griglia di immagini
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(imageUrls) { url ->
                // Mostra ogni immagine in un box quadrato
                androidx.compose.foundation.Image(
                    painter = rememberAsyncImagePainter(url),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
            // Se ci sono meno di 5 foto, mostra un box "Aggiungi foto"
            if (imageUrls.size < 5) {
                item {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            .clickable { onAddImage?.invoke() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SingleTopicNavigator(
    topics: List<String>,
    modifier: Modifier = Modifier
) {
    var currentTopicIndex by remember { mutableStateOf(0) }
    if (topics.isEmpty()) {
        Text("Nessun argomento", modifier = modifier)
        return
    }

    val configuration = LocalConfiguration.current
    // Ottieni la larghezza dello schermo in dp
    val screenWidth = configuration.screenWidthDp.dp
    // Imposta il chip per occupare, ad esempio, il 60% della larghezza dello schermo
    val chipMaxWidth = screenWidth * 0.6f // sta roba va messa anche altrove

    val arrowSize = 48.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        IconButton(
            onClick = { if (currentTopicIndex > 0) currentTopicIndex-- },
            enabled = currentTopicIndex > 0,
            modifier = Modifier.size(arrowSize)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Argomento precedente",
                tint = if (currentTopicIndex > 0)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.0f)
            )
        }

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(MaterialTheme.shapes.medium)
                .widthIn(max = chipMaxWidth)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = topics[currentTopicIndex],
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        IconButton(
            onClick = { if (currentTopicIndex < topics.lastIndex) currentTopicIndex++ },
            enabled = currentTopicIndex < topics.lastIndex,
            modifier = Modifier.size(arrowSize)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Argomento successivo",
                tint = if (currentTopicIndex < topics.lastIndex)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.0f)
            )
        }
    }
}

@Composable
fun TopicsBox(
    topics: List<String>,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    // Usiamo il 60% della larghezza dello schermo come limite massimo
    val boxMaxWidth = screenWidth * 0.6f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp)
            .widthIn(max = boxMaxWidth),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.topics_title).uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        SingleTopicNavigator(topics = topics)
    }
}


@Composable
fun BioBox(
    bioText: String,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    // Usiamo il 60% della larghezza dello schermo come limite massimo
    val boxMaxWidth = screenWidth * 0.6f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp)
            .widthIn(max = boxMaxWidth),
        horizontalAlignment = Alignment.CenterHorizontally  // Titolo centrato
    ) {
        Text(
            text = stringResource(R.string.user_bio).uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = bioText,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}