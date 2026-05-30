package com.phuongthanh.effiwork_android.data.model.response.chat

data class WebListMessagesResponse(
    val success: Boolean,
    val message: String,
    val data: List<ChatMessageResponse>,
    val meta: PaginationMeta
)