package com.gentestrana.users

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.gentestrana.utils.uploadProfileImage

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore

    /**
     * Registra un nuovo utente con email e password e, se presente, carica l'immagine profilo.
     * Sta roba va AGGIORNATA
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
                // Dati iniziali per l'utente
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
                            // Usa la funzione centralizzata per caricare l'immagine
                            uploadProfileImage(uid, selectedImageUri) { imageUrl ->
                                if (imageUrl.isNotEmpty()) {
                                    // Aggiorna Firestore con l'URL della foto profilo
                                    firestore.collection("users").document(uid)
                                        .update("profilePicUrl", imageUrl)
                                        .addOnSuccessListener {
                                            // Aggiorna anche il profilo in FirebaseAuth
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
                            // Se non è stata selezionata un'immagine, aggiorna solo il displayName
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
     * Aggiorna il profilo utente in Firestore e FirebaseAuth.
     */
    fun updateUserProfile(
        uid: String,
        username: String,
        bio: String,
        description: String,
        profilePicUrl: String,
        age: Int,
        sex: String,
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit
    ) {
        val updatedData = mapOf(
            "username" to username,
            "bio" to bio,
            "description" to description,
            "profilePicUrl" to profilePicUrl,
            "age" to age,
            "sex" to sex
        )
        firestore.collection("users").document(uid)
            .set(updatedData, SetOptions.merge())
            .addOnSuccessListener {
                val user = auth.currentUser
                val profileUpdates = userProfileChangeRequest {
                    displayName = username
                    photoUri = if (profilePicUrl.isNotEmpty()) Uri.parse(profilePicUrl) else null
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
    }

    /**
     * Autentica l'utente su Firebase utilizzando un ID token ottenuto tramite Google.
     * Questa funzione incapsula la chiamata a signInWithCredential.
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
                    // Qui potresti, se necessario, sincronizzare il documento utente su Firestore
                    onSuccess()
                } else {
                    onFailure(task.exception?.localizedMessage ?: "Authentication failed.")
                }
            }
    }
}
