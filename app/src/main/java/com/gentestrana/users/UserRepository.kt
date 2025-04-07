package com.gentestrana.users

import android.content.Context
import android.net.Uri
import android.util.Log
import com.gentestrana.utils.FirestoreDeletionUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.gentestrana.utils.uploadMainProfileImage
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

/**
 * 1. Controllare lo stato di verifica email all'avvio dell'app e/o al login.
 * 2. Se l'email NON è verificata, reindirizzare l'utente a VerifyEmailScreen.
 * 3. Impedire l'accesso completo all'app (soprattutto a MainTabsScreen e funzionalità principali)
 *    finché l'email non è verificata.
 */

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()

) {

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
                // Ottieni il tipo di registrazione (per ora, valore di default)
                val registrationType = "self_assessment" // Per ora, supponiamo "autovalutazione"
                // Crea un Timestamp per la data di registrazione (data e ora corrente)
                val registrationTimestamp = Timestamp.now()

                // Initial user data
                val userData = mapOf(
                    "username" to username,
                    "bio" to bio,
                    "profilePicUrl" to emptyList<String>(),
                    "sex" to sex,
                    "topics" to emptyList<String>(),
                    "rawBirthTimestamp" to null,
                    "docId" to uid,
                    "fcmToken" to "",
                    "spokenLanguages" to emptyList<String>(),
                    "location" to "",
                    "isAdmin" to false,
                    "registrationDate" to registrationTimestamp,
                    "registrationType" to registrationType,
                    "lastActive" to registrationTimestamp
                )
                firestore.collection("users").document(uid)
                    .set(userData)
                    .addOnSuccessListener {
                        retrieveAndSaveFcmToken(uid)
                        if (selectedImageUri != null) {
                            // Use centralized function to upload the image
                              uploadMainProfileImage(context, uid, selectedImageUri
                              ) { imageUrl ->
                                  if (imageUrl.isNotEmpty()) {
                                      // Update Firestore with the profile photo URL
                                      firestore.collection("users").document(uid)
                                          .update("profilePicUrl", listOf(imageUrl))
                                          .addOnSuccessListener {
                                              // Also update the profile in FirebaseAuth
                                              val user = auth.currentUser
                                              val profileUpdates = UserProfileChangeRequest.Builder()
                                                  .setPhotoUri(Uri.parse(imageUrl)) // Usa .setPhotoUri()
                                                  .setDisplayName(username)          // Usa .setDisplayName()
                                                  .build()
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
                            // Nessuna immagine selezionata dall'utente.
                            // Il documento Firestore ha già profilePicUrl: [].
                            // Aggiorniamo solo il displayName in FirebaseAuth.
                            Log.d("UserRepository", "No image selected. Updating FirebaseAuth displayName only.")
                            val user = auth.currentUser
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(username)
                                .build()
                            user?.updateProfile(profileUpdates)
                                ?.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Log.d("UserRepository", "FirebaseAuth profile updated (displayName only).")
                                        onSuccess() // La registrazione di base è riuscita
                                    } else {
                                        // Il fallimento dell'aggiornamento del displayName in Auth
                                        // non dovrebbe bloccare il successo della registrazione.
                                        Log.w("UserRepository", "FirebaseAuth profile update (displayName only) failed: ${task.exception?.message}")
                                        onSuccess() // Consideriamo comunque la registrazione riuscita
                                    }
                                }
                        }
                    }
                    .addOnFailureListener { e -> // Questo è il listener del .set(userData)
                        Log.e("UserRepository", "Failed to create user document in Firestore: ${e.message}")
                        onFailure(e.message)
                        // Considera qui logica di cleanup per l'utente Auth se necessario
                    }
            } // Chiusura del .addOnSuccessListener del createUserWithEmailAndPassword
            .addOnFailureListener { e ->
                // Questo è il listener del createUserWithEmailAndPassword
                Log.e("UserRepository", "Failed to create user in FirebaseAuth: ${e.message}")
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
     * Authenticates the user on Firebase using a Google ID token.
     * **UPDATED:** Checks if the user document exists in Firestore and creates it if it's the first login.
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
                    // --- INIZIO NUOVA LOGICA ---
                    val firebaseUser = task.result?.user // Ottieni l'utente Firebase appena autenticato
                    if (firebaseUser != null) {
                        val uid = firebaseUser.uid
                        val userDocRef = firestore.collection("users").document(uid)

                        // Controlla se il documento utente esiste già in Firestore
                        userDocRef.get().addOnSuccessListener { documentSnapshot ->
                            if (!documentSnapshot.exists()) {
                                // 1. L'UTENTE NON ESISTE IN FIRESTORE (Primo login con Google)
                                Log.d("UserRepository", "Primo login con Google per l'utente $uid. Creo documento Firestore.")

                                // Prepara i dati utente di default (PUOI PERSONALIZZARLI)
                                val registrationTimestamp = Timestamp.now()
                                val googleUserData = mapOf(
                                    // Prendi username ed email da FirebaseAuth (se disponibili)
                                    "username" to (firebaseUser.displayName ?: "User"),
                                    "bio" to "", // Bio vuota di default
                                    "topics" to emptyList<String>(), // Topics vuoti
                                    // Usa l'URL della foto profilo da Google Auth (se c'è), altrimenti lista vuota
                                    "profilePicUrl" to (firebaseUser.photoUrl?.let { listOf(it.toString()) } ?: emptyList()),
                                    "rawBirthTimestamp" to null, // Data di nascita non impostata
                                    "sex" to "Undefined", // Sesso non definito
                                    "docId" to uid, // Importante salvare l'UID anche nel documento
                                    "fcmToken" to "", // Token FCM vuoto inizialmente
                                    "spokenLanguages" to emptyList<String>(), // Lingue vuote
                                    "location" to "", // Località vuota
                                    "isAdmin" to false, // Non admin di default
                                    "registrationDate" to registrationTimestamp, // Data registrazione
                                    "lastActive" to registrationTimestamp // Ultimo accesso = registrazione
                                    // Aggiungi altri campi di default se necessario
                                )

                                // Crea il documento utente in Firestore
                                userDocRef.set(googleUserData)
                                    .addOnSuccessListener {
                                        Log.d("UserRepository", "Documento Firestore creato con successo per l'utente $uid.")
                                        retrieveAndSaveFcmToken(uid) // Chiama la funzione helper
                                        // --- FINE NUOVA LOGICA PER FCM TOKEN ---
                                        onSuccess() // Chiama onSuccess SOLO DOPO aver creato il documento
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("UserRepository", "Errore nella creazione del documento Firestore per l'utente $uid", e)
                                        onFailure("Errore nella creazione dei dati utente: ${e.localizedMessage}")
                                    }
                            } else {
                                // 2. L'UTENTE ESISTE GIA' IN FIRESTORE (Login successivo)
                                Log.d("UserRepository", "Login Google successivo per l'utente $uid. Documento Firestore già esistente.")
                                retrieveAndSaveFcmToken(uid)
                                // Aggiorniamo solo 'lastActive'
                                userDocRef.update("lastActive", Timestamp.now())
                                    .addOnSuccessListener { Log.d("UserRepository", "Aggiornato lastActive per $uid") }
                                    .addOnFailureListener { e -> Log.w("UserRepository", "Errore aggiornamento lastActive per $uid: ${e.message}") }
                                onSuccess() // Chiama onSuccess perché l'utente esiste
                            }
                        }.addOnFailureListener { e ->
                            // Errore durante il controllo dell'esistenza del documento
                            Log.e("UserRepository", "Errore nel controllo esistenza documento per $uid", e)
                            onFailure("Errore accesso ai dati utente: ${e.localizedMessage}")
                        }
                    } else {
                        // Errore: utente Firebase null dopo login riuscito (molto raro)
                        Log.e("UserRepository", "Utente Firebase null dopo signInWithCredential riuscito.")
                        onFailure("Errore interno: Utente non trovato dopo il login.")
                    }

                } else {
                    // Errore durante l'autenticazione con le credenziali Google
                    Log.e("UserRepository", "signInWithCredential fallito", task.exception)
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
                val user = auth.currentUser
                if (user != null) {
                    // Ricarica i dati dell'utente per avere lo stato aggiornato della verifica
                    user.reload().addOnSuccessListener {
                        if (user.isEmailVerified) {
                            onVerified()
                        } else {
                            onNotVerified()
                        }
                    }.addOnFailureListener { e ->
                        onFailure(e.message)
                    }
                } else {
                    onFailure("Utente non trovato")
                }
            }
            .addOnFailureListener { e ->
                onFailure(e.message)
            }
    }

    /**
     * Elimina l'account utente corrente da Firebase Authentication.
     * Questa funzione elimina SOLO l'account di autenticazione, non i dati utente in Firestore.
     */
    suspend fun deleteUserAccount(
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit
    ) {
        val user = auth.currentUser ?: return onFailure("Utente non trovato")

        try {
            // Refresh token
            user.reload().await()
            user.getIdToken(true).await()

            // Elimina dati Firestore
            FirestoreDeletionUtils.deleteUserDataFromFirestore(user.uid)

            // Elimina utente Auth
            user.delete().await()

            onSuccess()
        } catch (e: Exception) {
            onFailure("Errore: ${e.message}")
        }
    }

    private fun retrieveAndSaveFcmToken(userId: String) {
        // Import necessario qui dentro o all'inizio del file
        // import com.google.firebase.messaging.FirebaseMessaging
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnSuccessListener { fcmToken ->
                if (fcmToken != null) {
//                    Log.d("UserRepository", "Token FCM recuperato per $userId: $fcmToken")
                    // Aggiorna il token in Firestore
                    firestore.collection("users").document(userId)
                        .update("fcmToken", fcmToken)
                        .addOnSuccessListener {
//                            Log.d("UserRepository", "Token FCM salvato con successo in Firestore per $userId.")
                        }
                        .addOnFailureListener { e ->
//                            Log.e("UserRepository", "Errore nel salvataggio del token FCM in Firestore per $userId", e)
                        }
                } else {
//                    Log.w("UserRepository", "Token FCM recuperato è null per $userId.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("UserRepository", "Errore nel recupero del token FCM per $userId", e)
            }
    }

    /**
     * Blocca un utente. Aggiunge l'ID dell'utente bloccato alla lista 'blockedUsers' dell'utente corrente.
     */
    suspend fun blockUser(
        blockedUserId: String,
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: run {
            onFailure("Utente non autenticato")
            return
        }

        try {
            firestore.collection("users").document(currentUserId)
                .update("blockedUsers", FieldValue.arrayUnion(blockedUserId)) // Usa arrayUnion per aggiungere, se non presente
                .await()
            onSuccess()
            Log.d("UserRepository", "Utente $blockedUserId bloccato con successo da $currentUserId.")
        } catch (e: Exception) {
            Log.e("UserRepository", "Errore bloccando utente $blockedUserId da $currentUserId: ${e.message}")
            onFailure(e.message)
        }
    }

    /**
     * Sblocca un utente. Rimuove l'ID dell'utente sbloccato dalla lista 'blockedUsers' dell'utente corrente.
     */
    suspend fun unblockUser(
        unblockedUserId: String,
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: run {
            onFailure("Utente non autenticato")
            return
        }

        try {
            firestore.collection("users").document(currentUserId)
                .update("blockedUsers", FieldValue.arrayRemove(unblockedUserId)) // Usa arrayRemove per rimuovere, se presente
                .await()
            onSuccess()
            Log.d("UserRepository", "Utente $unblockedUserId sbloccato con successo da $currentUserId.")
        } catch (e: Exception) {
            Log.e("UserRepository", "Errore sbloccando utente $unblockedUserId da $currentUserId: ${e.message}")
            onFailure(e.message)
        }
    }

    /**
     * Verifica se un utente è bloccato dall'utente corrente.
     */
    suspend fun isUserBlocked(
        userIdToCheck: String,
        onResult: (Boolean) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: run {
            onResult(false) // Se non c'è utente loggato, non è bloccato
            return
        }

        try {
            val userDocument = firestore.collection("users").document(currentUserId).get().await()
            val blockedList = userDocument.get("blockedUsers") as? List<String> ?: emptyList()
            onResult(blockedList.contains(userIdToCheck))
        } catch (e: Exception) {
            Log.e("UserRepository", "Errore controllando se l'utente $userIdToCheck è bloccato: ${e.message}")
            onResult(false) // In caso di errore, considera non bloccato per sicurezza
        }
    }

    /**
     * Ottieni la lista degli utenti bloccati dall'utente corrente.
     */
    suspend fun getBlockedUsers(
        onSuccess: (List<User>) -> Unit,
        onFailure: (String?) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: run {
            onFailure("Utente non autenticato")
            return
        }

        try {
            val userDocument = firestore.collection("users").document(currentUserId).get().await()
            val blockedUserIds = userDocument.get("blockedUsers") as? List<String> ?: emptyList()

            // Se la lista è vuota, restituisci subito una lista vuota di User
            if (blockedUserIds.isEmpty()) {
                onSuccess(emptyList())
                return
            }

            // Recupera i documenti utente per ogni ID bloccato
            val users = mutableListOf<User>()
            blockedUserIds.forEach { blockedUserId ->
                val userDoc = firestore.collection("users").document(blockedUserId).get().await()
                userDoc.toObject(User::class.java)?.let { user ->
                    users.add(user.copy(docId = userDoc.id)) // copia e imposta docId
                }
            }
            onSuccess(users)
            Log.d("UserRepository", "Recuperata lista di ${users.size} utenti bloccati per $currentUserId.")

        } catch (e: Exception) {
            Log.e("UserRepository", "Errore recuperando lista utenti bloccati per $currentUserId: ${e.message}")
            onFailure(e.message)
        }
    }


}