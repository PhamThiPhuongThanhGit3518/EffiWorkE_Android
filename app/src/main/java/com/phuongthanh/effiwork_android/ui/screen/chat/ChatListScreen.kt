package com.phuongthanh.effiwork_android.ui.screen.chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.data.model.chat.ChatConversationType
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatConversationResponse
import com.phuongthanh.effiwork_android.ui.screen.chat.item.previewText
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import com.phuongthanh.effiwork_android.viewmodel.chat.state.NewMessageEffect
import com.phuongthanh.effiwork_android.viewmodel.chat.state.NewMessageUiState
import com.phuongthanh.effiwork_android.viewmodel.chat.NewMessageViewModel
import com.phuongthanh.effiwork_android.viewmodel.chat.state.PrivateChatItem
import com.phuongthanh.effiwork_android.viewmodel.chat.state.ProjectGroup
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    projectId: String,
    onBackClick: () -> Unit,
    onNavigateToChat: (projectId: String, conversationId: String, conversationName: String) -> Unit,
    onCreateGroupClick: () -> Unit,
    viewModel: NewMessageViewModel = hiltViewModel(),
    authRepository: com.phuongthanh.effiwork_android.data.repository.AuthRepository
) {
    android.util.Log.d("ChatListScreenDebug", ">>> ChatListScreen composed, projectId=$projectId")
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }

    val currentUserId = authRepository.getCurrentUserId() ?: ""

    LaunchedEffect(projectId) {
        android.util.Log.d("ChatListScreenDebug", ">>> LaunchedEffect triggered for projectId=$projectId")
        viewModel.onScreenVisible(projectId)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is NewMessageEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is NewMessageEffect.NavigateToChat -> {
                    onNavigateToChat(effect.projectId, effect.conversationId, effect.conversationName)
                }
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState !is NewMessageUiState.Loading) {
            isRefreshing = false
        }
    }

    ChatListScreenContent(
        uiState = uiState,
        selectedTab = selectedTab,
        searchQuery = searchQuery,
        isRefreshing = isRefreshing,
        currentUserId = currentUserId,
        onSelectedTabChange = { selectedTab = it },
        onSearchQueryChange = { searchQuery = it },
        onRefresh = {
            isRefreshing = true
            viewModel.refresh(projectId)
        },
        onConversationClick = { viewModel.openConversation(it) },
        onGroupClick = { viewModel.openGroupConversation(it) },
        onPrivateChatClick = { viewModel.openPrivateConversation(it) },
        onBackClick = onBackClick,
        onCreateGroupClick = onCreateGroupClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreenContent(
    uiState: NewMessageUiState,
    selectedTab: Int,
    searchQuery: String,
    isRefreshing: Boolean,
    currentUserId: String,
    onSelectedTabChange: (Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onConversationClick: (ChatConversationResponse) -> Unit,
    onGroupClick: (ProjectGroup) -> Unit,
    onPrivateChatClick: (PrivateChatItem) -> Unit,
    onBackClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tin nhắn", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onCreateGroupClick) {
                        Icon(Icons.Default.Add, contentDescription = "Create group")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFF0F0F0))
                        .padding(4.dp)
                ) {
                    Row {
                        TabItem(
                            text = "Tin nhắn",
                            selected = selectedTab == 0,
                            onClick = { onSelectedTabChange(0) }
                        )
                        TabItem(
                            text = "Nhóm",
                            selected = selectedTab == 1,
                            onClick = { onSelectedTabChange(1) }
                        )
                        TabItem(
                            text = "Cá nhân",
                            selected = selectedTab == 2,
                            onClick = { onSelectedTabChange(2) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Tìm kiếm") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                when (uiState) {
                    is NewMessageUiState.Loading -> {
                        if (!isRefreshing) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Blue500)
                            }
                        }
                    }
                    is NewMessageUiState.Success -> {
                        when (selectedTab) {
                            0 -> ConversationList(
                                conversations = filterConversations(uiState.conversations, searchQuery),
                                currentUserId = currentUserId,
                                onConversationClick = onConversationClick
                            )
                            1 -> GroupList(
                                groups = uiState.groups.filter {
                                    searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
                                },
                                onGroupClick = onGroupClick
                            )
                            2 -> PrivateChatList(
                                privateChats = uiState.privateChats.filter {
                                    searchQuery.isBlank() || it.displayName.contains(searchQuery, ignoreCase = true)
                                },
                                onPrivateChatClick = onPrivateChatClick
                            )
                        }
                    }
                    is NewMessageUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(uiState.message, color = Color.Red)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun filterConversations(conversations: List<ChatConversationResponse>, query: String): List<ChatConversationResponse> {
    if (query.isBlank()) return conversations
    return conversations.filter { conv ->
        conv.name?.contains(query, ignoreCase = true) == true ||
        conv.members?.any { it.user?.fullName?.contains(query, ignoreCase = true) == true } == true
    }
}

@Composable
private fun TabItem(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Blue500 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.Gray,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ConversationList(
    conversations: List<ChatConversationResponse>,
    currentUserId: String,
    onConversationClick: (ChatConversationResponse) -> Unit
) {
    if (conversations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Chat,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Chưa có cuộc trò chuyện nào", color = Color.Gray)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(conversations, key = { it.id }) { conversation ->
                ConversationItem(
                    conversation = conversation,
                    currentUserId = currentUserId,
                    onClick = { onConversationClick(conversation) }
                )
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: ChatConversationResponse,
    currentUserId: String,
    onClick: () -> Unit
) {
    val displayName = when {
        conversation.name != null -> conversation.name
        conversation.type == ChatConversationType.PRIVATE -> {
            // For PRIVATE, get the other member (not current user)
            conversation.members?.find { it.userId != currentUserId }?.user?.fullName
        }
        else -> conversation.members?.firstOrNull()?.user?.fullName
    } ?: "Unknown"
    val lastMessage = conversation.messages?.firstOrNull()
    val unreadCount = conversation.unreadCount

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Blue500.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (conversation.type == ChatConversationType.GROUP)
                        Icons.Default.Group else Icons.Default.Person,
                    contentDescription = null,
                    tint = Blue500,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = Blue500) {
                            Text(if (unreadCount > 99) "99+" else unreadCount.toString())
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastMessage?.previewText()?.takeIf { it.isNotEmpty() } ?: "No messages yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GroupList(
    groups: List<ProjectGroup>,
    onGroupClick: (ProjectGroup) -> Unit
) {
    if (groups.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Không có nhóm nào", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(groups) { group ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGroupClick(group) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8E0F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                tint = Color(0xFF9C7BB8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(group.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(group.description, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivateChatList(
    privateChats: List<PrivateChatItem>,
    onPrivateChatClick: (PrivateChatItem) -> Unit
) {
    if (privateChats.isEmpty()) {
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
                Text("Chưa có cuộc trò chuyện cá nhân", color = Color.Gray)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(privateChats) { privateChat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPrivateChatClick(privateChat) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(privateChat.avatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = privateChat.displayName.take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(privateChat.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            privateChat.email?.let { email ->
                                Text(email, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                            privateChat.lastMessage?.let { lastMessage ->
                                Text(lastMessage, color = Color.Gray, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (privateChat.unreadCount > 0) {
                            Badge(containerColor = Blue500) {
                                Text(if (privateChat.unreadCount > 99) "99+" else privateChat.unreadCount.toString())
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
private fun ChatListScreenPreview() {
    MaterialTheme {
        ChatListScreenContent(
            uiState = NewMessageUiState.Success(
                conversations = emptyList(),
                groups = emptyList(),
                privateChats = listOf(
                    PrivateChatItem("1", "Nguyễn Văn A", Color.Blue, 1, "Hello", null, "a@example.com"),
                    PrivateChatItem("2", "Trần Thị B", Color.Green, 0, null, null, "b@example.com")
                )
            ),
            selectedTab = 0,
            searchQuery = "",
            isRefreshing = false,
            currentUserId = "",
            onSelectedTabChange = {},
            onSearchQueryChange = {},
            onRefresh = {},
            onConversationClick = {},
            onGroupClick = {},
            onPrivateChatClick = {},
            onBackClick = {},
            onCreateGroupClick = {}
        )
    }
}