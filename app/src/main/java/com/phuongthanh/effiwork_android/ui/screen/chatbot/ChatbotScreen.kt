package com.phuongthanh.effiwork_android.ui.screen.chatbot

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.viewmodel.chatbot.ChatbotEffect
import com.phuongthanh.effiwork_android.viewmodel.chatbot.ChatbotMessageUi
import com.phuongthanh.effiwork_android.viewmodel.chatbot.ChatbotRole
import com.phuongthanh.effiwork_android.viewmodel.chatbot.ChatbotUiState
import com.phuongthanh.effiwork_android.viewmodel.chatbot.ChatbotViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(
    projectId: String,
    onBackClick: () -> Unit = {},
    viewModel: ChatbotViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Log.d("ChatbotScreen", "recompose uiState=${uiState::class.simpleName} msgsCount=${(uiState as? ChatbotUiState.Success)?.messages?.size} streaming=${(uiState as? ChatbotUiState.Success)?.isStreaming}")
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var showResetConfirm by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(projectId) {
        Log.d("ChatbotScreen", "LaunchedEffect loadHistory projectId=$projectId")
        viewModel.loadHistory(projectId, refresh = true)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ChatbotEffect.ScrollToBottom -> {
                    val total = (uiState as? ChatbotUiState.Success)?.messages?.size ?: 0
                    if (total > 0) listState.animateScrollToItem(total - 1)
                }
                is ChatbotEffect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ChatbotEffect.ResetCompleted -> {
                    Toast.makeText(context, "Đã xóa hội thoại", Toast.LENGTH_SHORT).show()
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
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Trợ lý ảo",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadHistory(projectId, refresh = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Tải lại")
                    }
                    IconButton(onClick = { showResetConfirm = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Xóa hội thoại")
                    }
                }
            )
        },
        bottomBar = {
            ChatbotInputBar(
                text = input,
                onTextChange = { input = it },
                isStreaming = (uiState as? ChatbotUiState.Success)?.isStreaming == true,
                onSend = {
                    val text = input
                    input = ""
                    viewModel.sendMessage(projectId, text)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F7FB))
        ) {
            when (val state = uiState) {
                is ChatbotUiState.Idle, is ChatbotUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ChatbotUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message, color = Color.Red)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadHistory(projectId, refresh = true) }) {
                            Text("Thử lại")
                        }
                    }
                }
                is ChatbotUiState.Success -> {
                    if (state.messages.isEmpty()) {
                        EmptyHint()
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items = state.messages, key = { it.id }) { message ->
                                MessageBubble(message = message)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Xóa hội thoại?") },
            text = { Text("Toàn bộ tin nhắn trong hội thoại này sẽ bị xóa. Hành động không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    viewModel.reset(projectId)
                }) { Text("Xóa", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
private fun MessageBubble(message: ChatbotMessageUi) {
    val isUser = message.role == ChatbotRole.USER
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primary else Color(0xFFE5E7EB)
    val textColor = if (isUser) Color.White else Color(0xFF1F2937)
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleShape = if (isUser) {
        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    } else {
        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (message.isStreaming && message.content.isEmpty()) {
                    ThinkingDots(color = textColor)
                } else {
                    Text(
                        text = if (message.isStreaming) message.content + "▍" else message.content,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinkingDots(color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color.copy(alpha = 0.5f))
            )
            if (it < 2) Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
private fun EmptyHint() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.SmartToy,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Hỏi tôi về công việc, cuộc họp, thành viên trong dự án hiện tại.",
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ChatbotInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    isStreaming: Boolean,
    onSend: () -> Unit
) {
    Surface(
        shadowElevation = 6.dp,
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Hỏi trợ lý ảo...") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                enabled = !isStreaming,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                )
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank() && !isStreaming,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (text.isNotBlank() && !isStreaming) MaterialTheme.colorScheme.primary
                        else Color(0xFFE5E7EB)
                    )
            ) {
                if (isStreaming) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Gửi",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
