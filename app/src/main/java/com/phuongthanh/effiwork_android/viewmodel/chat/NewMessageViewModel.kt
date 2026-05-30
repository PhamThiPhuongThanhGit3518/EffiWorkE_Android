package com.phuongthanh.effiwork_android.viewmodel.chat

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.repository.ProjectRepository
import com.phuongthanh.effiwork_android.data.repository.chat.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewMessageViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewMessageUiState>(NewMessageUiState.Loading)
    val uiState: StateFlow<NewMessageUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<NewMessageEffect>()
    val effect: SharedFlow<NewMessageEffect> = _effect.asSharedFlow()

    private var currentProjectId: String? = null

    private val avatarColors = listOf(
        Color(0xFF2196F3),
        Color(0xFF4CAF50),
        Color(0xFFFF9800),
        Color(0xFFE91E63),
        Color(0xFF9C27B0),
        Color(0xFF00BCD4),
        Color(0xFFFF5722),
        Color(0xFF607D8B)
    )

    fun loadProjectData(projectId: String) {
        currentProjectId = projectId

        viewModelScope.launch {
            _uiState.value = NewMessageUiState.Loading

            // Load conversations and project members in parallel
            val conversationsResult = chatRepository.getConversations(projectId, 1, 50, null, null)
            val membersResult = projectRepository.getProjectMembers(projectId)

            when {
                conversationsResult is ApiResult.Success && membersResult is ApiResult.Success -> {
                    val conversations = (conversationsResult as ApiResult.Success).data.items.orEmpty()
                    val membersData = (membersResult as ApiResult.Success).data.orEmpty()

                    // Convert members to ProjectMember
                    val members = membersData.mapIndexedNotNull { index, member ->
                        member ?: return@mapIndexedNotNull null
                        ProjectMember(
                            id = member.userId,
                            fullName = member.user?.fullName ?: "Unknown",
                            email = member.user?.email ?: "",
                            role = member.role,
                            avatarColor = avatarColors[index % avatarColors.size]
                        )
                    }

                    // Filter groups (PRIVATE conversations are 1-on-1, GROUP are actual groups)
                    val groups = conversations
                        .filter { it.type == com.phuongthanh.effiwork_android.data.model.chat.ChatConversationType.GROUP }
                        .mapNotNull { conv ->
                            conv.name ?: return@mapNotNull null
                            ProjectGroup(
                                id = conv.id,
                                name = conv.name,
                                description = "${conv._count?.members ?: 0} members",
                                memberCount = conv._count?.members ?: 0
                            )
                        }

                    _uiState.value = NewMessageUiState.Success(
                        conversations = conversations,
                        groups = groups,
                        members = members
                    )
                }
                conversationsResult is ApiResult.Error -> {
                    _uiState.value = NewMessageUiState.Error((conversationsResult as ApiResult.Error).message)
                }
                membersResult is ApiResult.Error -> {
                    _uiState.value = NewMessageUiState.Error((membersResult as ApiResult.Error).message)
                }
            }
        }
    }

    fun openPrivateConversation(member: ProjectMember) {
        val projectId = currentProjectId ?: return

        viewModelScope.launch {
            when (val result = chatRepository.createPrivateConversation(projectId, member.id)) {
                is ApiResult.Success -> {
                    _effect.emit(NewMessageEffect.NavigateToChat(
                        projectId = projectId,
                        conversationId = result.data.id,
                        conversationName = member.fullName
                    ))
                }
                is ApiResult.Error -> {
                    _effect.emit(NewMessageEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> { }
            }
        }
    }

    fun openGroupConversation(group: ProjectGroup) {
        val projectId = currentProjectId ?: return

        viewModelScope.launch {
            _effect.emit(NewMessageEffect.NavigateToChat(
                projectId = projectId,
                conversationId = group.id,
                conversationName = group.name
            ))
        }
    }

    fun openConversation(conversation: com.phuongthanh.effiwork_android.data.model.response.chat.ChatConversationResponse) {
        val projectId = currentProjectId ?: return
        val name = conversation.name
            ?: conversation.members?.firstOrNull()?.user?.fullName
            ?: "Chat"

        viewModelScope.launch {
            _effect.emit(NewMessageEffect.NavigateToChat(
                projectId = projectId,
                conversationId = conversation.id,
                conversationName = name
            ))
        }
    }

    fun createGroupConversation(projectId: String, name: String?, memberIds: List<String>) {
        viewModelScope.launch {
            _uiState.value = NewMessageUiState.Loading

            when (val result = chatRepository.createGroupConversation(projectId, name, memberIds)) {
                is ApiResult.Success -> {
                    _effect.emit(NewMessageEffect.NavigateToChat(
                        projectId = projectId,
                        conversationId = result.data.id,
                        conversationName = result.data.name ?: "Group"
                    ))
                    loadProjectData(projectId)
                }
                is ApiResult.Error -> {
                    _uiState.value = NewMessageUiState.Error(result.message)
                    _effect.emit(NewMessageEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> { }
            }
        }
    }

    fun refresh(projectId: String) {
        loadProjectData(projectId)
    }
}