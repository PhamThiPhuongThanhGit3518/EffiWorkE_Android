package com.phuongthanh.effiwork_android.viewmodel.chat

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.repository.AuthRepository
import com.phuongthanh.effiwork_android.data.repository.ProjectRepository
import com.phuongthanh.effiwork_android.data.repository.TaskRepository
import com.phuongthanh.effiwork_android.data.repository.chat.ChatRepository
import com.phuongthanh.effiwork_android.data.socket.ChatSocketManager
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatMessageResponse
import com.phuongthanh.effiwork_android.viewmodel.chat.state.NewMessageEffect
import com.phuongthanh.effiwork_android.viewmodel.chat.state.NewMessageUiState
import com.phuongthanh.effiwork_android.viewmodel.chat.state.PrivateChatItem
import com.phuongthanh.effiwork_android.viewmodel.chat.state.ProjectGroup
import com.phuongthanh.effiwork_android.viewmodel.chat.state.ProjectMember
import com.phuongthanh.effiwork_android.viewmodel.chat.state.TaskSummary
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
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository,
    private val authRepository: AuthRepository,
    private val socketManager: ChatSocketManager
) : ViewModel() {

    init {
        observeSocketEvents()
    }

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

    private fun observeSocketEvents() {
        viewModelScope.launch {
            socketManager.conversationUpdatedFlow.collect { event ->
                val projId = currentProjectId
                if (projId != null) {
                    loadProjectData(projId)
                } else {
                    android.util.Log.w("NewMessageVM", "  currentProjectId is null, cannot reload")
                }
            }
        }

        viewModelScope.launch {
            socketManager.newMessageFlow.collect { event ->
                val currentState = _uiState.value
                if (currentState is NewMessageUiState.Success) {
                    val updatedConversations = updateConversationWithMessage(
                        currentState.conversations,
                        event.message
                    )
                    val updatedPrivateChats = updatePrivateChatWithMessage(
                        currentState.privateChats,
                        event.message
                    )
                    _uiState.value = currentState.copy(
                        conversations = updatedConversations,
                        privateChats = updatedPrivateChats
                    )
                } else {
                    android.util.Log.w("NewMessageVM", "  currentState is not Success, cannot update")
                }
            }
            android.util.Log.d("NewMessageVM", "observeSocketEvents: newMessageFlow collector COMPLETED!")
        }
    }

    private fun updateConversationWithMessage(
        conversations: List<com.phuongthanh.effiwork_android.data.model.response.chat.ChatConversationResponse>,
        message: ChatMessageResponse
    ): List<com.phuongthanh.effiwork_android.data.model.response.chat.ChatConversationResponse> {
        val currentUserId = authRepository.getCurrentUserId() ?: ""

        return conversations.map { conv ->
            if (conv.id == message.conversationId) {
                conv.copy(
                    lastMessageId = message.id,
                    lastMessageAt = message.createdAt,
                    unreadCount = if (message.senderId != currentUserId) (conv.unreadCount ?: 0) + 1 else conv.unreadCount
                )
            } else {
                conv
            }
        }.sortedByDescending { it.lastMessageAt ?: it.updatedAt }
    }

    private fun updatePrivateChatWithMessage(
        privateChats: List<PrivateChatItem>,
        message: ChatMessageResponse
    ): List<PrivateChatItem> {
        val currentUserId = authRepository.getCurrentUserId() ?: ""

        return privateChats.map { chat ->
            if (chat.id == message.conversationId) {
                chat.copy(
                    lastMessage = message.content,
                    lastMessageAt = message.createdAt,
                    unreadCount = if (message.senderId != currentUserId) chat.unreadCount + 1 else chat.unreadCount
                )
            } else {
                chat
            }
        }.sortedByDescending { it.lastMessageAt }
    }

    fun onScreenVisible(projectId: String) {
        android.util.Log.d("NewMessageVM", ">>> onScreenVisible($projectId)")
        android.util.Log.d("NewMessageVM", "  socket connected = ${socketManager.isConnected()}")
        if (!socketManager.isConnected()) {
            socketManager.connect()
        }
        loadProjectData(projectId)
    }

    fun loadProjectData(projectId: String) {
        currentProjectId = projectId

        viewModelScope.launch {
            _uiState.value = NewMessageUiState.Loading

            val currentUserId = authRepository.getCurrentUserId() ?: ""
            val conversationsResult = chatRepository.getConversations(projectId, 1, 50, null, null)

            when (conversationsResult) {
                is ApiResult.Success -> {
                    val rawData = conversationsResult.data
                    android.util.Log.d("NewMessageViewModel", "📦 rawData type: ${rawData::class.simpleName}")
                    android.util.Log.d("NewMessageViewModel", "📦 items: ${rawData.items?.size}, meta: ${rawData.meta}")

                    val conversations = rawData.items.orEmpty()
                    android.util.Log.d("NewMessageViewModel", "✅ API Success - Total conversations: ${conversations.size}")

                    // Get members from projectRepository for CreateGroupChatScreen
                    val membersResult = projectRepository.getProjectMembers(projectId)
                    android.util.Log.d("NewMessageViewModel", "Members result type: ${membersResult::class.simpleName}")
                    val members = when (membersResult) {
                        is ApiResult.Success -> membersResult.data.mapIndexedNotNull { index, member ->
                            member ?: return@mapIndexedNotNull null
                            ProjectMember(
                                id = member.userId,
                                fullName = member.user?.fullName ?: "Unknown",
                                email = member.user?.email ?: "",
                                role = member.role,
                                avatarColor = avatarColors[index % avatarColors.size]
                            )
                        }
                        else -> emptyList()
                    }
                    android.util.Log.d("NewMessageViewModel", "✅ Parsed members: ${members.size}")

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
                    android.util.Log.d("NewMessageViewModel", "✅ Parsed groups: ${groups.size}")

                    val privateChats = conversations
                        .filter { it.type == com.phuongthanh.effiwork_android.data.model.chat.ChatConversationType.PRIVATE }
                        .mapIndexedNotNull { index, conv ->
                            android.util.Log.d("NewMessageViewModel", "PRIVATE conv: ${conv.id}, currentUserId: $currentUserId")
                            conv.members?.forEach { m ->
                                android.util.Log.d("NewMessageViewModel", "  member: userId=${m.userId}, user=${m.user?.fullName}")
                            }
                            val otherMember = conv.members
                                ?.filter { it.userId != currentUserId }
                                ?.firstOrNull()
                                ?.user

                            android.util.Log.d("NewMessageViewModel", "  otherMember: ${otherMember?.fullName}")

                            PrivateChatItem(
                                id = conv.id,
                                displayName = otherMember?.fullName ?: "Unknown",
                                avatarColor = avatarColors[index % avatarColors.size],
                                unreadCount = conv.unreadCount ?: 0,
                                lastMessage = conv.messages?.firstOrNull()?.content,
                                lastMessageAt = conv.lastMessageAt,
                                email = otherMember?.email
                            )
                        }
                    android.util.Log.d("NewMessageViewModel", "✅ Parsed privateChats: ${privateChats.size}")

                    val sections = when (val sectionsResult = taskRepository.getTaskGroups(projectId)) {
                        is ApiResult.Success -> sectionsResult.data
                        is ApiResult.Error -> {
                            android.util.Log.w("NewMessageViewModel", "⚠️ getTaskGroups failed: ${sectionsResult.message}")
                            emptyList()
                        }
                        is ApiResult.Loading -> emptyList()
                    }
                    android.util.Log.d("NewMessageViewModel", "✅ Parsed sections: ${sections.size}")

                    _uiState.value = NewMessageUiState.Success(
                        conversations = conversations,
                        groups = groups,
                        members = members,
                        privateChats = privateChats,
                        sections = sections
                    )
                }
                is ApiResult.Error -> {
                    android.util.Log.e("NewMessageViewModel", "❌ API Error: ${conversationsResult.message}")
                    _uiState.value = NewMessageUiState.Error(conversationsResult.message)
                }
                is ApiResult.Loading -> { }
            }
        }
    }

    fun openPrivateConversation(privateChat: PrivateChatItem) {
        val projectId = currentProjectId ?: return

        viewModelScope.launch {
            _effect.emit(
                NewMessageEffect.NavigateToChat(
                projectId = projectId,
                conversationId = privateChat.id,
                conversationName = privateChat.displayName
            ))
        }
    }

    fun createPrivateConversationAndNavigate(
        projectId: String,
        targetUserId: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = NewMessageUiState.Loading  // IMPROVEMENT #3

            when (val result = chatRepository.createPrivateConversation(projectId, targetUserId)) {
                is ApiResult.Success -> {
                    val newConv = result.data
                    val currentUserId = authRepository.getCurrentUserId() ?: ""
                    val otherMember = newConv.members?.find { it.userId != currentUserId }
                    val conversationName = otherMember?.user?.fullName ?: "Chat"

                    _effect.emit(
                        NewMessageEffect.NavigateToChat(
                        projectId = projectId,
                        conversationId = newConv.id,
                        conversationName = conversationName
                    ))
                    loadProjectData(projectId)
                    // IMPROVEMENT #5: onComplete không cần gọi ở đây - LaunchedEffect sẽ xử lý
                }
                is ApiResult.Error -> {
                    _uiState.value = NewMessageUiState.Error(result.message)
                    _effect.emit(NewMessageEffect.ShowToast(result.message))
                    onComplete()  // IMPROVEMENT #2: Gọi onError để reset isCreating
                }
                is ApiResult.Loading -> { }
            }
        }
    }

    fun openGroupConversation(group: ProjectGroup) {
        val projectId = currentProjectId ?: return

        viewModelScope.launch {
            _effect.emit(
                NewMessageEffect.NavigateToChat(
                projectId = projectId,
                conversationId = group.id,
                conversationName = group.name
            ))
        }
    }

    fun openExistingConversation(conversationId: String, conversationName: String) {
        val projectId = currentProjectId ?: return

        viewModelScope.launch {
            _effect.emit(
                NewMessageEffect.NavigateToChat(
                projectId = projectId,
                conversationId = conversationId,
                conversationName = conversationName
            ))
        }
    }

    fun openConversation(conversation: com.phuongthanh.effiwork_android.data.model.response.chat.ChatConversationResponse) {
        val projectId = currentProjectId ?: return
        val currentUserId = authRepository.getCurrentUserId() ?: ""

        val name = when {
            conversation.name != null -> conversation.name
            conversation.type == com.phuongthanh.effiwork_android.data.model.chat.ChatConversationType.PRIVATE -> {
                conversation.members?.find { it.userId != currentUserId }?.user?.fullName
            }
            else -> conversation.members?.firstOrNull()?.user?.fullName
        } ?: "Chat"

        viewModelScope.launch {
            _effect.emit(
                NewMessageEffect.NavigateToChat(
                projectId = projectId,
                conversationId = conversation.id,
                conversationName = name
            ))
        }
    }

    fun createGroupConversation(
        projectId: String,
        name: String?,
        memberIds: List<String>,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = NewMessageUiState.Loading

            when (val result = chatRepository.createGroupConversation(projectId, name, memberIds)) {
                is ApiResult.Success -> {
                    _effect.emit(
                        NewMessageEffect.NavigateToChat(
                        projectId = projectId,
                        conversationId = result.data.id,
                        conversationName = result.data.name ?: "Group"
                    ))
                    loadProjectData(projectId)
                    // IMPROVEMENT #5: onComplete không cần gọi ở đây - LaunchedEffect sẽ xử lý
                }
                is ApiResult.Error -> {
                    _uiState.value = NewMessageUiState.Error(result.message)
                    _effect.emit(NewMessageEffect.ShowToast(result.message))
                    onComplete()  // IMPROVEMENT #2: Error vẫn cần reset
                }
                is ApiResult.Loading -> { }
            }
        }
    }

    fun loadTaskDetail(taskId: String) {
        val projectId = currentProjectId ?: return

        viewModelScope.launch {
            val current = _uiState.value
            if (current is NewMessageUiState.Success) {
                _uiState.value = current.copy(
                    isLoadingTaskDetail = true,
                    selectedTaskDetail = null
                )
            }

            when (val result = taskRepository.getTaskDetail(projectId, taskId)) {
                is ApiResult.Success -> {
                    val updated = _uiState.value
                    if (updated is NewMessageUiState.Success) {
                        _uiState.value = updated.copy(
                            selectedTaskDetail = result.data,
                            isLoadingTaskDetail = false
                        )
                    }
                }
                is ApiResult.Error -> {
                    val updated = _uiState.value
                    if (updated is NewMessageUiState.Success) {
                        _uiState.value = updated.copy(isLoadingTaskDetail = false)
                    }
                    _effect.emit(NewMessageEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> { }
            }
        }
    }

    fun loadParentTasksForSection(sectionId: String) {
        val projectId = currentProjectId ?: return

        viewModelScope.launch {
            val current = _uiState.value
            if (current is NewMessageUiState.Success) {
                _uiState.value = current.copy(
                    isLoadingParentTasks = true,
                    currentParentTasks = emptyList(),
                    currentSubtasks = emptyList(),
                    isLoadingSubtasks = false,
                    selectedTaskDetail = null,
                    isLoadingTaskDetail = false
                )
            }

            when (val result = taskRepository.getTasks(projectId, sectionId = sectionId)) {
                is ApiResult.Success -> {
                    val parents = result.data
                        .filter { it.parentTaskId.isNullOrBlank() }
                        .map { task ->
                            TaskSummary(
                                id = task.id,
                                name = task.name ?: "Không tên",
                                assigneeName = task.assigneeName ?: task.owner?.fullName,
                                participantCount = task.participants?.size ?: 0
                            )
                        }
                    val updated = _uiState.value
                    if (updated is NewMessageUiState.Success) {
                        _uiState.value = updated.copy(
                            currentParentTasks = parents,
                            isLoadingParentTasks = false
                        )
                    }
                }
                is ApiResult.Error -> {
                    val updated = _uiState.value
                    if (updated is NewMessageUiState.Success) {
                        _uiState.value = updated.copy(isLoadingParentTasks = false)
                    }
                    _effect.emit(NewMessageEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> { }
            }
        }
    }

    fun loadSubtasksForParent(parentTaskId: String) {
        val projectId = currentProjectId ?: return

        viewModelScope.launch {
            val current = _uiState.value
            if (current is NewMessageUiState.Success) {
                _uiState.value = current.copy(
                    isLoadingSubtasks = true,
                    currentSubtasks = emptyList(),
                    selectedTaskDetail = null,
                    isLoadingTaskDetail = false
                )
            }

            when (val result = taskRepository.getSubtasks(projectId, parentTaskId)) {
                is ApiResult.Success -> {
                    val subs = result.data.map { task ->
                        TaskSummary(
                            id = task.id,
                            name = task.name ?: "Không tên",
                            assigneeName = task.assigneeName ?: task.owner?.fullName,
                            participantCount = task.participants?.size ?: 0
                        )
                    }
                    val updated = _uiState.value
                    if (updated is NewMessageUiState.Success) {
                        _uiState.value = updated.copy(
                            currentSubtasks = subs,
                            isLoadingSubtasks = false
                        )
                    }
                }
                is ApiResult.Error -> {
                    val updated = _uiState.value
                    if (updated is NewMessageUiState.Success) {
                        _uiState.value = updated.copy(isLoadingSubtasks = false)
                    }
                    _effect.emit(NewMessageEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> { }
            }
        }
    }

    fun createGroupFromTask(
        projectId: String,
        taskId: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val current = _uiState.value
            val task = (current as? NewMessageUiState.Success)?.selectedTaskDetail
            if (task == null || task.id != taskId) {
                _effect.emit(NewMessageEffect.ShowToast("Vui lòng chọn lại công việc"))
                onComplete()
                return@launch
            }

            val currentUserId = authRepository.getCurrentUserId() ?: ""
            val memberIds = LinkedHashSet<String>()
            task.assigneeId?.takeIf { it.isNotBlank() }?.let { memberIds.add(it) }
            task.participants?.forEach { p ->
                p.user?.id?.takeIf { it.isNotBlank() }?.let { memberIds.add(it) }
            }
            memberIds.remove(currentUserId)

            if (memberIds.isEmpty()) {
                _effect.emit(NewMessageEffect.ShowToast("Công việc chưa có thành viên nào"))
                onComplete()
                return@launch
            }

            _uiState.value = NewMessageUiState.Loading

            val name = task.title?.takeIf { it.isNotBlank() } ?: "Nhóm"
            when (val result = chatRepository.createGroupConversation(projectId, name, memberIds.toList())) {
                is ApiResult.Success -> {
                    _effect.emit(
                        NewMessageEffect.NavigateToChat(
                            projectId = projectId,
                            conversationId = result.data.id,
                            conversationName = result.data.name ?: name
                        )
                    )
                    loadProjectData(projectId)
                }
                is ApiResult.Error -> {
                    _uiState.value = NewMessageUiState.Error(result.message)
                    _effect.emit(NewMessageEffect.ShowToast(result.message))
                    onComplete()
                }
                is ApiResult.Loading -> { }
            }
        }
    }

    fun refresh(projectId: String) {
        loadProjectData(projectId)
    }
}