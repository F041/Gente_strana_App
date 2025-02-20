package com.gentestrana.utils

import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

/**
 * Uploads the profile image to Firebase Storage and returns the download URL via onComplete.
 * @param uid The user's unique ID.
 * @param imageUri The URI of the selected image.
 * @param onComplete Callback that returns the download URL if successful, or an empty string on failure.
 */
fun uploadProfileImage(uid: String, imageUri: Uri, onComplete: (String) -> Unit) {
    val storageRef = Firebase.storage.reference.child("profile_images/$uid.jpg")
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
