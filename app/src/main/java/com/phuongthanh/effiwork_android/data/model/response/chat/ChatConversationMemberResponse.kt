package com.phuongthanh.effiwork_android.data.model.response.chat

data class ChatConversationMemberResponse(
    val id: String,
    val conversationId: String,
    val userId: String,
    val joinedAt: String,
    val user: ChatUserResponse?
)