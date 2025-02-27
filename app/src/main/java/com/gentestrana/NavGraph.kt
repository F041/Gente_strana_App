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
fun AppNavHost(navController: NavHostController) {
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
    val startDestination = if (isLoggedIn) "main" else "auth"

    NavHost(
        navController = navController,
        startDestination = startDestination
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
                    }
                )
            }
            composable("registration") {
                RegistrationScreen(
                    onRegistrationSuccess = {
                        navController.navigate("main") { popUpTo("auth") }
                    }
                )
            }
        }

        // Main Flow with Bottom Navigation
        navigation(startDestination = "mainTabs", route = "main") {
            composable("mainTabs") {
                MainTabsScreen(navController)
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
    }
}

@Composable
fun MainTabsScreen(navController: NavHostController) {
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
//            composable("services") {
//                ServicesScreen()
//            }
        }
    }
}