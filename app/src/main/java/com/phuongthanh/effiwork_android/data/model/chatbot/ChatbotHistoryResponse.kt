package com.phuongthanh.effiwork_android.data.model.chatbot

import com.google.gson.annotations.SerializedName

data class ChatbotMessageDto(
    val id: String,
    val role: String,
    val content: String,
    @SerializedName("createdAt") val createdAt: String?
)

data class ChatbotHistoryResponse(
    @SerializedName("conversationId") val conversationId: String,
    val messages: List<ChatbotMessageDto>
)

sealed class ChatStreamEvent {
    data class Start(val messageId: String) : ChatStreamEvent()
    data class Token(val text: String) : ChatStreamEvent()
    data class Done(val messageId: String, val content: String) : ChatStreamEvent()
    data class Error(val code: String, val message: String) : ChatStreamEvent()
}
