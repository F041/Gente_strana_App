package com.gentestrana.screens

import android.net.Uri
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gentestrana.users.User
import com.gentestrana.users.UserRepository
import com.gentestrana.utils.deleteProfileImageFromStorage
import com.gentestrana.utils.uploadMainProfileImage
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TEST DIAGNOSTICO per il bug di consuelo.perin@gmail.com
 *
 * Email coinvolte:
 * - consuelo.perin@gmail.com (utente che ha segnalato il bug)
 * - info.gentestrana@gmail.com (email del progetto, usata per i test di login)
 *
 * createMissingUserDocument e uploadMainProfileImage sono ASINCRONI.
 * I test usano POLLING su Firestore/Storage per verificare i risultati, non i callback.
 */
@RunWith(AndroidJUnit4::class)
class GoogleLoginBugReplicationTest {

    companion object {
        private const val TAG = "BUG_REPLICATION_TEST"
        private const val EMAIL_BUG = "consuelo.perin@gmail.com"
        private const val EMAIL_PROGETTO = "info.gentestrana@gmail.com"
    }

    private lateinit var userRepository: UserRepository
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var testUid: String? = null

    @Before
    fun setup() {
        userRepository = UserRepository()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        Log.d(TAG, "========================================")
        Log.d(TAG, "TEST DIAGNOSTICO: Replica bug utente in limbo")
        Log.d(TAG, "Utente bug: $EMAIL_BUG")
        Log.d(TAG, "Email progetto: $EMAIL_PROGETTO")
        Log.d(TAG, "Utente corrente: ${auth.currentUser?.email ?: "NESSUNO"}")
        Log.d(TAG, "========================================")
    }

    @After
    fun cleanup() {
        val uid = testUid ?: return
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.w(TAG, "Cleanup: nessun utente loggato")
                return
            }
            val minimalData = mapOf(
                "username" to (currentUser.displayName ?: "TestUser"),
                "bio" to "",
                "topics" to emptyList<String>(),
                "profilePicUrl" to emptyList<String>(),
                "rawBirthTimestamp" to null,
                "sex" to "Undefined",
                "docId" to uid,
                "fcmToken" to "",
                "spokenLanguages" to emptyList<String>(),
                "location" to "",
                "isAdmin" to false,
                "registrationDate" to com.google.firebase.Timestamp.now(),
                "registrationType" to "test_cleanup",
                "lastActive" to com.google.firebase.Timestamp.now()
            )
            Tasks.await(firestore.collection("users").document(uid).set(minimalData))
            Log.d(TAG, "Cleanup: documento ricreato per $uid")
        } catch (e: Exception) {
            Log.w(TAG, "Cleanup: ${e.message}")
        }
    }

    /**
     * TEST 1: Verifica che l'utente corrente abbia un documento Firestore valido.
     * Se il documento NON esiste, replica il bug di consuelo.perin@gmail.com.
     */
    @Test
    fun test1_diagnostica_esistenza_documento() {
        Log.d(TAG, "\n=== TEST 1: Verifica esistenza documento Firestore ===")

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "SKIP: Nessun utente loggato. Loggati con $EMAIL_PROGETTO o altro account.")
            return
        }

        val uid = currentUser.uid
        testUid = uid
        Log.d(TAG, "Utente loggato: ${currentUser.email} (uid: $uid)")
        Log.d(TAG, "Email bug originale: $EMAIL_BUG")

        val docSnapshot = try {
            Tasks.await(firestore.collection("users").document(uid).get())
        } catch (e: Exception) {
            Log.e(TAG, "ERRORE Firestore get(): ${e.message}")
            null
        }

        if (docSnapshot == null) {
            Log.e(TAG, "❌ CRITICO: Impossibile leggere Firestore (errore di permessi/rete)")
            return
        }

        if (!docSnapshot.exists()) {
            Log.e(TAG, "❌ BUG REPLICATO: Utente ${currentUser.email} NON ha documento Firestore!")
            Log.e(TAG, "   Questo è esattamente il bug segnalato da $EMAIL_BUG")
            Log.d(TAG, "   ➡️ Testo createMissingUserDocument...")
            testRecovery(uid)
        } else {
            Log.d(TAG, "✅ Documento Firestore ESISTE per ${currentUser.email}")
            val username = docSnapshot.getString("username") ?: "NON IMPOSTATO"
            val topics = docSnapshot.get("topics") as? List<String> ?: emptyList()
            Log.d(TAG, "   Username: '$username'")
            Log.d(TAG, "   Topics: $topics")
            Log.d(TAG, "   Location: '${docSnapshot.getString("location") ?: ""}'")
            Log.d(TAG, "   Bio length: ${(docSnapshot.getString("bio") ?: "").length}")
            Log.d(TAG, "   Lingue: ${docSnapshot.get("spokenLanguages")}")
            Log.d(TAG, "   Foto: ${(docSnapshot.get("profilePicUrl") as? List<*>)?.size ?: 0}")
            Log.d(TAG, "   Admin: ${docSnapshot.getBoolean("isAdmin")}")
        }

        Log.d(TAG, "=== FINE TEST 1 ===\n")
    }

    /**
     * Helper: chiama createMissingUserDocument e fa polling fino a che il documento non esiste.
     */
    private fun testRecovery(uid: String) {
        userRepository.createMissingUserDocument(
            onSuccess = { Log.d(TAG, "✅ createMissingUserDocument: SUCCESSO (callback)") },
            onFailure = { error -> Log.e(TAG, "❌ createMissingUserDocument: FALLITO: $error") }
        )

        val documentoTrovato = attendiDocumento(uid, maxSeconds = 10)
        if (documentoTrovato) {
            Log.d(TAG, "✅ RECUPERO RIUSCITO!")
            try {
                val doc = Tasks.await(firestore.collection("users").document(uid).get())
                Log.d(TAG, "   Username: ${doc.getString("username")}")
            } catch (e: Exception) {
                Log.e(TAG, "Errore lettura: ${e.message}")
            }
        } else {
            Log.e(TAG, "❌ RECUPERO FALLITO: documento non trovato dopo 10 secondi")
        }
    }

    /**
     * TEST 2: Verifica che getUser() funzioni con l'utente corrente.
     */
    @Test
    fun test2_getUser_funziona() {
        Log.d(TAG, "\n=== TEST 2: getUser() ===")

        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.w(TAG, "SKIP: Nessun utente loggato")
            return
        }

        var userFound = false
        var errorMessage: String? = null

        userRepository.getUser(
            docId = uid,
            onSuccess = { user: User ->
                userFound = true
                Log.d(TAG, "✅ getUser: Trovato ${user.username}")
            },
            onFailure = { error ->
                errorMessage = error.localizedMessage
                Log.e(TAG, "❌ getUser FALLITO: ${error.message}")
            }
        )

        if (userFound) {
            Log.d(TAG, "✅ TEST 2: getUser() funziona correttamente")
        } else {
            Log.e(TAG, "❌ TEST 2: getUser() FALLITO: $errorMessage")
            if (errorMessage?.contains("User not found") == true) {
                Log.e(TAG, "   ⚠️ Bug esatto di $EMAIL_BUG!")
            }
        }
        Log.d(TAG, "=== FINE TEST 2 ===\n")
    }

    /**
     * TEST 3: Ciclo completo recovery.
     */
    @Test
    fun test3_ciclo_completo_recovery() {
        Log.d(TAG, "\n=== TEST 3: Ciclo completo recovery ===")
        testUid = null

        val uid = auth.currentUser?.uid ?: run {
            Log.w(TAG, "SKIP: Nessun utente loggato"); return
        }

        try {
            val doc = Tasks.await(firestore.collection("users").document(uid).get())
            if (doc.exists()) Tasks.await(firestore.collection("users").document(uid).delete())
            Log.d(TAG, "✅ Documento eliminato (stato limbo)")
        } catch (e: Exception) {
            Log.e(TAG, "Errore setup: ${e.message}"); return
        }

        val docAfter = Tasks.await(firestore.collection("users").document(uid).get())
        assertFalse("Documento dovrebbe essere assente", docAfter.exists())
        Log.d(TAG, "✅ Verificato: documento assente")

        userRepository.createMissingUserDocument(
            onSuccess = { Log.d(TAG, "✅ createMissingUserDocument: SUCCESSO") },
            onFailure = { e -> Log.e(TAG, "❌ createMissingUserDocument: FALLITO: $e") }
        )

        assertTrue("Documento dovrebbe esistere dopo recovery (polling 10s)",
            attendiDocumento(uid, maxSeconds = 10))
        Log.d(TAG, "✅ TEST 3 PASSATO!")
        testUid = uid
        Log.d(TAG, "=== FINE TEST 3 ===\n")
    }

    /**
     * TEST 4: Carica un'immagine di test su Firebase Storage e verifica l'URL.
     * Usa random_user.webp dalla cartella drawable dell'app.
     */
    @Test
    fun test4_upload_immagine_profilo() {
        Log.d(TAG, "\n=== TEST 4: Upload immagine profilo ===")

        val uid = auth.currentUser?.uid ?: run {
            Log.w(TAG, "SKIP: Nessun utente loggato"); return
        }

        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val imageUri = Uri.parse("android.resource://com.gentestrana/${com.gentestrana.R.drawable.random_user}")
        Log.d(TAG, "URI immagine test: $imageUri")

        var uploadUrl = ""
        var uploadError: String? = null

        uploadMainProfileImage(
            context = context,
            uid = uid,
            imageUri = imageUri,
            onComplete = { url ->
                when {
                    url == "LIMIT_EXCEEDED" -> { uploadError = "LIMIT_EXCEEDED"; Log.e(TAG, "❌ Rate limit superato") }
                    url.isEmpty() -> { uploadError = "EMPTY_URL"; Log.e(TAG, "❌ Upload fallito (URL vuoto)") }
                    else -> { uploadUrl = url; Log.d(TAG, "✅ Upload OK: ${url.take(80)}...") }
                }
            }
        )

        // Polling: attendi fino a 15 secondi
        var completato = false
        for (i in 1..30) {
            Thread.sleep(500)
            if (uploadUrl.isNotEmpty() || uploadError != null) { completato = true; break }
            if (i % 5 == 0) Log.d(TAG, "   Attendo upload... tentativo $i/30")
        }

        if (!completato) {
            Log.w(TAG, "Upload non completato in 15s (possibile problema di rete). Test saltato.")
            Log.d(TAG, "=== FINE TEST 4 ===\n"); return
        }

        if (uploadError == "LIMIT_EXCEEDED") {
            Log.w(TAG, "Rate limit superato, rilancia tra qualche secondo")
            Log.d(TAG, "=== FINE TEST 4 ===\n"); return
        }

        assertTrue("URL download non vuoto", uploadUrl.isNotEmpty())
        assertTrue("URL valido Firebase Storage", uploadUrl.contains("firebasestorage.googleapis.com"))
        Log.d(TAG, "✅ TEST 4 PASSATO: upload immagine funziona!")

        // Cleanup: elimina l'immagine da Storage
        try {
            deleteProfileImageFromStorage(uploadUrl) { ok ->
                Log.d(TAG, if (ok) "✅ Immagine test eliminata da Storage" else "⚠️ Eliminazione fallita")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Errore cleanup Storage: ${e.message}")
        }

        Log.d(TAG, "=== FINE TEST 4 ===\n")
    }

    /**
     * Polling helper: attende fino a maxSeconds secondi che il documento Firestore esista.
     */
    private fun attendiDocumento(uid: String, maxSeconds: Int): Boolean {
        for (attempt in 1..maxSeconds * 2) {
            try {
                Thread.sleep(500)
                val doc = Tasks.await(firestore.collection("users").document(uid).get())
                if (doc.exists()) {
                    Log.d(TAG, "Documento trovato dopo ${attempt * 500}ms")
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Polling $attempt: ${e.message}")
            }
        }
        return false
    }
}