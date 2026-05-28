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
    val unreadCount: Int?,
    val items: List<NotificationResponse>?
)

data class NotificationListData(
    val items: List<NotificationResponse>,
    val meta: PaginationMeta
)

data class PaginationMeta(
    val page: Int?,
    val limit: Int?,
    val total: Int?,
    val totalPages: Int?,
    val unreadCount: Int?
)

data class NotificationListResponse(
    val data: List<NotificationResponse>,
    val meta: PaginationMeta?
) {
    val page: Int get() = meta?.page ?: 1
    val limit: Int get() = meta?.limit ?: 20
    val total: Int get() = meta?.total ?: 0
    val totalPages: Int get() = meta?.totalPages ?: 1
    val unreadCount: Int get() = meta?.unreadCount ?: 0
}