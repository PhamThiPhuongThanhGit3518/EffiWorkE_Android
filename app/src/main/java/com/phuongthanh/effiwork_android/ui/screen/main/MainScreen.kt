package com.phuongthanh.effiwork_android.ui.screen.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.phuongthanh.effiwork_android.R
import com.phuongthanh.effiwork_android.ui.common.BottomNavigationBar
import com.phuongthanh.effiwork_android.ui.screen.notis.NotificationScreen
import com.phuongthanh.effiwork_android.ui.screen.profile.ProfileScreen
import com.phuongthanh.effiwork_android.ui.screen.projects.ProjectsScreen

sealed class BottomNavItem(
    val route: String,
    val labelResId: Int
) {
    data object Projects : BottomNavItem("projects", R.string.nav_projects)
    data object Notifications : BottomNavItem("notifications", R.string.nav_notifications)
    data object Profile : BottomNavItem("profile", R.string.nav_profile)
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navItems = listOf(
        BottomNavItem.Projects,
        BottomNavItem.Notifications,
        BottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                items = navItems,
                onItemClick = { item ->
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                navController = navController
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Projects.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Projects.route) {
                ProjectsScreen()
            }
            composable(BottomNavItem.Notifications.route) {
                NotificationScreen()
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen()
            }
        }
    }
}
