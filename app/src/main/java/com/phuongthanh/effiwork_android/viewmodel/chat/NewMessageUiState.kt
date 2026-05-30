package com.phuongthanh.effiwork_android.viewmodel.chat

import androidx.compose.ui.graphics.Color
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatConversationResponse

data class ProjectMember(
    val id: String,
    val fullName: String,
    val email: String,
    val role: String,
    val avatarColor: Color
)

data class ProjectGroup(
    val id: String,
    val name: String,
    val description: String,
    val memberCount: Int
)

sealed class NewMessageUiState {
    object Loading : NewMessageUiState()
    data class Success(
        val conversations: List<ChatConversationResponse> = emptyList(),
        val groups: List<ProjectGroup> = emptyList(),
        val members: List<ProjectMember> = emptyList()
    ) : NewMessageUiState()
    data class Error(val message: String) : NewMessageUiState()
}

sealed class NewMessageEffect {
    data class ShowToast(val message: String) : NewMessageEffect()
    data class NavigateToChat(
        val projectId: String,
        val conversationId: String,
        val conversationName: String
    ) : NewMessageEffect()
}