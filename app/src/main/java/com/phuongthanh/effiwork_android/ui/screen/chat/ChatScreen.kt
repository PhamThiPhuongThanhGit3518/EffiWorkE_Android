package com.phuongthanh.effiwork_android.ui.screen.chat

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.data.model.chat.ChatMessageType
import com.phuongthanh.effiwork_android.data.model.response.DocumentResponse
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatMessageResponse
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatUserResponse
import com.phuongthanh.effiwork_android.ui.screen.chat.item.ChatInputBar
import com.phuongthanh.effiwork_android.ui.screen.chat.item.ChatMessageItem
import com.phuongthanh.effiwork_android.ui.theme.Blue500
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
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<DocumentResponse?>(null) }
    var showProjectPicker by remember { mutableStateOf(false) }
    val attachmentViewModel: ChatAttachmentViewModel = hiltViewModel()
    val attachmentState by attachmentViewModel.uiState.collectAsStateWithLifecycle()

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
        conversationName = conversationName,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenContent(
    uiState: ChatUiState,
    conversationName: String,
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
                    Text(
                        text = conversationName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
