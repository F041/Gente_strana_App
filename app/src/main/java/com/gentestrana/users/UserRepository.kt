package com.gentestrana.users

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.gentestrana.utils.uploadProfileImage

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore

    /**
     * Registers a new user with email and password, and uploads a profile image if provided.
     */
    fun registerUserAndUploadImage(
        email: String,
        password: String,
        username: String,
        bio: String,
        selectedImageUri: Uri?,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: run {
                    onFailure("User ID is null")
                    return@addOnSuccessListener
                }
                // Initial user data
                val userData = mapOf(
                    "username" to username,
                    "bio" to bio,
                    "profilePicUrl" to "",
                    "age" to 0,
                    "sex" to ""
                )
                firestore.collection("users").document(uid)
                    .set(userData)
                    .addOnSuccessListener {
                        if (selectedImageUri != null) {
                            // Use centralized function to upload the image
                            uploadProfileImage(uid, selectedImageUri) { imageUrl ->
                                if (imageUrl.isNotEmpty()) {
                                    // Update Firestore with the profile photo URL
                                    firestore.collection("users").document(uid)
                                        .update("profilePicUrl", imageUrl)
                                        .addOnSuccessListener {
                                            // Also update the profile in FirebaseAuth
                                            val user = auth.currentUser
                                            val profileUpdates = userProfileChangeRequest {
                                                photoUri = Uri.parse(imageUrl)
                                                displayName = username
                                            }
                                            user?.updateProfile(profileUpdates)
                                                ?.addOnCompleteListener { task ->
                                                    if (task.isSuccessful) {
                                                        onSuccess()
                                                    } else {
                                                        onFailure(task.exception?.message)
                                                    }
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            onFailure(e.message)
                                        }
                                } else {
                                    onFailure("Image upload failed")
                                }
                            }
                        } else {
                            // If no image was selected, update only the displayName
                            val user = auth.currentUser
                            val profileUpdates = userProfileChangeRequest {
                                displayName = username
                            }
                            user?.updateProfile(profileUpdates)
                                ?.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        onSuccess()
                                    } else {
                                        onFailure(task.exception?.message)
                                    }
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        onFailure(e.message)
                    }
            }
            .addOnFailureListener { e ->
                onFailure(e.message)
            }
    }

    /**
     * Adds a new profile image URL to the user's profilePicUrl list.
     */
    fun addProfileImage(
        docId: String,
        newImageUrl: String,
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit
    ) {
        firestore.collection("users").document(docId).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                val currentImages = user?.profilePicUrl ?: emptyList()
                val updatedImages = currentImages + newImageUrl

                firestore.collection("users").document(docId)
                    .update("profilePicUrl", updatedImages)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e -> onFailure(e.message) }
            }
            .addOnFailureListener { e -> onFailure(e.message) }
    }

    /**
     * Authenticates the user on Firebase using a Google ID token.
     */
    fun signInWithGoogle(
        idToken: String,
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Here, you could synchronize the user document on Firestore if needed
                    onSuccess()
                } else {
                    onFailure(task.exception?.localizedMessage ?: "Authentication failed.")
                }
            }
    }

    /**
     * Fetches a user by their document ID (Firestore document ID, which should match their Firebase UID).
     */
    fun getUser(
        docId: String,
        onSuccess: (User) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("users").document(docId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Ensure the User class has a `docId` field
                    val user = document.toObject(User::class.java)?.copy(docId = document.id)
                    if (user != null) {
                        onSuccess(user)
                    } else {
                        onFailure(Exception("User data conversion failed"))
                    }
                } else {
                    onFailure(Exception("User not found"))
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}