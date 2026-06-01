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
import kotlinx.coroutines.flow.first
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
        android.util.Log.d("ViewModelDebug", ">>> observeSocketEvents started, currentConversationId=$currentConversationId")
        viewModelScope.launch {
            android.util.Log.d("ViewModelDebug", "observeSocketEvents: STARTED collecting newMessageFlow")
            socketManager.newMessageFlow.collect { event ->
                android.util.Log.d("ViewModelDebug", ">>> newMessageFlow received: messageId=${event.message.id}, convId=${event.message.conversationId}")
                android.util.Log.d("ViewModelDebug", "  currentConversationId = $currentConversationId")
                val currentConvId = currentConversationId
                android.util.Log.d("ViewModelDebug", "  match = ${event.message.conversationId == currentConvId}")
                if (event.message.conversationId == currentConvId) {
                    val currentState = _uiState.value
                    android.util.Log.d("ViewModelDebug", "  currentState type = ${currentState::class.simpleName}")
                    if (currentState is ChatUiState.Success) {
                        android.util.Log.d("ViewModelDebug", "  messages count before = ${currentState.messages.size}")
                        if (currentState.messages.none { it.id == event.message.id }) {
                            val updatedMessages = currentState.messages + event.message
                            _uiState.value = currentState.copy(messages = updatedMessages)
                            android.util.Log.d("ViewModelDebug", "  messages count after = ${updatedMessages.size}, UI state updated")
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
            android.util.Log.d("ViewModelDebug", "observeSocketEvents: COLLECTOR COMPLETED (should not happen)")
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
                    // Add sent message to the list immediately
                    val currentState = _uiState.value as? ChatUiState.Success
                    if (currentState != null && currentState.messages.none { it.id == result.data.id }) {
                        val updatedMessages = currentState.messages + result.data
                        _uiState.value = currentState.copy(messages = updatedMessages)
                        android.util.Log.d("ViewModelDebug", "sendMessage: added to list, total=${updatedMessages.size}")
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

    override fun onCleared() {
        super.onCleared()
        leaveConversation()
    }
}