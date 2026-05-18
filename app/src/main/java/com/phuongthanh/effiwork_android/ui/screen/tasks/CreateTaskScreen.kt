package com.phuongthanh.effiwork_android.ui.screen.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import com.phuongthanh.effiwork_android.viewmodel.task.TaskEffect
import com.phuongthanh.effiwork_android.viewmodel.task.TaskGroup
import com.phuongthanh.effiwork_android.viewmodel.task.TaskMember
import com.phuongthanh.effiwork_android.viewmodel.task.TaskUiState
import com.phuongthanh.effiwork_android.viewmodel.task.TaskViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

data class CreateTaskFormState(
    val taskName: String = "",
    val selectedGroupId: String = "",
    val description: String = "",
    val taskLevel: String = "", // Used for display, empty when creating subtask
    val assigneeId: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val reminder: String = "",
    val status: String = "Chưa bắt đầu",
    val participantIds: List<String> = emptyList(),
    val groupExpanded: Boolean = false,
    val assigneeExpanded: Boolean = false,
    val statusExpanded: Boolean = false,
    val levelExpanded: Boolean = false,
    val participantSearchQuery: String = "",
    val showStartDatePicker: Boolean = false,
    val showEndDatePicker: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskListScreen(
    projectId: String = "",
    taskId: String? = null,
    preselectedGroupId: String = "",
    parentTaskId: String = "",
    parentTaskName: String = "",
    onBackClick: () -> Unit = {},
    onCreateClick: (String) -> Unit = { _ -> },
    onUpdateClick: (String) -> Unit = { _ -> },
    viewModel: TaskViewModel = hiltViewModel()
) {
    var formState by remember { mutableStateOf(CreateTaskFormState(selectedGroupId = preselectedGroupId)) }
    val taskGroups by viewModel.taskGroups.collectAsStateWithLifecycle()
    val taskMembers by viewModel.taskMembers.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isEditMode = taskId != null
    val isSubtaskMode = parentTaskId.isNotBlank()

    LaunchedEffect(projectId) {
        viewModel.setProjectInfo(projectId, "")
        viewModel.loadTaskGroupsForCreate()
        viewModel.loadMembersForCreate()
    }

    // Load task details if in edit mode
    LaunchedEffect(taskId) {
        if (taskId != null) {
            viewModel.loadTaskDetailForEdit(taskId)
        }
    }

    // Populate form when task detail is loaded
    LaunchedEffect(uiState, taskId) {
        if (taskId != null) {
            val currentState = uiState
            if (currentState is TaskUiState.Success) {
                val task = currentState.tasks.firstOrNull { t -> t.id == taskId }
                if (task != null) {
                    formState = formState.copy(
                        taskName = task.name,
                        description = task.description,
                        startDate = task.startDate,
                        endDate = task.endDate,
                        selectedGroupId = task.category,
                        assigneeId = task.assigneeId.takeIf { it.isNotBlank() } ?: ""
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is TaskEffect.ShowToast -> {}
                is TaskEffect.TaskCreated -> {
                    onCreateClick(effect.taskName)
                }
                is TaskEffect.TaskUpdated -> {
                    onUpdateClick(effect.taskName)
                }
                is TaskEffect.TaskDeleted -> {}
            }
        }
    }

    CreateTaskListScreenContent(
        formState = formState,
        taskGroups = taskGroups,
        taskMembers = taskMembers,
        onFormStateChange = { formState = it },
        onBackClick = onBackClick,
        onCreateClick = {
            viewModel.createSubtask(
                name = formState.taskName,
                description = formState.description,
                parentTaskId = parentTaskId,
                groupId = formState.selectedGroupId.ifBlank { null },
                assigneeId = formState.assigneeId,
                startDate = formState.startDate,
                endDate = formState.endDate,
                reminderTime = formState.reminder.ifBlank { null },
                participantIds = formState.participantIds
            )
        },
        onUpdateClick = {
            taskId?.let { id ->
                viewModel.updateTask(
                    taskId = id,
                    name = formState.taskName,
                    description = formState.description,
                    groupId = formState.selectedGroupId.ifBlank { null },
                    assigneeId = formState.assigneeId,
                    startDate = formState.startDate,
                    endDate = formState.endDate,
                    reminderTime = formState.reminder.ifBlank { null },
                    participantIds = formState.participantIds
                )
            }
        },
        isEditMode = isEditMode,
        isSubtaskMode = isSubtaskMode,
        parentTaskName = parentTaskName
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskListScreenContent(
    formState: CreateTaskFormState,
    taskGroups: List<TaskGroup>,
    taskMembers: List<TaskMember>,
    onFormStateChange: (CreateTaskFormState) -> Unit,
    onBackClick: () -> Unit,
    onCreateClick: (String) -> Unit,
    onUpdateClick: () -> Unit = {},
    isEditMode: Boolean = false,
    isSubtaskMode: Boolean = false,
    parentTaskName: String = ""
) {
    val statuses = listOf("Chưa bắt đầu", "Đang thực hiện", "Hoàn thành", "Tạm dừng")
    val taskLevels = listOf("Công việc lớn của dự án", "Công việc nhỏ")

    val screenTitle = when {
        isEditMode -> "Chỉnh sửa công việc"
        isSubtaskMode -> "Thêm công việc con"
        else -> "Tạo công việc"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = screenTitle,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Button(
                    onClick = { if (isEditMode) onUpdateClick() else onCreateClick(formState.taskName) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    enabled = formState.taskName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue500,
                        disabledContainerColor = Blue500.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isEditMode) "Cập nhật công việc" else "Tạo công việc",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            Text(
                text = "Tạo công việc cấp 1 của dự án. Công việc con sẽ được tạo trong mục chi tiết công việc.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            TaskInfoCard(
                formState = formState,
                onFormStateChange = onFormStateChange,
                taskGroups = taskGroups
            )

            Spacer(modifier = Modifier.height(16.dp))

            AssignmentCard(
                formState = formState,
                onFormStateChange = onFormStateChange,
                taskLevels = taskLevels,
                taskMembers = taskMembers,
                isSubtaskMode = isSubtaskMode,
                parentTaskName = parentTaskName
            )

            Spacer(modifier = Modifier.height(16.dp))

            CollaboratorsCard(
                formState = formState,
                onFormStateChange = onFormStateChange,
                taskMembers = taskMembers
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskInfoCard(
    formState: CreateTaskFormState, // Thêm dòng này
    onFormStateChange: (CreateTaskFormState) -> Unit, // Thêm dòng này
    taskGroups: List<TaskGroup>
) {
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
                text = "Thông tin công việc",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = formState.taskName, // Đổi thành formState.taskName
                onValueChange = { onFormStateChange(formState.copy(taskName = it)) }, // Sửa tại đây
                label = { Text("Tên công việc") },
                placeholder = { Text("Chuẩn bị tài liệu sprint review") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = formState.groupExpanded, // Đổi thành formState.groupExpanded
                onExpandedChange = { onFormStateChange(formState.copy(groupExpanded = it)) } // Sửa tại đây
            ) {
                OutlinedTextField(
                    value = taskGroups.find { it.id == formState.selectedGroupId }?.name ?: "", // Sửa tại đây
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Nhóm công việc") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formState.groupExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = formState.groupExpanded,
                    onDismissRequest = { onFormStateChange(formState.copy(groupExpanded = false)) }
                ) {
                    taskGroups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name) },
                            onClick = {
                                // Gom cụm cập nhật state an toàn, không bị ghi đè dữ liệu
                                onFormStateChange(
                                    formState.copy(
                                        selectedGroupId = group.id,
                                        groupExpanded = false
                                    )
                                )
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = formState.description, // Đổi thành formState.description
                onValueChange = { onFormStateChange(formState.copy(description = it)) }, // Sửa tại đây
                label = { Text("Mô tả") },
                placeholder = { Text("Nhập mô tả chi tiết mục tiêu, đầu ra hoặc các lưu ý triển khai...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignmentCard(
    formState: CreateTaskFormState,
    onFormStateChange: (CreateTaskFormState) -> Unit,
    taskLevels: List<String>,
    taskMembers: List<TaskMember>,
    isSubtaskMode: Boolean = false,
    parentTaskName: String = ""
) {
    val statuses = listOf("Chưa bắt đầu", "Đang thực hiện", "Hoàn thành", "Tạm dừng")

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
                text = "Phân công và theo dõi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isSubtaskMode) {
                OutlinedTextField(
                    value = parentTaskName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Cấp công việc") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            } else {
                ExposedDropdownMenuBox(
                    expanded = formState.levelExpanded,
                    onExpandedChange = { onFormStateChange(formState.copy(levelExpanded = it)) }
                ) {
                    OutlinedTextField(
                        value = formState.taskLevel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cấp công việc") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formState.levelExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = formState.levelExpanded,
                        onDismissRequest = { onFormStateChange(formState.copy(levelExpanded = false)) }
                    ) {
                        taskLevels.forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level) },
                                onClick = {
                                    onFormStateChange(formState.copy(taskLevel = level, levelExpanded = false))
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = formState.assigneeExpanded,
                onExpandedChange = { onFormStateChange(formState.copy(assigneeExpanded = it)) }
            ) {
                OutlinedTextField(
                    value = taskMembers.find { it.userId == formState.assigneeId }?.fullName ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Người phụ trách") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formState.assigneeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = formState.assigneeExpanded,
                    onDismissRequest = { onFormStateChange(formState.copy(assigneeExpanded = false)) }
                ) {
                    taskMembers.forEach { member ->
                        DropdownMenuItem(
                            text = { Text(member.fullName) },
                            onClick = {
                                onFormStateChange(formState.copy(assigneeId = member.userId, assigneeExpanded = false))
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = formState.startDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Bắt đầu") },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        IconButton(onClick = { onFormStateChange(formState.copy(showStartDatePicker = true)) }) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray)
                        }
                    }
                )
                OutlinedTextField(
                    value = formState.endDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kết thúc") },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        IconButton(onClick = { onFormStateChange(formState.copy(showEndDatePicker = true)) }) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray)
                        }
                    }
                )
            }

            if (formState.showStartDatePicker) {
                val initialMillis = formState.startDate.let {
                    try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it)?.time }
                    catch (e: Exception) { null }
                }
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
                DatePickerDialog(
                    onDismissRequest = { onFormStateChange(formState.copy(showStartDatePicker = false)) },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                onFormStateChange(formState.copy(startDate = sdf.format(Date(millis)), showStartDatePicker = false))
                            } ?: onFormStateChange(formState.copy(showStartDatePicker = false))
                        }) {
                            Text("Chọn")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { onFormStateChange(formState.copy(showStartDatePicker = false)) }) {
                            Text("Hủy")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            if (formState.showEndDatePicker) {
                val initialMillis = formState.endDate.let {
                    try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it)?.time }
                    catch (e: Exception) { null }
                }
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
                DatePickerDialog(
                    onDismissRequest = { onFormStateChange(formState.copy(showEndDatePicker = false)) },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                onFormStateChange(formState.copy(endDate = sdf.format(Date(millis)), showEndDatePicker = false))
                            } ?: onFormStateChange(formState.copy(showEndDatePicker = false))
                        }) {
                            Text("Chọn")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { onFormStateChange(formState.copy(showEndDatePicker = false)) }) {
                            Text("Hủy")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = formState.reminder,
                onValueChange = { onFormStateChange(formState.copy(reminder = it)) },
                label = { Text("Nhắc nhở") },
                placeholder = { Text("Chọn thời gian nhắc nhở") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                trailingIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Gray)
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = formState.statusExpanded,
                onExpandedChange = { onFormStateChange(formState.copy(statusExpanded = it)) }
            ) {
                OutlinedTextField(
                    value = formState.status,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Trạng thái") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formState.statusExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = formState.statusExpanded,
                    onDismissRequest = { onFormStateChange(formState.copy(statusExpanded = false)) }
                ) {
                    statuses.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s) },
                            onClick = {
                                onFormStateChange(formState.copy(status = s, statusExpanded = false))
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollaboratorsCard(
    formState: CreateTaskFormState,
    onFormStateChange: (CreateTaskFormState) -> Unit,
    taskMembers: List<TaskMember>
) {
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
                text = "Người tham gia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = false,
                onExpandedChange = {}
            ) {
                OutlinedTextField(
                    value = formState.participantSearchQuery,
                    onValueChange = { onFormStateChange(formState.copy(participantSearchQuery = it)) },
                    placeholder = { Text("Tìm và chọn người tham gia") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                taskMembers.filter { formState.participantIds.contains(it.userId) }.forEach { member ->
                    InputChip(
                        selected = true,
                        onClick = {},
                        label = { Text(member.fullName, fontSize = 12.sp) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Xóa",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        onFormStateChange(
                                            formState.copy(
                                                participantIds = formState.participantIds.filter { id -> id != member.userId }
                                            )
                                        )
                                    }
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = Blue500.copy(alpha = 0.1f),
                            selectedLabelColor = Blue500
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Create Task Screen")
@Composable
fun CreateTaskListScreenPreview() {
    MaterialTheme {
        CreateTaskListScreenContent(
            formState = CreateTaskFormState(
                taskName = "Thiết kế giao diện màn hình chính",
                description = "Thiết kế UI/UX cho ứng dụng",
                assigneeId = "user1",
                startDate = "2026-05-20",
                endDate = "2026-05-25"
            ),
            taskGroups = listOf(
                TaskGroup("g1", "Thiết kế"),
                TaskGroup("g2", "Lập trình")
            ),
            taskMembers = listOf(
                TaskMember("user1", "Phạm Thị Phương Thanh"),
                TaskMember("user2", "Nguyễn Văn Minh")
            ),
            onFormStateChange = {},
            onBackClick = {},
            onCreateClick = {}
        )
    }
}