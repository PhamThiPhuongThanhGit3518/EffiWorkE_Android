package com.phuongthanh.effiwork_android.viewmodel.chat.state

import com.phuongthanh.effiwork_android.data.model.response.chat.ChatMessageResponse

sealed class ChatUiState {
    object Idle : ChatUiState()
    object Loading : ChatUiState()
    data class Success(
        val messages: List<ChatMessageResponse>,
        val page: Int,
        val totalPages: Int,
        val isLoadingMore: Boolean = false,
        val isSending: Boolean = false
    ) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}