package com.phuongthanh.effiwork_android.data.socket

import com.phuongthanh.effiwork_android.data.model.chat.ChatMessageType
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatMessageResponse
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatUserResponse
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatConversationResponse

// Client → Server Payloads
data class JoinConversationPayload(
    val projectId: String,
    val conversationId: String
)

data class LeaveConversationPayload(
    val conversationId: String
)

data class SendMessageSocketPayload(
    val projectId: String,
    val conversationId: String,
    val message: SendMessageDto
)

data class SendMessageDto(
    val type: ChatMessageType = ChatMessageType.TEXT,
    val content: String? = null,
    val documentId: String? = null
)

// Server → Client Events
data class NewMessageEvent(
    val message: ChatMessageResponse
)

data class ConversationUpdatedEvent(
    val conversation: ChatConversationResponse
)

data class IncomingCallEvent(
    val callId: String,
    val conversationId: String,
    val projectId: String,
    val mode: String,
    val initiator: ChatUserResponse
)