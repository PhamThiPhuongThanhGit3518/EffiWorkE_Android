package com.phuongthanh.effiwork_android.data.model.response.chat

data class ListMessagesResponse(
    val items: List<ChatMessageResponse>,
    val meta: PaginationMeta
)

data class PaginationMeta(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int
)