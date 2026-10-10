package com.gentestrana.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.*
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test

class ChangePasswordScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launchChangePasswordScreen() {
        composeTestRule.setContent {
            ChangePasswordScreen(navController = rememberNavController())
        }
    }

    @Test
    fun emptyFields_showsFillFieldsError() {
        launchChangePasswordScreen()
        composeTestRule.onAllNodesWithText("change password")
            .filter(hasClickAction())
            .onFirst()
            .performClick()
        composeTestRule.onNodeWithText("fill in all the fields").assertIsDisplayed()
    }

    @Test
    fun passwordMismatch_showsMismatchError() {
        launchChangePasswordScreen()
        composeTestRule.onNodeWithText("current password").performTextInput("current")
        composeTestRule.onNodeWithText("new password").performTextInput("newpass")
        composeTestRule.onNodeWithText("confirm new password").performTextInput("nomatch")
        composeTestRule.onAllNodesWithText("change password")
            .filter(hasClickAction())
            .onFirst()
            .performClick()
        composeTestRule.onNodeWithText("the new passwords do not match").assertIsDisplayed()
    }

    @Test
    fun shortPassword_showsMinLengthError() {
        launchChangePasswordScreen()
        composeTestRule.onNodeWithText("current password").performTextInput("current")
        composeTestRule.onNodeWithText("new password").performTextInput("123")
        composeTestRule.onNodeWithText("confirm new password").performTextInput("123")
        composeTestRule.onAllNodesWithText("change password")
            .filter(hasClickAction())
            .onFirst()
            .performClick()
        composeTestRule.onNodeWithText("the new password must be at least 6 characters").assertIsDisplayed()
    }

}