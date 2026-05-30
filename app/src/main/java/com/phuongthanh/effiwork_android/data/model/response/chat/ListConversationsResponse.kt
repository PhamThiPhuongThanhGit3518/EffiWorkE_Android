package com.phuongthanh.effiwork_android.data.model.response.chat

data class ListConversationsResponse(
    val items: List<ChatConversationResponse>,
    val meta: PaginationMeta
)