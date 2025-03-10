package com.gentestrana

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.gentestrana.users.UserRepository
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.mockito.Mockito.mock
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


// TODO: NON VA, spostare su AndroidTest come dice GPT?
@RunWith(RobolectricTestRunner::class)
class UserRepositoryTest {

    private lateinit var userRepository: UserRepository
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockUser: FirebaseUser

    @Before
    fun setup() {
        // Inizializza Mockito
        MockitoAnnotations.openMocks(this)

        // Crea dei mock per FirebaseAuth e FirebaseUser
        mockAuth = mock(FirebaseAuth::class.java)
        mockUser = mock(FirebaseUser::class.java)

        // Configura il mock: FirebaseAuth.currentUser restituisce il nostro mockUser
        `when`(mockAuth.currentUser).thenReturn(mockUser)

        // Simula il comportamento di reload() che restituisce un Task completato con successo
        `when`(mockUser.reload()).thenReturn(Tasks.forResult(null))

        // Imposta l'utente come verificato
        `when`(mockUser.isEmailVerified).thenReturn(true)

        // Poiché UserRepository istanzia FirebaseAuth internamente,
        // qui si assume di modificare UserRepository per poter iniettare le dipendenze (per test) oppure di usare una tecnica come reflection.
        // Per questo esempio, ipotizziamo che tu abbia una versione di UserRepository che accetta FirebaseAuth come parametro.
        userRepository = UserRepository(mockAuth)
    }

    @Test
    fun testCheckEmailVerificationStatus_verified() {
        var verifiedCalled = false
        var notVerifiedCalled = false

        userRepository.checkEmailVerificationStatus(
            onVerified = { verifiedCalled = true },
            onNotVerified = { notVerifiedCalled = true }
        )

        // Attesa (se necessaria, in base al Task) per la propagazione
        // In questo esempio il Task è completato subito, quindi possiamo verificare immediatamente
        assertTrue(verifiedCalled)
        assertFalse(notVerifiedCalled)
    }
}
