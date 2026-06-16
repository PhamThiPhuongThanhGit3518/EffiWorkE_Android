package com.phuongthanh.effiwork_android.viewmodel.chatbot

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.model.chatbot.ChatStreamEvent
import com.phuongthanh.effiwork_android.data.repository.ChatbotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatbotViewModel @Inject constructor(
    private val chatbotRepository: ChatbotRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ChatbotVM"
    }

    private val _uiState = MutableStateFlow<ChatbotUiState>(ChatbotUiState.Idle)
    val uiState: StateFlow<ChatbotUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ChatbotEffect>()
    val effect: SharedFlow<ChatbotEffect> = _effect.asSharedFlow()

    private var projectId: String? = null
    private var streamJob: Job? = null
    private var loadJob: Job? = null
    private var loadEpoch = 0L

    fun loadHistory(projectId: String, refresh: Boolean = false) {
        Log.d(TAG, "loadHistory start projectId=$projectId refresh=$refresh currentState=${_uiState.value::class.simpleName}")
        this.projectId = projectId
        if (refresh) streamJob?.cancel()
        val epoch = ++loadEpoch
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (refresh) _uiState.value = ChatbotUiState.Loading
            when (val result = chatbotRepository.getHistory(projectId)) {
                is ApiResult.Success -> {
                    Log.d(TAG, "loadHistory SUCCESS epoch=$epoch/$loadEpoch msgs=${result.data.messages.size}")
                    if (epoch != loadEpoch) {
                        Log.w(TAG, "loadHistory STALE epoch=$epoch current=$loadEpoch, skip")
                        return@launch
                    }
                    val current = _uiState.value
                    val serverMessages = result.data.messages.map {
                        ChatbotMessageUi(
                            id = it.id,
                            role = if (it.role.equals("USER", ignoreCase = true)) ChatbotRole.USER else ChatbotRole.ASSISTANT,
                            content = it.content,
                            isStreaming = false
                        )
                    }
                    val existingOptimistic = (current as? ChatbotUiState.Success)
                        ?.messages
                        ?.filter { it.id.startsWith("user-") || it.id.startsWith("assistant-") }
                        ?: emptyList()
                    val merged = mergeMessages(serverMessages, existingOptimistic)
                    Log.d(TAG, "loadHistory merged: server=${serverMessages.size} optimistic=${existingOptimistic.size} total=${merged.size}")
                    _uiState.value = ChatbotUiState.Success(
                        messages = merged,
                        isStreaming = (_uiState.value as? ChatbotUiState.Success)?.isStreaming ?: false
                    )
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "loadHistory ERROR epoch=$epoch: ${result.message}")
                    if (epoch != loadEpoch) return@launch
                    if (_uiState.value !is ChatbotUiState.Success) {
                        _uiState.value = ChatbotUiState.Error(result.message)
                    }
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    private fun mergeMessages(
        server: List<ChatbotMessageUi>,
        optimistic: List<ChatbotMessageUi>
    ): List<ChatbotMessageUi> {
        if (optimistic.isEmpty()) return server
        val trailingOptimistic = mutableListOf<ChatbotMessageUi>()
        for (msg in optimistic) {
            val isServerKnown = msg.id.startsWith("user-") && server.any { it.role == ChatbotRole.USER && it.content == msg.content }
            if (!isServerKnown) trailingOptimistic.add(msg)
        }
        return server + trailingOptimistic
    }

    fun sendMessage(projectId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || isStreaming()) {
            Log.w(TAG, "sendMessage SKIP empty=$trimmed.isEmpty() streaming=${isStreaming()}")
            return
        }
        Log.d(TAG, "sendMessage START projectId=$projectId text='${trimmed.take(50)}'")
        this.projectId = projectId
        loadEpoch++
        loadJob?.cancel()

        val userMessage = ChatbotMessageUi(
            id = "user-${UUID.randomUUID()}",
            role = ChatbotRole.USER,
            content = trimmed
        )
        val assistantPlaceholder = ChatbotMessageUi(
            id = "assistant-${UUID.randomUUID()}",
            role = ChatbotRole.ASSISTANT,
            content = "",
            isStreaming = true
        )

        val current = _uiState.value
        val currentMessages = if (current is ChatbotUiState.Success) current.messages else emptyList()
        _uiState.value = ChatbotUiState.Success(
            messages = currentMessages + userMessage + assistantPlaceholder,
            isStreaming = true
        )
        Log.d(TAG, "sendMessage state set: msgs=${currentMessages.size + 2} streaming=true")
        viewModelScope.launch { _effect.emit(ChatbotEffect.ScrollToBottom) }

        streamJob = viewModelScope.launch {
            var pendingId = assistantPlaceholder.id
            var accumulated = ""
            Log.d(TAG, "streamJob started, collecting events...")
            chatbotRepository.streamMessage(projectId, trimmed).collect { event ->
                Log.d(TAG, "streamJob received: $event")
                when (event) {
                    is ChatStreamEvent.Start -> {
                        pendingId = event.messageId
                        updateAssistant(pendingId, accumulated, isStreaming = true)
                    }
                    is ChatStreamEvent.Token -> {
                        accumulated += event.text
                        updateAssistant(pendingId, accumulated, isStreaming = true)
                    }
                    is ChatStreamEvent.Done -> {
                        pendingId = event.messageId
                        val finalContent = if (event.content.isNotEmpty()) event.content else accumulated
                        updateAssistant(event.messageId, finalContent, isStreaming = false)
                        _uiState.value = (_uiState.value as? ChatbotUiState.Success)?.copy(isStreaming = false)
                            ?: ChatbotUiState.Success(emptyList(), isStreaming = false)
                        Log.d(TAG, "streamJob DONE, isStreaming=false")
                    }
                    is ChatStreamEvent.Error -> {
                        Log.e(TAG, "streamJob ERROR: ${event.message}")
                        removeAssistant()
                        _uiState.value = (_uiState.value as? ChatbotUiState.Success)?.copy(isStreaming = false)
                            ?: ChatbotUiState.Success(emptyList(), isStreaming = false)
                        _effect.emit(ChatbotEffect.ShowError(event.message))
                    }
                }
            }
            Log.d(TAG, "streamJob collect finished")
        }
    }

    fun reset(projectId: String) {
        Log.d(TAG, "reset projectId=$projectId")
        loadEpoch++
        loadJob?.cancel()
        viewModelScope.launch {
            when (val result = chatbotRepository.reset(projectId)) {
                is ApiResult.Success -> {
                    _uiState.value = ChatbotUiState.Success(messages = emptyList(), isStreaming = false)
                    _effect.emit(ChatbotEffect.ResetCompleted)
                }
                is ApiResult.Error -> {
                    _effect.emit(ChatbotEffect.ShowError(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    private fun isStreaming(): Boolean = (_uiState.value as? ChatbotUiState.Success)?.isStreaming == true

    private fun updateAssistant(id: String, content: String, isStreaming: Boolean) {
        val current = _uiState.value as? ChatbotUiState.Success
        if (current == null) {
            Log.w(TAG, "updateAssistant FAILED, current is not Success: ${_uiState.value::class.simpleName}")
            return
        }
        val newMessages = current.messages.map { m ->
            if (m.id == id || (m.role == ChatbotRole.ASSISTANT && m.isStreaming && m.content.isEmpty())) {
                m.copy(id = id, content = content, isStreaming = isStreaming)
            } else m
        }
        val found = newMessages.any { it.id == id && it.role == ChatbotRole.ASSISTANT }
        if (!found) {
            Log.w(TAG, "updateAssistant: no assistant msg with id=$id found, msgs=${current.messages.map { "${it.id}(${it.role},streaming=${it.isStreaming},len=${it.content.length})" }}")
        }
        _uiState.value = current.copy(messages = newMessages, isStreaming = isStreaming)
    }

    private fun removeAssistant() {
        val current = _uiState.value as? ChatbotUiState.Success ?: return
        val newMessages = current.messages.filterNot { it.isStreaming && it.role == ChatbotRole.ASSISTANT }
        _uiState.value = current.copy(messages = newMessages, isStreaming = false)
    }
}
