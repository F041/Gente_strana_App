package com.gentestrana


import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.google.firebase.auth.FirebaseAuth
import com.gentestrana.screens.*

@Composable
fun AppNavHost(navController: NavHostController,
               onThemeChange: (AppTheme) -> Unit,
               isOnboardingCompleted: () -> Boolean,
               onVerifyEmailScreenNavigation: () -> Unit ) {
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
    val startDestination = if (isOnboardingCompleted()) {
        // Se onboarding COMPLETO
        if (isLoggedIn) "main" else "auth"
    // ...vai a "main" se loggato, altrimenti a "auth"
    } else {
        // Altrimenti (se onboarding NON COMPLETO)
        "onboarding"
        // ...vai a "onboarding"
    }

    NavHost(
        navController = navController,
        startDestination = "onboarding"
        // cambiare con = startDestination, se testo con = "onboarding"
        // "onboarding" l'ho usato quando ho creato il suo screen
    ) {
        // Authentication Flow
        navigation(startDestination = "login", route = "auth") {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate("main") { popUpTo("auth") }
                    },
                    onNavigateToRegistration = {
                        navController.navigate("registration")
                    },
                    navController = navController
                )
            }

            composable("registration") {
                RegistrationScreen(
                    onRegistrationSuccess = {
                    },
                    onVerifyEmailScreenNavigation = onVerifyEmailScreenNavigation)
            }
        }

        composable("verifyEmail") {
            VerifyEmailScreen(
                navController = navController 
            )
        }

        // Main Flow with Bottom Navigation
        navigation(startDestination = "mainTabs", route = "main") {
            composable("mainTabs") {
                MainTabsScreen(navController, onThemeChange = onThemeChange)
            }
            composable("userProfile/{docId}") { backStackEntry ->
                val docId = backStackEntry.arguments?.getString("docId") ?: ""
                UserProfileScreen(docId = docId, navController = navController)
            }
            composable(
                "chat/{docId}",
                enterTransition = { null },
                exitTransition = { null },
                popEnterTransition = { null },
                popExitTransition = { null }
            ) { backStackEntry ->
                val docId = backStackEntry.arguments?.getString("docId") ?: ""
                ChatScreen(docId = docId, navController = navController)
            }
        }

        composable("profile_pictures_screen") {
            val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
            val imageUrls = savedStateHandle?.get<List<String>>("imageUrls") ?: emptyList()

            ProfilePicturesScreen(
                imageUrls = imageUrls,
                navController = navController
            )
        }

        composable("onboarding") {
            OnboardingScreen(navController = navController)
        }
    }
}

@Composable
fun MainTabsScreen(navController: NavHostController,
                   onThemeChange: (AppTheme) -> Unit) {
    val tabsNavController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(tabsNavController) }
    ) { innerPadding ->
        NavHost(
            navController = tabsNavController,
            startDestination = "community",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("community") {
                UsersListScreen(navController = navController)
            }
            composable("chat") {
                ChatListScreen(navController = navController)
            }
            composable("profile") {
                PersonalProfileScreen(
                    navController = navController
                )
            }

//  services will become a part of Gente Strana, but for your own social you can delete it
            composable("services") {
                ServicesScreen()
            }
            composable("settings") {
                SettingsScreen(
                    rootNavController = navController,
                    navController = tabsNavController,
                    onThemeChange = onThemeChange)
            }

            composable("changePassword") {
                ChangePasswordScreen(navController = navController)
            }
        }
    }
}