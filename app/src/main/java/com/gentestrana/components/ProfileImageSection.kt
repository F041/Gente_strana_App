package com.gentestrana.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.gentestrana.R
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.material3.Text

@Composable
fun ProfileImageSection(
    profilePicUrl: List<String>,   // URL corrente
    newImageUri: Uri?,              // Nuova URI selezionata
    onImageSelected: (Uri) -> Unit, // Callback quando si seleziona un'immagine
    onUploadImage: () -> Unit,      // Callback per l'upload
    isUploading: Boolean            // Stato di caricamento
) {
    // 1. Crea il launcher per la selezione immagini
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }

    // 2. Layout della sezione
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 3. Box immagine profilo
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = newImageUri ?: profilePicUrl.firstOrNull() ?: "res/drawable/random_user.webp" // <-- Prendi il primo elemento
                ),
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Pulsante "Change Profile Picture"
        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.change_profile_picture))
        }

        // 5. Pulsante "Upload" (mostrato solo se c'è una nuova immagine)
        newImageUri?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onUploadImage,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(text = stringResource(R.string.upload_new_profile_picture))
                }
            }
        }
    }
}