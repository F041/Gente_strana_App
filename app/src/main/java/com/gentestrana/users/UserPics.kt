package com.gentestrana.users

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@Composable
fun UserPicsGallery(
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
    imageSize: Int = 160
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(imageUrls) { url ->
            Image(
                painter = rememberAsyncImagePainter(
                    model = url.ifEmpty {
                        "https://icons.veryicon.com/png/o/system/ali-mom-icon-library/random-user.png" // Sostituisci con URL reale
                    }
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(imageSize.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
        }
    }
}