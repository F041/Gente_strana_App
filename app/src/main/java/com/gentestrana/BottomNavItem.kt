package com.gentestrana

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleResId: Int,  // da String a Int (ID risorsa)
    val icon: ImageVector
) {
    object Community : BottomNavItem(
        "community",
        R.string.community,
        Icons.Filled.Groups
    )

    object Chat : BottomNavItem(
        "chat",
        R.string.chat,
        Icons.Filled.ChatBubble
    )

    object Profile : BottomNavItem(
        "profile",
        R.string.profile,
        Icons.Default.Person
    )

    // Services will become a part of Gente Strana, but for your own social you can delete it
//    object Services : BottomNavItem(
//        "services",
//        R.string.services,
//        Icons.Default.BusinessCenter
//    )

    object Settings : BottomNavItem(
        "settings",
        R.string.settings,
        Icons.Filled.Settings
    )
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Community,
        BottomNavItem.Chat,
        BottomNavItem.Profile,
 //  services will become a part of Gente Strana, but for your own social you can delete it

//        BottomNavItem.Services,
        BottomNavItem.Settings
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = null) },
                label = {
                    Text(
                        text = stringResource(id = item.titleResId),
                        fontSize = 15.sp
                    )
                },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

