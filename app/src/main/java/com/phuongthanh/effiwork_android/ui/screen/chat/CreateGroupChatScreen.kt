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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import com.phuongthanh.effiwork_android.viewmodel.chat.NewMessageEffect
import com.phuongthanh.effiwork_android.viewmodel.chat.NewMessageUiState
import com.phuongthanh.effiwork_android.viewmodel.chat.NewMessageViewModel
import com.phuongthanh.effiwork_android.viewmodel.chat.ProjectMember
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupChatScreen(
    projectId: String,
    onBackClick: () -> Unit,
    onGroupCreated: () -> Unit,
    viewModel: NewMessageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var groupName by remember { mutableStateOf("") }
    var selectedMemberIds by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(projectId) {
        viewModel.loadProjectData(projectId)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is NewMessageEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is NewMessageEffect.NavigateToChat -> {
                    onGroupCreated()
                }
            }
        }
    }

    CreateGroupChatScreenContent(
        uiState = uiState,
        groupName = groupName,
        selectedMemberIds = selectedMemberIds,
        onGroupNameChange = { groupName = it },
        onMemberToggle = { memberId ->
            selectedMemberIds = if (selectedMemberIds.contains(memberId)) {
                selectedMemberIds - memberId
            } else {
                selectedMemberIds + memberId
            }
        },
        onCreateClick = {
            if (selectedMemberIds.size >= 2) {
                viewModel.createGroupConversation(
                    projectId = projectId,
                    name = groupName.takeIf { it.isNotBlank() },
                    memberIds = selectedMemberIds.toList()
                )
            } else {
                Toast.makeText(context, "Cần chọn ít nhất 2 thành viên", Toast.LENGTH_SHORT).show()
            }
        },
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupChatScreenContent(
    uiState: NewMessageUiState,
    groupName: String,
    selectedMemberIds: Set<String>,
    onGroupNameChange: (String) -> Unit,
    onMemberToggle: (String) -> Unit,
    onCreateClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val members = when (uiState) {
        is NewMessageUiState.Success -> uiState.members
        else -> emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo nhóm chat", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = onCreateClick,
                        enabled = selectedMemberIds.size >= 2
                    ) {
                        Text(
                            "Tạo",
                            color = if (selectedMemberIds.size >= 2) Blue500 else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = groupName,
                onValueChange = onGroupNameChange,
                label = { Text("Tên nhóm (tùy chọn)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Chọn thành viên (${selectedMemberIds.size} đã chọn)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (uiState) {
                is NewMessageUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Blue500)
                    }
                }
                else -> {
                    LazyColumn {
                        items(members) { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMemberToggle(member.id) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedMemberIds.contains(member.id),
                                    onCheckedChange = { onMemberToggle(member.id) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(member.avatarColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = member.fullName.take(2).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(member.fullName, fontWeight = FontWeight.Bold)
                                    Text(
                                        member.role,
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
    }
}

@Preview
@Composable
private fun CreateGroupChatScreenPreview() {
    MaterialTheme {
        CreateGroupChatScreenContent(
            uiState = NewMessageUiState.Success(
                conversations = emptyList(),
                groups = emptyList(),
                members = listOf(
                    ProjectMember("1", "Nguyễn Văn A", "a@example.com", "Developer", Color.Blue),
                    ProjectMember("2", "Trần Thị B", "b@example.com", "Designer", Color.Green),
                    ProjectMember("3", "Lê Văn C", "c@example.com", "Manager", Color.Red)
                )
            ),
            groupName = "Nhóm Test",
            selectedMemberIds = setOf("1", "2"),
            onGroupNameChange = {},
            onMemberToggle = {},
            onCreateClick = {},
            onBackClick = {}
        )
    }
}