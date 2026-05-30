package com.phuongthanh.effiwork_android.viewmodel.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.model.chat.ChatMessageType
import com.phuongthanh.effiwork_android.data.model.request.chat.CreateChatMessageRequest
import com.phuongthanh.effiwork_android.data.repository.chat.ChatRepository
import com.phuongthanh.effiwork_android.data.socket.ChatSocketManager
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
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val socketManager: ChatSocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ChatEffect>()
    val effect: SharedFlow<ChatEffect> = _effect.asSharedFlow()

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
                if (event.message.conversationId == currentConversationId) {
                    val currentState = _uiState.value
                    if (currentState is ChatUiState.Success) {
                        if (currentState.messages.none { it.id == event.message.id }) {
                            val updatedMessages = currentState.messages + event.message
                            _uiState.value = currentState.copy(messages = updatedMessages)
                            _effect.emit(ChatEffect.NewMessageReceived(event.message))
                            _effect.emit(ChatEffect.ScrollToBottom)
                        }
                    }
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
                    val messages = data.items.reversed()

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

        if (content.isBlank() && type == ChatMessageType.TEXT) {
            viewModelScope.launch {
                _effect.emit(ChatEffect.ShowToast("Message cannot be empty"))
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
                content = content.takeIf { type == ChatMessageType.TEXT || type == ChatMessageType.SYSTEM },
                documentId = documentId
            )

            when (val result = chatRepository.sendMessage(projectId, conversationId, request)) {
                is ApiResult.Success -> {
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
        currentProjectId = projectId
        currentConversationId = conversationId
        socketManager.joinConversation(projectId, conversationId)
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

    override fun onCleared() {
        super.onCleared()
        leaveConversation()
    }
}