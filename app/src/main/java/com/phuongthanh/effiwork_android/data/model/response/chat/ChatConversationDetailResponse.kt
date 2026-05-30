package com.phuongthanh.effiwork_android.data.model.response.chat

import com.phuongthanh.effiwork_android.data.model.chat.ChatConversationType

data class ChatConversationDetailResponse(
    val id: String,
    val projectId: String,
    val name: String?,
    val type: ChatConversationType,
    val createdById: String,
    val lastMessageId: String?,
    val lastMessageAt: String?,
    val createdAt: String,
    val updatedAt: String?,
    val createdBy: ChatUserResponse?,
    val members: List<ChatConversationMemberResponse>?,
    val messages: List<ChatMessageResponse>?,
    val _count: ConversationCount?
)