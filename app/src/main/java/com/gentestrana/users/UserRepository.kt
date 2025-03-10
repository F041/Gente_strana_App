package com.gentestrana.users

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.gentestrana.utils.uploadProfileImage


/**
 * TODO: IMPLEMENTARE VERIFICA EMAIL OBBLIGATORIA
 *
 * 1. Controllare lo stato di verifica email all'avvio dell'app e/o al login.
 * 2. Se l'email NON è verificata, reindirizzare l'utente a VerifyEmailScreen.
 * 3. Impedire l'accesso completo all'app (soprattutto a MainTabsScreen e funzionalità principali)
 *    finché l'email non è verificata.
 */

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
// Parametro iniettato con valore di default
) {
    private val firestore = Firebase.firestore

    /**
     * Registers a new user with email and password, and uploads a profile image if provided.
     */
    fun registerUserAndUploadImage(
        email: String,
        password: String,
        username: String,
        sex: String,
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

                // Invia email di verifica subito dopo la registrazione
                authResult.user?.sendEmailVerification()

                // Initial user data
                val userData = mapOf(
                    "username" to username,
                    "bio" to bio,
                    "profilePicUrl" to listOf<String>(),
                    "age" to 0,
                    "sex" to sex //
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
                                        .update("profilePicUrl", listOf(imageUrl))
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
     * Sends a verification email to the current user.
     */
    fun sendVerificationEmail(
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = auth.currentUser
        if (user != null && !user.isEmailVerified) { // Check if user is logged in and email is NOT already verified
            user.sendEmailVerification()
                .addOnSuccessListener {
                    onSuccess() // Callback for success
                }
                .addOnFailureListener { e ->
                    onFailure(e.message ?: "Failed to send verification email.") // Callback for failure with error message
                }
        } else if (user?.isEmailVerified == true) {
            onFailure("Email già verificata.") // Email already verified
        }
        else {
            onFailure("Utente non autenticato o email già verificata.")
        // User not logged in or email already verified
        // TODO: Stringabile
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
    fun reportUser(
        reportedUserId: String,
        reason: ReportReason,
        additionalComments: String = "",
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit
    ) {
        // Recupera l'ID dell'utente che segnala, da FirebaseAuth.
        val reporterUserId = auth.currentUser?.uid
        if (reporterUserId == null) {
            onFailure("Utente non autenticato")
            return
        }

        // Crea l'oggetto report
        val report = ReportUser(
            reportedUserId = reportedUserId,
            reporterUserId = reporterUserId,
            reason = reason,
            additionalComments = additionalComments
        )

        // Aggiungi il report alla collezione "reports" su Firestore
        firestore.collection("reports")
            .add(report)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e.message)
            }
    }

    // checkEmailVerificationStatus

    fun checkEmailVerificationStatus(
        onVerified: () -> Unit,
        onNotVerified: () -> Unit
    ) {
        val user = auth.currentUser
        if (user != null) {
            // Ricarica i dati dell'utente per avere lo stato aggiornato
            user.reload().addOnSuccessListener {
                if (user.isEmailVerified) {
                    onVerified()
                } else {
                    onNotVerified()
                }
            }.addOnFailureListener {
                // In caso di errore, consideriamo l'email come non verificata
                onNotVerified()
            }
        } else {
            // Se l'utente non è autenticato, invoca onNotVerified()
            onNotVerified()
        }

    }
    fun loginAndCheckEmail(
        email: String,
        password: String,
        onVerified: () -> Unit,
        onNotVerified: () -> Unit,
        onFailure: (String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                // Dopo il login, controlla lo stato di verifica dell'email
                checkEmailVerificationStatus(
                    onVerified = onVerified,
                    onNotVerified = onNotVerified
                )
            }
            .addOnFailureListener { e ->
                onFailure(e.message)
            }
    }
}