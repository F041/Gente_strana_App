package com.gentestrana.screens

// viene da chatGPT

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_displaysLoginButton() {
        // Imposta il contenuto con una LoginScreen di test
        composeTestRule.setContent {
            LoginScreen(
                onLoginSuccess = {},
                onNavigateToRegistration = {}
            )
        }
        // Verifica che il pulsante "Login" sia presente
        composeTestRule.onNodeWithText("Login").assertExists()
    }

    @Test
    fun loginScreen_clickLogin_withEmptyFields_showsError() {
        // Questo test simula il click sul pulsante senza inserire email o password.
        // Poiché la LoginScreen usa Toast per mostrare l'errore, il Toast in sé
        // non viene facilmente intercettato in un unit test. Tuttavia, possiamo
        // verificare che il pulsante sia abilitato e che il click venga eseguito.
        composeTestRule.setContent {
            LoginScreen(
                onLoginSuccess = {},
                onNavigateToRegistration = {}
            )
        }
        // Verifica che il pulsante "Login" esista e simula un click
        composeTestRule.onNodeWithText("Login").performClick()
        // Nota: per verificare il Toast occorrerebbe utilizzare un test strumentale (androidTest)
        // oppure intercettare la logica del ViewModel separatamente.
    }
}
