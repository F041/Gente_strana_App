package com.gentestrana.utils

import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import android.util.Log // <-- Importa la classe Log
import com.google.firebase.auth.FirebaseAuth // <-- Importa FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Uploads the profile image to Firebase Storage and returns the download URL via onComplete.
 * @param uid The user's unique ID.
 * @param imageUri The URI of the selected image.
 * @param onComplete Callback that returns the download URL if successful, or an empty string on failure.
 */
fun uploadMainProfileImage(uid: String, imageUri: Uri, onComplete: (String) -> Unit) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid // Get current user's UID
    Log.d("ImageUpload", "Function Parameter UID (passed to uploadProfileImage): $uid") // Log the UID parameter
    Log.d("ImageUpload", "Current User UID from FirebaseAuth: $currentUserId") // Log the UID from FirebaseAuth

    if (currentUserId != uid) { // **Crucial Check: Compare UIDs**
        Log.e("ImageUpload", "UID MISMATCH! Function UID: $uid, FirebaseAuth UID: $currentUserId")
    }

    // Modifica il percorso per includere la cartella userId
    // Percorso di Storage: profile_images/{userId}/{userId}.jpg
    val storagePath = "profile_images/$uid/${uid}.jpg"
    Log.d("ImageUpload", "Storage Path being used: $storagePath") // Log del percorso di Storage

    val storageRef = Firebase.storage.reference.child(storagePath)
    storageRef.putFile(imageUri)
        .addOnSuccessListener { taskSnapshot ->
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val imageUrl = downloadUri.toString()
                Log.d("ImageUpload", "Image uploaded successfully. Download URL: $imageUrl") // Log successo e URL
                onComplete(imageUrl)
            }.addOnFailureListener { e ->
                Log.e("ImageUpload", "Failed to get download URL: ${e.message}", e) // Log fallimento URL download
                onComplete("")
            }
        }
        .addOnFailureListener { e ->
            Log.e("ImageUpload", "Image upload failed: ${e.message}", e) // Log fallimento upload immagine
            onComplete("")
        }
}

suspend fun uploadMultipleImages(
    uid: String,
    uris: List<Uri>
): List<String> {
    val storage = Firebase.storage
    val results = mutableListOf<String>()

    uris.forEach { uri ->
        val imageRef = storage.reference.child(
            "users/$uid/gallery/${System.currentTimeMillis()}.jpg"
        )

        val downloadUrl = imageRef.putFile(uri)
            .await()
            .storage
            .downloadUrl
            .await()
            .toString()

        results.add(downloadUrl)
    }

    return results
}