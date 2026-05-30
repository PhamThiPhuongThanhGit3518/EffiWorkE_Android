package com.phuongthanh.effiwork_android.data.model.response.chat

import com.phuongthanh.effiwork_android.data.model.chat.ChatMessageType

data class ChatMessageResponse(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val type: ChatMessageType,
    val content: String?,
    val documentId: String?,
    val createdAt: String,
    val updatedAt: String?,
    val deletedAt: String?,
    val sender: ChatUserResponse?,
    val document: ChatDocumentResponse?
)