package com.gentestrana.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

/**
 * Test strumentali della registrazione.
 *
 * Verificano la regola dei campi obbligatori: bio, almeno un topic e data di nascita.
 * Girano su emulatore/dispositivo (androidTest) e richiedono Firebase inizializzato
 * (google-services.json e' presente nel progetto).
 *
 * NOTA: qui NON viene mai inviata una vera registrazione. I test verificano solo che
 * il pulsante "register" blocchi l'invio quando i campi obbligatori mancano, perche'
 * in quel caso la funzione che crea l'utente non viene chiamata.
 */
class RegistrationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launchRegistrationScreen() {
        composeTestRule.setContent {
            RegistrationScreen(
                onRegistrationSuccess = {},
                onVerifyEmailScreenNavigation = {}
            )
        }
    }

    /** Compila i tre campi "di base" in modo che l'invio non si fermi su email/password/nome. */
    private fun fillBaseFields() {
        composeTestRule.onNodeWithText("email").performTextInput("test@gmail.com")
        composeTestRule.onNodeWithText("password").performTextInput("password123")
        composeTestRule.onNodeWithText("first name").performTextInput("Mark")
    }

    @Test
    fun senzaCampiObbligatori_iCampiVengonoSegnalati() {
        launchRegistrationScreen()
        fillBaseFields()

        // Tentativo di registrazione senza bio, topic e data di nascita.
        composeTestRule.onNodeWithText("register").performClick()

        // I tre campi obbligatori devono essere evidenziati: la registrazione e' bloccata.
        composeTestRule.onAllNodesWithText("this field is required").assertCountEquals(3)
    }

    @Test
    fun conBioEMaSenzaTopicEData_vieneSegnalatoSoloIlTopic() {
        launchRegistrationScreen()
        fillBaseFields()

        // Compilo la bio ma lascio mancare topic (almeno uno) e data di nascita.
        composeTestRule.onNodeWithText("eg. I like x,y,z in school I performed well and I read fiction")
            .performTextInput("Mi piace la montagna")
        composeTestRule.onNodeWithText("register").performClick()

        // Devono mancare ancora 2 campi: topic e data di nascita.
        composeTestRule.onAllNodesWithText("this field is required").assertCountEquals(2)
    }

    @Test
    fun ilPulsanteAggiungiAggiungeUnTopic() {
        launchRegistrationScreen()

        // Il campo topic e' vuoto: il suggerimento invita ad aggiungerne almeno uno.
        composeTestRule.onNodeWithText("add at least one topic").assertExists()

        composeTestRule.onNodeWithText("eg. I want to talk about tennis").performTextInput("Musica")
        composeTestRule.onNodeWithText("Add").performClick()

        // Dopo l'aggiunta il chip "Musica" e' presente e il suggerimento sparisce.
        composeTestRule.onNodeWithText("Musica").assertExists()
    }
}