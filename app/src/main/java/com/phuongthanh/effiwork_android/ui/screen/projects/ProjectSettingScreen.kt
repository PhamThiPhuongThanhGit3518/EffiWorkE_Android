package com.phuongthanh.effiwork_android.ui.screen.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.data.model.response.JoinRequestResponse
import com.phuongthanh.effiwork_android.data.model.response.ProjectsCount
import com.phuongthanh.effiwork_android.data.model.response.ProjectDetailResponse
import com.phuongthanh.effiwork_android.data.model.response.ProjectMemberResponse
import com.phuongthanh.effiwork_android.data.model.response.UserInfoResponse
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import com.phuongthanh.effiwork_android.viewmodel.project.ProjectSettingEffect
import com.phuongthanh.effiwork_android.viewmodel.project.ProjectSettingUiState
import com.phuongthanh.effiwork_android.viewmodel.project.ProjectSettingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSettingScreen(
    projectId: String,
    viewModel: ProjectSettingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(projectId) {
        viewModel.loadProjectSettings(projectId)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProjectSettingEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(effect.message)
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
                        text = "Cài đặt dự án",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp /
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is ProjectSettingUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Blue500)
                }
                is ProjectSettingUiState.Success -> {
                    ProjectSettingContent(
                        project = state.project,
                        members = state.members,
                        joinRequests = state.joinRequests,
                        onApproveRequest = { requestId ->
                            viewModel.approveJoinRequest(projectId, requestId)
                        },
                        onRejectRequest = { requestId ->
                            viewModel.rejectJoinRequest(projectId, requestId)
                        },
                        onRemoveMember = { userId ->
                            viewModel.removeMember(projectId, userId)
                        }
                    )
                }
                is ProjectSettingUiState.Error -> {
                    Text(state.message, modifier = Modifier.align(Alignment.Center), color = Color.Red)
                }
            }
        }
    }
}

@Composable
private fun ProjectSettingContent(
    project: ProjectDetailResponse,
    members: List<ProjectMemberResponse>,
    joinRequests: List<JoinRequestResponse>,
    onApproveRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onRemoveMember: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            ProjectInfoCard(project = project)
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Text(
                "Danh sách thành viên (${members.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(members) { member ->
            MemberItem(
                member = member,
                onRemove = { onRemoveMember(member.userId) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Yêu cầu tham gia (${joinRequests.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (joinRequests.isEmpty()) {
            item {
                Text(
                    "Không có yêu cầu nào",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            items(joinRequests) { request ->
                JoinRequestItem(
                    request = request,
                    onApprove = { onApproveRequest(request.requestId) },
                    onReject = { onRejectRequest(request.requestId) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun ProjectInfoCard(
    project: com.phuongthanh.effiwork_android.data.model.response.ProjectDetailResponse
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = project.name ?: "Không tên", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = project.projectCode ?: "Không mã", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
                Text(
                    " ${if (project.status == "ACTIVE") "Đang hoạt động" else project.status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50)
                )
            }

            if (!project.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = project.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            ProjectPreviewStats(
                memberCount = project.memberCount,
                taskCount = project.taskCount,
                meetingsCount = project.meetingsCount,
                documentsCount = project.documentsCount
            )
        }
    }
}

@Composable
private fun ProjectPreviewStats(
    memberCount: Int,
    taskCount: Int,
    meetingsCount: Int,
    documentsCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PreviewStatItem(icon = Icons.Default.Person, count = memberCount, label = "Thành viên")
        PreviewStatItem(icon = Icons.Default.Task, count = taskCount, label = "Nhiệm vụ")
        PreviewStatItem(icon = Icons.Default.MeetingRoom, count = meetingsCount, label = "Cuộc họp")
        PreviewStatItem(icon = Icons.Default.Description, count = documentsCount, label = "Tài liệu")
    }
}

@Composable
private fun PreviewStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Blue500,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun MemberItem(
    member: com.phuongthanh.effiwork_android.data.model.response.ProjectMemberResponse,
    onRemove: () -> Unit
) {
    val safeFullName =member.user?.fullName ?: "Thành viên"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Blue500.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = safeFullName.take(1).uppercase(),
                        color = Blue500,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = safeFullName, fontWeight = FontWeight.Medium)
                Text(text = member.user?.email ?: "", fontSize = 12.sp, color = Color.Gray)
            }
            Text(
                member.role,
                fontSize = 12.sp,
                color = Blue500,
                modifier = Modifier
                    .background(Blue500.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Xóa",
                    tint = Color.Red.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun JoinRequestItem(
    request: JoinRequestResponse,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val safeName = request.user?.fullName ?: "Người dùng"
    val safeEmail = request.user?.email ?: ""
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Color(0xFFFF9800).copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = safeName.take(1).uppercase(),
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = safeName, fontWeight = FontWeight.Medium)
                Text(text = safeEmail, fontSize = 12.sp, color = Color.Gray)
                if (!request.note.isNullOrEmpty()) {
                    Text(
                        "Ghi chú: ${request.note}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            IconButton(onClick = onApprove) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Phê duyệt",
                    tint = Color(0xFF4CAF50)
                )
            }
            IconButton(onClick = onReject) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Từ chối",
                    tint = Color.Red
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProjectSettingScreenPreview() {
    val fakeProject = ProjectDetailResponse(
        id = "1",
        projectCode = "PRJ001",
        name = "Dự án Alpha",
        description = "Dự án phát triển ứng dụng di động cho công ty XYZ",
        status = "ACTIVE",
        createdById = "user1",
        currentAdminId = "user1",
        createdAt = "2024-01-01T00:00:00Z",
        updatedAt = "2024-01-15T00:00:00Z",
        count = ProjectsCount(members = 5, tasks = 12, meetings = 3, documents = 8)
    )


    val fakeMembers = listOf(
        ProjectMemberResponse(
            userId = "u1",
            role = "ADMIN",
            user = UserInfoResponse(
                fullName = "Nguyễn Văn A",
                email = "a@example.com",
                avatarUrl = null
            )
        ),
        ProjectMemberResponse(
            userId = "u2",
            role = "MEMBER",
            user = UserInfoResponse(fullName = "Trần Thị B", email = "b@example.com", avatarUrl = null)
        ),
        ProjectMemberResponse(
            userId = "u3",
            role = "MEMBER",
            user = UserInfoResponse(fullName = "Lê Văn C", email = "c@example.com", avatarUrl = null)
        )
    )

    val fakeRequests = listOf(
        JoinRequestResponse(
            requestId = "r1",
            projectId = "1",
            userId = "u4",
            status = "PENDING",
            note = "Em muốn tham gia dự án này để học hỏi thêm về Jetpack Compose",
            createdAt = "2024-01-10",
            user = UserInfoResponse(fullName = "Phạm Văn D", email = "d@example.com", avatarUrl = null)
        )
    )

    MaterialTheme {
        ProjectSettingContent(
            project = fakeProject,
            members = fakeMembers,
            joinRequests = fakeRequests,
            onApproveRequest = {},
            onRejectRequest = {},
            onRemoveMember = {}
        )
    }
}