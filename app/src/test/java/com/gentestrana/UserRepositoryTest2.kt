//package com.gentestrana
//
//import com.gentestrana.users.UserRepository
//import com.google.firebase.FirebaseApp
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.auth.FirebaseUser
//import org.junit.Test
//import org.mockito.Mockito.*
//import org.junit.Assert.*
//import org.junit.Before
//import org.mockito.kotlin.mock
//import org.mockito.kotlin.verify
//import org.junit.runner.RunWith // Import RunWith annotation
//import org.robolectric.RobolectricTestRunner // Import RobolectricTestRunner
//import org.robolectric.RuntimeEnvironment
//import org.robolectric.annotation.Config
//
//
//@RunWith(RobolectricTestRunner::class)
//@Config(manifest = Config.DEFAULT)
//class UserRepositoryTest2 {
//
//    @Before // ✅✅✅ ADD @Before METHOD
//    fun setup() {
//        // Initialize FirebaseApp before each test
//        FirebaseApp.initializeApp(RuntimeEnvironment.getApplication())
//    }
//    @Test
//    fun sendVerificationEmail_success() {
//        // 1. Setup (Arrange): Crea "mock" di FirebaseAuth e FirebaseUser, simula scenario di successo
//        val firebaseAuthMock = mock(FirebaseAuth::class.java) // Crea un mock di FirebaseAuth
//        val firebaseUserMock = mock(FirebaseUser::class.java) // Crea un mock di FirebaseUser
//
//        `when`(firebaseAuthMock.currentUser).thenReturn(firebaseUserMock) // Quando FirebaseAuth.getInstance().currentUser viene chiamato, restituisci firebaseUserMock
//        `when`(firebaseUserMock.isEmailVerified).thenReturn(false) // Simula che l'email NON sia verificata (importante per il test)
//
//        val userRepository = UserRepository() // Crea un'istanza di UserRepository (quella che stiamo testando)
//        // **IMPORTANTE:**  Dobbiamo usare *reflection* per "iniettare" il mock di FirebaseAuth dentro userRepository,
//        // altrimenti userRepository userebbe FirebaseAuth.getInstance() *reale* (e non il mock).
//        val field = UserRepository::class.java.getDeclaredField("auth") // Ottieni il campo "auth" (private)
//        field.isAccessible = true // Rendi il campo accessibile (anche se è private)
//        field.set(userRepository, firebaseAuthMock) // Imposta il valore del campo "auth" con il mock di firebaseAuthMock
//
//
//        var onSuccessCalled = false // Variabile per tracciare se la callback onSuccess è stata chiamata
//        var onFailureCalled = false // Variabile per tracciare se la callback onFailure è stata chiamata
//
//        // 2. Action (Act): Chiama la funzione sendVerificationEmail da testare
//        userRepository.sendVerificationEmail(
//            onSuccess = {
//                onSuccessCalled = true // Imposta onSuccessCalled a true se la callback viene chiamata
//            },
//            onFailure = { errorMessage ->
//                onFailureCalled = true // Imposta onFailureCalled a true se la callback viene chiamata (non ci aspettiamo che venga chiamata in questo test)
//            }
//        )
//
//        // 3. Assertion (Assert): Verifica che il comportamento sia quello atteso
//        verify(firebaseUserMock).sendEmailVerification() // Verifica che firebaseUserMock.sendEmailVerification() sia stato chiamato
//        assertTrue("onSuccess callback should be called", onSuccessCalled) // Verifica che onSuccessCalled sia true (callback onSuccess chiamata)
//        assertFalse("onFailure callback should NOT be called", onFailureCalled) // Verifica che onFailureCalled sia false (callback onFailure NON chiamata)
//    }
//}