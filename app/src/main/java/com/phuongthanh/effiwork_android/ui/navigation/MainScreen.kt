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
import com.phuongthanh.effiwork_android.ui.common.rememberAuthRepository
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
import com.phuongthanh.effiwork_android.ui.screen.meetings.MeetingDetailScreen
import com.phuongthanh.effiwork_android.ui.screen.chat.ChatListScreen
import com.phuongthanh.effiwork_android.ui.screen.chat.ChatScreen
import com.phuongthanh.effiwork_android.ui.screen.chat.CreateGroupChatScreen
import com.phuongthanh.effiwork_android.ui.screen.document.DocumentBrowserScreen
import com.phuongthanh.effiwork_android.ui.screen.document.DocumentPreviewScreen
import com.phuongthanh.effiwork_android.viewmodel.login.AuthViewModel
import com.phuongthanh.effiwork_android.viewmodel.meeting.MeetingViewModel
import com.phuongthanh.effiwork_android.viewmodel.project.ProjectsViewModel
import com.phuongthanh.effiwork_android.viewmodel.task.TaskViewModel
import com.phuongthanh.effiwork_android.data.local.TokenManager

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
    const val CREATE_TASK = "create_task/{projectId}/{groupId}"
    const val CREATE_SUBTASK = "create_subtask/{projectId}/{parentTaskId}/{parentTaskName}/{groupId}"
    const val EDIT_TASK = "edit_task/{projectId}/{taskId}"
    const val MEETING_LIST = "meeting_list/{projectId}"
    const val CREATE_MEETING = "create_meeting/{projectId}"
    const val MEETING_DETAIL = "meeting_detail/{projectId}/{meetingId}"
    const val EDIT_MEETING = "edit_meeting/{projectId}/{meetingId}"
    const val NEW_MESSAGE = "new_message/{projectId}"
    const val CHAT = "chat/{projectId}/{conversationId}/{conversationName}/{currentUserId}"
    const val CREATE_GROUP_CHAT = "create_group_chat/{projectId}"
    const val DOCUMENT_BROWSER = "document_browser/{projectId}"
    const val DOCUMENT_PREVIEW = "document_preview/{projectId}/{documentId}"

    fun projectSetting(projectId: String) = "project_setting/$projectId"
    fun taskGroupList(projectId: String, projectName: String) = "task_group_list/$projectId/$projectName"
    fun taskScreen(projectId: String, projectName: String, groupId: String) = "task/$projectId/$projectName/$groupId"
    fun taskDetail(projectId: String, taskId: String) = "task_detail/$projectId/$taskId"
    fun createTask(projectId: String, groupId: String = "") = "create_task/$projectId/$groupId"
    fun createSubtask(projectId: String, parentTaskId: String, parentTaskName: String, groupId: String) = "create_subtask/$projectId/$parentTaskId/$parentTaskName/$groupId"
    fun editTask(projectId: String, taskId: String) = "edit_task/$projectId/$taskId"
    fun meetingList(projectId: String) = "meeting_list/$projectId"
    fun createMeeting(projectId: String) = "create_meeting/$projectId"
    fun meetingDetail(projectId: String, meetingId: String) = "meeting_detail/$projectId/$meetingId"
    fun editMeeting(projectId: String, meetingId: String) = "edit_meeting/$projectId/$meetingId"
    fun newMessage(projectId: String) = "new_message/$projectId"
    fun chat(projectId: String, conversationId: String, conversationName: String, currentUserId: String) = "chat/$projectId/$conversationId/$conversationName/$currentUserId"
    fun createGroupChat(projectId: String) = "create_group_chat/$projectId"
    fun documentBrowser(projectId: String) = "document_browser/$projectId"
    fun documentPreview(projectId: String, documentId: String) = "document_preview/$projectId/$documentId"
}

@Composable
fun MainScreen(
    projectsViewModel: ProjectsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    meetingViewModel: MeetingViewModel = hiltViewModel()
) {
    val authRepository = rememberAuthRepository()
    val currentUserId by authViewModel.currentUserId.collectAsStateWithLifecycle()
    android.util.Log.d("MainScreenDebug", "currentUserId from authViewModel: $currentUserId")
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
        } else {
            authViewModel.syncCurrentUserId()
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
                    },
                    onNavigateToMessage = { projectId ->
                        navController.navigate(NavRoutes.newMessage(projectId))
                    },
                    onNavigateToDocument = { projectId ->
                        navController.navigate(NavRoutes.documentBrowser(projectId))
                    }
                )
            }
            composable(BottomNavItem.Notifications.route) {
                NotificationScreen(
                    onNavigateToTaskDetail = { projectId, taskId ->
                        navController.navigate(NavRoutes.taskDetail(projectId, taskId))
                    },
                    onNavigateToMeetingDetail = { projectId, meetingId ->
                        navController.navigate(NavRoutes.meetingDetail(projectId, meetingId))
                    },
                    onNavigateToProject = { projectId ->
                        projectsViewModel.requestFocusProject(projectId)
                        navController.navigate(BottomNavItem.Projects.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
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
                    authRepository = authRepository,
                    onBackClick = { navController.popBackStack() },
                    onNavigateToCreateTask = { pid, gid ->
                        navController.navigate(NavRoutes.createTask(pid, gid))
                    },
                    onNavigateToTaskDetail = { pid, tid ->
                        navController.navigate(NavRoutes.taskDetail(pid, tid))
                    },
                    onNavigateToEditTask = { pid, tid ->
                        navController.navigate(NavRoutes.editTask(pid, tid))
                    },
                    onAddSubtask = { pid, parentId, parentName, gid ->
                        navController.navigate(NavRoutes.createSubtask(pid, parentId, parentName, gid))
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
                    authRepository = authRepository,
                    onBackClick = { navController.popBackStack() },
                    onEditTask = { pid, tid ->
                        navController.navigate(NavRoutes.editTask(pid, tid))
                    },
                    onAddSubtask = { pid, parentId, parentName, groupId ->
                        navController.navigate(NavRoutes.createSubtask(pid, parentId, parentName, groupId))
                    },
                    onNavigateToSubtaskDetail = { pid, subtaskId ->
                        navController.navigate(NavRoutes.taskDetail(pid, subtaskId))
                    },
                    onAttachmentClick = { documentId ->
                        navController.navigate(NavRoutes.documentPreview(projectId, documentId))
                    }
                )
            }
            composable(
                route = NavRoutes.CREATE_TASK,
                arguments = listOf(
                    navArgument("projectId") { type = NavType.StringType },
                    navArgument("groupId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                CreateTaskListScreen(
                    projectId = projectId,
                    preselectedGroupId = groupId,
                    onBackClick = { navController.popBackStack() },
                    onCreateClick = {
                        navController.popBackStack()
                    },
                    onUpdateClick = {
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = NavRoutes.CREATE_SUBTASK,
                arguments = listOf(
                    navArgument("projectId") { type = NavType.StringType },
                    navArgument("parentTaskId") { type = NavType.StringType },
                    navArgument("parentTaskName") { type = NavType.StringType },
                    navArgument("groupId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                val parentTaskId = backStackEntry.arguments?.getString("parentTaskId") ?: ""
                val parentTaskName = backStackEntry.arguments?.getString("parentTaskName") ?: ""
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                CreateTaskListScreen(
                    projectId = projectId,
                    preselectedGroupId = groupId,
                    parentTaskId = parentTaskId,
                    parentTaskName = parentTaskName,
                    onBackClick = { navController.popBackStack() },
                    onCreateClick = {
                        navController.popBackStack()
                    },
                    onUpdateClick = {
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = NavRoutes.EDIT_TASK,
                arguments = listOf(
                    navArgument("projectId") { type = NavType.StringType },
                    navArgument("taskId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
                CreateTaskListScreen(
                    projectId = projectId,
                    taskId = taskId,
                    onBackClick = { navController.popBackStack() },
                    onCreateClick = {
                        navController.popBackStack()
                    },
                    onUpdateClick = {
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
                    currentUserId = currentUserId,
                    onBackClick = { navController.popBackStack() },
                    onCreateClick = { navController.navigate(NavRoutes.createMeeting(projectId)) },
                    onEditClick = { meeting ->
                        navController.navigate(NavRoutes.editMeeting(projectId, meeting.id))
                    },
                    onDeleteClick = { meeting ->
                        meetingViewModel.deleteMeeting(projectId, meeting.id)
                    },
                    onCardClick = { meeting ->
                        navController.navigate(NavRoutes.meetingDetail(projectId, meeting.id))
                    },
                    viewModel = meetingViewModel
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
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = NavRoutes.MEETING_DETAIL,
                arguments = listOf(
                    navArgument("projectId") { type = NavType.StringType },
                    navArgument("meetingId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                val meetingId = backStackEntry.arguments?.getString("meetingId") ?: return@composable
                MeetingDetailScreen(
                    projectId = projectId,
                    meetingId = meetingId,
                    currentUserId = currentUserId,
                    onBackClick = { navController.popBackStack() },
                    onEditClick = {
                        navController.navigate(NavRoutes.editMeeting(projectId, meetingId))
                    },
                    onDeleteClick = {
                        meetingViewModel.deleteMeeting(projectId, meetingId)
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = NavRoutes.EDIT_MEETING,
                arguments = listOf(
                    navArgument("projectId") { type = NavType.StringType },
                    navArgument("meetingId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                val meetingId = backStackEntry.arguments?.getString("meetingId") ?: return@composable
                CreateMeetingScreen(
                    projectId = projectId,
                    meetingId = meetingId,
                    isEdit = true,
                    onBackClick = { navController.popBackStack() },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = NavRoutes.NEW_MESSAGE,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                val currentUserId = authViewModel.currentUserId.value ?: ""
                ChatListScreen(
                    projectId = projectId,
                    onBackClick = { navController.popBackStack() },
                    onCreateGroupClick = { navController.navigate(NavRoutes.createGroupChat(projectId)) },
                    onNavigateToChat = { pid, cid, cname ->
                        navController.navigate(NavRoutes.chat(pid, cid, cname, currentUserId))
                    },
                    authRepository = rememberAuthRepository()
                )
            }
            composable(
                route = NavRoutes.CHAT,
                arguments = listOf(
                    navArgument("projectId") { type = NavType.StringType },
                    navArgument("conversationId") { type = NavType.StringType },
                    navArgument("conversationName") { type = NavType.StringType },
                    navArgument("currentUserId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
                val conversationName = backStackEntry.arguments?.getString("conversationName") ?: return@composable
                val currentUserId = backStackEntry.arguments?.getString("currentUserId") ?: return@composable
                ChatScreen(
                    projectId = projectId,
                    conversationId = conversationId,
                    conversationName = conversationName,
                    currentUserId = currentUserId,
                    onBackClick = { navController.popBackStack() },
                    onDocumentPreviewClick = { documentId ->
                        navController.navigate(NavRoutes.documentPreview(projectId, documentId))
                    }
                )
            }
            composable(
                route = NavRoutes.CREATE_GROUP_CHAT,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                CreateGroupChatScreen(
                    projectId = projectId,
                    onBackClick = { navController.popBackStack() },
                    onGroupCreated = { navController.popBackStack() },
                    authRepository = rememberAuthRepository()
                )
            }
            composable(
                route = NavRoutes.DOCUMENT_BROWSER,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                DocumentBrowserScreen(
                    projectId = projectId,
                    currentUserId = currentUserId,
                    onBackClick = { navController.popBackStack() },
                    onPreview = { documentId ->
                        navController.navigate(NavRoutes.documentPreview(projectId, documentId))
                    }
                )
            }
            composable(
                route = NavRoutes.DOCUMENT_PREVIEW,
                arguments = listOf(
                    navArgument("projectId") { type = NavType.StringType },
                    navArgument("documentId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                val documentId = backStackEntry.arguments?.getString("documentId") ?: return@composable
                DocumentPreviewScreen(
                    projectId = projectId,
                    documentId = documentId,
                    currentUserId = currentUserId,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}