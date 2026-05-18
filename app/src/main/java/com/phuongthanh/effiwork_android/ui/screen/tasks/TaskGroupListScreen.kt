package com.phuongthanh.effiwork_android.ui.screen.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import com.phuongthanh.effiwork_android.viewmodel.task.TaskGroupItem
import com.phuongthanh.effiwork_android.viewmodel.task.TaskGroupUiState
import com.phuongthanh.effiwork_android.viewmodel.task.TaskGroupViewModel

data class TaskGroupScreenState(
    val projectId: String = "",
    val projectName: String = "",
    val uiState: TaskGroupUiState = TaskGroupUiState.Idle,
    val showAddDialog: Boolean = false,
    val newGroupName: String = "",
    val selectedColor: Color = Color(0xFF2196F3)
)

data class TaskGroupColor(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskGroupListScreen(
    projectId: String = "",
    projectName: String = "",
    onBackClick: () -> Unit = {},
    onNavigateToTask: (String, String, String) -> Unit = { _, _, _ -> },
    viewModel: TaskGroupViewModel = hiltViewModel()
) {
    var screenState by remember { mutableStateOf(TaskGroupScreenState(projectId = projectId, projectName = projectName)) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(projectId, projectName) {
        viewModel.setProjectInfo(projectId, projectName)
        viewModel.loadTaskGroups()
        screenState = screenState.copy(projectId = projectId, projectName = projectName)
    }

    LaunchedEffect(uiState) {
        screenState = screenState.copy(uiState = uiState)
    }

    TaskGroupListScreenContent(
        state = screenState,
        onBackClick = onBackClick,
        onNavigateToTask = onNavigateToTask,
        onShowAddDialog = { screenState = screenState.copy(showAddDialog = true) },
        onDismissAddDialog = {
            screenState = screenState.copy(showAddDialog = false, newGroupName = "", selectedColor = Color(0xFF2196F3))
        },
        onNewGroupNameChange = { screenState = screenState.copy(newGroupName = it) },
        onSaveGroup = { name ->
            viewModel.createTaskGroup(
                name = name,
                onSuccess = {
                    screenState = screenState.copy(showAddDialog = false, newGroupName = "", selectedColor = Color(0xFF2196F3))
                },
                onError = {}
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskGroupListScreenContent(
    state: TaskGroupScreenState,
    onBackClick: () -> Unit,
    onNavigateToTask: (String, String, String) -> Unit,
    onShowAddDialog: () -> Unit,
    onDismissAddDialog: () -> Unit,
    onNewGroupNameChange: (String) -> Unit,
    onSaveGroup: (String) -> Unit
) {
    if (state.showAddDialog) {
        AddTaskGroupDialog(
            groupName = state.newGroupName,
            onGroupNameChange = onNewGroupNameChange,
            onCancel = onDismissAddDialog,
            onSave = { onSaveGroup(state.newGroupName) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = "Nhóm công việc",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
                    }
                    IconButton(onClick = {}) {
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
            FloatingActionButton(
                onClick = onShowAddDialog,
                containerColor = Blue500,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm nhóm công việc")
            }
        }
    ) { innerPadding ->
        when (val uiState = state.uiState) {
            is TaskGroupUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Blue500)
                }
            }
            is TaskGroupUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(Color(0xFFF5F5F5)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.groups) { group ->
                        TaskGroupCard(
                            group = group,
                            onClick = { onNavigateToTask(state.projectId, state.projectName, group.id) }
                        )
                    }
                }
            }
            is TaskGroupUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(uiState.message, color = Color.Red)
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun TaskGroupCard(
    group: TaskGroupItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                color = group.color.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = group.icon,
                    contentDescription = null,
                    tint = group.color,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${group.taskCount} công việc",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Xem chi tiết",
                tint = Color.Gray
            )
        }
    }
}

@Composable
private fun AddTaskGroupDialog(
    groupName: String,
    onGroupNameChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = "Thêm nhóm công việc",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = onGroupNameChange,
                    label = { Text("Tên nhóm công việc") },
                    placeholder = { Text("Nhập tên nhóm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Màu sắc",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = groupName.isNotBlank()
            ) {
                Text("Lưu", color = Blue500)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Hủy", color = Color.Gray)
            }
        }
    )
}

@Composable
private fun ColorOption(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) Modifier.border(2.dp, Color.DarkGray, CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Task Group List Screen")
@Composable
fun TaskGroupListScreenPreview() {
    MaterialTheme {
        TaskGroupListScreenContent(
            state = TaskGroupScreenState(
                projectId = "proj123",
                projectName = "NCKH",
                uiState = TaskGroupUiState.Success(
                    listOf(
                        TaskGroupItem("g1", "Thiết kế giao diện", Icons.Default.Folder, Color(0xFF2196F3), 5),
                        TaskGroupItem("g2", "Thực hiện code", Icons.Default.Edit, Color(0xFF4CAF50), 8),
                        TaskGroupItem("g3", "Kiểm thử", Icons.Default.Code, Color(0xFF9C27B0), 3)
                    )
                ),
                showAddDialog = false,
                newGroupName = "",
                selectedColor = Color(0xFF2196F3)
            ),
            onBackClick = {},
            onNavigateToTask = { _, _, _ -> },
            onShowAddDialog = {},
            onDismissAddDialog = {},
            onNewGroupNameChange = {},
            onSaveGroup = {}
        )
    }
}