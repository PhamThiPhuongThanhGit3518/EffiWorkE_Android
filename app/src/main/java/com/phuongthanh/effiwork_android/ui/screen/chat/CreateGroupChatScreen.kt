package com.phuongthanh.effiwork_android.ui.screen.chat

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.phuongthanh.effiwork_android.data.repository.AuthRepository
import com.phuongthanh.effiwork_android.data.model.chat.ChatConversationType
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatConversationResponse
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatConversationMemberResponse
import androidx.compose.foundation.layout.size
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

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
    val currentUserId = authRepository.getCurrentUserId() ?: ""

    // IMPROVEMENT #5: Đợi effect consumed
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            if (effect is NewMessageEffect.NavigateToChat) {
                delay(100) // Small delay to ensure navigation is processed
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
                is NewMessageEffect.NavigateToChat -> {
                    // Navigation handled by the other LaunchedEffect above
                }
            }
        }
    }

    CreateGroupChatScreenContent(
        uiState = uiState,
        groupName = groupName,
        selectedMemberIds = selectedMemberIds,
        currentUserId = currentUserId,
        onGroupNameChange = { groupName = it },
        onMemberToggle = { memberId ->
            selectedMemberIds = if (selectedMemberIds.contains(memberId)) {
                selectedMemberIds - memberId
            } else {
                selectedMemberIds + memberId
            }
        },
        onCreateClick = {
            // IMPROVEMENT #1: Đưa if (!isCreating) ra ngoài when
            if (!isCreating) {
                isCreating = true
                handleCreateClick(
                    context = context,
                    selectedMemberIds = selectedMemberIds,
                    currentUserId = currentUserId,
                    groupName = groupName,
                    projectId = projectId,
                    viewModel = viewModel,
                    uiState = uiState,
                    onSuccess = {
                        isCreating = false
                        // IMPROVEMENT #5: Navigation được xử lý qua LaunchedEffect
                    },
                    onError = {
                        isCreating = false
                    }
                )
            }
        },
        onBackClick = onBackClick,
        isCreating = isCreating
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
    // IMPROVEMENT #8: Validate - không cho phép chọn chính mình
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
            // SINGLE USER: Kiểm tra existing hoặc tạo mới
            val targetUserId = selectedMemberIds.first()

            // Tìm existing PRIVATE conversation
            val conversations = if (uiState is NewMessageUiState.Success) uiState.conversations else emptyList()
            val existingConv = conversations
                .filter { conv: ChatConversationResponse -> conv.type == ChatConversationType.PRIVATE }
                .find { conv: ChatConversationResponse ->
                    conv.members?.any { member: ChatConversationMemberResponse -> member.userId == targetUserId } == true
                }

            if (existingConv != null) {
                // Đã có → Mở conversation
                Toast.makeText(context, "Đã tồn tại cuộc trò chuyện với người này", Toast.LENGTH_SHORT).show()
                val otherName = existingConv.members
                    ?.find { member: ChatConversationMemberResponse -> member.userId != currentUserId }
                    ?.user
                    ?.fullName ?: "Chat"
                viewModel.openExistingConversation(existingConv.id, otherName)
                // IMPROVEMENT #5: KHÔNG gọi onSuccess() ở đây - sẽ được gọi qua LaunchedEffect
            } else {
                // IMPROVEMENT #3: Chưa có → Tạo mới với Loading state
                Toast.makeText(context, "Tạo cuộc trò chuyện thành công", Toast.LENGTH_SHORT).show()
                viewModel.createPrivateConversationAndNavigate(
                    projectId = projectId,
                    targetUserId = targetUserId,
                    onComplete = {
                        // IMPROVEMENT #5: KHÔNG gọi onSuccess() ở đây - sẽ được gọi qua LaunchedEffect
                    }
                )
            }
        }
        else -> {
            // >= 2 members → Tạo GROUP
            Toast.makeText(context, "Tạo cuộc trò chuyện thành công", Toast.LENGTH_SHORT).show()
            viewModel.createGroupConversation(
                projectId = projectId,
                name = groupName.takeIf { it.isNotBlank() },
                memberIds = selectedMemberIds.toList(),
                onComplete = {
                    // IMPROVEMENT #5: KHÔNG gọi onSuccess() ở đây - sẽ được gọi qua LaunchedEffect
                }
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
    onGroupNameChange: (String) -> Unit,
    onMemberToggle: (String) -> Unit,
    onCreateClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCreating: Boolean = false
) {
    val members = when (uiState) {
        is NewMessageUiState.Success -> uiState.members
        else -> emptyList()
    }

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
                        enabled = selectedMemberIds.size >= 1 && !isCreating
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
                                color = if (selectedMemberIds.size >= 1) Blue500 else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
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
                        // IMPROVEMENT #6: Empty state
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
                )
            ),
            groupName = "Nhóm Test",
            selectedMemberIds = setOf("1", "2"),
            currentUserId = "",
            onGroupNameChange = {},
            onMemberToggle = {},
            onCreateClick = {},
            onBackClick = {},
            isCreating = false
        )
    }
}