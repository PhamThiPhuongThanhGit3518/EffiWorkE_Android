package com.phuongthanh.effiwork_android.ui.screen.chat.item

import com.phuongthanh.effiwork_android.data.model.chat.ChatMessageType
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatMessageResponse

fun String.normalizeMessage(): String? {
    val trimmed = trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.equals("null", ignoreCase = true)) return null
    return trimmed
}

fun ChatMessageResponse.previewText(): String {
    val caption = content?.normalizeMessage()
    return when (type) {
        ChatMessageType.TEXT, ChatMessageType.SYSTEM -> caption ?: ""
        ChatMessageType.IMAGE -> {
            val name = document?.fileName?.takeIf { it.isNotBlank() }
            when {
                !caption.isNullOrEmpty() && name != null -> "📷 $name"
                !caption.isNullOrEmpty() -> "📷 $caption"
                name != null -> "📷 Hình ảnh: $name"
                else -> "📷 Hình ảnh"
            }
        }
        ChatMessageType.FILE -> {
            val name = document?.fileName?.takeIf { it.isNotBlank() }
            when {
                !caption.isNullOrEmpty() && name != null -> "📎 $name: $caption"
                !caption.isNullOrEmpty() -> "📎 $caption"
                name != null -> "📎 $name"
                else -> "📎 Tệp đính kèm"
            }
        }
    }
}
