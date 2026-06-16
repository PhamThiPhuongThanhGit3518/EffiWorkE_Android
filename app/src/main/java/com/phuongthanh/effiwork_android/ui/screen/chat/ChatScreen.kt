package com.phuongthanh.effiwork_android.ui.screen.chat

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.data.model.chat.ChatConversationType
import com.phuongthanh.effiwork_android.data.model.chat.ChatMessageType
import com.phuongthanh.effiwork_android.data.model.response.DocumentResponse
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatMessageResponse
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatUserResponse
import com.phuongthanh.effiwork_android.ui.screen.chat.item.ChatInputBar
import com.phuongthanh.effiwork_android.ui.screen.chat.item.ChatMessageItem
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import com.phuongthanh.effiwork_android.ui.theme.EffiWork_AndroidTheme
import com.phuongthanh.effiwork_android.viewmodel.chat.ChatAttachmentViewModel
import com.phuongthanh.effiwork_android.viewmodel.chat.state.ChatEffect
import com.phuongthanh.effiwork_android.viewmodel.chat.state.ChatUiState
import com.phuongthanh.effiwork_android.viewmodel.chat.ChatViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    projectId: String,
    conversationId: String,
    conversationName: String,
    currentUserId: String,
    onBackClick: () -> Unit,
    onDocumentPreviewClick: (String) -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val conversationDetail by viewModel.conversationDetail.collectAsStateWithLifecycle()
    val projectMembers by viewModel.projectMembers.collectAsStateWithLifecycle()
    val isGroupMutating by viewModel.isGroupMutating.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<DocumentResponse?>(null) }
    var showProjectPicker by remember { mutableStateOf(false) }
    var displayName by remember(conversationName) { mutableStateOf(conversationName) }
    var showGroupMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMembersDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    val attachmentViewModel: ChatAttachmentViewModel = hiltViewModel()
    val attachmentState by attachmentViewModel.uiState.collectAsStateWithLifecycle()

    val isGroup = conversationDetail?.type == ChatConversationType.GROUP
    val isCreator = isGroup && conversationDetail?.createdById == currentUserId

    val pickDeviceFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
            }
            attachmentViewModel.uploadDeviceFile(projectId, it)
        }
    }

    LaunchedEffect(projectId, conversationId) {
        android.util.Log.d("ScreenDebug", ">>> ChatScreen LaunchedEffect triggered: projectId=$projectId, conversationId=$conversationId")
        android.util.Log.d("ScreenDebug", "  calling loadMessages")
        viewModel.loadMessages(projectId, conversationId, refresh = true)
        android.util.Log.d("ScreenDebug", "  calling joinConversation")
        viewModel.joinConversation(projectId, conversationId)
        android.util.Log.d("ScreenDebug", "  calling markAsRead")
        viewModel.markAsRead()
        android.util.Log.d("ScreenDebug", "  calling loadConversationDetail")
        viewModel.loadConversationDetail()
        android.util.Log.d("ScreenDebug", "  calling loadProjectMembers")
        viewModel.loadProjectMembers()
        android.util.Log.d("ScreenDebug", "  LaunchedEffect completed")
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ChatEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ChatEffect.MessageSent -> {
                    messageText = ""
                    selectedFile = null
                }
                is ChatEffect.ScrollToBottom -> {
                    val currentState = uiState
                    if (currentState is ChatUiState.Success && currentState.messages.isNotEmpty()) {
                        listState.animateScrollToItem(currentState.messages.size - 1)
                    }
                }
                is ChatEffect.NewMessageReceived -> {}
                is ChatEffect.GroupNameUpdated -> {
                    displayName = effect.newName
                }
                is ChatEffect.LeftGroup -> {
                    onBackClick()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        attachmentViewModel.effect.collect { effect ->
            when (effect) {
                is ChatAttachmentViewModel.Effect.UploadComplete -> {
                    selectedFile = effect.document
                }
                is ChatAttachmentViewModel.Effect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState !is ChatUiState.Loading) {
            isRefreshing = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.leaveConversation()
        }
    }

    ChatScreenContent(
        uiState = uiState,
        conversationName = displayName,
        isGroup = isGroup,
        isCreator = isCreator,
        showGroupMenu = showGroupMenu,
        onGroupMenuClick = { showGroupMenu = true },
        onGroupMenuDismiss = { showGroupMenu = false },
        onRenameClick = {
            showGroupMenu = false
            showRenameDialog = true
        },
        onEditMembersClick = {
            showGroupMenu = false
            viewModel.loadProjectMembers()
            showMembersDialog = true
        },
        onLeaveClick = {
            showGroupMenu = false
            showLeaveDialog = true
        },
        currentUserId = currentUserId,
        projectId = projectId,
        messageText = messageText,
        isRefreshing = isRefreshing,
        isUploadingFile = attachmentState.isUploading,
        selectedFile = selectedFile,
        listState = listState,
        onMessageTextChange = { messageText = it },
        onSendClick = {
            val file = selectedFile
            when {
                file != null -> {
                    val type = if (file.mimeType?.startsWith("image/") == true)
                        ChatMessageType.IMAGE else ChatMessageType.FILE
                    viewModel.sendMessage(
                        content = messageText.trim(),
                        type = type,
                        documentId = file.id
                    )
                }
                messageText.isNotBlank() -> {
                    viewModel.sendMessage(messageText.trim(), ChatMessageType.TEXT)
                }
            }
        },
        onProjectFileClick = { showProjectPicker = true },
        onDeviceFileClick = { pickDeviceFileLauncher.launch(arrayOf("*/*")) },
        onClearFile = { selectedFile = null },
        onDocumentClick = { documentId ->
            onDocumentPreviewClick(documentId)
        },
        onRefresh = {
            isRefreshing = true
            viewModel.refresh()
        },
        onBackClick = onBackClick
    )

    if (showProjectPicker) {
        ChatDocumentPickerDialog(
            projectId = projectId,
            onDismiss = { showProjectPicker = false },
            onSelect = { document ->
                selectedFile = document
                showProjectPicker = false
            }
        )
    }

    if (showRenameDialog) {
        RenameGroupDialog(
            currentName = displayName,
            isLoading = isGroupMutating,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                showRenameDialog = false
                viewModel.renameGroup(newName)
            }
        )
    }

    if (showMembersDialog) {
        EditMembersDialog(
            projectMembers = projectMembers,
            currentMemberIds = conversationDetail?.members
                ?.map { it.userId }
                ?.toSet()
                ?: emptySet(),
            isLoading = isGroupMutating,
            currentUserId = currentUserId,
            creatorId = conversationDetail?.createdById,
            onDismiss = { showMembersDialog = false },
            onSave = { toAdd, toRemove ->
                showMembersDialog = false
                if (toAdd.isNotEmpty()) viewModel.addMembers(toAdd)
                if (toRemove.isNotEmpty()) viewModel.removeMembers(toRemove)
            }
        )
    }

    if (showLeaveDialog) {
        LeaveGroupConfirmDialog(
            groupName = displayName,
            isCreator = isCreator,
            isLoading = isGroupMutating,
            onDismiss = { showLeaveDialog = false },
            onConfirm = {
                showLeaveDialog = false
                viewModel.leaveGroup()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenContent(
    uiState: ChatUiState,
    conversationName: String,
    isGroup: Boolean,
    isCreator: Boolean,
    showGroupMenu: Boolean,
    onGroupMenuClick: () -> Unit,
    onGroupMenuDismiss: () -> Unit,
    onRenameClick: () -> Unit,
    onEditMembersClick: () -> Unit,
    onLeaveClick: () -> Unit,
    currentUserId: String,
    projectId: String,
    messageText: String,
    isRefreshing: Boolean,
    isUploadingFile: Boolean,
    selectedFile: DocumentResponse?,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onProjectFileClick: () -> Unit,
    onDeviceFileClick: () -> Unit,
    onClearFile: () -> Unit,
    onDocumentClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = if (isGroup) {
                            Modifier.clickable(onClick = onGroupMenuClick)
                        } else Modifier
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isGroup) Blue500.copy(alpha = 0.15f) else Color(0xFFE0E0E0)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isGroup) Icons.Default.Group else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isGroup) Blue500 else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = conversationName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        if (isGroup) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isGroup) {
                        Box {
                            IconButton(onClick = onGroupMenuClick) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn nhóm")
                            }
                            DropdownMenu(
                                expanded = showGroupMenu,
                                onDismissRequest = onGroupMenuDismiss
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Đổi tên nhóm") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                    },
                                    onClick = onRenameClick
                                )
                                if (isCreator) {
                                    DropdownMenuItem(
                                        text = { Text("Chỉnh sửa thành viên") },
                                        leadingIcon = {
                                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                                        },
                                        onClick = onEditMembersClick
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Rời khỏi nhóm",
                                            color = Color.Red
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = null,
                                            tint = Color.Red
                                        )
                                    },
                                    onClick = onLeaveClick
                                )
                            }
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        bottomBar = {
            Column {
                if (selectedFile != null || isUploadingFile) {
                    SelectedFileChip(
                        file = selectedFile,
                        isUploading = isUploadingFile,
                        onClear = onClearFile
                    )
                }
                ChatInputBar(
                    messageText = messageText,
                    onMessageChange = onMessageTextChange,
                    onSendClick = onSendClick,
                    onProjectFileClick = onProjectFileClick,
                    onDeviceFileClick = onDeviceFileClick,
                    canSend = messageText.isNotBlank() || selectedFile != null,
                    isUploading = isUploadingFile
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5))
        ) {
            when (uiState) {
                is ChatUiState.Idle -> {
                    if (!isRefreshing) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Blue500)
                        }
                    }
                }
                is ChatUiState.Loading -> {
                    if (!isRefreshing) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Blue500)
                        }
                    }
                }
                is ChatUiState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (uiState.messages.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("No messages yet. Say hi!", color = Color.Gray)
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = uiState.messages,
                                    key = { it.id }
                                ) { message ->
                                    ChatMessageItem(
                                        message = message,
                                        isOwnMessage = message.senderId == currentUserId,
                                        projectId = projectId,
                                        onDocumentClick = onDocumentClick
                                    )
                                }
                            }
                        }
                    }
                }
                is ChatUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(uiState.message, color = Color.Red)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onRefresh) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedFileChip(
    file: DocumentResponse?,
    isUploading: Boolean,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFE3F2FD)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF1565C0)
                )
            } else {
                Icon(
                    Icons.Default.Description,
                    null,
                    tint = Color(0xFF1565C0),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isUploading) "Đang tải lên..." else file?.fileName ?: "",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1565C0),
                    maxLines = 1
                )
                if (file != null && !isUploading) {
                    Text(
                        text = file.mimeType ?: "Tài liệu",
                        fontSize = 11.sp,
                        color = Color(0xFF1565C0)
                    )
                }
            }
            IconButton(
                onClick = onClear,
                enabled = !isUploading,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    "Bỏ chọn",
                    tint = Color(0xFF1565C0),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Preview()
@Composable
private fun ChatScreenPreview() {
    MaterialTheme {
        val mockMessages = listOf(
            ChatMessageResponse(
                id = "1",
                conversationId = "1",
                senderId = "2",
                type = ChatMessageType.TEXT,
                content = "Xin chào bạn!",
                documentId = null,
                createdAt = "2024-01-15T10:30:00",
                updatedAt = null,
                deletedAt = null,
                sender = ChatUserResponse("2", "Trần Thị B", "b@example.com", null),
                document = null
            ),
            ChatMessageResponse(
                id = "2",
                conversationId = "1",
                senderId = "1",
                type = ChatMessageType.TEXT,
                content = "Chào bạn! Rất vui được trò chuyện.",
                documentId = null,
                createdAt = "2024-01-15T10:32:00",
                updatedAt = null,
                deletedAt = null,
                sender = ChatUserResponse("1", "Nguyễn Văn A", "a@example.com", null),
                document = null
            )
        )

        ChatScreenContent(
            uiState = ChatUiState.Success(
                messages = mockMessages,
                page = 1,
                totalPages = 1
            ),
            conversationName = "Nhóm A",
            isGroup = true,
            isCreator = true,
            showGroupMenu = false,
            onGroupMenuClick = {},
            onGroupMenuDismiss = {},
            onRenameClick = {},
            onEditMembersClick = {},
            onLeaveClick = {},
            currentUserId = "1",
            projectId = "p1",
            messageText = "",
            isRefreshing = false,
            isUploadingFile = false,
            selectedFile = null,
            listState = rememberLazyListState(),
            onMessageTextChange = {},
            onSendClick = {},
            onProjectFileClick = {},
            onDeviceFileClick = {},
            onClearFile = {},
            onDocumentClick = {},
            onRefresh = {},
            onBackClick = {}
        )
    }
}

@Composable
private fun RenameGroupDialog(
    currentName: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember(currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đổi tên nhóm") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Tên nhóm") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = !isLoading && text.trim().isNotEmpty() && text.trim() != currentName
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Lưu")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Hủy")
            }
        }
    )
}

@Composable
private fun EditMembersDialog(
    projectMembers: List<com.phuongthanh.effiwork_android.data.model.response.ProjectMemberResponse>,
    currentMemberIds: Set<String>,
    isLoading: Boolean,
    currentUserId: String,
    creatorId: String?,
    onDismiss: () -> Unit,
    onSave: (toAdd: List<String>, toRemove: List<String>) -> Unit
) {
    var selected by remember(currentMemberIds) { mutableStateOf(currentMemberIds) }
    val initial = currentMemberIds

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa thành viên") },
        text = {
            if (projectMembers.isEmpty()) {
                Text("Chưa tải được danh sách thành viên dự án.")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    projectMembers.forEach { member ->
                        val uid = member.userId
                        if (uid == currentUserId) return@forEach
                        val isChecked = selected.contains(uid)
                        val isOriginalMember = initial.contains(uid)
                        val isCreatorRow = uid == creatorId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = if (isChecked) selected - uid else selected + uid
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected + uid else selected - uid
                                },
                                enabled = !isCreatorRow
                            )
                            Spacer(Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Blue500),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (member.user?.fullName ?: "?").take(2).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.user?.fullName ?: "Không tên",
                                    fontWeight = FontWeight.Medium
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = member.role,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    if (isCreatorRow) {
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Blue500.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "Trưởng nhóm",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Blue500,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (isOriginalMember) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Đã tham gia",
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
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val toAdd = (selected - initial).toList()
                    val toRemove = (initial - selected).toList()
                    onSave(toAdd, toRemove)
                },
                enabled = !isLoading && (selected != initial)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Lưu")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Hủy")
            }
        }
    )
}

@Composable
private fun LeaveGroupConfirmDialog(
    groupName: String,
    isCreator: Boolean,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rời khỏi nhóm?") },
        text = {
            Text(
                if (isCreator) {
                    "Bạn là người tạo nhóm. Khi rời, quyền trưởng nhóm sẽ được chuyển cho thành viên cũ nhất. Hành động không thể hoàn tác."
                } else {
                    "Bạn sẽ rời khỏi nhóm \"$groupName\". Bạn sẽ không nhận được tin nhắn mới từ nhóm này nữa."
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.Red
                    )
                } else {
                    Text("Rời nhóm", color = Color.Red)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Hủy")
            }
        }
    )
}

@Composable
private fun GroupMenuItems(
    isCreator: Boolean,
    onRenameClick: () -> Unit,
    onEditMembersClick: () -> Unit,
    onLeaveClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text("Đổi tên nhóm") },
        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
        onClick = onRenameClick
    )
    if (isCreator) {
        DropdownMenuItem(
            text = { Text("Chỉnh sửa thành viên") },
            leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
            onClick = onEditMembersClick
        )
    }
    DropdownMenuItem(
        text = { Text("Rời khỏi nhóm", color = Color.Red) },
        leadingIcon = {
            Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = Color.Red
            )
        },
        onClick = onLeaveClick
    )
}

@Preview(showBackground = true, name = "Group menu (creator)")
@Composable
private fun GroupMenuCreatorPreview() {
    EffiWork_AndroidTheme {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            DropdownMenu(expanded = true, onDismissRequest = {}) {
                GroupMenuItems(
                    isCreator = true,
                    onRenameClick = {},
                    onEditMembersClick = {},
                    onLeaveClick = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Group menu (member)")
@Composable
private fun GroupMenuMemberPreview() {
    EffiWork_AndroidTheme {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            DropdownMenu(expanded = true, onDismissRequest = {}) {
                GroupMenuItems(
                    isCreator = false,
                    onRenameClick = {},
                    onEditMembersClick = {},
                    onLeaveClick = {}
                )
            }
        }
    }
}
