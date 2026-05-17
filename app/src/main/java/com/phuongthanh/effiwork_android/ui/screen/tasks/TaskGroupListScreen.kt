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

data class TaskGroupColor(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

private val groupColors = listOf(
    TaskGroupColor("Xanh dương", Icons.Default.Folder, Color(0xFF2196F3)),
    TaskGroupColor("Xanh lá", Icons.Default.Edit, Color(0xFF4CAF50)),
    TaskGroupColor("Tím", Icons.Default.Code, Color(0xFF9C27B0)),
    TaskGroupColor("Cam", Icons.Default.Science, Color(0xFFFF9800)),
    TaskGroupColor("Hồng", Icons.Default.RocketLaunch, Color(0xFFE91E63)),
    TaskGroupColor("Xám", Icons.Default.MoreHoriz, Color.Gray)
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
    var showAddDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color(0xFF2196F3)) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(projectId, projectName) {
        viewModel.setProjectInfo(projectId, projectName)
        viewModel.loadTaskGroups()
    }

    if (showAddDialog) {
        AddTaskGroupDialog(
            groupName = newGroupName,
            onGroupNameChange = { newGroupName = it },
            groupColors = groupColors,
            selectedColor = selectedColor,
            onColorSelect = { selectedColor = it },
            onCancel = {
                newGroupName = ""
                showAddDialog = false
            },
            onSave = {
                viewModel.createTaskGroup(
                    name = newGroupName,
                    onSuccess = {
                        newGroupName = ""
                        showAddDialog = false
                    },
                    onError = { errorMessage ->
                        // Could show snackbar here
                    }
                )
            }
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
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
                    }
                    IconButton(onClick = { /* More options */ }) {
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
                onClick = { showAddDialog = true },
                containerColor = Blue500,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm nhóm công việc")
            }
        }
    ) { innerPadding ->
        when (val state = uiState) {
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
                    items(state.groups) { group ->
                        TaskGroupCard(
                            group = group,
                            onClick = { onNavigateToTask(projectId, projectName, group.id) }
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
                    Text(state.message, color = Color.Red)
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
    groupColors: List<TaskGroupColor>,
    selectedColor: Color,
    onColorSelect: (Color) -> Unit,
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

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(groupColors) { colorItem ->
                        ColorOption(
                            color = colorItem.color,
                            isSelected = selectedColor == colorItem.color,
                            onClick = { onColorSelect(colorItem.color) }
                        )
                    }
                }
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

@Preview(showBackground = true)
@Composable
private fun TaskGroupListScreenPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            // Top App Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Nhóm công việc",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(3) { index ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val colors = listOf(Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFF9C27B0))
                            val icons = listOf(Icons.Default.Folder, Icons.Default.Edit, Icons.Default.Code)
                            val names = listOf("Thiết kế giao diện", "Thực hiện code", "Kiểm thử")
                            val counts = listOf(5, 8, 3)

                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = colors[index].copy(alpha = 0.1f)
                            ) {
                                Icon(
                                    imageVector = icons[index],
                                    contentDescription = null,
                                    tint = colors[index],
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(names[index], fontWeight = FontWeight.Bold)
                                Text("${counts[index]} công việc", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
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