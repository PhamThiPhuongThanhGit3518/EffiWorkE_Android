package com.phuongthanh.effiwork_android.viewmodel.chat

import androidx.compose.ui.graphics.Color

data class PrivateChatItem(
    val id: String,
    val displayName: String,
    val avatarColor: Color,
    val unreadCount: Int,
    val lastMessage: String?,
    val lastMessageAt: String?,
    val email: String?
)