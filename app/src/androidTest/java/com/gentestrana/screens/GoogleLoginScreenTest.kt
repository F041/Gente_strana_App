package com.gentestrana.screens

// viene da chatGPT

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test
import com.gentestrana.R
import com.gentestrana.TestActivity

// TODO: non va, problema con TestActivity
class GoogleLoginScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun googleLoginScreen_displaysSignInButton() {
        composeTestRule.setContent {
            GoogleLoginScreen(
                onLoginSuccess = {},
                onError = {}
            )
        }
        val expectedText = composeTestRule.activity.getString(R.string.sign_in_with_google_button)
        composeTestRule.onNodeWithText(expectedText).assertIsDisplayed()
    }
}