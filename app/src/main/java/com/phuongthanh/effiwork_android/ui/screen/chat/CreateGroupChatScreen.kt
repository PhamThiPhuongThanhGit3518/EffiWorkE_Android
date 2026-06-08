package com.phuongthanh.effiwork_android.ui.screen.chat

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import com.phuongthanh.effiwork_android.viewmodel.chat.state.NewMessageEffect
import com.phuongthanh.effiwork_android.viewmodel.chat.state.NewMessageUiState
import com.phuongthanh.effiwork_android.viewmodel.chat.NewMessageViewModel
import com.phuongthanh.effiwork_android.viewmodel.chat.state.ProjectMember
import com.phuongthanh.effiwork_android.viewmodel.chat.state.TaskSummary
import com.phuongthanh.effiwork_android.data.repository.AuthRepository
import com.phuongthanh.effiwork_android.data.model.chat.ChatConversationType
import com.phuongthanh.effiwork_android.data.model.response.TaskDetailResponse
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatConversationResponse
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatConversationMemberResponse
import androidx.compose.foundation.layout.size
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private const val TAB_MANUAL = 0
private const val TAB_BY_TASK = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupChatScreen(
    projectId: String,
    onBackClick: () -> Unit,
    onGroupCreated: () -> Unit,
    viewModel: NewMessageViewModel = hiltViewModel(),
    authRepository: AuthRepository
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var groupName by remember { mutableStateOf("") }
    var selectedMemberIds by remember { mutableStateOf(setOf<String>()) }
    var isCreating by remember { mutableStateOf(false) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(TAB_MANUAL) }
    var openSectionId by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedTaskPath by rememberSaveable { mutableStateOf(listOf<String>()) }
    var selectedTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    val currentUserId = authRepository.getCurrentUserId() ?: ""

    fun resetTaskNav() {
        openSectionId = null
        expandedTaskPath = emptyList()
        selectedTaskId = null
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            if (effect is NewMessageEffect.NavigateToChat) {
                delay(100)
                onGroupCreated()
            }
        }
    }

    LaunchedEffect(projectId) {
        viewModel.loadProjectData(projectId)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is NewMessageEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is NewMessageEffect.NavigateToChat -> { }
            }
        }
    }

    val successState = uiState as? NewMessageUiState.Success
    val canCreate = when (selectedTabIndex) {
        TAB_MANUAL -> selectedMemberIds.size >= 1 && !isCreating
        TAB_BY_TASK -> {
            selectedTaskId != null &&
                successState?.selectedTaskDetail != null &&
                !successState.isLoadingTaskDetail &&
                !isCreating
        }
        else -> false
    }

    CreateGroupChatScreenContent(
        uiState = uiState,
        groupName = groupName,
        selectedMemberIds = selectedMemberIds,
        currentUserId = currentUserId,
        selectedTabIndex = selectedTabIndex,
        openSectionId = openSectionId,
        expandedTaskPath = expandedTaskPath,
        selectedTaskId = selectedTaskId,
        onTabSelected = { index ->
            selectedTabIndex = index
            if (index == TAB_MANUAL) resetTaskNav()
        },
        onGroupNameChange = { groupName = it },
        onMemberToggle = { memberId ->
            selectedMemberIds = if (selectedMemberIds.contains(memberId)) {
                selectedMemberIds - memberId
            } else {
                selectedMemberIds + memberId
            }
        },
        onBackToSections = { resetTaskNav() },
        onBreadcrumbSectionClick = {
            expandedTaskPath = emptyList()
            selectedTaskId = null
        },
        onBreadcrumbTaskClick = { index ->
            expandedTaskPath = expandedTaskPath.take(index)
            selectedTaskId = null
            val newLast = expandedTaskPath.lastOrNull()
            if (newLast != null) {
                viewModel.loadSubtasksForParent(newLast)
            } else {
                val section = openSectionId
                if (section != null) {
                    viewModel.loadParentTasksForSection(section)
                }
            }
        },
        onSectionSelected = { sectionId ->
            openSectionId = sectionId
            expandedTaskPath = emptyList()
            selectedTaskId = null
            viewModel.loadParentTasksForSection(sectionId)
        },
        onTaskSelect = { taskId ->
            selectedTaskId = taskId
            viewModel.loadTaskDetail(taskId)
        },
        onTaskExpand = { taskId ->
            expandedTaskPath = expandedTaskPath + taskId
            selectedTaskId = null
            viewModel.loadSubtasksForParent(taskId)
        },
        onCreateClick = {
            if (!isCreating) {
                isCreating = true
                when (selectedTabIndex) {
                    TAB_MANUAL -> handleCreateClick(
                        context = context,
                        selectedMemberIds = selectedMemberIds,
                        currentUserId = currentUserId,
                        groupName = groupName,
                        projectId = projectId,
                        viewModel = viewModel,
                        uiState = uiState,
                        onSuccess = { isCreating = false },
                        onError = { isCreating = false }
                    )
                    TAB_BY_TASK -> {
                        val taskId = selectedTaskId
                        if (taskId == null) {
                            isCreating = false
                        } else {
                            viewModel.createGroupFromTask(
                                projectId = projectId,
                                taskId = taskId,
                                onComplete = { isCreating = false }
                            )
                        }
                    }
                }
            }
        },
        onBackClick = onBackClick,
        isCreating = isCreating,
        canCreate = canCreate
    )
}

private fun handleCreateClick(
    context: Context,
    selectedMemberIds: Set<String>,
    currentUserId: String,
    groupName: String,
    projectId: String,
    viewModel: NewMessageViewModel,
    uiState: NewMessageUiState,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    if (selectedMemberIds.contains(currentUserId)) {
        Toast.makeText(context, "Không thể chọn chính bạn", Toast.LENGTH_SHORT).show()
        onError()
        return
    }

    when (selectedMemberIds.size) {
        0 -> {
            Toast.makeText(context, "Cần chọn ít nhất 1 thành viên", Toast.LENGTH_SHORT).show()
            onError()
        }
        1 -> {
            val targetUserId = selectedMemberIds.first()
            val conversations = if (uiState is NewMessageUiState.Success) uiState.conversations else emptyList()
            val existingConv = conversations
                .filter { conv: ChatConversationResponse -> conv.type == ChatConversationType.PRIVATE }
                .find { conv: ChatConversationResponse ->
                    conv.members?.any { member: ChatConversationMemberResponse -> member.userId == targetUserId } == true
                }

            if (existingConv != null) {
                Toast.makeText(context, "Đã tồn tại cuộc trò chuyện với người này", Toast.LENGTH_SHORT).show()
                val otherName = existingConv.members
                    ?.find { member: ChatConversationMemberResponse -> member.userId != currentUserId }
                    ?.user
                    ?.fullName ?: "Chat"
                viewModel.openExistingConversation(existingConv.id, otherName)
            } else {
                Toast.makeText(context, "Tạo cuộc trò chuyện thành công", Toast.LENGTH_SHORT).show()
                viewModel.createPrivateConversationAndNavigate(
                    projectId = projectId,
                    targetUserId = targetUserId,
                    onComplete = { }
                )
            }
        }
        else -> {
            Toast.makeText(context, "Tạo cuộc trò chuyện thành công", Toast.LENGTH_SHORT).show()
            viewModel.createGroupConversation(
                projectId = projectId,
                name = groupName.takeIf { it.isNotBlank() },
                memberIds = selectedMemberIds.toList(),
                onComplete = { }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupChatScreenContent(
    uiState: NewMessageUiState,
    groupName: String,
    selectedMemberIds: Set<String>,
    currentUserId: String,
    selectedTabIndex: Int,
    openSectionId: String?,
    expandedTaskPath: List<String>,
    selectedTaskId: String?,
    onTabSelected: (Int) -> Unit,
    onGroupNameChange: (String) -> Unit,
    onMemberToggle: (String) -> Unit,
    onBackToSections: () -> Unit,
    onBreadcrumbSectionClick: () -> Unit,
    onBreadcrumbTaskClick: (Int) -> Unit,
    onSectionSelected: (String) -> Unit,
    onTaskSelect: (String) -> Unit,
    onTaskExpand: (String) -> Unit,
    onCreateClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCreating: Boolean = false,
    canCreate: Boolean = false
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo nhóm chat", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onCreateClick,
                        enabled = canCreate
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Blue500,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Tạo",
                                color = if (canCreate) Blue500 else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = Blue500
            ) {
                Tab(
                    selected = selectedTabIndex == TAB_MANUAL,
                    onClick = { onTabSelected(TAB_MANUAL) },
                    text = { Text("Chọn thủ công", fontWeight = FontWeight.Medium) }
                )
                Tab(
                    selected = selectedTabIndex == TAB_BY_TASK,
                    onClick = { onTabSelected(TAB_BY_TASK) },
                    text = { Text("Theo công việc", fontWeight = FontWeight.Medium) }
                )
            }

            when (selectedTabIndex) {
                TAB_MANUAL -> ManualTabContent(
                    uiState = uiState,
                    groupName = groupName,
                    selectedMemberIds = selectedMemberIds,
                    currentUserId = currentUserId,
                    onGroupNameChange = onGroupNameChange,
                    onMemberToggle = onMemberToggle
                )
                TAB_BY_TASK -> ByTaskTabContent(
                    uiState = uiState,
                    openSectionId = openSectionId,
                    expandedTaskPath = expandedTaskPath,
                    selectedTaskId = selectedTaskId,
                    onBackToSections = onBackToSections,
                    onBreadcrumbSectionClick = onBreadcrumbSectionClick,
                    onBreadcrumbTaskClick = onBreadcrumbTaskClick,
                    onSectionSelected = onSectionSelected,
                    onTaskSelect = onTaskSelect,
                    onTaskExpand = onTaskExpand
                )
            }
        }
    }
}

@Composable
private fun ManualTabContent(
    uiState: NewMessageUiState,
    groupName: String,
    selectedMemberIds: Set<String>,
    currentUserId: String,
    onGroupNameChange: (String) -> Unit,
    onMemberToggle: (String) -> Unit
) {
    val members = when (uiState) {
        is NewMessageUiState.Success -> uiState.members
        else -> emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = groupName,
            onValueChange = onGroupNameChange,
            label = { Text("Tên nhóm (tùy chọn)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Chọn thành viên (${selectedMemberIds.size} đã chọn)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (uiState) {
            is NewMessageUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Blue500)
                }
            }
            else -> {
                val filteredMembers = members.filter { it.id != currentUserId }
                if (filteredMembers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Không có thành viên nào để chọn", color = Color.Gray)
                        }
                    }
                } else {
                    LazyColumn {
                        items(
                            items = filteredMembers,
                            key = { it.id }
                        ) { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMemberToggle(member.id) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedMemberIds.contains(member.id),
                                    onCheckedChange = { onMemberToggle(member.id) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(member.avatarColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = member.fullName.take(2).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(member.fullName, fontWeight = FontWeight.Bold)
                                    Text(
                                        member.role,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ByTaskTabContent(
    uiState: NewMessageUiState,
    openSectionId: String?,
    expandedTaskPath: List<String>,
    selectedTaskId: String?,
    onBackToSections: () -> Unit,
    onBreadcrumbSectionClick: () -> Unit,
    onBreadcrumbTaskClick: (Int) -> Unit,
    onSectionSelected: (String) -> Unit,
    onTaskSelect: (String) -> Unit,
    onTaskExpand: (String) -> Unit
) {
    val success = uiState as? NewMessageUiState.Success
    val sections = success?.sections.orEmpty()
    val parentTasks = success?.currentParentTasks.orEmpty()
    val subtasks = success?.currentSubtasks.orEmpty()
    val isLoadingSections = success?.isLoadingSections == true || (uiState is NewMessageUiState.Loading && sections.isEmpty())
    val isLoadingParents = success?.isLoadingParentTasks == true
    val isLoadingSubs = success?.isLoadingSubtasks == true
    val selectedDetail = success?.selectedTaskDetail
    val isLoadingDetail = success?.isLoadingTaskDetail == true

    val currentSection = sections.firstOrNull { it.id == openSectionId }
    val pathTaskNames = expandedTaskPath.mapNotNull { id ->
        parentTasks.firstOrNull { it.id == id }?.name
            ?: subtasks.firstOrNull { it.id == id }?.name
    }

    val currentList: List<TaskSummary>
    val currentListIsLoading: Boolean
    val currentListTitle: String
    val currentListEmptyMessage: String

    when {
        openSectionId == null -> {
            currentList = emptyList()
            currentListIsLoading = isLoadingSections
            currentListTitle = "Chọn nhóm công việc"
            currentListEmptyMessage = "Chưa có nhóm công việc nào trong dự án"
        }
        expandedTaskPath.isEmpty() -> {
            currentList = parentTasks
            currentListIsLoading = isLoadingParents
            currentListTitle = "Công việc trong \"${currentSection?.name ?: ""}\""
            currentListEmptyMessage = "Nhóm này chưa có công việc nào"
        }
        else -> {
            currentList = subtasks
            currentListIsLoading = isLoadingSubs
            val parentName = pathTaskNames.lastOrNull() ?: ""
            currentListTitle = "Công việc con của \"$parentName\""
            currentListEmptyMessage = "Công việc này không có công việc con"
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (openSectionId != null) {
            Breadcrumb(
                sectionName = currentSection?.name,
                pathTaskNames = pathTaskNames,
                onClickAll = onBackToSections,
                onClickSection = onBreadcrumbSectionClick,
                onClickTask = onBreadcrumbTaskClick
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        when {
            openSectionId == null -> SectionsList(
                sections = sections,
                isLoading = isLoadingSections,
                onSectionSelected = onSectionSelected
            )
            else -> TaskList(
                title = currentListTitle,
                tasks = currentList,
                isLoading = currentListIsLoading,
                emptyMessage = currentListEmptyMessage,
                selectedTaskId = selectedTaskId,
                onTaskSelect = onTaskSelect,
                onTaskExpand = onTaskExpand
            )
        }

        if (selectedTaskId != null) {
            Spacer(modifier = Modifier.height(16.dp))
            TaskMemberPreview(
                detail = selectedDetail,
                isLoading = isLoadingDetail
            )
        }
    }
}

@Composable
private fun Breadcrumb(
    sectionName: String?,
    pathTaskNames: List<String>,
    onClickAll: () -> Unit,
    onClickSection: () -> Unit,
    onClickTask: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onClickAll,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
        ) {
            Text(
                text = "Tất cả nhóm",
                color = Blue500,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (sectionName != null) {
            Text(" > ", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            TextButton(
                onClick = onClickSection,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = sectionName,
                    color = Blue500,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        pathTaskNames.forEachIndexed { index, taskName ->
            Text(" > ", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            val isLast = index == pathTaskNames.lastIndex
            if (isLast) {
                Text(
                    text = taskName,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            } else {
                TextButton(
                    onClick = { onClickTask(index) },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = taskName,
                        color = Blue500,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionsList(
    sections: List<com.phuongthanh.effiwork_android.data.model.response.SectionResponse>,
    isLoading: Boolean,
    onSectionSelected: (String) -> Unit
) {
    Text(
        text = "Chọn nhóm công việc",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium
    )
    Spacer(modifier = Modifier.height(8.dp))

    when {
        isLoading -> CenteredSpinner()
        sections.isEmpty() -> EmptyState(
            icon = Icons.AutoMirrored.Filled.Assignment,
            message = "Chưa có nhóm công việc nào trong dự án"
        )
        else -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sections.forEach { section ->
                SectionRow(
                    name = section.name ?: "Không tên",
                    onClick = { onSectionSelected(section.id) }
                )
            }
        }
    }
}

@Composable
private fun SectionRow(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(width = 1.dp, color = Color(0xFFE0E0E0), shape = RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color.Gray
        )
    }
}

@Composable
private fun TaskList(
    title: String,
    tasks: List<TaskSummary>,
    isLoading: Boolean,
    emptyMessage: String,
    selectedTaskId: String?,
    onTaskSelect: (String) -> Unit,
    onTaskExpand: (String) -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium
    )
    Spacer(modifier = Modifier.height(8.dp))

    when {
        isLoading -> CenteredSpinner()
        tasks.isEmpty() -> EmptyState(
            icon = Icons.AutoMirrored.Filled.Assignment,
            message = emptyMessage
        )
        else -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tasks.forEach { task ->
                TaskListRow(
                    task = task,
                    isSelected = task.id == selectedTaskId,
                    onSelect = { onTaskSelect(task.id) },
                    onExpand = { onTaskExpand(task.id) }
                )
            }
        }
    }
}

@Composable
private fun TaskListRow(
    task: TaskSummary,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onExpand: () -> Unit
) {
    val borderColor = if (isSelected) Blue500 else Color(0xFFE0E0E0)
    val backgroundColor = if (isSelected) Blue500.copy(alpha = 0.08f) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(task.name, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Phụ trách: ${task.assigneeName ?: "Chưa có"} \n ${task.participantCount} người tham gia",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Blue500,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        IconButton(onClick = onExpand) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Xem công việc con",
                tint = Color.Gray
            )
        }
    }
}

@Composable
private fun CenteredSpinner() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Blue500)
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = Color.Gray)
        }
    }
}

@Composable
private fun TaskMemberPreview(
    detail: TaskDetailResponse?,
    isLoading: Boolean
) {
    Text(
        text = "Thành viên sẽ được thêm",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium
    )
    Spacer(modifier = Modifier.height(8.dp))

    when {
        isLoading || detail == null -> {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Blue500,
                    strokeWidth = 2.dp
                )
            }
        }
        else -> {
            val members = buildPreviewMembers(detail)
            if (members.isEmpty()) {
                Text(
                    text = "Công việc này chưa có người phụ trách hoặc người tham gia",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Column {
                    members.forEach { (name, role) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Blue500),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name.take(2).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(name, fontWeight = FontWeight.Medium)
                                if (role.isNotBlank()) {
                                    Text(
                                        text = role,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tên nhóm: ${detail.title ?: "Nhóm"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

private fun buildPreviewMembers(detail: TaskDetailResponse): List<Pair<String, String>> {
    val result = LinkedHashMap<String, Pair<String, String>>()
    val assignee = detail.assignee
    if (assignee != null && !assignee.id.isBlank()) {
        result[assignee.id] = (assignee.fullName ?: "Unknown") to "Phụ trách"
    }
    detail.participants?.forEach { p ->
        val user = p.user ?: return@forEach
        if (user.id.isBlank()) return@forEach
        if (result.containsKey(user.id)) return@forEach
        result[user.id] = (user.fullName ?: "Unknown") to "Tham gia"
    }
    return result.values.toList()
}

@Preview
@Composable
private fun CreateGroupChatScreenPreview() {
    MaterialTheme {
        CreateGroupChatScreenContent(
            uiState = NewMessageUiState.Success(
                conversations = emptyList(),
                groups = emptyList(),
                members = listOf(
                    ProjectMember("1", "Nguyễn Văn A", "a@example.com", "Developer", Color.Blue),
                    ProjectMember("2", "Trần Thị B", "b@example.com", "Designer", Color.Green),
                    ProjectMember("3", "Lê Văn C", "c@example.com", "Manager", Color.Red)
                ),
                sections = listOf(
                    com.phuongthanh.effiwork_android.data.model.response.SectionResponse(
                        id = "s1", name = "Backend", projectId = "p1", sortOrder = 1, createdAt = null
                    ),
                    com.phuongthanh.effiwork_android.data.model.response.SectionResponse(
                        id = "s2", name = "Frontend", projectId = "p1", sortOrder = 2, createdAt = null
                    )
                )
            ),
            groupName = "Nhóm Test",
            selectedMemberIds = setOf("1", "2"),
            currentUserId = "",
            selectedTabIndex = TAB_MANUAL,
            openSectionId = null,
            expandedTaskPath = emptyList(),
            selectedTaskId = null,
            onTabSelected = {},
            onGroupNameChange = {},
            onMemberToggle = {},
            onBackToSections = {},
            onBreadcrumbSectionClick = {},
            onBreadcrumbTaskClick = {},
            onSectionSelected = {},
            onTaskSelect = {},
            onTaskExpand = {},
            onCreateClick = {},
            onBackClick = {},
            isCreating = false,
            canCreate = true
        )
    }
}

@Preview
@Composable
private fun CreateGroupChatScreenPreviewByTaskLevel2() {
    MaterialTheme {
        CreateGroupChatScreenContent(
            uiState = NewMessageUiState.Success(
                sections = listOf(
                    com.phuongthanh.effiwork_android.data.model.response.SectionResponse(
                        id = "s1", name = "Backend", projectId = "p1", sortOrder = 1, createdAt = null
                    ),
                    com.phuongthanh.effiwork_android.data.model.response.SectionResponse(
                        id = "s2", name = "Frontend", projectId = "p1", sortOrder = 2, createdAt = null
                    )
                ),
                currentParentTasks = listOf(
                    TaskSummary(id = "t1", name = "Thiết kế API login", assigneeName = "Nguyễn Văn A", participantCount = 3),
                    TaskSummary(id = "t2", name = "Thiết kế API đăng ký", assigneeName = "Trần Thị B", participantCount = 2),
                    TaskSummary(id = "t3", name = "Setup database", assigneeName = "Lê Văn C", participantCount = 1)
                ),
                selectedTaskDetail = TaskDetailResponse(
                    id = "t1",
                    projectId = "p1",
                    title = "Thiết kế API login",
                    description = null,
                    groupId = "s1",
                    group = null,
                    parentTaskId = null,
                    status = "IN_PROGRESS",
                    assigneeId = "u1",
                    assignee = com.phuongthanh.effiwork_android.data.model.response.MemberInfo(
                        id = "u1", fullName = "Nguyễn Văn A", email = "a@example.com", avatarUrl = null
                    ),
                    creator = null,
                    startDate = "2026-06-01",
                    endDate = "2026-06-10",
                    reminderAt = null,
                    createdAt = null,
                    updatedAt = null,
                    participants = listOf(
                        com.phuongthanh.effiwork_android.data.model.response.TaskParticipantDetail(
                            user = com.phuongthanh.effiwork_android.data.model.response.MemberInfo(
                                id = "u2", fullName = "Trần Thị B", email = "b@example.com", avatarUrl = null
                            )
                        ),
                        com.phuongthanh.effiwork_android.data.model.response.TaskParticipantDetail(
                            user = com.phuongthanh.effiwork_android.data.model.response.MemberInfo(
                                id = "u3", fullName = "Lê Văn C", email = "c@example.com", avatarUrl = null
                            )
                        )
                    ),
                    attachments = null,
                    comments = null,
                    count = null
                )
            ),
            groupName = "",
            selectedMemberIds = emptySet(),
            currentUserId = "",
            selectedTabIndex = TAB_BY_TASK,
            openSectionId = "s1",
            expandedTaskPath = emptyList(),
            selectedTaskId = "t1",
            onTabSelected = {},
            onGroupNameChange = {},
            onMemberToggle = {},
            onBackToSections = {},
            onBreadcrumbSectionClick = {},
            onBreadcrumbTaskClick = {},
            onSectionSelected = {},
            onTaskSelect = {},
            onTaskExpand = {},
            onCreateClick = {},
            onBackClick = {},
            isCreating = false,
            canCreate = true
        )
    }
}
