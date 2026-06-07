package com.phuongthanh.effiwork_android.ui.screen.notis

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.data.model.response.NotificationData
import com.phuongthanh.effiwork_android.data.model.response.NotificationResponse
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import com.phuongthanh.effiwork_android.viewmodel.notis.NotificationEffect
import com.phuongthanh.effiwork_android.viewmodel.notis.NotificationUiState
import com.phuongthanh.effiwork_android.viewmodel.notis.NotificationViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    modifier: Modifier = Modifier,
    onNavigateToTaskDetail: (String, String) -> Unit = { _, _ -> },
    onNavigateToMeetingDetail: (String, String) -> Unit = { _, _ -> },
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unreadOnly by viewModel.unreadOnly.collectAsStateWithLifecycle()
    val projectNameMap by viewModel.projectNameMap.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.loadNotifications(refresh = true)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is NotificationEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is NotificationEffect.RefreshList -> {
                    viewModel.loadNotifications(refresh = true)
                }
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= 0) {
                    val state = uiState
                    if (state is NotificationUiState.Success && lastIndex >= state.notifications.size - 3) {
                        viewModel.loadMoreNotifications()
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = "Thông báo",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleUnreadFilter() }) {
                        Icon(
                            imageVector = if (unreadOnly) Icons.Default.FilterAlt else Icons.Default.FilterAltOff,
                            contentDescription = "Lọc chưa đọc",
                            tint = if (unreadOnly) Blue500 else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.markAllAsRead() }) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Đánh dấu tất cả đã đọc"
                        )
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Làm mới"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5))
        ) {
            when (val state = uiState) {
                is NotificationUiState.Idle, is NotificationUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Blue500)
                    }
                }
                is NotificationUiState.Success -> {
                    if (state.notifications.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Không có thông báo",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray
                                )
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
                                items = state.notifications,
                                key = { it.id }
                            ) { notification ->
                                NotificationItem(
                                    notification = notification,
                                    projectName = notification.data?.projectId?.let { projectNameMap[it] },
                                    onMarkAsRead = { viewModel.markAsRead(notification.id) },
                                    onMarkAsUnread = { viewModel.markAsUnread(notification.id) },
                                    onClick = {
                                        if (notification.isRead != true) {
                                            viewModel.markAsRead(notification.id)
                                        }
                                        handleNotificationClick(
                                            notification = notification,
                                            onNavigateToTaskDetail = onNavigateToTaskDetail,
                                            onNavigateToMeetingDetail = onNavigateToMeetingDetail
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                is NotificationUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(state.message, color = Color.Red)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.refresh() },
                                colors = ButtonDefaults.buttonColors(containerColor = Blue500)
                            ) {
                                Text("Thử lại")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: NotificationResponse,
    projectName: String?,
    onMarkAsRead: () -> Unit,
    onMarkAsUnread: () -> Unit,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val isUnread = notification.isRead != true
    val senderName = notification.data?.senderName

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread) Blue500.copy(alpha = 0.08f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isUnread) 2.dp else 1.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (isUnread) {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp, top = 12.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF43F5E))
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(getNotificationIconColor(notification.type).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getNotificationIcon(notification.type),
                        contentDescription = null,
                        tint = getNotificationIconColor(notification.type),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = notification.title ?: "Thông báo",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!projectName.isNullOrBlank()) {
                            ProjectPill(projectName)
                        }
                        ReadStatusPill(isUnread)
                    }
                    if (!senderName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Từ: $senderName",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                    if (!notification.message.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = notification.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatTimestamp(notification.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Tùy chọn",
                            tint = Color.Gray
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (isUnread) {
                            DropdownMenuItem(
                                text = { Text("Đánh dấu đã đọc") },
                                onClick = {
                                    showMenu = false
                                    onMarkAsRead()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Đánh dấu chưa đọc") },
                                onClick = {
                                    showMenu = false
                                    onMarkAsUnread()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectPill(projectName: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFFF1F5F9)
    ) {
        Text(
            text = "Dự án: $projectName",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF475569),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ReadStatusPill(isUnread: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isUnread) Color(0xFFFFE4E6) else Color(0xFFF1F5F9)
    ) {
        Text(
            text = if (isUnread) "Chưa đọc" else "Đã đọc",
            style = MaterialTheme.typography.labelSmall,
            color = if (isUnread) Color(0xFFE11D48) else Color(0xFF64748B),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun getNotificationIcon(type: String?): androidx.compose.ui.graphics.vector.ImageVector {
    return when (type?.lowercase()) {
        "task" -> Icons.Default.Task
        "meeting" -> Icons.Default.Event
        "project" -> Icons.Default.Folder
        "comment" -> Icons.Default.Comment
        else -> Icons.Default.Notifications
    }
}

private fun getNotificationIconColor(type: String?): Color {
    return when (type?.lowercase()) {
        "task" -> Color(0xFF4CAF50)
        "meeting" -> Color(0xFF2196F3)
        "project" -> Color(0xFFFF9800)
        "comment" -> Color(0xFF9C27B0)
        else -> Blue500
    }
}

private fun formatTimestamp(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return ""
    return try {
        val parsers = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss"
        )
        val parsed = parsers.firstNotNullOfOrNull { pattern ->
            runCatching { SimpleDateFormat(pattern, Locale.US).parse(timestamp) }.getOrNull()
        } ?: return timestamp
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN")).format(parsed)
    } catch (e: Exception) {
        timestamp
    }
}

private fun handleNotificationClick(
    notification: NotificationResponse,
    onNavigateToTaskDetail: (String, String) -> Unit,
    onNavigateToMeetingDetail: (String, String) -> Unit
) {
    val data = notification.data
    when {
        data?.taskId != null && data.projectId != null -> {
            onNavigateToTaskDetail(data.projectId, data.taskId)
        }
        data?.meetingId != null && data.projectId != null -> {
            onNavigateToMeetingDetail(data.projectId, data.meetingId)
        }
    }
}