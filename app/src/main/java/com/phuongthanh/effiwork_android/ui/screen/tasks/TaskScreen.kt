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
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import com.phuongthanh.effiwork_android.viewmodel.task.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    projectName: String = "NCKH",
    projectId: String = "",
    groupId: String = "",
    viewModel: TaskViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onNavigateToCreateTask: (String) -> Unit = {},
    onNavigateToTaskDetail: (String, String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentProjectName by viewModel.projectName.collectAsStateWithLifecycle()

    LaunchedEffect(projectId, projectName, groupId) {
        viewModel.setProjectInfo(projectId, projectName)
        viewModel.setGroupId(groupId)
        val sectionId = if (groupId.isNotBlank()) groupId else null
        viewModel.loadTasks(sectionId)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is TaskEffect.ShowToast -> {
                    // Handle toast - would need a SnackbarHost or similar
                }
                is TaskEffect.TaskCreated -> {
                    // Task created, list will refresh via loadTasks
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Công việc",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Blue500.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = currentProjectName,
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Menu options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToCreateTask(projectId) },
                containerColor = Blue500,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tạo công việc")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5))
        ) {
            // Search and Filter Bar
            SearchAndFilterBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onFilterClick = { /* Open filter dialog */ }
            )

            // Tab Row
            TabRow(
                selectedTabIndex = if (selectedTab == TaskTab.COMMON_TASKS) 0 else 1,
                containerColor = Color.White,
                contentColor = Blue500
            ) {
                Tab(
                    selected = selectedTab == TaskTab.COMMON_TASKS,
                    onClick = { viewModel.selectTab(TaskTab.COMMON_TASKS) },
                    text = {
                        Text(
                            "Công việc chung",
                            color = if (selectedTab == TaskTab.COMMON_TASKS) Blue500 else Color.Gray
                        )
                    }
                )
                Tab(
                    selected = selectedTab == TaskTab.ASSIGNED_TO_ME,
                    onClick = { viewModel.selectTab(TaskTab.ASSIGNED_TO_ME) },
                    text = {
                        Text(
                            "Được giao",
                            color = if (selectedTab == TaskTab.ASSIGNED_TO_ME) Blue500 else Color.Gray
                        )
                    }
                )
            }

            // Task List
            when (val state = uiState) {
                is TaskUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Blue500)
                    }
                }
                is TaskUiState.Success -> {
                    val filteredTasks = state.tasks.filter { task ->
                        val matchesSearch = searchQuery.isEmpty() ||
                            task.name.contains(searchQuery, ignoreCase = true) ||
                            task.description.contains(searchQuery, ignoreCase = true)
                        val matchesCategory = selectedCategory == TaskCategory.ALL ||
                            task.category == selectedCategory.displayName
                        matchesSearch && matchesCategory
                    }
                    TaskList(
                        tasks = filteredTasks,
                        onSubtaskToggle = { taskId, subtaskId ->
                            viewModel.toggleSubtask(taskId, subtaskId)
                        },
                        onTaskClick = { taskId ->
                            onNavigateToTaskDetail(projectId, taskId)
                        }
                    )
                }
                is TaskUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(state.message, color = Color.Red)
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
        FilledTonalButton(
            onClick = onFilterClick,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Blue500.copy(alpha = 0.1f),
                contentColor = Blue500
            )
        ) {
            Icon(Icons.Default.FilterList, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Bộ lọc")
        }
    }
}

@Composable
private fun TaskList(
    tasks: List<Task>,
    onSubtaskToggle: (String, String) -> Unit,
    onTaskClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tasks, key = { it.id }) { task ->
            TaskCard(
                task = task,
                onSubtaskToggle = { subtaskId -> onSubtaskToggle(task.id, subtaskId) },
                onClick = { onTaskClick(task.id) }
            )
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onSubtaskToggle: (String) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                StatusDropdown(task.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Task details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Assignee
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = task.assignee,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${task.startDate} - ${task.endDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Participants
            if (task.participants.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = task.participants.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Subtasks
            if (task.subtasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp)
                ) {
                    task.subtasks.forEachIndexed { index, subtask ->
                        SubtaskItem(
                            subtask = subtask,
                            onToggle = { onSubtaskToggle(subtask.id) },
                            modifier = Modifier.padding(start = (index * 16).dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDropdown(currentStatus: TaskStatus) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier.clickable { expanded = true },
            shape = RoundedCornerShape(4.dp),
            color = when (currentStatus) {
                TaskStatus.NOT_STARTED -> Color.Gray.copy(alpha = 0.1f)
                TaskStatus.IN_PROGRESS -> Color(0xFF2196F3).copy(alpha = 0.1f)
                TaskStatus.COMPLETED -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                TaskStatus.ON_HOLD -> Color(0xFFFF9800).copy(alpha = 0.1f)
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentStatus.displayName,
                    fontSize = 12.sp,
                    color = when (currentStatus) {
                        TaskStatus.NOT_STARTED -> Color.Gray
                        TaskStatus.IN_PROGRESS -> Color(0xFF2196F3)
                        TaskStatus.COMPLETED -> Color(0xFF4CAF50)
                        TaskStatus.ON_HOLD -> Color(0xFFFF9800)
                    }
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = when (currentStatus) {
                        TaskStatus.NOT_STARTED -> Color.Gray
                        TaskStatus.IN_PROGRESS -> Color(0xFF2196F3)
                        TaskStatus.COMPLETED -> Color(0xFF4CAF50)
                        TaskStatus.ON_HOLD -> Color(0xFFFF9800)
                    }
                )
            }
        }
    }
}

@Composable
private fun SubtaskItem(
    subtask: Subtask,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tree line
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color.LightGray)
            )
        }
        Checkbox(
            checked = subtask.isCompleted,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF4CAF50),
                uncheckedColor = Color.Gray
            )
        )
        Text(
            text = subtask.name,
            style = MaterialTheme.typography.bodySmall,
            color = if (subtask.isCompleted) Color.Gray else Color.DarkGray,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = subtask.dueDate,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Task Screen")
@Composable
fun TaskScreenPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            // Top App Bar simulation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Công việc",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Blue500.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "NCKH",
                            color = Blue500,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Tab Row
            TabRow(
                selectedTabIndex = 0,
                containerColor = Color.White,
                contentColor = Blue500
            ) {
                Tab(selected = true, onClick = {}, text = { Text("Công việc chung", color = Blue500) })
                Tab(selected = false, onClick = {}, text = { Text("Được giao", color = Color.Gray) })
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Thiết kế giao diện",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Thiết kế UI/UX cho màn hình chính",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Phuong Thanh", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("20/05 - 25/05", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}