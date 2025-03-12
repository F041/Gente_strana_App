package com.gentestrana.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.Composable
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.pager.HorizontalPager // NEW IMPORT
import androidx.compose.foundation.pager.rememberPagerState // NEW IMPORT

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfilePhotoCarousel(
    // ha senso metterlo qui? Secondo Gemini si
    imageUrls: List<String>,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { imageUrls.size })
    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .aspectRatio(1f)
                    .fillMaxSize()
//                    .padding(8.dp)
                // rimosso, più simile a Tandem
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = imageUrls[page]
                    ),
                    contentDescription = "foto utente",
                    contentScale = ContentScale.Crop, modifier = Modifier
                        .fillMaxSize()
//                        .clip(RoundedCornerShape(8.dp))
                    // rimosso, più simile a Tandem
                )
            }
        }

        // Indicatori (pallini) sotto il carosello
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            for (i in imageUrls.indices) {
                val color = if (i == pagerState.currentPage) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                }
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun SingleTopicNavigator(
    topics: List<String>,
    modifier: Modifier = Modifier
) {
    var currentTopicIndex by remember { mutableIntStateOf(0) }
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
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
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