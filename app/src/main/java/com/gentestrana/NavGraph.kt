package com.gentestrana

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.google.firebase.auth.FirebaseAuth
import com.gentestrana.components.GenericLoadingScreen
import com.gentestrana.screens.*
import com.gentestrana.users.User
import com.gentestrana.users.UserRepository
import com.gentestrana.utils.isProfileComplete

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
        startDestination = startDestination
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
                ProfileRequirementGate(
                    navController = navController,
                    onThemeChange = onThemeChange
                )
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

        // Route per il reset password tramite deep link (oobCode dal link email)
        composable("resetPassword/{oobCode}") { backStackEntry ->
            val oobCode = backStackEntry.arguments?.getString("oobCode") ?: ""
            ResetPasswordScreen(oobCode = oobCode, navController = navController)
        }

    }
}

@Composable
fun MainTabsScreen(navController: NavHostController,
                   onThemeChange: (AppTheme) -> Unit) {
    val tabsNavController = rememberNavController()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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

            composable("blockedUsers") {
                // Usa tabsNavController perché è accessibile dalle Impostazioni
                BlockedUsersScreen(navController = tabsNavController)
            }

            composable("changePassword") {
                ChangePasswordScreen(navController = tabsNavController)
            }

            composable("communityGuidelines") {
                CommunityGuidelinesScreen(navController = navController)
            }

            composable("termsOfService") {
                TermsOfServiceScreen(navController = navController)
            }

        }
    }
}

/**
 * Blocca l'accesso all'app finche' il profilo non ha bio, almeno un topic e data di nascita.
 *
 * Copre tutti i modi in cui un utente arriva a "main":
 * - login con email
 * - login con Google (il documento viene creato vuoto)
 * - app riaperta da utente gia' loggato
 * - account recuperati da createMissingUserDocument()
 * - utenti registrati prima dell'introduzione di questo obbligo
 *
 * Se il profilo e' completo mostra MainTabsScreen, altrimenti CompleteProfileScreen.
 */
@Composable
fun ProfileRequirementGate(
    navController: NavHostController,
    onThemeChange: (AppTheme) -> Unit
) {
    val userRepository = remember { UserRepository() }
    var user by remember { mutableStateOf<User?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }

    fun loadProfile() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            loading = false
            return
        }
        userRepository.getUser(
            docId = uid,
            onSuccess = {
                user = it
                loading = false
            },
            onFailure = {
                loadFailed = true
                loading = false
            }
        )
    }

    LaunchedEffect(Unit) {
        loadProfile()
    }

    if (loading) {
        GenericLoadingScreen(modifier = Modifier.fillMaxSize())
        return
    }

    val current = user
    // In caso di errore di caricamento non blocchiamo l'utente fuori dall'app.
    if (loadFailed || current == null || isProfileComplete(current)) {
        MainTabsScreen(navController, onThemeChange = onThemeChange)
    } else {
        CompleteProfileScreen(
            initialBio = current.bio,
            initialTopics = current.topics,
            initialBirthTimestamp = current.birthTimestamp,
            onProfileCompleted = {
                loading = true
                loadProfile()
            }
        )
    }
}
