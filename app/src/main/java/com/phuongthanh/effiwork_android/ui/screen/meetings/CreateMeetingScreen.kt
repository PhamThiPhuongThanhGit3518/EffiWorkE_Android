package com.phuongthanh.effiwork_android.ui.screen.meetings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.viewmodel.meeting.MeetingEffect
import com.phuongthanh.effiwork_android.viewmodel.meeting.MeetingViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMeetingScreen(
    projectId: String = "",
    onBackClick: () -> Unit = {},
    onCreateClick: (String) -> Unit = { _ -> },
    viewModel: MeetingViewModel = hiltViewModel()
) {
    var meetingTitle by remember { mutableStateOf("") }
    var organizer by remember { mutableStateOf("") }
    var meetingFormat by remember { mutableStateOf("") }
    var dateTime by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var organizerExpanded by remember { mutableStateOf(false) }
    var formatExpanded by remember { mutableStateOf(false) }

    val organizers = listOf("Phạm Thị Phương Thanh", "Dương Hùng Phong", "Hoàng Thị Dương", "Nguyễn Văn A", "Trần Thị B")
    val formats = listOf("Online", "Offline")
    val participants = listOf("Phạm Thị Phương Thanh", "Dương Hùng Phong", "Hoàng Thị Dương", "Nguyễn Văn A", "Trần Thị B")

    LaunchedEffect(projectId) {
        viewModel.setProjectId(projectId)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is MeetingEffect.ShowToast -> {
                    // Handle toast
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
                        text = "Tạo cuộc họp",
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
                    onClick = {
                        val organizerId = organizers.indexOf(organizer).takeIf { it >= 0 }?.toString() ?: ""
                        viewModel.createMeeting(
                            title = meetingTitle,
                            content = content,
                            organizerId = organizerId,
                            format = meetingFormat.lowercase(),
                            scheduledTime = dateTime,
                            notes = notes.takeIf { it.isNotBlank() },
                            participantIds = participants.mapIndexedNotNull { index, _ -> index.toString() }
                        )
                        onCreateClick(meetingTitle)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    enabled = meetingTitle.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue500,
                        disabledContainerColor = Blue500.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Tạo cuộc họp",
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
            // Description text
            Text(
                text = "Ghi lại thời gian, nội dung, người phụ trách và thành phần tham gia để buổi họp diễn ra có tổ chức",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Input Fields Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Meeting Title
                    OutlinedTextField(
                        value = meetingTitle,
                        onValueChange = { meetingTitle = it },
                        label = { Text("Tiêu đề cuộc họp") },
                        placeholder = { Text("Ví dụ: Weekly sync sprint 12") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Organizer Dropdown
                    ExposedDropdownMenuBox(
                        expanded = organizerExpanded,
                        onExpandedChange = { organizerExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = organizer,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Người phụ trách") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = organizerExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = organizerExpanded,
                            onDismissRequest = { organizerExpanded = false }
                        ) {
                            organizers.forEach { name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        organizer = name
                                        organizerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Meeting Format Dropdown
                    ExposedDropdownMenuBox(
                        expanded = formatExpanded,
                        onExpandedChange = { formatExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = meetingFormat,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Hình thức họp") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = formatExpanded,
                            onDismissRequest = { formatExpanded = false }
                        ) {
                            formats.forEach { format ->
                                DropdownMenuItem(
                                    text = { Text(format) },
                                    onClick = {
                                        meetingFormat = format
                                        formatExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // DateTime Picker
                    OutlinedTextField(
                        value = dateTime,
                        onValueChange = { dateTime = it },
                        label = { Text("Thời gian họp") },
                        placeholder = { Text("Chọn ngày và giờ") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = {
                            IconButton(onClick = { /* Open datetime picker */ }) {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Detailed Info Card
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
                        text = "Thông tin chi tiết",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Content
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Nội dung họp") },
                        placeholder = { Text("Nhập tóm tắt nội dung hoặc chương trình nghị sự...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Ghi chú") },
                        placeholder = { Text("Nhập các lưu ý đặc biệt hoặc chuẩn bị trước...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Attachments Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Đính kèm tài liệu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Dashed border upload area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .border(
                                width = 1.dp,
                                color = Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { /* Open file picker */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tải tài liệu đính kèm",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Blue500
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX (tối đa 20MB)",
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Create Meeting Screen")
@Composable
fun CreateMeetingScreenPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            // Top App Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Tạo cuộc họp",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Input Fields Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = "Họp kick-off dự án",
                            onValueChange = {},
                            label = { Text("Tiêu đề cuộc họp") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = "Phạm Thị Phương Thanh",
                            onValueChange = {},
                            label = { Text("Người phụ trách") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detailed Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Thông tin chi tiết", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = "Thảo luận kế hoạch triển khai",
                            onValueChange = {},
                            label = { Text("Nội dung họp") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }
    }
}