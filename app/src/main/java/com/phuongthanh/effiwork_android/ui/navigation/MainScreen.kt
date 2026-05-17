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
import com.phuongthanh.effiwork_android.ui.screen.tasks.TaskListScreen
import com.phuongthanh.effiwork_android.ui.screen.tasks.TaskGroupListScreen
import com.phuongthanh.effiwork_android.ui.screen.tasks.CreateTaskListScreen
import com.phuongthanh.effiwork_android.ui.screen.tasks.TaskDetailScreen
import com.phuongthanh.effiwork_android.ui.screen.meetings.MeetingListScreen
import com.phuongthanh.effiwork_android.ui.screen.meetings.CreateMeetingScreen
import com.phuongthanh.effiwork_android.viewmodel.login.AuthViewModel
import com.phuongthanh.effiwork_android.viewmodel.meeting.MeetingViewModel
import com.phuongthanh.effiwork_android.viewmodel.project.ProjectsViewModel
import com.phuongthanh.effiwork_android.viewmodel.task.TaskViewModel

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
    const val TASK_GROUP_LIST = "task_group_list/{projectId}/{projectName}"
    const val TASK_SCREEN = "task/{projectId}/{projectName}/{groupId}"
    const val TASK_DETAIL = "task_detail/{projectId}/{taskId}"
    const val CREATE_TASK = "create_task/{projectId}"
    const val MEETING_LIST = "meeting_list/{projectId}"
    const val CREATE_MEETING = "create_meeting/{projectId}"

    fun projectSetting(projectId: String) = "project_setting/$projectId"
    fun taskGroupList(projectId: String, projectName: String) = "task_group_list/$projectId/$projectName"
    fun taskScreen(projectId: String, projectName: String, groupId: String) = "task/$projectId/$projectName/$groupId"
    fun taskDetail(projectId: String, taskId: String) = "task_detail/$projectId/$taskId"
    fun createTask(projectId: String) = "create_task/$projectId"
    fun meetingList(projectId: String) = "meeting_list/$projectId"
    fun createMeeting(projectId: String) = "create_meeting/$projectId"
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
            navController.navigate("login_route") {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
            }
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
                    },
                    onNavigateToTask = { projectId, projectName ->
                        navController.navigate(NavRoutes.taskGroupList(projectId, projectName))
                    },
                    onNavigateToMeeting = { projectId ->
                        navController.navigate(NavRoutes.meetingList(projectId))
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
            composable(
                route = NavRoutes.TASK_GROUP_LIST,
                arguments = listOf(
                    navArgument("projectId") { type = NavType.StringType },
                    navArgument("projectName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                val projectName = backStackEntry.arguments?.getString("projectName") ?: "NCKH"
                TaskGroupListScreen(
                    projectId = projectId,
                    projectName = projectName,
                    onBackClick = { navController.popBackStack() },
                    onNavigateToTask = { pid, pname, gid ->
                        navController.navigate(NavRoutes.taskScreen(pid, pname, gid))
                    }
                )
            }
            composable(
                route = NavRoutes.TASK_SCREEN,
                arguments = listOf(
                    navArgument("projectId") { type = NavType.StringType },
                    navArgument("projectName") { type = NavType.StringType },
                    navArgument("groupId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                val projectName = backStackEntry.arguments?.getString("projectName") ?: "NCKH"
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                TaskListScreen(
                    projectId = projectId,
                    projectName = projectName,
                    groupId = groupId,
                    onBackClick = { navController.popBackStack() },
                    onNavigateToCreateTask = { pid ->
                        navController.navigate(NavRoutes.createTask(pid))
                    },
                    onNavigateToTaskDetail = { pid, tid ->
                        navController.navigate(NavRoutes.taskDetail(pid, tid))
                    }
                )
            }
            composable(
                route = NavRoutes.TASK_DETAIL,
                arguments = listOf(
                    navArgument("projectId") { type = NavType.StringType },
                    navArgument("taskId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
                TaskDetailScreen(
                    projectId = projectId,
                    taskId = taskId,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = NavRoutes.CREATE_TASK,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                CreateTaskListScreen(
                    projectId = projectId,
                    onBackClick = { navController.popBackStack() },
                    onCreateClick = {
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = NavRoutes.MEETING_LIST,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                MeetingListScreen(
                    projectId = projectId,
                    onBackClick = { navController.popBackStack() },
                    onCreateClick = { navController.navigate(NavRoutes.createMeeting(projectId)) }
                )
            }
            composable(
                route = NavRoutes.CREATE_MEETING,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                CreateMeetingScreen(
                    projectId = projectId,
                    onBackClick = { navController.popBackStack() },
                    onCreateClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}