package com.phuongthanh.effiwork_android.viewmodel.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.model.chat.ChatMessageType
import com.phuongthanh.effiwork_android.data.model.request.chat.AddMembersRequest
import com.phuongthanh.effiwork_android.data.model.request.chat.CreateChatMessageRequest
import com.phuongthanh.effiwork_android.data.model.request.chat.RemoveMembersRequest
import com.phuongthanh.effiwork_android.data.model.request.chat.UpdateChatConversationRequest
import com.phuongthanh.effiwork_android.data.model.response.ProjectMemberResponse
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatConversationDetailResponse
import com.phuongthanh.effiwork_android.data.repository.ProjectRepository
import com.phuongthanh.effiwork_android.data.repository.chat.ChatRepository
import com.phuongthanh.effiwork_android.data.socket.ChatSocketManager
import com.phuongthanh.effiwork_android.viewmodel.chat.state.ChatEffect
import com.phuongthanh.effiwork_android.viewmodel.chat.state.ChatUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val projectRepository: ProjectRepository,
    private val socketManager: ChatSocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ChatEffect>()
    val effect: SharedFlow<ChatEffect> = _effect.asSharedFlow()

    private val _conversationDetail = MutableStateFlow<ChatConversationDetailResponse?>(null)
    val conversationDetail: StateFlow<ChatConversationDetailResponse?> = _conversationDetail.asStateFlow()

    private val _projectMembers = MutableStateFlow<List<ProjectMemberResponse>>(emptyList())
    val projectMembers: StateFlow<List<ProjectMemberResponse>> = _projectMembers.asStateFlow()

    private val _isGroupMutating = MutableStateFlow(false)
    val isGroupMutating: StateFlow<Boolean> = _isGroupMutating.asStateFlow()

    private var currentPage = 1
    private var totalPages = 1
    private val pageSize = 30

    private var currentProjectId: String? = null
    private var currentConversationId: String? = null

    init {
        observeSocketEvents()
    }

    private fun observeSocketEvents() {
        viewModelScope.launch {
            socketManager.newMessageFlow.collect { event ->
                val currentConvId = currentConversationId
                if (event.message.conversationId == currentConvId) {
                    val currentState = _uiState.value
                    if (currentState is ChatUiState.Success) {
                        if (currentState.messages.none { it.id == event.message.id }) {
                            val updatedMessages = currentState.messages + event.message
                            _uiState.value = currentState.copy(messages = updatedMessages)
                            viewModelScope.launch {
                                _effect.emit(ChatEffect.NewMessageReceived(event.message))
                                _effect.emit(ChatEffect.ScrollToBottom)
                            }
                        } else {
                            android.util.Log.d("ViewModelDebug", "  duplicate message, skipping")
                        }
                    } else {
                        android.util.Log.w("ViewModelDebug", "  currentState is not Success, cannot update messages")
                    }
                } else {
                    android.util.Log.d("ViewModelDebug", "  conversationId mismatch, ignoring")
                }
            }
        }
    }

    fun loadMessages(projectId: String, conversationId: String, refresh: Boolean = false) {
        if (refresh) {
            currentPage = 1
        }
        currentProjectId = projectId
        currentConversationId = conversationId

        viewModelScope.launch {
            _uiState.value = if (refresh) ChatUiState.Loading else _uiState.value

            when (val result = chatRepository.getMessages(projectId, conversationId, currentPage, pageSize, null)) {
                is ApiResult.Success -> {
                    val data = result.data
                    currentPage = data.meta.page
                    totalPages = data.meta.totalPages
                    val messages = data.items

                    _uiState.value = ChatUiState.Success(
                        messages = messages,
                        page = currentPage,
                        totalPages = totalPages
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = ChatUiState.Error(result.message)
                }
                is ApiResult.Loading -> {
                    _uiState.value = ChatUiState.Loading
                }
            }
        }
    }

    fun loadMoreMessages() {
        val currentState = _uiState.value
        if (currentState !is ChatUiState.Success || currentState.isLoadingMore) return
        if (currentPage >= totalPages) return

        currentPage++
        _uiState.value = currentState.copy(isLoadingMore = true)

        viewModelScope.launch {
            val projectId = currentProjectId ?: return@launch
            val conversationId = currentConversationId ?: return@launch

            when (val result = chatRepository.getMessages(projectId, conversationId, currentPage, pageSize, null)) {
                is ApiResult.Success -> {
                    val data = result.data
                    currentPage = data.meta.page
                    totalPages = data.meta.totalPages
                    val newMessages = data.items

                    _uiState.value = currentState.copy(
                        messages = newMessages + currentState.messages,
                        page = currentPage,
                        totalPages = totalPages,
                        isLoadingMore = false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = currentState.copy(isLoadingMore = false)
                    _effect.emit(ChatEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun sendMessage(content: String, type: ChatMessageType = ChatMessageType.TEXT, documentId: String? = null) {
        val projectId = currentProjectId ?: return
        val conversationId = currentConversationId ?: return

        if (content.isBlank() && documentId == null) {
            viewModelScope.launch {
                _effect.emit(ChatEffect.ShowToast("Tin nhắn không được trống"))
            }
            return
        }

        val currentState = _uiState.value
        if (currentState is ChatUiState.Success) {
            _uiState.value = currentState.copy(isSending = true)
        }

        viewModelScope.launch {
            val request = CreateChatMessageRequest(
                type = type,
                content = content.ifBlank { null },
                documentId = documentId
            )

            when (val result = chatRepository.sendMessage(projectId, conversationId, request)) {
                is ApiResult.Success -> {
                    val currentState = _uiState.value as? ChatUiState.Success
                    if (currentState != null && currentState.messages.none { it.id == result.data.id }) {
                        val updatedMessages = currentState.messages + result.data
                        _uiState.value = currentState.copy(messages = updatedMessages)
                    }
                    _effect.emit(ChatEffect.MessageSent(result.data))
                    _effect.emit(ChatEffect.ScrollToBottom)
                    _uiState.value = (_uiState.value as? ChatUiState.Success)?.copy(isSending = false) ?: _uiState.value
                }
                is ApiResult.Error -> {
                    _effect.emit(ChatEffect.ShowToast(result.message))
                    _uiState.value = (_uiState.value as? ChatUiState.Success)?.copy(isSending = false) ?: _uiState.value
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun joinConversation(projectId: String, conversationId: String) {
        android.util.Log.d("ViewModelDebug", ">>> joinConversation($projectId, $conversationId)")
        currentProjectId = projectId
        currentConversationId = conversationId
        android.util.Log.d("ViewModelDebug", "  currentProjectId=$currentProjectId, currentConversationId=$currentConversationId")

        viewModelScope.launch {
            android.util.Log.d("ViewModelDebug", "  checking connection state...")
            val currentState = socketManager.connectionStateFlow.first()
            android.util.Log.d("ViewModelDebug", "  connectionState = $currentState")

            when (currentState) {
                ChatSocketManager.ConnectionState.CONNECTED -> {
                    android.util.Log.d("ViewModelDebug", "  state CONNECTED, calling socketManager.joinConversation")
                    socketManager.joinConversation(projectId, conversationId)
                }
                ChatSocketManager.ConnectionState.CONNECTING -> {
                    android.util.Log.d("ViewModelDebug", "  state CONNECTING, calling waitForConnection")
                    socketManager.waitForConnection()
                    android.util.Log.d("ViewModelDebug", "  waitForConnection returned, calling joinConversation")
                    socketManager.joinConversation(projectId, conversationId)
                }
                ChatSocketManager.ConnectionState.DISCONNECTED,
                ChatSocketManager.ConnectionState.ERROR -> {
                    android.util.Log.d("ViewModelDebug", "  state $currentState, calling connect()")
                    socketManager.connect()
                    android.util.Log.d("ViewModelDebug", "  connect() called, calling waitForConnection")
                    socketManager.waitForConnection()
                    android.util.Log.d("ViewModelDebug", "  waitForConnection returned, calling joinConversation")
                    socketManager.joinConversation(projectId, conversationId)
                }
            }
            android.util.Log.d("ViewModelDebug", "  joinConversation completed")
        }
    }

    fun leaveConversation() {
        currentConversationId?.let { socketManager.leaveConversation(it) }
    }

    fun markAsRead() {
        val projectId = currentProjectId ?: return
        val conversationId = currentConversationId ?: return

        viewModelScope.launch {
            chatRepository.markAsRead(projectId, conversationId)
        }
    }

    fun refresh() {
        val projectId = currentProjectId ?: return
        val conversationId = currentConversationId ?: return
        loadMessages(projectId, conversationId, refresh = true)
    }

    fun loadConversationDetail() {
        val projectId = currentProjectId ?: return
        val conversationId = currentConversationId ?: return
        viewModelScope.launch {
            when (val result = chatRepository.getConversationDetail(projectId, conversationId)) {
                is ApiResult.Success -> {
                    _conversationDetail.value = result.data
                }
                is ApiResult.Error -> {
                    _effect.emit(ChatEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun loadProjectMembers() {
        val projectId = currentProjectId ?: return
        viewModelScope.launch {
            when (val result = projectRepository.getProjectMembers(projectId)) {
                is ApiResult.Success -> {
                    _projectMembers.value = result.data
                }
                is ApiResult.Error -> {}
                is ApiResult.Loading -> {}
            }
        }
    }

    fun renameGroup(newName: String) {
        val projectId = currentProjectId ?: return
        val conversationId = currentConversationId ?: return
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            viewModelScope.launch { _effect.emit(ChatEffect.ShowToast("Tên nhóm không được trống")) }
            return
        }
        viewModelScope.launch {
            _isGroupMutating.value = true
            when (val result = chatRepository.updateConversation(
                projectId, conversationId, UpdateChatConversationRequest(name = trimmed)
            )) {
                is ApiResult.Success -> {
                    loadConversationDetail()
                    _effect.emit(ChatEffect.GroupNameUpdated(trimmed))
                    _effect.emit(ChatEffect.ShowToast("Đã đổi tên nhóm"))
                }
                is ApiResult.Error -> {
                    _effect.emit(ChatEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
            _isGroupMutating.value = false
        }
    }

    fun addMembers(memberIds: List<String>) {
        val projectId = currentProjectId ?: return
        val conversationId = currentConversationId ?: return
        if (memberIds.isEmpty()) return
        viewModelScope.launch {
            _isGroupMutating.value = true
            when (val result = chatRepository.addMembers(
                projectId, conversationId, AddMembersRequest(memberIds)
            )) {
                is ApiResult.Success -> {
                    loadConversationDetail()
                    _effect.emit(ChatEffect.ShowToast("Đã thêm thành viên"))
                }
                is ApiResult.Error -> {
                    _effect.emit(ChatEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
            _isGroupMutating.value = false
        }
    }

    fun removeMembers(userIds: List<String>) {
        val projectId = currentProjectId ?: return
        val conversationId = currentConversationId ?: return
        if (userIds.isEmpty()) return
        viewModelScope.launch {
            _isGroupMutating.value = true
            when (val result = chatRepository.removeMembers(
                projectId, conversationId, RemoveMembersRequest(userIds)
            )) {
                is ApiResult.Success -> {
                    loadConversationDetail()
                    _effect.emit(ChatEffect.ShowToast("Đã xóa thành viên"))
                }
                is ApiResult.Error -> {
                    _effect.emit(ChatEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
            _isGroupMutating.value = false
        }
    }

    fun leaveGroup() {
        val projectId = currentProjectId ?: return
        val conversationId = currentConversationId ?: return
        viewModelScope.launch {
            _isGroupMutating.value = true
            when (val result = chatRepository.leaveConversation(projectId, conversationId)) {
                is ApiResult.Success -> {
                    _effect.emit(ChatEffect.LeftGroup)
                }
                is ApiResult.Error -> {
                    _effect.emit(ChatEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
            _isGroupMutating.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        leaveConversation()
    }
}