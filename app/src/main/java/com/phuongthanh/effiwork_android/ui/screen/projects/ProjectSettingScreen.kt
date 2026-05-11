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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.R
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import com.phuongthanh.effiwork_android.viewmodel.project_setting.ProjectSettingEffect
import com.phuongthanh.effiwork_android.viewmodel.project_setting.ProjectSettingUiState
import com.phuongthanh.effiwork_android.viewmodel.project_setting.ProjectSettingViewModel

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
            CenterAlignedTopAppBar(
                title = { Text("Cài đặt dự án", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
    project: com.phuongthanh.effiwork_android.data.model.response.ProjectDetailResponse,
    members: List<com.phuongthanh.effiwork_android.data.model.response.ProjectMemberResponse>,
    joinRequests: List<com.phuongthanh.effiwork_android.data.model.response.JoinRequestResponse>,
    onApproveRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit,
    onRemoveMember: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            ProjectInfoCard(
                name = project.name,
                code = project.projectCode,
                status = project.status
            )
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
private fun ProjectInfoCard(name: String, code: String, status: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = name ?: "Không tên", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = code ?: "Không mã", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
                Text(
                    " ${if (status == "ACTIVE") "Đang hoạt động" else status}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun MemberItem(
    member: com.phuongthanh.effiwork_android.data.model.response.ProjectMemberResponse,
    onRemove: () -> Unit
) {
    val safeFullName = member.fullName ?: "Thành viên"
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
                Text(text = member.email ?: "", fontSize = 12.sp, color = Color.Gray)
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
    request: com.phuongthanh.effiwork_android.data.model.response.JoinRequestResponse,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val safeName = request.fullName ?: "Người dùng"
    val safeEmail = request.email ?: ""
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