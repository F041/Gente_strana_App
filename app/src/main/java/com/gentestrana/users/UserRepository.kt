package com.gentestrana.users

import android.content.Context
import android.net.Uri
import android.util.Log
import com.gentestrana.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
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
import com.google.firebase.messaging.ktx.messaging

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
        topics: List<String> = emptyList(),
        selectedImageUri: Uri?,
        rawBirthTimestamp: Long = 0L,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit
    ) {
        Log.d("USER_REPO_DEBUG", "loginAndCheckEmail: Inizio login per $email")
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                Log.d("USER_REPO_DEBUG", "loginAndCheckEmail: Auth success.")
                val uid = authResult.user?.uid ?: run {
                    onFailure("User ID is null")
                    return@addOnSuccessListener
                }

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
                    "topics" to topics,
                    // != 0L perche' una data precedente al 1970 ha timestamp negativo ed e valida.
                    "rawBirthTimestamp" to (if (rawBirthTimestamp != 0L) rawBirthTimestamp else null),
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

                        // FIX: l'email di verifica parte solo DOPO il salvataggio del profilo,
                        // cosi' non arriva mai per un account che viene poi cancellato.
                        authResult.user?.sendEmailVerification()
                            ?.addOnFailureListener { e ->
                                Log.w("UserRepository", "Invio email di verifica fallito: ${e.message}")
                            }

                        if (selectedImageUri == null) {
                            updateAuthProfile(username, null) { onSuccess() }
                            return@addOnSuccessListener
                        }

                        // FIX: a questo punto account e profilo esistono gia'.
                        // Se la foto non si carica, la registrazione resta valida
                        // (l'utente potra' aggiungere la foto dal profilo) invece di
                        // lasciarlo bloccato con "email gia' in uso" al nuovo tentativo.
                        uploadMainProfileImage(context, uid, selectedImageUri) { imageUrl ->
                            if (imageUrl.isEmpty()) {
                                Log.w("UserRepository", "Upload foto fallito: registrazione completata senza foto.")
                                updateAuthProfile(username, null) { onSuccess() }
                                return@uploadMainProfileImage
                            }
                            firestore.collection("users").document(uid)
                                .update("profilePicUrl", listOf(imageUrl))
                                .addOnCompleteListener { task ->
                                    if (!task.isSuccessful) {
                                        Log.w("UserRepository", "Salvataggio URL foto fallito: ${task.exception?.message}")
                                    }
                                    updateAuthProfile(
                                        username,
                                        if (task.isSuccessful) imageUrl else null
                                    ) { onSuccess() }
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("UserRepository", "Failed to create user document in Firestore: ${e.message}")
                        // CLEANUP: Se la creazione del documento Firestore fallisce,
                        // l'utente Auth esiste già ma è orfano (nessun dato in Firestore).
                        // Eliminiamo l'utente Auth per evitare un account in "limbo".
                        Log.w("UserRepository", "Firestore document creation failed. Cleaning up Auth user $uid...")
                        auth.currentUser?.delete()?.addOnCompleteListener { cleanupTask ->
                            if (cleanupTask.isSuccessful) {
                                Log.d("UserRepository", "Auth user $uid deleted successfully after Firestore failure.")
                            } else {
                                Log.e("UserRepository", "CRITICAL: Failed to delete Auth user $uid after Firestore failure! Orphan user created.")
                            }
                        }
                        onFailure(e.message)
                    }
            } // Chiusura del .addOnSuccessListener del createUserWithEmailAndPassword
            .addOnFailureListener { e ->
                // Questo è il listener del createUserWithEmailAndPassword
                Log.e("UserRepository", "Failed to create user in FirebaseAuth: ${e.message}")
                // FIX: gli errori di Firebase arrivano in inglese. Li traduciamo usando i codici di errore.
                val errorCode = (e as? FirebaseAuthException)?.errorCode
                onFailure(
                    when (errorCode) {
                        "ERROR_EMAIL_ALREADY_IN_USE" -> context.getString(R.string.registration_error_email_in_use)
                        "ERROR_WEAK_PASSWORD" -> context.getString(R.string.registration_error_weak_password)
                        "ERROR_INVALID_EMAIL" -> context.getString(R.string.registration_error_invalid_email)
                        "ERROR_NETWORK_REQUEST_FAILED" -> context.getString(R.string.registration_error_network)
                        else -> context.getString(R.string.registration_error_generic)
                    }
                )
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
     * **FIX BUG:** If Firestore document creation fails, the Auth user is deleted to avoid orphan accounts.
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
                                        retrieveAndSaveFcmToken(uid)
                                        onSuccess()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("UserRepository", "Errore nella creazione del documento Firestore per l'utente $uid", e)
                                        // FIX BUG: Se la creazione del documento Firestore fallisce,
                                        // l'utente Auth esiste ma è orfano (nessun dato in Firestore).
                                        // Eliminiamo l'utente Auth per evitare account in "limbo".
                                        Log.w("UserRepository", "Google sign-in: Firestore document creation failed. Cleaning up Auth user $uid...")
                                        auth.currentUser?.delete()?.addOnCompleteListener { cleanupTask ->
                                            if (cleanupTask.isSuccessful) {
                                                Log.d("UserRepository", "Auth user $uid deleted successfully after Google sign-in Firestore failure.")
                                            } else {
                                                Log.e("UserRepository", "CRITICAL: Failed to delete Auth user $uid after Google sign-in Firestore failure! Orphan user created.")
                                            }
                                        }
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
                            // FIX BUG: Se il controllo del documento fallisce, l'utente Auth è orfano.
                            // Eliminiamo l'utente Auth per sicurezza.
                            Log.w("UserRepository", "Google sign-in: Document check failed. Cleaning up Auth user $uid...")
                            auth.currentUser?.delete()?.addOnCompleteListener { cleanupTask ->
                                if (cleanupTask.isSuccessful) {
                                    Log.d("UserRepository", "Auth user $uid deleted successfully after Google sign-in document check failure.")
                                } else {
                                    Log.e("UserRepository", "CRITICAL: Failed to delete Auth user $uid after Google sign-in document check failure!")
                                }
                            }
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
        Log.d("USER_REPO_DEBUG", "getUser: Tentativo recupero utente con docId: $docId")
        firestore.collection("users").document(docId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
//                    Log.d("USER_REPO_DEBUG", "getUser: Documento $docId trovato.")
                    try {
                        // Leggi direttamente isAdmin (default a false se non trovato o tipo errato)
                        val isAdminFromGet = document.getBoolean("isAdmin") ?: false
//                        Log.d("USER_REPO_DEBUG", "getUser [CHECK]: Valore letto con document.getBoolean('isAdmin'): $isAdminFromGet")

                        // Esegui toObject
                        var user = document.toObject(User::class.java)?.copy(docId = document.id)

                        if (user != null) {
//                            Log.d("USER_REPO_DEBUG", "getUser [toObject PRE-WORKAROUND]: isAdmin: ${user.isAdmin}")

                            // Se il valore letto da toObject è diverso da quello letto direttamente,
                            // sovrascrivi quello nell'oggetto User.
                            if (user.isAdmin != isAdminFromGet) {
//                                Log.w("USER_REPO_DEBUG", "getUser [WORKAROUND APPLICATO]: Sovrascrivo isAdmin da ${user.isAdmin} a $isAdminFromGet")
                                user = user.copy(isAdmin = isAdminFromGet) // Sovrascrittura!
                            }

//                            Log.d("USER_REPO_DEBUG", "getUser [FINALE]: Deserializzazione completata. isAdmin finale nell'oggetto User: ${user.isAdmin}")
                            // Log degli altri campi (puoi rimuoverli se non servono più al debug)
                            // Log.d("USER_REPO_DEBUG", "getUser [toObject]: topics: ${user.topics}")
                            // ... altri log ...

                            onSuccess(user) // Passa l'oggetto User (potenzialmente corretto dal workaround)
                        } else {
//                            Log.e("USER_REPO_DEBUG", "getUser: document.toObject(User::class.java) ha restituito NULL per $docId.")
                            onFailure(Exception("User data conversion failed (toObject returned null)"))
                        }
                    } catch (e: Exception) {
//                        Log.e("USER_REPO_DEBUG", "getUser: Eccezione durante getBoolean/get/toObject/copy per $docId", e)
                        onFailure(Exception("User data processing error: ${e.message}", e))
                    }
                } else {
//                    Log.w("USER_REPO_DEBUG", "getUser: Documento $docId NON trovato.")
                    onFailure(Exception("User not found"))
                }
            }
            .addOnFailureListener { e ->
//                Log.e("USER_REPO_DEBUG", "getUser: Fallimento chiamata Firestore get() per $docId", e)
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
//        Log.d("USER_REPO_DEBUG", "loginAndCheckEmail: Inizio login per $email")
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
//                Log.d("USER_REPO_DEBUG", "loginAndCheckEmail: Auth success.")
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    // Ricarica i dati dell'utente per avere lo stato aggiornato della verifica
                    firebaseUser.reload().addOnSuccessListener {
//                        Log.d("USER_REPO_DEBUG", "loginAndCheckEmail: User reload success. isEmailVerified: ${firebaseUser.isEmailVerified}") // <-- LOG 3
                        if (firebaseUser.isEmailVerified) {
//                            Log.d("USER_REPO_DEBUG", "loginAndCheckEmail: Email verificata. Recupero dettagli utente Firestore...") // <-- LOG 4
                            getUser( // Chiamiamo getUser qui DENTRO onVerified
                                docId = firebaseUser.uid,
                                onSuccess = { user ->
//                                    Log.d("USER_REPO_DEBUG", "loginAndCheckEmail: Dettagli utente recuperati. Utente: ${user.username}, isAdmin: ${user.isAdmin}") // <-- LOG 5
                                    if (user.isAdmin) {
//                                        Log.d("USER_REPO_DEBUG", "loginAndCheckEmail: Utente è ADMIN. Tentativo iscrizione a adminReports...") // <-- LOG 6 (FONDAMENTALE)
                                        Firebase.messaging.subscribeToTopic("adminReports")
                                            .addOnCompleteListener { subscribeTask ->
                                                if (subscribeTask.isSuccessful) {
                                                    Log.d("FCM", "Iscritto con successo alle notifiche adminReports")
                                                } else {
                                                    Log.e("FCM", "Errore nell'iscrizione al topic", subscribeTask.exception)
                                                }
                                                // ANCHE SE LA SOTTOSCRIZIONE FALLISCE, PROCEDI COMUNQUE CON onVerified
//                                                Log.d("USER_REPO_DEBUG", "loginAndCheckEmail: Chiamata onVerified dopo tentativo iscrizione.") // <-- LOG 7
                                                onVerified()
                                            }
                                    } else {
//                                        Log.d("USER_REPO_DEBUG", "loginAndCheckEmail: Utente NON è admin. Chiamata onVerified diretta.") // <-- LOG 8
                                        onVerified() // Utente NON admin, procedi diretto
                                    }
                                },
                                onFailure = { error ->
                                    Log.e("USER_REPO_DEBUG", "loginAndCheckEmail: Errore recupero dettagli utente Firestore.", error)
                                    // Distinguiamo tra "documento non trovato" (utente in limbo) e altri errori
                                    val errorMessage = error.localizedMessage ?: ""
                                    if (errorMessage.contains("User not found")) {
                                        // UTENTE IN LIMBO: autenticato su Firebase Auth ma senza documento Firestore
                                        // Tentiamo automaticamente il recupero ricreando il documento
                                        Log.w("USER_REPO_DEBUG", "loginAndCheckEmail: Rilevato utente in limbo (${firebaseUser.uid}). Tentativo recupero automatico...")
                                        createMissingUserDocument(
                                            onSuccess = {
                                                Log.d("USER_REPO_DEBUG", "loginAndCheckEmail: Recupero riuscito! Utente ${firebaseUser.uid} ora ha un documento Firestore.")
                                                onVerified() // Ora l'utente ha un documento, procediamo
                                            },
                                            onFailure = { recoveryError ->
                                                Log.e("USER_REPO_DEBUG", "loginAndCheckEmail: Recupero fallito: $recoveryError")
                                                onFailure("Account danneggiato. Contatta il supporto: $recoveryError")
                                            }
                                        )
                                    } else {
                                        // Altro errore (problemi di rete, permessi, ecc.)
                                        onFailure("Errore nel recupero dei dati utente: $errorMessage")
                                    }
                                }
                            )
                        } else {
                            Log.d("USER_REPO_DEBUG", "loginAndCheckEmail: Email NON verificata. Chiamata onNotVerified.") // <-- LOG 10
                            onNotVerified()
                        }
                    }.addOnFailureListener { e ->
                        Log.e("USER_REPO_DEBUG", "loginAndCheckEmail: Errore durante user.reload()", e) // <-- LOG 11
                        onFailure("Errore durante la verifica dello stato utente: ${e.localizedMessage}")
                    }
                } else {
                    Log.e("USER_REPO_DEBUG", "loginAndCheckEmail: firebaseUser è null dopo login successo.") // <-- LOG 12
                    onFailure("Utente non trovato dopo il login")
                }
            }
            .addOnFailureListener { e ->
                Log.e("USER_REPO_DEBUG", "loginAndCheckEmail: Auth fallito.", e) // <-- LOG 13
                onFailure(e.message)
            }
    }


    /**
     * Crea un documento Firestore per un utente già autenticato su Firebase Auth
     * ma che non ha un documento corrispondente in Firestore (utente in "limbo").
     * Questo può accadere se la creazione del documento fallisce durante la registrazione
     * a causa di errori di rete, timeout, o permessi.
     *
     * @param username Nome utente da usare (se null, usa un placeholder)
     * @param onSuccess Callback in caso di successo
     * @param onFailure Callback in caso di fallimento
     */
    fun createMissingUserDocument(
        username: String? = null,
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit
    ) {
        val firebaseUser = auth.currentUser ?: run {
            onFailure("Utente non autenticato")
            return
        }
        val uid = firebaseUser.uid
        val registrationTimestamp = Timestamp.now()

        Log.w("UserRepository", "createMissingUserDocument: Utente $uid autenticato ma senza documento Firestore. Ricreazione...")

        val userData = mapOf(
            "username" to (username ?: firebaseUser.displayName ?: "User"),
            "bio" to "",
            "profilePicUrl" to emptyList<String>(),
            "sex" to "Undefined",
            "topics" to emptyList<String>(),
            "rawBirthTimestamp" to null,
            "docId" to uid,
            "fcmToken" to "",
            "spokenLanguages" to emptyList<String>(),
            "location" to "",
            "isAdmin" to false,
            "registrationDate" to registrationTimestamp,
            "registrationType" to "recovery",
            "lastActive" to registrationTimestamp
        )

        firestore.collection("users").document(uid)
            .set(userData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Log.d("UserRepository", "createMissingUserDocument: Documento Firestore ricreato con successo per $uid")
                retrieveAndSaveFcmToken(uid)
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("UserRepository", "createMissingUserDocument: Errore nella ricreazione del documento Firestore per $uid", e)
                onFailure("Errore nel recupero dei dati: ${e.localizedMessage}")
            }
    }

    /**
     * Aggiorna i campi obbligatori del profilo (bio, topics, data di nascita) dell'utente corrente.
     *
     * Usato da:
     * - CompleteProfileScreen (completamento per login Google e utenti gia registrati)
     * - salvataggio del profilo personale
     *
     * @param rawBirthTimestamp 0L lascia la data invariata, altrimenti la imposta
     *        (anche se negativa, cioe' precedente al 1970).
     */
    suspend fun updateRequiredProfileFields(
        bio: String,
        topics: List<String>,
        rawBirthTimestamp: Long,
        onSuccess: () -> Unit,
        onFailure: (String?) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: run {
            onFailure("Utente non autenticato")
            return
        }

        try {
            val updates = mutableMapOf<String, Any>(
                "bio" to bio,
                "topics" to topics
            )
            if (rawBirthTimestamp != 0L) {
                updates["rawBirthTimestamp"] = rawBirthTimestamp
            }
            firestore.collection("users").document(currentUserId)
                .update(updates)
                .await()
            onSuccess()
            Log.d("UserRepository", "Campi obbligatori del profilo aggiornati per $currentUserId.")
        } catch (e: Exception) {
            Log.e("UserRepository", "Errore aggiornando i campi obbligatori per $currentUserId: ${e.message}")
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

            // Elimina utente Auth (la Cloud Function cleanupDeletedUser provvederà a cancellare dati Firestore, Storage e chat)
            user.delete().await()

            onSuccess()
        } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
            onFailure("Per motivi di sicurezza, effettua nuovamente l'accesso prima di eliminare l'account.")
        } catch (e: Exception) {
            onFailure("Errore durante l'eliminazione dell'account: ${e.message}")
        }
    }

    /**
     * Aggiorna displayName (e foto, se presente) in FirebaseAuth.
     * Chiama SEMPRE [onDone], anche se l'utente e' null o l'aggiornamento fallisce:
     * cosi' la UI non resta mai bloccata con la rotellina di caricamento.
     */
    private fun updateAuthProfile(username: String, photoUrl: String?, onDone: () -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            Log.w("UserRepository", "updateAuthProfile: currentUser null, salto l'aggiornamento.")
            onDone()
            return
        }
        val builder = UserProfileChangeRequest.Builder().setDisplayName(username)
        if (photoUrl != null) builder.setPhotoUri(Uri.parse(photoUrl))
        user.updateProfile(builder.build())
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("UserRepository", "Aggiornamento profilo Auth fallito: ${task.exception?.message}")
                }
                onDone()
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

    fun hasUserWrittenMemory(
        targetUserId: String,
        onResult: (Memory?) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: run {
            onResult(null)
            return
        }
        firestore.collection("memories")
            .whereEqualTo("targetUserId", targetUserId)
            .whereEqualTo("authorId", currentUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                val existing = snapshot.documents.firstOrNull()?.let { doc ->
                    val mem = doc.toObject(Memory::class.java)
                    mem?.copy(id = doc.id)
                }
                onResult(existing)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun addMemory(
        targetUserId: String,
        text: String,
        chatId: String? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: run {
            onFailure("Utente non autenticato")
            return
        }

        // Se l'utente ha già scritto una memoria per questo destinatario, aggiorna quella esistente
        hasUserWrittenMemory(targetUserId) { existing ->
            if (existing != null && existing.id.isNotBlank()) {
                updateMemory(existing.id, targetUserId, text, onSuccess, onFailure)
                return@hasUserWrittenMemory
            }

            val userDocRef = firestore.collection("users").document(currentUserId)
            userDocRef.get().addOnSuccessListener { authorDoc ->
                val authorObj = authorDoc.toObject(User::class.java)
                val authorName = authorObj?.username ?: "Utente"
                val authorPhotoUrl = authorObj?.profilePicUrl?.firstOrNull { it.isNotBlank() } ?: ""

                val memoryData = mapOf(
                    "targetUserId" to targetUserId,
                    "authorId" to currentUserId,
                    "authorName" to authorName,
                    "authorPhotoUrl" to authorPhotoUrl,
                    "text" to text,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )

                firestore.collection("memories")
                    .add(memoryData)
                    .addOnSuccessListener {
                        val sysMsg = mapOf(
                            "sender" to currentUserId,
                            "message" to "MEMORY_CARD|$authorName|$text",
                            "timestamp" to com.google.firebase.Timestamp.now(),
                            "status" to "SENT"
                        )

                        fun sendToChat(cId: String) {
                            firestore.collection("chats").document(cId).collection("messages").add(sysMsg)
                            firestore.collection("chats").document(cId).update("hasMessages", true, "timestamp", com.google.firebase.Timestamp.now())
                            onSuccess()
                        }

                        if (!chatId.isNullOrEmpty()) {
                            sendToChat(chatId)
                        } else {
                            firestore.collection("chats")
                                .whereArrayContains("participants", currentUserId)
                                .get()
                                .addOnSuccessListener { chatsSnap ->
                                    val chatDoc = chatsSnap.documents.firstOrNull { doc ->
                                        val participants = doc.get("participants") as? List<*>? ?: emptyList<Any>()
                                        participants.contains(targetUserId)
                                    }
                                    if (chatDoc != null) {
                                        sendToChat(chatDoc.id)
                                    } else {
                                        onSuccess()
                                    }
                                }
                                .addOnFailureListener { onSuccess() }
                        }
                    }
                    .addOnFailureListener { err ->
                        onFailure(err.localizedMessage ?: "Errore salvataggio memoria")
                    }
            }.addOnFailureListener { e ->
                onFailure(e.localizedMessage ?: "Errore recupero utente")
            }
        }
    }

    fun updateMemory(
        memoryId: String,
        targetUserId: String,
        newText: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: run {
            onFailure("Utente non autenticato")
            return
        }
        firestore.collection("memories").document(memoryId)
            .update("text", newText)
            .addOnSuccessListener {
                firestore.collection("users").document(currentUserId).get().addOnSuccessListener { authorDoc ->
                    val authorName = authorDoc.getString("username") ?: "Utente"
                    val sysMsg = mapOf(
                        "sender" to currentUserId,
                        "message" to "MEMORY_CARD|$authorName|$newText",
                        "timestamp" to com.google.firebase.Timestamp.now(),
                        "status" to "SENT"
                    )
                    firestore.collection("chats")
                        .whereArrayContains("participants", currentUserId)
                        .get()
                        .addOnSuccessListener { chatsSnap ->
                            val chatDoc = chatsSnap.documents.firstOrNull { doc ->
                                val participants = doc.get("participants") as? List<*>? ?: emptyList<Any>()
                                participants.contains(targetUserId)
                            }
                            if (chatDoc != null) {
                                firestore.collection("chats").document(chatDoc.id).collection("messages").add(sysMsg)
                                firestore.collection("chats").document(chatDoc.id).update("hasMessages", true, "timestamp", com.google.firebase.Timestamp.now())
                            }
                            onSuccess()
                        }
                        .addOnFailureListener { onSuccess() }
                }.addOnFailureListener { onSuccess() }
            }
            .addOnFailureListener { err ->
                onFailure(err.localizedMessage ?: "Errore aggiornamento memoria")
            }
    }

    fun getMemories(
        targetUserId: String,
        onSuccess: (List<Memory>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        firestore.collection("memories")
            .whereEqualTo("targetUserId", targetUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    val mem = doc.toObject(Memory::class.java)
                    mem?.copy(id = doc.id)
                }.sortedByDescending { it.timestamp?.seconds ?: 0L }
                onSuccess(list)
            }
            .addOnFailureListener { e ->
                Log.e("UserRepository", "Errore getMemories: ${e.message}", e)
                onFailure(e.localizedMessage ?: "Errore caricamento memorie")
            }
    }

    fun updateUserAcceptCalls(acceptCalls: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .update("acceptCalls", acceptCalls)
            .addOnFailureListener { e ->
                Log.e("UserRepository", "Error updating acceptCalls: ${e.message}", e)
            }
    }


}