package com.phuongthanh.effiwork_android.data.model.request.chat

data class UpdateChatConversationRequest(
    val name: String? = null,
    val addMemberIds: List<String>? = null
)