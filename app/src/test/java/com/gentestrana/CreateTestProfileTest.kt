package com.gentestrana

import com.gentestrana.users.User
import com.gentestrana.utils.computeAgeFromTimestamp
import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * Mostra come creare un profilo finto per test manuali.
 * L'oggetto User risultante può essere usato in qualsiasi componente UI
 * (UserProfileCard, ProfileContent, PersonalProfilePreviewCard, ecc.)
 * senza bisogno di Firebase.
 */
class CreateTestProfileTest {

    /**
     * Crea un profilo finto con tutti i campi compilati.
     * Questo mostra esattamente cosa serve a ciascun componente
     * (galleria immagini, testo, lingue, età, ecc.)
     */
    @Test
    fun create_test_profile() {
        // Crea una data di nascita (es. 15 anni fa)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -25) // 25 anni
        val birthTimestamp = calendar.timeInMillis

        val testUser = User(
            username = "TestUser",                    // Nome visibile
            bio = "Ciao! Sono un profilo di test.",   // Biografia
            topics = listOf("Musica", "Viaggi", "Sport"),  // Argomenti
            profilePicUrl = listOf(                    // Foto profilo (URL o vuota)
                "https://firebasestorage.googleapis.com/v0/b/project.appspot.com/o/users%2Ftest%2Ffoto1.webp?alt=media",
                "https://firebasestorage.googleapis.com/v0/b/project.appspot.com/o/users%2Ftest%2Ffoto2.webp?alt=media"
            ),
            rawBirthTimestamp = com.google.firebase.Timestamp(birthTimestamp / 1000, 0),
            sex = "M",                                 // "M", "F" o "Undefined"
            docId = "test_user_123",                   // ID univoco
            fcmToken = "",
            spokenLanguages = listOf("it", "en", "es"), // Codici lingue
            location = "Italia",                        // Nazione
            isAdmin = false,
            registrationDate = null,
            registrationType = null,
            lastActive = null,
            blockedUsers = emptyList()
        )

        // Verifica che l'età sia calcolata correttamente
        val eta = computeAgeFromTimestamp(testUser.birthTimestamp)
        assertEquals("Età calcolata: $eta", 25, eta)
        println("✅ Profilo di test creato: ${testUser.username}, $eta anni")
        println("   Lingue: ${testUser.spokenLanguages.joinToString(", ")}")
        println("   Topics: ${testUser.topics.joinToString(", ")}")
        println("   Foto: ${testUser.profilePicUrl.size}")
        println("   Posizione: ${testUser.location}")
        println()
        println("📌 Per usarlo manualmente:")
        println("   1. Copiare questo oggetto User nel codice di test")
        println("   2. Passarlo a: UserProfileCard(user = testUser)")
        println("   3. Oppure a: PersonalProfilePreviewCard(user = testUser)")
        println("   4. Oppure a: ProfileContent(user = testUser, ...)")
    }

    /**
     * Crea un profilo MINIMO (campi vuoti) per testare
     * come si comportano i componenti con dati mancanti.
     */
    @Test
    fun create_minimal_test_profile() {
        val emptyUser = User(
            username = "MinimalUser",
            bio = "",
            topics = emptyList(),
            profilePicUrl = emptyList(),  // Nessuna foto
            rawBirthTimestamp = null,      // Nessuna data
            sex = "Undefined",
            docId = "minimal_user",
            fcmToken = "",
            spokenLanguages = emptyList(), // Nessuna lingua
            location = "",
            isAdmin = false,
            registrationDate = null,
            registrationType = null,
            lastActive = null,
            blockedUsers = emptyList()
        )

        val eta = computeAgeFromTimestamp(emptyUser.birthTimestamp)
        println("✅ Profilo minimo creato: ${emptyUser.username}")
        println("   Età: $eta (0 = nessuna data di nascita)")
        println("   Topics: ${emptyUser.topics.size} (vuoto)")
        println("   Foto: ${emptyUser.profilePicUrl.size} (vuoto - mostra fallback)")
        println()
        println("📌 Questo profilo testa cosa succede quando un utente")
        println("   non ha completato il profilo (dati mancanti)")
    }
}