package com.phuongthanh.effiwork_android.data.model.request.chat

data class CreateGroupConversationRequest(
    val name: String? = null,
    val memberIds: List<String>
)