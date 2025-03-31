//package com.gentestrana.screens
//
//// viene da chatGPT
//
//import androidx.compose.ui.test.junit4.createComposeRule
//import androidx.compose.ui.test.onNodeWithText
//import androidx.compose.ui.test.performClick
//import androidx.compose.ui.test.performTextInput
//import org.junit.Rule
//import org.junit.Test
//
//class LoginScreenTest {
//
//    @get:Rule
//    val composeTestRule = createComposeRule()
//
//    @Test
//    fun loginScreen_displaysLoginButton() {
//        // Imposta il contenuto della LoginScreen per il test
//        composeTestRule.setContent {
//            LoginScreen(
//                onLoginSuccess = {},
//                onNavigateToRegistration = {}
//            )
//        }
//        // Verifica che il pulsante "Login" sia presente
//        composeTestRule.onNodeWithText("Login").assertExists()
//    }
//
//    @Test
//    fun loginScreen_fieldsExistAndInteract() {
//        // Imposta il contenuto della LoginScreen per il test
//        composeTestRule.setContent {
//            LoginScreen(
//                onLoginSuccess = {},
//                onNavigateToRegistration = {}
//            )
//        }
//        // Asserzione per la presenza del campo Email (label "Email")
//        composeTestRule.onNodeWithText("Email").assertExists()
//        // Asserzione per la presenza del campo Password (label "Password")
//        composeTestRule.onNodeWithText("Password").assertExists()
//
//        // Simula l'inserimento di una email e di una password
//        composeTestRule.onNodeWithText("Email").performTextInput("test@example.com")
//        composeTestRule.onNodeWithText("Password").performTextInput("securePassword")
//
//        // A questo punto, se la LoginScreen visualizzasse il testo inserito (o se avessimo testTag specifici), potremmo
//        // asserire che il valore dei campi sia aggiornato.
//    }
//
//    @Test
//    fun loginScreen_clickLogin_withEmptyFields_showsError() {
//        // Questo test simula il click sul pulsante "Login" senza inserire dati
//        composeTestRule.setContent {
//            LoginScreen(
//                onLoginSuccess = {},
//                onNavigateToRegistration = {}
//            )
//        }
//        // Verifica che il pulsante "Login" esista e simula il click
//        composeTestRule.onNodeWithText("Login").performClick()
//
//        // Se la LoginScreen mostrasse un messaggio d'errore visibile in UI (oltre al Toast),
//        // potremmo asserire che quel messaggio esista. Ad esempio:
//        // composeTestRule.onNodeWithText("Inserisci email e password").assertExists()
//        //
//        // Nota: Poiché il Toast non è facilmente intercettabile in un unit test, questa asserzione potrebbe richiedere
//        // un test strumentale oppure una diversa strategia di verifica a livello di ViewModel.
//    }
//}
//
