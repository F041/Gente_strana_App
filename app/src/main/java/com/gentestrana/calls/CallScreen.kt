package com.gentestrana.calls

import android.view.SurfaceView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberAsyncImagePainter
import com.gentestrana.R
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    viewModel: CallViewModel,
    onCallFinished: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Auto chiusura dopo 2.5s dalla fine della chiamata
    LaunchedEffect(uiState.isEnded) {
        if (uiState.isEnded) {
            delay(2500)
            onCallFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Render Video Remoto (se attivo)
        val agoraManager = viewModel.getAgoraManager()
        val remoteUid = uiState.remoteUid

        if (uiState.isVideoEnabled && remoteUid != null && agoraManager != null) {
            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        agoraManager.setupRemoteVideoView(this, remoteUid)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Render Video Locale (in miniatura nell'angolo se attivo)
        if (uiState.isVideoEnabled && agoraManager != null) {
            Box(
                modifier = Modifier
                    .size(120.dp, 160.dp)
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
            ) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            agoraManager.setupLocalVideoView(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Informazioni Chiamata e Avatar (al centro se video disattivo)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!uiState.isVideoEnabled) {
                val imagePainter = if (uiState.callerPhoto.isNotEmpty()) {
                    rememberAsyncImagePainter(uiState.callerPhoto)
                } else {
                    rememberAsyncImagePainter(R.drawable.random_user)
                }

                Image(
                    painter = imagePainter,
                    contentDescription = "Foto ${uiState.callerName}",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(
                text = uiState.callerName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (uiState.isActive) uiState.formattedDuration else uiState.statusMessage,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            uiState.warningMessage?.let { warning ->
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = Color.Red.copy(alpha = 0.8f),
                    shape = CircleShape
                ) {
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Pulsanti di Controllo in Basso
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.isIncoming && !uiState.isActive && !uiState.isEnded) {
                // Rifiuta
                FloatingActionButton(
                    onClick = { viewModel.declineCall() },
                    containerColor = Color.Red,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "Rifiuta")
                }

                // Rispondi
                FloatingActionButton(
                    onClick = { viewModel.answerCall() },
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Rispondi")
                }
            } else if (!uiState.isEnded) {
                // Mute Mic
                IconButton(
                    onClick = { viewModel.toggleAudioMute() },
                    modifier = Modifier
                        .size(56.dp)
                        .background(if (uiState.isAudioMuted) Color.Red else Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (uiState.isAudioMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Microfono",
                        tint = Color.White
                    )
                }

                // Toggle Camera
                IconButton(
                    onClick = { viewModel.toggleVideo() },
                    modifier = Modifier
                        .size(56.dp)
                        .background(if (uiState.isVideoEnabled) Color(0xFF2E7D32) else Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (uiState.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = "Fotocamera",
                        tint = Color.White
                    )
                }

                // End Call
                FloatingActionButton(
                    onClick = {
                        if (uiState.isOutgoing && !uiState.isActive) {
                            viewModel.cancelCall()
                        } else {
                            viewModel.endCall()
                        }
                    },
                    containerColor = Color.Red,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "Termina chiamata")
                }
            }
        }
    }
}
