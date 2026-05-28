package com.phuongthanh.effiwork_android.data.model.response

import com.google.gson.annotations.SerializedName

data class NotificationResponse(
    val id: String,
    val userId: String?,
    val title: String?,
    val message: String?,
    val type: String?,
    val isRead: Boolean?,
    @SerializedName("readAt") val readAt: String?,
    val data: NotificationData?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("updatedAt") val updatedAt: String?
)

data class NotificationData(
    val taskId: String?,
    val projectId: String?,
    val meetingId: String?,
    val commentId: String?,
    val senderId: String?,
    val senderName: String?,
    val avatarUrl: String?
)

data class ListNotificationsResponse(
    val page: Int?,
    val limit: Int?,
    val total: Int?,
    val totalPages: Int?,
    val items: List<NotificationResponse>?
)