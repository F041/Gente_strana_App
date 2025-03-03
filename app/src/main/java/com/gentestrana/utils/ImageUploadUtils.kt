package com.gentestrana.utils

import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await

/**
 * Uploads the profile image to Firebase Storage and returns the download URL via onComplete.
 * @param uid The user's unique ID.
 * @param imageUri The URI of the selected image.
 * @param onComplete Callback that returns the download URL if successful, or an empty string on failure.
 */

fun uploadProfileImage(uid: String, imageUri: Uri, onComplete: (String) -> Unit) {
    // Modifica il percorso per includere la cartella userId
    val storageRef = Firebase.storage.reference.child("profile_images/${uid}/${uid}.jpg")
    storageRef.putFile(imageUri)
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                onComplete(downloadUri.toString())
            }.addOnFailureListener {
                onComplete("")
            }
        }
        .addOnFailureListener {
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