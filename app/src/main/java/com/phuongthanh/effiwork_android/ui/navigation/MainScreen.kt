package com.phuongthanh.effiwork_android.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.phuongthanh.effiwork_android.R
import com.phuongthanh.effiwork_android.ui.common.BottomNavigationBar
import com.phuongthanh.effiwork_android.ui.screen.notis.NotificationScreen
import com.phuongthanh.effiwork_android.ui.screen.profile.ProfileScreen
import com.phuongthanh.effiwork_android.ui.screen.projects.CreateProjectScreen
import com.phuongthanh.effiwork_android.ui.screen.projects.JoinByCodeScreen
import com.phuongthanh.effiwork_android.ui.screen.projects.ProjectsScreen
import com.phuongthanh.effiwork_android.ui.screen.projects.ProjectSettingScreen
import com.phuongthanh.effiwork_android.viewmodel.login.AuthViewModel
import com.phuongthanh.effiwork_android.viewmodel.projects.ProjectsViewModel

sealed class BottomNavItem(
    val route: String,
    val labelResId: Int
) {
    data object Projects : BottomNavItem("projects", R.string.nav_projects)
    data object Notifications : BottomNavItem("notifications", R.string.nav_notifications)
    data object Profile : BottomNavItem("profile", R.string.nav_profile)
}

object NavRoutes {
    const val JOIN_BY_CODE = "join_by_code"
    const val CREATE_PROJECT = "create_project"
    const val PROJECT_SETTING = "project_setting/{projectId}"

    fun projectSetting(projectId: String) = "project_setting/$projectId"
}

@Composable
fun MainScreen(
    projectsViewModel: ProjectsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val navItems = listOf(
        BottomNavItem.Projects,
        BottomNavItem.Notifications,
        BottomNavItem.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in navItems.map { it.route }

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            // Thực hiện điều hướng về màn hình Login của bạn
             navController.navigate("login_route") { popUpTo(0) }
            // Hoặc nếu MainScreen được bọc trong một NavHost lớn hơn, bạn có thể gọi callback thoát
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Projects.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Projects.route) {
                ProjectsScreen(
                    projectsViewModel = projectsViewModel,
                    onNavigateToJoinByCode = {
                        navController.navigate(NavRoutes.JOIN_BY_CODE)
                    },
                    onNavigateToCreateProject = {
                        navController.navigate(NavRoutes.CREATE_PROJECT)
                    },
                    onNavigateToSettings = { projectId ->
                        navController.navigate(NavRoutes.projectSetting(projectId))
                    }
                )
            }
            composable(BottomNavItem.Notifications.route) {
                NotificationScreen()
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(authViewModel = authViewModel)
            }
            composable(NavRoutes.JOIN_BY_CODE) {
                JoinByCodeScreen(
                    viewModel = projectsViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.CREATE_PROJECT) {
                CreateProjectScreen(
                    viewModel = projectsViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = NavRoutes.PROJECT_SETTING,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                ProjectSettingScreen(
                    projectId = projectId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}