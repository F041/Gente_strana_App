package com.gentestrana.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.gentestrana.R
import com.gentestrana.ui_controller.ProfileViewModel

@Composable
fun ProfilePictureSection(
    profilePicUrl: String,
    newImageUri: MutableState<Uri?>,
    isUploading: MutableState<Boolean>,
    uploadNewProfileImage: (Uri, (String?) -> Unit) -> Unit // Funzione upload dal ViewModel
) {
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        newImageUri.value = uri // Usa .value per modificare il MutableState
    }

    Spacer(modifier = Modifier.height(16.dp))
    // Mostra l'immagine del profilo
    Box(
        modifier = Modifier
            .fillMaxWidth(0.4f) // <--- Usa una frazione di fillMaxWidth, es. 0.5f (50% della larghezza)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
//            .align(Alignment.CenterHorizontally)    // Centra il Box orizzontalmente (opzionale)
    ) {
        Image(
            painter = rememberAsyncImagePainter(newImageUri.value ?: profilePicUrl), // Usa .value per leggere il MutableState
            contentDescription = "Profile Picture",
            modifier = Modifier.fillMaxSize(), // Mantieni fillMaxSize per riempire il Box (ora più piccolo)
            contentScale = ContentScale.Crop
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    // Bottone per scegliere una nuova immagine dalla galleria
    Button(
        onClick = { imagePickerLauncher.launch("image/*") },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = stringResource(R.string.change_profile_picture))
    }
    if (newImageUri.value != null) { // Usa .value per leggere il MutableState
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                isUploading.value = true // Usa .value per modificare il MutableState
                newImageUri.value?.let { uri -> // Usa .value per leggere il MutableState
                    uploadNewProfileImage(uri) { downloadUrl -> // Chiama la funzione upload PASSATA come parametro
                        // Se downloadUrl è vuoto, il ViewModel imposterà il fallback
                        isUploading.value = false // Usa .value per modificare il MutableState
                        newImageUri.value = null // Usa .value per modificare il MutableState
                        Toast.makeText(
                            context, // Assuming 'context' is available in your scope
                            context.getString(R.string.profile_picture_updated), // Use getString to get resource
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isUploading.value // Usa .value per leggere il MutableState
        ) {
            if (isUploading.value) { // Usa .value per leggere il MutableState
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