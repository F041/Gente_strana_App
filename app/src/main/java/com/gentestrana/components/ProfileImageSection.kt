package com.gentestrana.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.gentestrana.R
import com.gentestrana.ui_controller.ProfileViewModel

@Composable
fun ProfileImageSection(
    profilePicUrl: String?,
    newImageUri: Uri?,
    imagePickerLauncher: ManagedActivityResultLauncher<String, Uri?>,
    isUploading: Boolean,
    profileViewModel: ProfileViewModel,
    context: Context,
    onNewImageUriChanged: (Uri?) -> Unit,
    onIsUploadingChanged: (Boolean) -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))
    // Mostra l'immagine del profilo
    Box(
        modifier = Modifier
            .fillMaxWidth(0.4f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
        // **MODIFICA: RIMOSSO .align(Alignment.CenterHorizontally) DA QUI**
    ) {
        Image(
            painter = rememberAsyncImagePainter(newImageUri ?: profilePicUrl),
            contentDescription = "Profile Picture",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    // Bottone per scegliere una nuova immagine dalla galleria
    Button(
        onClick = { imagePickerLauncher.launch("image/*") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(text = stringResource(R.string.change_profile_picture))
    }
    if (newImageUri != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                onIsUploadingChanged(true) // Aggiorna isUploading tramite callback
                newImageUri?.let { uri ->
                    profileViewModel.uploadNewProfileImage(uri) { downloadUrl ->
                        // Se downloadUrl è vuoto, il ViewModel imposterà il fallback
                        onIsUploadingChanged(false) // Aggiorna isUploading tramite callback
                        onNewImageUriChanged(null) // Resetta newImageUri tramite callback
                        Toast.makeText(
                            context,
                            context.getString(R.string.profile_picture_updated),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            enabled = !isUploading
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(id = R.string.upload_new_profile_picture))
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}