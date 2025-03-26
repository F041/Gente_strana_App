package com.gentestrana.components

import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource

import com.gentestrana.R
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import com.gentestrana.utils.TranslationHelper
import com.gentestrana.utils.TranslationHelper.isTranslationModelDownloaded
import java.util.Locale


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
private fun SingleTopicNavigator(
    topics: List<String>,
    currentTopicIndex: Int,
    translatedText: String?,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
    ) {
        IconButton(
            onClick = { if (currentTopicIndex > 0) onIndexChange(currentTopicIndex - 1) },
            enabled = currentTopicIndex > 0,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Argomento precedente",
                tint = if (currentTopicIndex > 0) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0f)
                // Trasparenza quando disabilitato
                }
            )
        }

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = translatedText ?: topics[currentTopicIndex],
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
        }

        IconButton(
            onClick = { if (currentTopicIndex < topics.lastIndex) onIndexChange(currentTopicIndex + 1) },
            enabled = currentTopicIndex < topics.lastIndex,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Argomento successivo",
                tint = if (currentTopicIndex < topics.lastIndex) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0f)
                // Trasparenza quando disabilitato
                }
            )
        }
    }
}


@Composable
fun ReadOnlyTopicsBox(
    topics: List<String>,
    modifier: Modifier = Modifier
) {
    var currentTopicIndex by remember { mutableIntStateOf(0) }
    var translatedTopic by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    val targetLanguageCode = Locale.getDefault().language

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.topics_title).uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Topic Navigator senza logica di traduzione
        SingleTopicNavigator(
            topics = topics,
            currentTopicIndex = currentTopicIndex,
            translatedText = translatedTopic,
            onIndexChange = { newIndex ->
                currentTopicIndex = newIndex
                translatedTopic = null
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Sezione traduzione
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = {
                    if (translatedTopic == null) {
                        isTranslating = true
                        TranslationHelper.translateTextWithDetection(
                            text = topics[currentTopicIndex],
                            targetLanguageCode = targetLanguageCode,
                            onSuccess = { result ->
                                translatedTopic = result
                                isTranslating = false
                            },
                            onFailure = {
                                isTranslating = false
                            }
                        )
                    } else {
                        translatedTopic = null
                    }
                },
                enabled = !isTranslating
            ) {
                if (isTranslating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Traduci topic",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReadOnlyBioBox(
    bioText: String,
    modifier: Modifier = Modifier,
    title: String = stringResource(id = R.string.bio_title),
    placeholder: String = stringResource(id = R.string.bio_placeholder)
) {
    var translatedText by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    val targetLanguageCode = Locale.getDefault().language

    // Controlla se il modello della lingua target è già disponibile
    LaunchedEffect(targetLanguageCode) {
        isTranslationModelDownloaded(targetLanguageCode) { downloaded ->
            isDownloading = !downloaded
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            .padding(16.dp)
            .animateContentSize()
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        val displayText = bioText.ifEmpty { placeholder }
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (bioText.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (isDownloading) {
                // Mostra una barra di progresso lineare a tutta larghezza
                LinearProgressIndicator(
                    //TODO: replicare anche in TopicsBox?
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp), // Altezza della barra
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                IconButton(
                    onClick = {
                        if (translatedText == null) {
                            isTranslating = true
                            TranslationHelper.translateTextWithDetection(
                                text = bioText,
                                targetLanguageCode = targetLanguageCode,
                                onSuccess = { result ->
                                    translatedText = result
                                    isTranslating = false
                                },
                                onFailure = {
                                    isTranslating = false
                                }
                            )
                        } else {
                            translatedText = null // Collassa la traduzione
                        }
                    },
                    enabled = !isTranslating
                ) {
                    if (isTranslating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Traduci bio",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        translatedText?.let {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}