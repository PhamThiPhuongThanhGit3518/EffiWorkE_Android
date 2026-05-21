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
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phuongthanh.effiwork_android.data.repository.AuthRepository
import com.phuongthanh.effiwork_android.ui.common.StatusBadge
import com.phuongthanh.effiwork_android.ui.common.TaskCard
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import com.phuongthanh.effiwork_android.viewmodel.task.*
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date

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
    authRepository: AuthRepository? = null,
    onBackClick: () -> Unit = {},
    onNavigateToCreateTask: (String, String) -> Unit = { _, _ -> },
    onNavigateToTaskDetail: (String, String) -> Unit = { _, _ -> },
    onEditTask: (String) -> Unit = {},
    onDeleteTask: (String) -> Unit = {}
) {
    val currentUserId = authRepository?.getCurrentUserId() ?: ""
    var screenState by remember { mutableStateOf(TaskListScreenState(projectId = projectId, projectName = projectName, groupId = groupId)) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val currentProjectName by viewModel.projectName.collectAsStateWithLifecycle()
    val taskGroups by viewModel.taskGroups.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    LaunchedEffect(projectId, projectName, groupId) {
        viewModel.setProjectInfo(projectId, projectName)
        viewModel.setGroupId(groupId)
        viewModel.loadTaskGroupsForCreate()
        val sectionId = groupId.ifBlank { null }
        viewModel.loadTasks(sectionId, null)
        screenState = screenState.copy(projectId = projectId, projectName = projectName, groupId = groupId)
    }

    LaunchedEffect(selectedTab, uiState) {
        screenState = screenState.copy(uiState = uiState)
        android.util.Log.d("TaskListDebug", "selectedTab: $selectedTab, uiState: $uiState")
    }

    LaunchedEffect(currentUserId) {
        android.util.Log.d("TaskListDebug", "currentUserId in TaskListScreen: $currentUserId")
    }

    LaunchedEffect(taskGroups) {
        val groupName = taskGroups.find { it.id == groupId }?.name ?: ""
        screenState = screenState.copy(groupName = groupName)
    }

    LaunchedEffect(searchQuery) {
        screenState = screenState.copy(searchQuery = searchQuery)
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
        onSearchQueryChange = { newQuery ->
            screenState = screenState.copy(searchQuery = newQuery)
            viewModel.updateSearchQuery(newQuery)
        },
        onStatusChange = { taskId, newStatus -> viewModel.updateTaskStatus(taskId, newStatus) },
        onEditTask = onEditTask,
        onDeleteTask = onDeleteTask,
        onRequestExtension = { taskId, newDueDate, reason -> viewModel.createExtensionRequest(taskId, newDueDate, reason) },
        onApproveExtension = { taskId, requestId -> viewModel.approveExtensionRequest(taskId, requestId) },
        onRejectExtension = { taskId, requestId -> viewModel.rejectExtensionRequest(taskId, requestId) },
        currentUserId = currentUserId
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
    onStatusChange: (String, TaskStatus) -> Unit,
    onEditTask: (String) -> Unit = {},
    onDeleteTask: (String) -> Unit = {},
    onRequestExtension: (String, String, String) -> Unit = { _, _, _ -> },
    onApproveExtension: (String, String) -> Unit = { _, _ -> },
    onRejectExtension: (String, String) -> Unit = { _, _ -> },
    currentUserId: String = ""
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
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
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
                        onStatusChange = onStatusChange,
                        onEditTask = onEditTask,
                        onDeleteTask = onDeleteTask,
                        onRequestExtension = onRequestExtension,
                        onApproveExtension = onApproveExtension,
                        onRejectExtension = onRejectExtension,
                        currentUserId = currentUserId
                    )
                    android.util.Log.d("TaskListDebug", "filteredTasks count: ${filteredTasks.size}, currentUserId: $currentUserId")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskList(
    tasks: List<Task>,
    onTaskClick: (String) -> Unit,
    onStatusChange: (String, TaskStatus) -> Unit,
    onEditTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onRequestExtension: (String, String, String) -> Unit,
    onApproveExtension: (String, String) -> Unit,
    onRejectExtension: (String, String) -> Unit,
    currentUserId: String = ""
) {
    android.util.Log.d("TaskListDebug", "TaskList called with ${tasks.size} tasks, currentUserId: $currentUserId")
    var selectedTaskId by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExtensionRequestDialog by remember { mutableStateOf(false) }
    var selectedExtensionTaskId by remember { mutableStateOf<String?>(null) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa công việc") },
            text = { Text("Bạn có chắc chắn muốn xóa công việc này? Hành động này không thể hoàn tác.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTaskId?.let { onDeleteTask(it) }
                        showDeleteDialog = false
                        selectedTaskId = null
                    }
                ) {
                    Text("Xóa", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Extension Request Dialog
    if (showExtensionRequestDialog && selectedExtensionTaskId != null) {
        var newDueDate by remember { mutableStateOf("") }
        var reason by remember { mutableStateOf("") }
        var showDatePicker by remember { mutableStateOf(false) }
        val sdf = remember { SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                newDueDate = sdf.format(Date(millis))
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Hủy")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        AlertDialog(
            onDismissRequest = { showExtensionRequestDialog = false },
            title = { Text("Xin gia hạn công việc") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newDueDate,
                            onValueChange = { },
                            label = { Text("Ngày gia hạn mới") },
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Chọn ngày")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Lý do") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newDueDate.isNotBlank() && reason.isNotBlank()) {
                            selectedExtensionTaskId?.let { onRequestExtension(it, newDueDate, reason) }
                            showExtensionRequestDialog = false
                            selectedExtensionTaskId = null
                            newDueDate = ""
                            reason = ""
                        }
                    }
                ) {
                    Text("Gửi yêu cầu")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExtensionRequestDialog = false
                    selectedExtensionTaskId = null
                    newDueDate = ""
                    reason = ""
                }) {
                    Text("Hủy")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tasks, key = { it.id }) { task ->
            val isOwner = task.assigneeId == currentUserId
            val isNotOwner = task.assigneeId != currentUserId
            val hasPendingExtensionRequest = task.pendingExtensionRequestStatus == "PENDING"
            android.util.Log.d("TaskListDebug", "Task: ${task.name}, assigneeId: ${task.assigneeId}, currentUserId: $currentUserId, isOwner: $isOwner")

            Box {
                TaskCard(
                    task = task,
                    onMoreClick = {
                        selectedTaskId = task.id
                        showMenu = true
                    },
                    onClick = { onTaskClick(task.id) },
                    onStatusChange = { newStatus -> onStatusChange(task.id, newStatus) },
                    canChangeStatus = isOwner
                )
                DropdownMenu(
                    expanded = showMenu && selectedTaskId == task.id,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Thêm công việc con") },
                        onClick = {
                            showMenu = false
                            // TODO: Add subtask navigation
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Chỉnh sửa") },
                        onClick = {
                            showMenu = false
                            onEditTask(task.id)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        },
                        enabled = isOwner
                    )
                    DropdownMenuItem(
                        text = { Text("Xóa") },
                        onClick = {
                            showMenu = false
                            selectedTaskId = task.id
                            showDeleteDialog = true
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                        },
                        enabled = isOwner
                    )
                    if (isNotOwner && !hasPendingExtensionRequest) {
                        DropdownMenuItem(
                            text = { Text("Xin gia hạn") },
                            onClick = {
                                showMenu = false
                                selectedExtensionTaskId = task.id
                                showExtensionRequestDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Schedule, contentDescription = null)
                            }
                        )
                    }
                    if (isOwner && hasPendingExtensionRequest) {
                        val requestId = task.pendingExtensionRequestId ?: ""
                        DropdownMenuItem(
                            text = { Text("Duyệt gia hạn") },
                            onClick = {
                                showMenu = false
                                if (requestId.isNotBlank()) {
                                    onApproveExtension(task.id, requestId)
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Từ chối gia hạn") },
                            onClick = {
                                showMenu = false
                                if (requestId.isNotBlank()) {
                                    onRejectExtension(task.id, requestId)
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        )
                    }
                }
            }
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
            onStatusChange = { _, _ -> },
            onEditTask = {},
            onDeleteTask = {},
            onRequestExtension = { _, _, _ -> },
            onApproveExtension = { _, _ -> },
            onRejectExtension = { _, _ -> },
            currentUserId = ""
        )
    }
}