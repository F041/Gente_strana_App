package com.gentestrana.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.gentestrana.chat.Chat
import com.gentestrana.chat.MessageStatus
import com.gentestrana.components.ChatListItem
import com.gentestrana.ui.theme.GenteStranaTheme
import com.google.firebase.Timestamp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Test strumentali del menu contestuale della conversazione.
 *
 * Verificano che la pressione lunga su una chat mostri il popup con "delete" e "pin"
 * e che le due voci invochino i callback corretti.
 * Girano su emulatore/dispositivo (androidTest).
 */
class ChatListItemMenuTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun testChat() = Chat(
        id = "chat1",
        participantId = "user2",
        participantName = "Mario",
        lastMessage = "ciao!",
        lastMessageStatus = MessageStatus.SENT,
        photoUrl = "",
        timestamp = Timestamp.now()
    )

    /**
     * Simula una pressione lunga (600ms, sopra la soglia di 500ms del sistema)
     * sul nome del partecipante, che e' dentro la riga cliccabile della chat.
     */
    private fun longPressOnChat() {
        composeTestRule.onNodeWithText("Mario").performTouchInput {
            down(center)
            moveTo(center, 600)
            up()
        }
    }

    @Test
    fun pressioneLungaMostraMenuConDeleteEPin() {
        composeTestRule.setContent {
            GenteStranaTheme(appTheme = AppTheme.SYSTEM) {
                ChatListItem(chat = testChat(), onClick = {})
            }
        }

        longPressOnChat()

        composeTestRule.onNodeWithText("delete").assertIsDisplayed()
        composeTestRule.onNodeWithText("pin").assertIsDisplayed()
    }

    @Test
    fun voceDeleteChiamaIlCallbackEDisattivaIlClick() {
        var deleted = false
        var clicked = false
        composeTestRule.setContent {
            GenteStranaTheme(appTheme = AppTheme.SYSTEM) {
                ChatListItem(
                    chat = testChat(),
                    onClick = { clicked = true },
                    onDelete = { deleted = true }
                )
            }
        }

        longPressOnChat()
        composeTestRule.onNodeWithText("delete").performClick()

        composeTestRule.runOnIdle {
            assertTrue("onDelete non chiamato", deleted)
            assertTrue("onClick non deve essere chiamato", !clicked)
        }
    }

    @Test
    fun vocePinChiamaIlCallback() {
        var pinned = false
        composeTestRule.setContent {
            GenteStranaTheme(appTheme = AppTheme.SYSTEM) {
                ChatListItem(
                    chat = testChat(),
                    onClick = {},
                    onPin = { pinned = true }
                )
            }
        }

        longPressOnChat()
        composeTestRule.onNodeWithText("pin").performClick()

        composeTestRule.runOnIdle { assertTrue("onPin non chiamato", pinned) }
    }
}