package com.phuongthanh.effiwork_android.viewmodel.chatbot

sealed class ChatbotUiState {
    object Idle : ChatbotUiState()
    object Loading : ChatbotUiState()
    data class Success(
        val messages: List<ChatbotMessageUi>,
        val isStreaming: Boolean = false
    ) : ChatbotUiState()
    data class Error(val message: String) : ChatbotUiState()
}

data class ChatbotMessageUi(
    val id: String,
    val role: ChatbotRole,
    val content: String,
    val isStreaming: Boolean = false
)

enum class ChatbotRole { USER, ASSISTANT }
