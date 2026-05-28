package com.phuongthanh.effiwork_android.api

import com.phuongthanh.effiwork_android.data.model.request.SaveFcmTokenRequest
import com.phuongthanh.effiwork_android.data.model.response.ApiResponse
import com.phuongthanh.effiwork_android.data.model.response.ListNotificationsResponse
import com.phuongthanh.effiwork_android.data.model.response.NotificationResponse
import retrofit2.http.*

interface NotificationService {
    @GET("v1/notifications")
    suspend fun getNotifications(
        @Query("page") page: Int? = 1,
        @Query("limit") limit: Int? = 20,
        @Query("unreadOnly") unreadOnly: Boolean? = null
    ): ApiResponse<ListNotificationsResponse>

    @PATCH("v1/notifications/{notificationId}/read")
    suspend fun markAsRead(
        @Path("notificationId") notificationId: String
    ): ApiResponse<NotificationResponse>

    @PATCH("v1/notifications/{notificationId}/unread")
    suspend fun markAsUnread(
        @Path("notificationId") notificationId: String
    ): ApiResponse<NotificationResponse>

    @PATCH("v1/notifications/read-all")
    suspend fun markAllAsRead(): ApiResponse<Unit>

    @POST("v1/notifications/fcm-tokens")
    suspend fun saveFcmToken(
        @Body request: SaveFcmTokenRequest
    ): ApiResponse<Unit>
}