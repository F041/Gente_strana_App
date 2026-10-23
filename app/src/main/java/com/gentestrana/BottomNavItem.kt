package com.gentestrana

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.gentestrana.users.UserRepository
import com.gentestrana.utils.calculateProfileCompletion
import com.google.firebase.auth.FirebaseAuth

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleResId: Int,
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
        BottomNavItem.Settings
    )

    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid }
    var unreadCount by remember { mutableIntStateOf(0) }
    var profileCompletionPct by remember { mutableIntStateOf(100) }

    val userRepo = remember { UserRepository() }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            userRepo.getUser(currentUserId, onSuccess = { u ->
                profileCompletionPct = calculateProfileCompletion(u).percentage
            }, onFailure = {})
        }
    }

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp

        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    if (item == BottomNavItem.Chat && unreadCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(if (unreadCount > 9) "9+" else "$unreadCount")
                                }
                            }
                        ) {
                            Icon(item.icon, contentDescription = null)
                        }
                    } else if (item == BottomNavItem.Profile && profileCompletionPct in 1..99) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text("$profileCompletionPct%")
                                }
                            }
                        ) {
                            Icon(item.icon, contentDescription = null)
                        }
                    } else {
                        Icon(item.icon, contentDescription = null)
                    }
                },
                label = {
                    Text(
                        text = stringResource(id = item.titleResId),
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        fontSize = (screenWidthDp * 0.026).sp,
                        maxLines = 1,
                        softWrap = false
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
