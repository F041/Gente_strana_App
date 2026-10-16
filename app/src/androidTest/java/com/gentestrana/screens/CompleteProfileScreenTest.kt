package com.gentestrana.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

/**
 * Test strumentali di CompleteProfileScreen: la schermata che obbliga a completare
 * il profilo (bio, almeno un topic, data di nascita) per chi entra con Google o
 * aveva un profilo incompleto.
 *
 * Girano su emulatore/dispositivo (androidTest).
 *
 * NOTA: i test verificano il blocco dell'invio e l'aggiunta dei topic.
 * Non eseguono un vero salvataggio su Firestore (si fermano prima, sui campi mancanti).
 */
class CompleteProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launchCompleteProfileScreen(
        initialBio: String = "",
        initialTopics: List<String> = emptyList()
    ) {
        composeTestRule.setContent {
            CompleteProfileScreen(
                initialBio = initialBio,
                initialTopics = initialTopics,
                initialBirthTimestamp = 0L,
                onProfileCompleted = {}
            )
        }
    }

    @Test
    fun laSchermataMostraTitoloEIstruzioni() {
        launchCompleteProfileScreen()
        composeTestRule.onNodeWithText("Complete your profile").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "To continue, add a bio, at least one topic and your birthdate."
        ).assertIsDisplayed()
    }

    @Test
    fun senzaCampiObbligatori_lInvioBloccaESegnalaTreCampi() {
        launchCompleteProfileScreen()

        // Provo a salvare senza compilare nulla: deve segnalare tutti e tre i campi.
        composeTestRule.onNodeWithText("save and continue").performClick()
        composeTestRule.onAllNodesWithText("this field is required").assertCountEquals(3)
    }

    @Test
    fun compilandoLaBio_restanoDueCampiMancanti() {
        launchCompleteProfileScreen()

        composeTestRule.onNodeWithText("eg. I like x,y,z in school I performed well and I read fiction")
            .performTextInput("Ciao, sono un nuovo utente")
        composeTestRule.onNodeWithText("save and continue").performClick()

        // Bio compilata: mancano ancora topic e data di nascita.
        composeTestRule.onAllNodesWithText("this field is required").assertCountEquals(2)
    }

    @Test
    fun conBioInizialeVieneMostrataNelCampo() {
        // Simula un utente che aveva gia' scritto la bio ma non topic/data.
        launchCompleteProfileScreen(initialBio = "Bio gia' presente")
        composeTestRule.onNodeWithText("Bio gia' presente").assertIsDisplayed()
    }

    @Test
    fun ilPulsanteAggiungiAggiungeUnTopic() {
        launchCompleteProfileScreen()

        composeTestRule.onNodeWithText("eg. I want to talk about tennis").performTextInput("Sport")
        composeTestRule.onNodeWithText("Add").performClick()

        composeTestRule.onNodeWithText("Sport").assertExists()
    }
}