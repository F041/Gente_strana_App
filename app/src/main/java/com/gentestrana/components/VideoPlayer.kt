package com.gentestrana.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import com.gentestrana.R

@Composable
fun VideoPlayer() {
    val context = LocalContext.current
    val videoUri = remember {
        Uri.parse("android.resource://${context.packageName}/${R.raw.onboarding_video1}")
    }
    val videoView = remember {
        VideoView(context).apply {
            setVideoURI(videoUri)
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val videoWidth = screenWidthDp * 0.60f

    Box(
        modifier = Modifier
            .width(videoWidth)
    ) {
        AndroidView(
            factory = { videoView },
            modifier = Modifier
                .fillMaxWidth()
        ) { view ->
            view.setZOrderOnTop(true)
            // FORZA IN PRIMO PIANO, INCREDIBILE, LA CHIAVE DI 30 minuti di problemi
            view.start()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoView.stopPlayback()
        }
    }
}