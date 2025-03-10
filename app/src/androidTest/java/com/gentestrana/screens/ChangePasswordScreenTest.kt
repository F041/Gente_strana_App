package com.gentestrana.screens

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.gentestrana.TestActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// TODO: non va come GoogleLoginScreen, stesso problema
class ChangePasswordScreenTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(TestActivity::class.java)

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    @Before
    fun setupNavController() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
        }
    }

    @Test
    fun changePasswordScreen_displaysTopBarAndEmailField() {
        composeTestRule.setContent {
            ChangePasswordScreen(navController = navController)
        }

        composeTestRule.onNodeWithText("Change Password").assertExists()
        composeTestRule.onNodeWithText("Email Address").assertExists()
    }

    @Test
    fun changePasswordScreen_enterEmailAndClickReset() {
        composeTestRule.setContent {
            ChangePasswordScreen(navController = navController)
        }

        composeTestRule.onNodeWithText("Email Address").performTextInput("test@example.com")
        composeTestRule.onNodeWithText("Send Reset Email").performClick()
    }
}
