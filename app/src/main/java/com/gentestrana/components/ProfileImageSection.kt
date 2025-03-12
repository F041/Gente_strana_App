package com.gentestrana.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.draw.alpha

@Composable
fun ProfileImageSection(
    // profilePicUrl ora è una lista di stringhe (nullable)
    profilePicUrl: List<String>?,
    newImageUri: Uri?,
    imagePickerLauncher: ManagedActivityResultLauncher<String, Uri?>,
    isUploading: Boolean,
    profileViewModel: ProfileViewModel,
    context: Context,
    onNewImageUriChanged: (Uri?) -> Unit,
    onIsUploadingChanged: (Boolean) -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))

    val isLimitReached = (profilePicUrl?.size ?: 0) >= 3

// Mostra la galleria di immagini del profilo
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp), // Spazio tra le immagini
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Elemento "Aggiungi immagine" (+)
        item {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    // Applica una leggera trasparenza se il limite è raggiunto
                    .alpha(if (!isLimitReached) 1f else 0.5f)
                    .then(
                        if (!isLimitReached) {
                            Modifier.clickable { imagePickerLauncher.launch("image/*") }
                        } else {
                            Modifier // Nessun comportamento cliccabile se il limite è raggiunto
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!isLimitReached) {
                    // Mostra il segno "+" se non si è raggiunto il limite
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Se il limite è raggiunto, mostra l'icona di divieto
                    Icon(
                        imageVector = Icons.Filled.Block,
                        contentDescription = stringResource(R.string.image_limit_reached),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Elementi della galleria (immagini profilo esistenti)
        if (profilePicUrl != null) {
            items(profilePicUrl) { url ->
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(url),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))


// Modifica per fornire feedback visivo in caso di tentativo di upload di un'immagine duplicata

    if (newImageUri != null) {
        Spacer(modifier = Modifier.height(8.dp))
        val dynamicButtonColors = if (!isUploading && !isLimitReached) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        } else {
            ButtonDefaults.buttonColors()
        }

        Button(
            onClick = {
                onIsUploadingChanged(true)
                newImageUri.let { uri ->
                    profileViewModel.uploadNewProfileImage(uri) { downloadUrl ->
                        onIsUploadingChanged(false)
                        if (downloadUrl.isEmpty()) {
                            // Feedback: immagine duplicata
                            Toast.makeText(
                                context,
                                context.getString(R.string.duplicate_image), // Aggiungere la stringa in resources
                                Toast.LENGTH_SHORT
                            ).show()
                            // Non resettiamo newImageUri per permettere all'utente di capire che l'upload non è andato a buon fine
                        } else {
                            onNewImageUriChanged(null)
                            Toast.makeText(
                                context,
                                context.getString(R.string.profile_picture_updated),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            enabled = !isUploading && !isLimitReached,
            colors = dynamicButtonColors
        ) {
            if (isUploading) {
                GenericLoadingScreen(modifier = Modifier.size(20.dp))
            } else {
                val buttonText = if (newImageUri != null) {
                    stringResource(R.string.confirm_upload_profile_picture)
                } else {
                    stringResource(R.string.upload_new_profile_picture)
                }
                Text(text = buttonText)
            }
        }
    }
}

