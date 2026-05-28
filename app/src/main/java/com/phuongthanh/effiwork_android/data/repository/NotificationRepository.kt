package com.phuongthanh.effiwork_android.data.repository

import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.model.response.NotificationListResponse
import com.phuongthanh.effiwork_android.data.model.response.NotificationResponse

interface NotificationRepository {
    suspend fun getNotifications(page: Int = 1, limit: Int = 20, unreadOnly: Boolean? = null): ApiResult<NotificationListResponse>
    suspend fun markAsRead(notificationId: String): ApiResult<NotificationResponse>
    suspend fun markAsUnread(notificationId: String): ApiResult<NotificationResponse>
    suspend fun markAllAsRead(): ApiResult<Unit>
    suspend fun saveFcmToken(token: String, deviceName: String?): ApiResult<Unit>
}