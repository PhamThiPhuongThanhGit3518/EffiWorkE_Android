package com.phuongthanh.effiwork_android.ui.screen.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import com.phuongthanh.effiwork_android.viewmodel.task.*
import kotlinx.coroutines.flow.collectLatest

data class TaskDetailScreenState(
    val projectId: String = "",
    val taskId: String = "",
    val uiState: TaskDetailUiState = TaskDetailUiState.Idle,
    val commentText: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    projectId: String = "",
    taskId: String = "",
    onBackClick: () -> Unit = {},
    onEditTask: (String, String) -> Unit = { _, _ -> },
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    var screenState by remember { mutableStateOf(TaskDetailScreenState(projectId = projectId, taskId = taskId)) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(projectId, taskId) {
        viewModel.loadTaskDetail(projectId, taskId)
    }

    LaunchedEffect(uiState) {
        screenState = screenState.copy(uiState = uiState)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is TaskDetailEffect.CommentPosted -> {
                    screenState = screenState.copy(commentText = "")
                }
                is TaskDetailEffect.ShowToast -> {}
                is TaskDetailEffect.TaskDeleted -> {
                    onBackClick()
                }
            }
        }
    }

    TaskDetailScreenContent(
        state = screenState,
        onBackClick = onBackClick,
        onCommentTextChange = { screenState = screenState.copy(commentText = it) },
        onPostComment = { viewModel.postComment(screenState.commentText) },
        onAddSubtask = {},
        onEditTask = { onEditTask(projectId, taskId) },
        onDeleteTask = { viewModel.deleteTask() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreenContent(
    state: TaskDetailScreenState,
    onBackClick: () -> Unit,
    onCommentTextChange: (String) -> Unit,
    onPostComment: () -> Unit,
    onAddSubtask: () -> Unit = {},
    onEditTask: () -> Unit = {},
    onDeleteTask: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa công việc") },
            text = { Text("Bạn có chắc chắn muốn xóa công việc này? Hành động này không thể hoàn tác.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteTask()
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

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Chi tiết công việc", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Thêm tùy chọn",
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Thêm công việc con") },
                                onClick = {
                                    showMenu = false
                                    onAddSubtask()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Chỉnh sửa") },
                                onClick = {
                                    showMenu = false
                                    onEditTask()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Xóa") },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.commentText,
                        onValueChange = onCommentTextChange,
                        placeholder = { Text("Viết bình luận...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.LightGray,
                            focusedBorderColor = Blue500
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onPostComment,
                        enabled = state.commentText.isNotBlank()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Gửi",
                            tint = if (state.commentText.isNotBlank()) Blue500 else Color.Gray
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        when (val uiState = state.uiState) {
            is TaskDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Blue500)
                }
            }
            is TaskDetailUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(Color(0xFFF5F5F5))
                ) {
                    item {
                        TaskDetailHeader(uiState.taskDetail)
                    }
                    item {
                        TaskDescriptionSection(uiState.taskDetail.description)
                    }
                    item {
                        TaskInfoSection(uiState.taskDetail)
                    }
                    item {
                        CommentsSection(uiState.taskDetail.comments)
                    }
                }
            }
            is TaskDetailUiState.Error -> {
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
private fun TaskDetailHeader(task: TaskDetail) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusChip(task.status)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Blue500.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = task.groupName,
                        color = Blue500,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (bgColor, textColor) = when (status.uppercase()) {
        "TODO", "NOT_STARTED" -> Color(0xFFF5F5F5) to Color.Gray
        "IN_PROGRESS" -> Color(0xFFE3F2FD) to Color(0xFF2196F3)
        "REVIEW" -> Color(0xFFFFF3E0) to Color(0xFFFF9800)
        "DONE", "COMPLETED" -> Color(0xFFE8F5E9) to Color(0xFF4CAF50)
        "ON_HOLD", "PAUSED" -> Color(0xFFFFF3E0) to Color(0xFFFF9800)
        "CANCELLED" -> Color(0xFFFFEBEE) to Color(0xFFF44336)
        else -> Color(0xFFF5F5F5) to Color.Gray
    }
    val displayText = when (status.uppercase()) {
        "TODO", "NOT_STARTED" -> "Chưa bắt đầu"
        "IN_PROGRESS" -> "Đang thực hiện"
        "REVIEW" -> "Đang review"
        "DONE", "COMPLETED" -> "Hoàn thành"
        "ON_HOLD", "PAUSED" -> "Tạm dừng"
        "CANCELLED" -> "Đã hủy"
        else -> status
    }
    Surface(shape = RoundedCornerShape(4.dp), color = bgColor) {
        Text(
            text = displayText,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TaskDescriptionSection(description: String) {
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
                text = "Mô tả",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description.ifBlank { "Không có mô tả" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (description.isBlank()) Color.Gray else Color.DarkGray
            )
        }
    }
}

@Composable
private fun TaskInfoSection(task: TaskDetail) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Thông tin",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(icon = Icons.Default.Person, label = "Người phụ trách", value = task.assigneeName)
            InfoRow(icon = Icons.Default.CalendarToday, label = "Ngày bắt đầu", value = task.startDate)
            InfoRow(icon = Icons.Default.Event, label = "Ngày kết thúc", value = task.endDate)
            InfoRow(icon = Icons.Default.Add, label = "Người tạo", value = task.creatorName)
            InfoRow(icon = Icons.Default.AccessTime, label = "Ngày tạo", value = task.createdAt)

            if (task.participantNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Người tham gia",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        task.participantNames.forEach { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value.ifBlank { "-" },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray
        )
    }
}

@Composable
private fun CommentsSection(comments: List<CommentItem>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp)
            .padding(bottom = 80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Bình luận",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(shape = CircleShape, color = Blue500) {
                    Text(
                        text = "${comments.size}",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (comments.isEmpty()) {
                Text(
                    text = "Chưa có bình luận nào",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            } else {
                comments.forEach { comment ->
                    CommentItem(comment = comment)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: CommentItem) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Blue500.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = comment.userName.take(1).uppercase(),
                color = Blue500,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.userName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = comment.createdAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
        }
    }
}

@Preview(showBackground = true, name = "Task Detail Screen")
@Composable
fun TaskDetailScreenPreview() {
    MaterialTheme {
        TaskDetailScreenContent(
            state = TaskDetailScreenState(
                projectId = "proj123",
                taskId = "task456",
                uiState = TaskDetailUiState.Success(
                    taskDetail = TaskDetail(
                        id = "task456",
                        title = "Thiết kế giao diện màn hình chính",
                        description = "Thiết kế UI/UX cho màn hình chính của ứng dụng. Bao gồm các màn hình: Home, Profile, Settings.",
                        status = "IN_PROGRESS",
                        groupName = "Thiết kế giao diện",
                        assigneeName = "Phạm Thị Phương Thanh",
                        creatorName = "Nguyễn Văn Minh",
                        startDate = "2026-05-20",
                        endDate = "2026-05-25",
                        createdAt = "2026-05-15 10:30",
                        participantNames = listOf("Trần Văn Hoàng", "Lê Thị Mai"),
                        commentCount = 2,
                        attachmentCount = 0,
                        comments = listOf(
                            CommentItem("c1", "Bản thiết kế đã sẵn sàng để review chưa?", "Nguyễn Văn Minh", null, "2026-05-16 14:20"),
                            CommentItem("c2", "Đã xong! Mình đã upload lên drive.", "Phạm Thị Phương Thanh", null, "2026-05-16 15:45")
                        )
                    )
                ),
                commentText = ""
            ),
            onBackClick = {},
            onCommentTextChange = {},
            onPostComment = {},
            onAddSubtask = {},
            onEditTask = {},
            onDeleteTask = {}
        )
    }
}