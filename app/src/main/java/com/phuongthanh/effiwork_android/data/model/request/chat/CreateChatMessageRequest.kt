package com.phuongthanh.effiwork_android.data.model.request.chat

import com.phuongthanh.effiwork_android.data.model.chat.ChatMessageType

data class CreateChatMessageRequest(
    val type: ChatMessageType = ChatMessageType.TEXT,
    val content: String? = null,
    val documentId: String? = null
)