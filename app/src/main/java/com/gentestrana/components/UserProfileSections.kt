package com.gentestrana.components

import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
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
    var isTranslating by remember { mutableStateOf(false) } // Stato per spinner (traduzione)
    var isDownloading by remember { mutableStateOf(false) } // Stato per barra lineare (download)
    val targetLanguageCode = Locale.getDefault().language
    val context = LocalContext.current // Per Toast

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
        // Titolo (invariato)
        Text(
            text = stringResource(R.string.topics_title).uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Topic Navigator (invariato)
        if (topics.isNotEmpty()) {
            // Assicurati che currentTopicIndex sia valido
            val validIndex = currentTopicIndex.coerceIn(topics.indices)

            SingleTopicNavigator(
                topics = topics,
                currentTopicIndex = validIndex, // Usa indice validato
                // Mostra il topic tradotto se disponibile, altrimenti l'originale
                translatedText = translatedTopic ?: topics.getOrNull(validIndex),
                onIndexChange = { newIndex ->
                    currentTopicIndex = newIndex
                    translatedTopic = null // Resetta la traduzione quando si cambia topic
                    isTranslating = false  // Resetta stato traduzione
                    isDownloading = false  // Resetta stato download
                },
                modifier = Modifier.fillMaxWidth()
            )

            // --- SEZIONE TRADUZIONE MODIFICATA ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 24.dp), // Altezza minima per indicatori/icona
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically // Centra verticalmente
            ) {
                if (isDownloading) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .padding(vertical = 8.dp, horizontal = 8.dp)
                    ) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .height(4.dp)
                        )
                        Text(
                            text = stringResource(R.string.downloading_language_model),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                } else {
                    // Altrimenti mostra l'IconButton (se non sta scaricando)
                    IconButton(
                        onClick = {
                            val topicToTranslate = topics.getOrNull(validIndex) ?: return@IconButton

                            if (translatedTopic == null) {
                                // *** NUOVA LOGICA onClick ***
                                // Imposta isTranslating a true QUI per mostrare subito lo spinner,
                                // a meno che onDownloadStarted non lo imposti a false.
                                isTranslating = true
                                isDownloading = false // Assicurati che isDownloading sia false all'inizio

                                Log.d("ReadOnlyTopicsBox", "Translate clicked. Initial state: isTranslating=true, isDownloading=false")

                                TranslationHelper.translateTextWithDetection(
                                    text = topicToTranslate,
                                    targetLanguageCode = targetLanguageCode,
                                    onSuccess = { result ->
                                        Log.d("ReadOnlyTopicsBox", "onSuccess received.")
                                        translatedTopic = result
                                        isTranslating = false
                                        isDownloading = false
                                    },
                                    onFailure = { exception ->
                                        Log.d("ReadOnlyTopicsBox", "onFailure received.")
                                        isTranslating = false
                                        isDownloading = false
                                        Toast.makeText(context, context.getString(R.string.translation_failed), Toast.LENGTH_SHORT).show()
                                        Log.e("ReadOnlyTopicsBox", "Translation/Download failed", exception)
                                    },
                                    onDownloadStarted = {
                                        Log.d("ReadOnlyTopicsBox", "onDownloadStarted received.")
                                        isDownloading = true  // Mostra barra lineare
                                        isTranslating = false // Nascondi spinner
                                    },
                                    onDownloadCompleted = {
                                        Log.d("ReadOnlyTopicsBox", "onDownloadCompleted received.")
                                        isDownloading = false // Nascondi barra lineare
                                        isTranslating = true  // Mostra di nuovo spinner per fase traduzione
                                    }
                                )
                                // *** FINE NUOVA LOGICA onClick ***
                            } else {
                                // Rimuovi traduzione (invariato)
                                translatedTopic = null
                                isTranslating = false
                                isDownloading = false
                            }
                        },
                        enabled = !isTranslating && !isDownloading
                    ) {
                        if (isTranslating) {
                            // Mostra spinner circolare SOLO se sta traducendo (e non scaricando)
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            // Altrimenti mostra l'icona di traduzione
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = stringResource(R.string.translate),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

        } else {
            // Messaggio placeholder se non ci sono topic (invariato)
            Text(
                text = stringResource(R.string.no_topics_defined_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
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
    var isTranslating by remember { mutableStateOf(false) } // Stato per indicatore circolare (traduzione)
    var isDownloading by remember { mutableStateOf(false) } // Stato per indicatore lineare (download)
    val targetLanguageCode = Locale.getDefault().language
    val context = LocalContext.current // Per eventuali Toast

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            .padding(16.dp)
            .animateContentSize()
    ) {
        // Titolo (invariato)
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Testo Bio (invariato)
        val displayText = bioText.ifEmpty { placeholder }
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (bioText.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 24.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDownloading) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .height(4.dp)
                    )
                    Text(
                        text = stringResource(R.string.downloading_language_model),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            } else {
                // Altrimenti mostra l'IconButton (se non sta scaricando)
                IconButton(
                    onClick = {
                        if (translatedText == null) {
                            // *** NUOVA LOGICA onClick ***
                            isTranslating = true
                            isDownloading = false // Assicurati sia false all'inizio

                            Log.d("ReadOnlyBioBox", "Translate clicked. Initial state: isTranslating=true, isDownloading=false")

                            TranslationHelper.translateTextWithDetection(
                                text = bioText, // Usa bioText qui
                                targetLanguageCode = targetLanguageCode,
                                onSuccess = { result ->
                                    Log.d("ReadOnlyBioBox", "onSuccess received.")
                                    translatedText = result
                                    isTranslating = false
                                    isDownloading = false
                                },
                                onFailure = { exception ->
                                    Log.d("ReadOnlyBioBox", "onFailure received.")
                                    isTranslating = false
                                    isDownloading = false
                                    Toast.makeText(context, context.getString(R.string.translation_failed), Toast.LENGTH_SHORT).show()
                                    Log.e("ReadOnlyBioBox", "Translation/Download failed", exception)
                                },
                                onDownloadStarted = {
                                    Log.d("ReadOnlyBioBox", "onDownloadStarted received.")
                                    isDownloading = true
                                    isTranslating = false
                                },
                                onDownloadCompleted = {
                                    Log.d("ReadOnlyBioBox", "onDownloadCompleted received.")
                                    isDownloading = false
                                    isTranslating = true
                                }
                            )
                            // *** FINE NUOVA LOGICA onClick ***
                        } else {
                            // Rimuovi traduzione (invariato)
                            translatedText = null
                            isTranslating = false
                            isDownloading = false
                        }
                    },
                    enabled = !isTranslating && !isDownloading // CORRETTO: Disabilita se sta facendo qualcosa
                ) {
                    if (isTranslating) {
                        // Mostra spinner circolare SOLO se sta traducendo (e non scaricando)
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        // Altrimenti mostra l'icona di traduzione
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = stringResource(R.string.translate),
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