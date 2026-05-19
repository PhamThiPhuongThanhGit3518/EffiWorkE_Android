package com.phuongthanh.effiwork_android.ui.screen.meetings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import com.phuongthanh.effiwork_android.viewmodel.meeting.*
import kotlinx.coroutines.flow.collectLatest

data class MeetingListScreenState(
    val projectId: String = "",
    val uiState: MeetingUiState = MeetingUiState.Idle,
    val selectedTab: MeetingFilterTab = MeetingFilterTab.ALL,
    val selectedFormat: String? = null,
    val searchQuery: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingListScreen(
    projectId: String = "",
    onBackClick: () -> Unit = {},
    onCreateClick: () -> Unit = {},
    viewModel: MeetingViewModel = hiltViewModel()
) {
    var screenState by remember { mutableStateOf(MeetingListScreenState(projectId = projectId)) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedFormat by viewModel.selectedFormat.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    LaunchedEffect(projectId) {
        viewModel.setProjectId(projectId)
        viewModel.loadMeetings()
    }

    LaunchedEffect(uiState) {
        screenState = screenState.copy(uiState = uiState)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is MeetingEffect.ShowToast -> {}
            }
        }
    }

    MeetingListScreenContent(
        state = screenState,
        onBackClick = onBackClick,
        onCreateClick = onCreateClick,
        onTabSelect = { viewModel.selectTab(it) },
        onFormatSelect = { viewModel.selectFormat(it) },
        onSearchQueryChange = { viewModel.updateSearchQuery(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingListScreenContent(
    state: MeetingListScreenState,
    onBackClick: () -> Unit,
    onCreateClick: () -> Unit,
    onTabSelect: (MeetingFilterTab) -> Unit,
    onFormatSelect: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = "Cuộc họp",
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
                    IconButton(onClick = onCreateClick) {
                        Icon(Icons.Default.Add, contentDescription = "Tạo cuộc họp")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5))
        ) {
            TabRow(
                selectedTabIndex = if (state.selectedTab == MeetingFilterTab.ALL) 0 else 1,
                containerColor = Color.White,
                contentColor = Blue500
            ) {
                Tab(
                    selected = state.selectedTab == MeetingFilterTab.ALL,
                    onClick = { onTabSelect(MeetingFilterTab.ALL) },
                    text = {
                        Text(
                            "Tất cả",
                            color = if (state.selectedTab == MeetingFilterTab.ALL) Blue500 else Color.Gray
                        )
                    }
                )
                Tab(
                    selected = state.selectedTab == MeetingFilterTab.JOINED,
                    onClick = { onTabSelect(MeetingFilterTab.JOINED) },
                    text = {
                        Text(
                            "Đã tham gia",
                            color = if (state.selectedTab == MeetingFilterTab.JOINED) Blue500 else Color.Gray
                        )
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
//                OutlinedTextField(
//                    value = state.searchQuery,
//                    onValueChange = onSearchQueryChange,
//                    modifier = Modifier.weight(1f),
//                    placeholder = {
//                        Text("Tìm theo tên hoặc nội dung cuộc họp")
//                    },
//                    leadingIcon = {
//                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
//                    },
//                    singleLine = true,
//                    shape = RoundedCornerShape(8.dp),
//                    colors = OutlinedTextFieldDefaults.colors(
//                        unfocusedBorderColor = Color.LightGray,
//                        focusedBorderColor = Blue500
//                    )
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                FormatFilterDropdown(
//                    selectedFormat = state.selectedFormat,
//                    onFormatSelect = onFormatSelect
//                )
            }

            when (val uiState = state.uiState) {
                is MeetingUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Blue500)
                    }
                }
                is MeetingUiState.Success -> {
                    val filteredMeetings = uiState.meetings.filter { meeting ->
                        val matchesSearch = state.searchQuery.isEmpty() ||
                            meeting.title.contains(state.searchQuery, ignoreCase = true) ||
                            meeting.description.contains(state.searchQuery, ignoreCase = true)
                        val matchesFormat = state.selectedFormat == null ||
                            meeting.format.displayName == state.selectedFormat
                        matchesSearch && matchesFormat
                    }
                    MeetingList(meetings = filteredMeetings)
                }
                is MeetingUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(uiState.message, color = Color.Red)
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun FormatFilterDropdown(
    selectedFormat: String?,
    onFormatSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val formats = listOf("Tất cả hình thức", "Online", "Offline")

    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = Blue500.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedFormat ?: "Tất cả hình thức",
                    color = Blue500,
                    fontSize = 14.sp
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Blue500,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            formats.forEach { format ->
                DropdownMenuItem(
                    text = { Text(format) },
                    onClick = {
                        onFormatSelect(if (format == "Tất cả hình thức") null else format)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MeetingList(meetings: List<Meeting>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(meetings, key = { it.id }) { meeting ->
            MeetingCard(meeting = meeting)
        }
    }
}

@Composable
private fun MeetingCard(meeting: Meeting) {
    val isOnline = meeting.format == MeetingFormat.ONLINE
    val formatColor = if (isOnline) Color(0xFF2196F3) else Color(0xFF9C27B0)
    val statusColor = when (meeting.status) {
        MeetingStatus.UPCOMING -> Color(0xFFFF9800)
        MeetingStatus.ENDED -> Color.Gray
        MeetingStatus.ONGOING -> Color(0xFF4CAF50)
        MeetingStatus.CANCELLED -> Color.Red
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = formatColor.copy(alpha = 0.1f)
                ) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.Videocam else Icons.Default.Groups,
                        contentDescription = null,
                        tint = formatColor,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meeting.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = meeting.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = formatColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = if (isOnline) "Online" else "Offline",
                        color = formatColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = meeting.status.displayName,
                    color = statusColor,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = meeting.organizer,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${meeting.time} ${meeting.date}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            if (meeting.participants.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = meeting.participants.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private val previewMeetings = listOf(
    Meeting(
        id = "m1",
        title = "Họp kick-off dự án",
        description = "Thảo luận kế hoạch triển khai",
        format = MeetingFormat.ONLINE,
        status = MeetingStatus.UPCOMING,
        organizer = "Nguyễn Văn Minh",
        date = "20/05/2026",
        time = "14:00",
        participants = listOf("Phạm Thị Phương Thanh", "Trần Văn Hoàng")
    ),
    Meeting(
        id = "m2",
        title = "Review sprint 1",
        description = "Đánh giá tiến độ sprint đầu tiên",
        format = MeetingFormat.OFFLINE,
        status = MeetingStatus.ENDED,
        organizer = "Lê Thị Mai",
        date = "15/05/2026",
        time = "09:00",
        participants = listOf("Hoàng Nam", "Minh Hoàng")
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Meeting List Screen")
@Composable
fun MeetingListScreenPreview() {
    MaterialTheme {
        MeetingListScreenContent(
            state = MeetingListScreenState(
                projectId = "proj123",
                uiState = MeetingUiState.Success(previewMeetings),
                selectedTab = MeetingFilterTab.ALL,
                selectedFormat = null,
                searchQuery = ""
            ),
            onBackClick = {},
            onCreateClick = {},
            onTabSelect = {},
            onFormatSelect = {},
            onSearchQueryChange = {}
        )
    }
}