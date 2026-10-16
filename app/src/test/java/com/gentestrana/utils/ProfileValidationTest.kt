package com.gentestrana.utils

import com.gentestrana.users.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test unitari della regola "profilo obbligatorio": bio, almeno un topic e data di nascita.
 *
 * Questi test NON richiedono Android, Firebase o Robolectric: la logica sotto test
 * ([missingRequiredProfileFields], [isProfileComplete]) è pura.
 */
class ProfileValidationTest {

    // --- Valori "validi" da riutilizzare ---
    private val validBio = "Amo la montagna e la fotografia analogica."
    private val validTopics = listOf("Musica")
    private val validBirth = 946684800000L // 2000-01-01 circa, positivo

    // ===== Caso completamente valido =====

    @Test
    fun `profilo completo quando bio topic e data sono presenti`() {
        assertTrue(isProfileComplete(validBio, validTopics, validBirth))
        assertTrue(missingRequiredProfileFields(validBio, validTopics, validBirth).isEmpty())
    }

    // ===== Bio =====

    @Test
    fun `bio mancante viene segnalata`() {
        val missing = missingRequiredProfileFields("", validTopics, validBirth)
        assertEquals(listOf(RequiredProfileField.BIO), missing)
        assertFalse(isProfileComplete("", validTopics, validBirth))
    }

    @Test
    fun `bio composta solo da spazi conta come mancante`() {
        // "   " non e' una bio valida: usa isNullOrBlank, non isNullOrEmpty
        assertFalse(isProfileComplete("    ", validTopics, validBirth))
        assertEquals(
            listOf(RequiredProfileField.BIO),
            missingRequiredProfileFields("  \t ", validTopics, validBirth)
        )
    }

    @Test
    fun `bio null conta come mancante`() {
        assertFalse(isProfileComplete(null, validTopics, validBirth))
    }

    // ===== Topics =====

    @Test
    fun `lista topic vuota viene segnalata`() {
        val missing = missingRequiredProfileFields(validBio, emptyList(), validBirth)
        assertEquals(listOf(RequiredProfileField.TOPICS), missing)
        assertFalse(isProfileComplete(validBio, emptyList(), validBirth))
    }

    @Test
    fun `lista topic null conta come mancante`() {
        assertFalse(isProfileComplete(validBio, null, validBirth))
    }

    @Test
    fun `un solo topic e' sufficiente`() {
        assertTrue(isProfileComplete(validBio, listOf("Musica"), validBirth))
    }

    // ===== Data di nascita =====

    @Test
    fun `data di nascita non impostata (0L) viene segnalata`() {
        val missing = missingRequiredProfileFields(validBio, validTopics, 0L)
        assertEquals(listOf(RequiredProfileField.BIRTH_DATE), missing)
        assertFalse(isProfileComplete(validBio, validTopics, 0L))
    }

    @Test
    fun `data di nascita precedente al 1970 e' valida perche' il timestamp e' negativo ma diverso da zero`() {
        // 1960-01-01: timestamp negativo. NON deve essere scambiato per "non impostata".
        val birth1960 = -315619200000L
        assertTrue(birth1960 < 0L)
        assertTrue(isProfileComplete(validBio, validTopics, birth1960))
    }

    // ===== Tutti i campi mancanti =====

    @Test
    fun `tutti i campi mancanti restituisce i tre campi nell'ordine bio topic data`() {
        val missing = missingRequiredProfileFields(null, null, 0L)
        assertEquals(
            listOf(
                RequiredProfileField.BIO,
                RequiredProfileField.TOPICS,
                RequiredProfileField.BIRTH_DATE
            ),
            missing
        )
    }

    // ===== Bio + topic mancanti, data presente =====

    @Test
    fun `mancano bio e topic ma la data c'e'`() {
        val missing = missingRequiredProfileFields("", emptyList(), validBirth)
        assertEquals(
            listOf(RequiredProfileField.BIO, RequiredProfileField.TOPICS),
            missing
        )
    }

    // ===== Overload su User =====

    @Test
    fun `User con profilo completo e' riconosciuto completo`() {
        val user = User(
            bio = validBio,
            topics = validTopics,
            rawBirthTimestamp = validBirth
        )
        assertTrue(isProfileComplete(user))
    }

    @Test
    fun `User creato da login Google (tutto vuoto) risulta incompleto`() {
        // Riproduce il documento creato da signInWithGoogle()/createMissingUserDocument().
        val user = User(
            username = "User",
            bio = "",
            topics = emptyList(),
            rawBirthTimestamp = null
        )
        assertFalse(isProfileComplete(user))
        assertEquals(
            listOf(
                RequiredProfileField.BIO,
                RequiredProfileField.TOPICS,
                RequiredProfileField.BIRTH_DATE
            ),
            missingRequiredProfileFields(user.bio, user.topics, user.birthTimestamp)
        )
    }
}