package com.phuongthanh.effiwork_android.ui.screen.meetings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import androidx.hilt.navigation.compose.hiltViewModel
import com.phuongthanh.effiwork_android.viewmodel.meeting.MeetingAttachment
import com.phuongthanh.effiwork_android.viewmodel.meeting.MeetingEffect
import com.phuongthanh.effiwork_android.viewmodel.meeting.MeetingViewModel
import com.phuongthanh.effiwork_android.viewmodel.meeting.ProjectMember
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMeetingScreen(
    projectId: String = "",
    meetingId: String? = null,
    isEdit: Boolean = false,
    onBackClick: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    viewModel: MeetingViewModel = hiltViewModel()
) {
    var meetingTitle by remember { mutableStateOf("") }
    var organizerId by remember { mutableStateOf("") }
    var meetingFormat by remember { mutableStateOf("Offline") }
    var dateTime by remember { mutableStateOf<Long?>(null) }
    var dateTimeText by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedParticipantIds by remember { mutableStateOf(setOf<String>()) }
    var selectedFiles by remember { mutableStateOf(listOf<Uri>( )) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf("") }
    var uploadedDocumentIds by remember { mutableStateOf(listOf<String>()) }
    var existingAttachments by remember { mutableStateOf<List<MeetingAttachment>>(emptyList()) }
    var removedAttachmentIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var organizerExpanded by remember { mutableStateOf(false) }
    var formatExpanded by remember { mutableStateOf(false) }
    var participantExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedFiles = selectedFiles + uris
    }

    val members by viewModel.projectMembers.collectAsStateWithLifecycle()
    val editingMeeting by viewModel.editingMeeting.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        viewModel.setProjectId(projectId)
        viewModel.loadProjectMembers()
    }

    LaunchedEffect(projectId, meetingId, isEdit) {
        if (isEdit && meetingId != null) {
            viewModel.loadMeetingForEdit(projectId, meetingId)
        } else {
            viewModel.resetEditingMeeting()
        }
    }

    LaunchedEffect(editingMeeting) {
        editingMeeting?.let { meeting ->
            if (isEdit) {
                meetingTitle = meeting.title
                content = meeting.description
                meetingFormat = meeting.format.displayName
                organizerId = meeting.organizerId
                notes = meeting.notes ?: ""
                selectedParticipantIds = emptySet()
                existingAttachments = meeting.attachments
                removedAttachmentIds = emptySet()
                meeting.meetingTime?.let { timeStr ->
                    try {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                        val date = inputFormat.parse(timeStr.replace("Z", ""))
                        date?.let { dateTime = it.time }
                    } catch (e: Exception) { }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is MeetingEffect.ShowToast -> {
                    // Handle toast
                }
                is MeetingEffect.NavigateBack -> {
                    onNavigateBack()
                }
                is MeetingEffect.MeetingCreated -> {
                    // Handle meeting created - attach documents
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
                        text = if (isEdit) "Sửa cuộc họp" else "Tạo cuộc họp",
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
                        isUploading = true
                        uploadProgress = "Đang tải tài liệu..."

                        viewModel.viewModelScope.launch {
                            try {
                                val uploadedIds = mutableListOf<String>()
                                selectedFiles.forEachIndexed { index, uri ->
                                    uploadProgress = "Đang tải tài liệu ${index + 1}/${selectedFiles.size}..."
                                    val mimeType = context.contentResolver.getType(uri)
                                    val fileName = queryDisplayName(context, uri, index, mimeType)
                                    val inputStream = context.contentResolver.openInputStream(uri)
                                    val fileBytes = inputStream?.readBytes() ?: byteArrayOf()
                                    inputStream?.close()

                                    val result = viewModel.uploadDocument(fileName, fileBytes, mimeType)
                                    android.util.Log.d("MeetingAttachDebug", "[Upload] uri=$uri, fileName=$fileName, bytes.size=${fileBytes.size}, mimeType=$mimeType")
                                    if (result is ApiResult.Success) {
                                        val d = result.data
                                        android.util.Log.d("MeetingAttachDebug", "[Upload] Success: id=${d.id}, fileName=${d.fileName}, fileSize=${d.fileSize}, filePath=${d.filePath}, mimeType=${d.mimeType}")
                                        uploadedIds.add(d.id)
                                    } else if (result is ApiResult.Error) {
                                        android.util.Log.d("MeetingAttachDebug", "[Upload] Error: ${result.message}")
                                    }
                                }

                                uploadedDocumentIds = uploadedIds
                                val keptExistingIds = existingAttachments
                                    .map { it.documentId }
                                    .filter { it !in removedAttachmentIds }
                                val finalAttachmentIds = keptExistingIds + uploadedIds

                                uploadProgress = if (isEdit) "Đang cập nhật cuộc họp..." else "Đang tạo cuộc họp..."

                                val formattedTime = dateTime?.let {
                                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                                        timeZone = TimeZone.getTimeZone("UTC")
                                    }.format(Date(it))
                                } ?: ""

                                if (isEdit && meetingId != null) {
                                    val allParticipantIds = selectedParticipantIds.toMutableList()
                                    if (!allParticipantIds.contains(organizerId)) {
                                        allParticipantIds.add(organizerId)
                                    }
                                    viewModel.updateMeeting(
                                        meetingId = meetingId,
                                        title = meetingTitle,
                                        content = content,
                                        hostUserId = organizerId,
                                        type = meetingFormat.uppercase(),
                                        meetingTime = formattedTime,
                                        notes = notes.takeIf { it.isNotBlank() },
                                        participantIds = allParticipantIds,
                                        attachmentDocumentIds = finalAttachmentIds
                                    )
                                    isUploading = false
                                    uploadProgress = ""
                                } else {
                                    val allParticipantIds = selectedParticipantIds.toMutableList()
                                    if (!allParticipantIds.contains(organizerId)) {
                                        allParticipantIds.add(organizerId)
                                    }
                                    android.util.Log.d("MeetingDebug", "Calling createMeeting: title=$meetingTitle, organizerId=$organizerId, type=${meetingFormat.uppercase()}, time=$formattedTime, attachmentDocumentIds=$finalAttachmentIds")
                                    viewModel.createMeeting(
                                        title = meetingTitle,
                                        content = content,
                                        hostUserId = organizerId,
                                        type = meetingFormat.uppercase(),
                                        meetingTime = formattedTime,
                                        notes = notes.takeIf { it.isNotBlank() },
                                        participantIds = allParticipantIds,
                                        attachmentDocumentIds = finalAttachmentIds
                                    ) {
                                        isUploading = false
                                        uploadProgress = ""
                                    }
                                }
                            } catch (e: Exception) {
                                isUploading = false
                                uploadProgress = ""
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    enabled = meetingTitle.isNotBlank() && organizerId.isNotBlank() && dateTime != null && !isUploading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Blue500,
                        disabledContainerColor = Blue500.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = uploadProgress.ifEmpty { "Đang xử lý..." })
                    } else {
                        Text(
                            text = if (isEdit) "Cập nhật" else "Tạo cuộc họp",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                text = "Ghi lại thời gian, nội dung, người phụ trách và thành phần tham gia để buổi họp diễn ra có tổ chức",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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

                    ExposedDropdownMenuBox(
                        expanded = organizerExpanded,
                        onExpandedChange = { organizerExpanded = it }
                    ) {
                        val selectedMember = members.find { it.userId == organizerId }
                        OutlinedTextField(
                            value = selectedMember?.fullName ?: "",
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
                            members.forEach { member ->
                                DropdownMenuItem(
                                    text = { Text(member.fullName) },
                                    onClick = {
                                        organizerId = member.userId
                                        organizerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

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
                            listOf("Online", "Offline").forEach { format ->
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

                    OutlinedTextField(
                        value = dateTimeText,
                        onValueChange = {},
                        label = { Text("Thời gian họp") },
                        placeholder = { Text("Chọn ngày và giờ") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        enabled = false,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = Color.Gray)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        expanded = participantExpanded,
                        onExpandedChange = { participantExpanded = it }
                    ) {
                        val selectedNames = members.filter { it.userId in selectedParticipantIds }.map { it.fullName }
                        OutlinedTextField(
                            value = if (selectedNames.isEmpty()) "Chọn người tham gia" else selectedNames.joinToString(", "),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = participantExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = participantExpanded,
                            onDismissRequest = { participantExpanded = false }
                        ) {
                            members.forEach { member ->
                                val isSelected = member.userId in selectedParticipantIds
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(member.fullName)
                                        }
                                    },
                                    onClick = {
                                        selectedParticipantIds = if (isSelected) {
                                            selectedParticipantIds - member.userId
                                        } else {
                                            selectedParticipantIds + member.userId
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                    Spacer(modifier = Modifier.height(12.dp))

                    val visibleExisting = existingAttachments.filter { it.documentId !in removedAttachmentIds }
                    if (visibleExisting.isNotEmpty()) {
                        Text(
                            text = "Tài liệu đang gắn",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        visibleExisting.forEach { att ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = att.fileName,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = {
                                        removedAttachmentIds = removedAttachmentIds + att.documentId
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Gỡ khỏi cuộc họp",
                                        tint = Color.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .border(
                                width = 1.dp,
                                color = Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { filePickerLauncher.launch("*/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = Blue500,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Thêm tài liệu",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Blue500
                            )
                            Text(
                                text = "PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX (tối đa 20MB)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    if (selectedFiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        selectedFiles.forEachIndexed { index, uri ->
                            val mimeType = context.contentResolver.getType(uri)
                            val fileName = queryDisplayName(context, uri, index, mimeType)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = fileName,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = { selectedFiles = selectedFiles.filterIndexed { i, _ -> i != index } },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Xóa",
                                        tint = Color.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        dateTime = millis
                        dateTimeText = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))
                    }
                    showDatePicker = false
                    showTimePicker = true
                }) {
                    Text("Tiếp theo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Hủy")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateTime?.let { dateMillis ->
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = dateMillis
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                        }
                        dateTime = calendar.timeInMillis
                        dateTimeText = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(calendar.timeInMillis))
                    }
                    showTimePicker = false
                }) {
                    Text("Xác nhận")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Hủy")
                }
            },
            title = { Text("Chọn giờ") },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

private fun queryDisplayName(
    context: android.content.Context,
    uri: Uri,
    index: Int = 0,
    mimeType: String? = null
): String {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                val name = it.getString(nameIndex)
                if (!name.isNullOrBlank()) return name
            }
        }
    }
    val lastSegment = uri.lastPathSegment
    if (!lastSegment.isNullOrBlank()) {
        val decoded = Uri.decode(lastSegment.substringAfterLast('/'))
        if (decoded.isNotBlank()) return decoded
    }
    val ext = mimeToExtension(mimeType)
    return if (ext.isNotEmpty()) "file_${index + 1}.$ext" else "file_${index + 1}"
}

private fun mimeToExtension(mime: String?): String = when (mime) {
    "application/pdf" -> "pdf"
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
    "application/msword" -> "doc"
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx"
    "application/vnd.ms-excel" -> "xls"
    "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "pptx"
    "application/vnd.ms-powerpoint" -> "ppt"
    "image/png" -> "png"
    "image/jpeg" -> "jpg"
    "text/plain" -> "txt"
    else -> ""
}