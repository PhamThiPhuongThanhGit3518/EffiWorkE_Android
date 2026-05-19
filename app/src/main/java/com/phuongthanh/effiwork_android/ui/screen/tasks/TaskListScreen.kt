package com.phuongthanh.effiwork_android.ui.screen.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.ui.common.StatusBadge
import com.phuongthanh.effiwork_android.ui.common.TaskCard
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import com.phuongthanh.effiwork_android.viewmodel.task.*
import kotlinx.coroutines.flow.collectLatest

data class TaskListScreenState(
    val projectName: String = "",
    val projectId: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val uiState: TaskUiState = TaskUiState.Idle,
    val selectedTab: TaskTab = TaskTab.COMMON_TASKS,
    val selectedCategory: TaskCategory = TaskCategory.ALL,
    val searchQuery: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    projectName: String = "NCKH",
    projectId: String = "",
    groupId: String = "",
    viewModel: TaskViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onNavigateToCreateTask: (String, String) -> Unit = { _, _ -> },
    onNavigateToTaskDetail: (String, String) -> Unit = { _, _ -> }
) {
    var screenState by remember { mutableStateOf(TaskListScreenState(projectId = projectId, projectName = projectName, groupId = groupId)) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentProjectName by viewModel.projectName.collectAsStateWithLifecycle()
    val taskGroups by viewModel.taskGroups.collectAsStateWithLifecycle()

    LaunchedEffect(projectId, projectName, groupId) {
        viewModel.setProjectInfo(projectId, projectName)
        viewModel.setGroupId(groupId)
        viewModel.loadTaskGroupsForCreate()
        val sectionId = groupId.ifBlank { null }
        viewModel.loadTasks(sectionId, "null")
        screenState = screenState.copy(projectId = projectId, projectName = projectName, groupId = groupId)
    }

    LaunchedEffect(uiState) {
        screenState = screenState.copy(uiState = uiState)
    }

    LaunchedEffect(taskGroups) {
        val groupName = taskGroups.find { it.id == groupId }?.name ?: ""
        screenState = screenState.copy(groupName = groupName)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is TaskEffect.ShowToast -> {}
                is TaskEffect.TaskCreated -> {}
                is TaskEffect.TaskUpdated -> {}
                is TaskEffect.TaskDeleted -> {}
            }
        }
    }

    TaskListScreenContent(
        state = screenState,
        onBackClick = onBackClick,
        onNavigateToCreateTask = onNavigateToCreateTask,
        onNavigateToTaskDetail = onNavigateToTaskDetail,
        onTabSelect = { viewModel.selectTab(it) },
        onCategorySelect = { viewModel.selectCategory(it) },
        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
        onStatusChange = { taskId, newStatus -> viewModel.updateTaskStatus(taskId, newStatus) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreenContent(
    state: TaskListScreenState,
    onBackClick: () -> Unit,
    onNavigateToCreateTask: (String, String) -> Unit,
    onNavigateToTaskDetail: (String, String) -> Unit,
    onTabSelect: (TaskTab) -> Unit,
    onCategorySelect: (TaskCategory) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onStatusChange: (String, TaskStatus) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.groupName.ifBlank { "Công việc" },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Blue500.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = state.projectName,
                                color = Blue500,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
//                actions = {
//                    IconButton(onClick = { }) {
//                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
//                    }
//                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToCreateTask(state.projectId, state.groupId) },
                containerColor = Blue500,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5))
        ) {
            SearchAndFilterBar(
                searchQuery = state.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onFilterClick = {}
            )

            TabRow(
                selectedTabIndex = if (state.selectedTab == TaskTab.COMMON_TASKS) 0 else 1,
                containerColor = Color.White,
                contentColor = Blue500
            ) {
                Tab(
                    selected = state.selectedTab == TaskTab.COMMON_TASKS,
                    onClick = { onTabSelect(TaskTab.COMMON_TASKS) },
                    text = {
                        Text(
                            "Công việc chung",
                            color = if (state.selectedTab == TaskTab.COMMON_TASKS) Blue500 else Color.Gray
                        )
                    }
                )
                Tab(
                    selected = state.selectedTab == TaskTab.ASSIGNED_TO_ME,
                    onClick = { onTabSelect(TaskTab.ASSIGNED_TO_ME) },
                    text = {
                        Text(
                            "Được giao",
                            color = if (state.selectedTab == TaskTab.ASSIGNED_TO_ME) Blue500 else Color.Gray
                        )
                    }
                )
            }

            when (val uiState = state.uiState) {
                is TaskUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Blue500)
                    }
                }
                is TaskUiState.Success -> {
                    val filteredTasks = uiState.tasks.filter { task ->
                        val matchesSearch = state.searchQuery.isEmpty() ||
                            task.name.contains(state.searchQuery, ignoreCase = true) ||
                            task.description.contains(state.searchQuery, ignoreCase = true)
                        val matchesCategory = state.selectedCategory == TaskCategory.ALL ||
                            task.category == state.selectedCategory.displayName
                        matchesSearch && matchesCategory
                    }
                    TaskList(
                        tasks = filteredTasks,
                        onTaskClick = { onNavigateToTaskDetail(state.projectId, it) },
                        onStatusChange = onStatusChange
                    )
                }
                is TaskUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(uiState.message, color = Color.Red)
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun SearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text("Tìm theo tên hoặc mô tả công việc")
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = Blue500
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
//        FilledTonalButton(
//            onClick = onFilterClick,
//            colors = ButtonDefaults.filledTonalButtonColors(
//                containerColor = Blue500.copy(alpha = 0.1f),
//                contentColor = Blue500
//            )
//        ) {
//            Icon(Icons.Default.FilterList, contentDescription = null)
//            Spacer(modifier = Modifier.width(4.dp))
//            Text("Bộ lọc")
//        }
    }
}

@Composable
private fun TaskList(
    tasks: List<Task>,
    onTaskClick: (String) -> Unit,
    onStatusChange: (String, TaskStatus) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tasks, key = { it.id }) { task ->
            TaskCard(
                task = task,
                onClick = { onTaskClick(task.id) },
                onStatusChange = { newStatus -> onStatusChange(task.id, newStatus) }
            )
        }
    }
}

private val previewTasks = listOf(
    Task(
        id = "1",
        name = "Thiết kế giao diện",
        description = "Thiết kế UI/UX cho màn hình chính",
        status = TaskStatus.IN_PROGRESS,
        assignee = "Phuong Thanh",
        participants = listOf("Nguyen Van A", "Tran Van B"),
        startDate = "20/05",
        endDate = "25/05",
        category = "Thiết kế giao diện",
        subtasks = listOf(
            Subtask("s1", "Thiết kế mockup", false, "22/05"),
            Subtask("s2", "Review design", true, "24/05")
        )
    ),
    Task(
        id = "2",
        name = "Phát triển tính năng đăng nhập",
        description = "Implement OAuth login flow",
        status = TaskStatus.NOT_STARTED,
        assignee = "Minh Hoàng",
        participants = listOf("Hoàng Nam"),
        startDate = "26/05",
        endDate = "30/05",
        category = "Thực hiện code",
        subtasks = emptyList()
    )
)

@Preview(showBackground = true, name = "Task Screen")
@Composable
fun TaskListScreenPreview() {
    MaterialTheme {
        TaskListScreenContent(
            state = TaskListScreenState(
                projectName = "NCKH",
                projectId = "proj123",
                uiState = TaskUiState.Success(previewTasks),
                selectedTab = TaskTab.COMMON_TASKS,
                selectedCategory = TaskCategory.ALL,
                searchQuery = ""
            ),
            onBackClick = {},
            onNavigateToCreateTask = { _, _ -> },
            onNavigateToTaskDetail = { _, _ -> },
            onTabSelect = {},
            onCategorySelect = {},
            onSearchQueryChange = {},
            onStatusChange = { _, _ -> }
        )
    }
}