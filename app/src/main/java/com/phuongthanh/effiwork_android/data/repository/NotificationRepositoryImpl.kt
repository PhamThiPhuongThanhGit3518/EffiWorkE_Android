package com.phuongthanh.effiwork_android.data.repository

import android.util.Log
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.api.NotificationService
import com.phuongthanh.effiwork_android.data.model.request.SaveFcmTokenRequest
import com.phuongthanh.effiwork_android.data.model.response.NotificationListResponse
import com.phuongthanh.effiwork_android.data.model.response.NotificationResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NotificationRepository"

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationService: NotificationService
) : NotificationRepository {

    private val _notificationsFlow = MutableStateFlow<List<NotificationResponse>>(emptyList())
    override val notificationsFlow: StateFlow<List<NotificationResponse>> = _notificationsFlow.asStateFlow()

    override suspend fun getNotifications(page: Int, limit: Int, unreadOnly: Boolean?): ApiResult<NotificationListResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "getNotifications: page=$page, limit=$limit, unreadOnly=$unreadOnly")
                val unreadOnlyParam = if (unreadOnly == true) true else null
                val response = notificationService.getNotifications(page, limit, unreadOnlyParam)
                Log.d(TAG, "getNotifications response: page=${response.page}, total=${response.total}, totalPages=${response.totalPages}, data size = ${response.data.size}")
                if (response.data.isEmpty()) {
                    Log.w(TAG, "WARNING: Empty notifications list - total=0 means server returned no notifications. Check: 1) Auth token correct? 2) User has notifications on backend? 3) Backend filtering issue?")
                }
                response.data.forEachIndexed { index, notif ->
                    Log.d(TAG, "Notification[$index]: id=${notif.id}, title=${notif.title}, message=${notif.message}, type=${notif.type}, isRead=${notif.isRead}, createdAt=${notif.createdAt}")
                }
                if (page == 1) {
                    _notificationsFlow.value = response.data
                } else {
                    _notificationsFlow.update { current -> current + response.data }
                }
                ApiResult.Success(response)
            } catch (e: Exception) {
                Log.e(TAG, "getNotifications EXCEPTION: ${e.message}", e)
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun markAsRead(notificationId: String): ApiResult<NotificationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = notificationService.markAsRead(notificationId)
                if (response.success && response.data != null) {
                    updateLocalItem(response.data)
                    ApiResult.Success(response.data)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun markAsUnread(notificationId: String): ApiResult<NotificationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = notificationService.markAsUnread(notificationId)
                if (response.success && response.data != null) {
                    updateLocalItem(response.data)
                    ApiResult.Success(response.data)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun markAllAsRead(): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = notificationService.markAllAsRead()
                if (response.success) {
                    _notificationsFlow.update { current ->
                        current.map { it.copy(isRead = true) }
                    }
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun saveFcmToken(token: String, deviceName: String?): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = notificationService.saveFcmToken(SaveFcmTokenRequest(token, deviceName))
                if (response.success) {
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun upsertNotification(item: NotificationResponse) {
        _notificationsFlow.update { current ->
            val index = current.indexOfFirst { it.id == item.id }
            if (index >= 0) {
                current.toMutableList().apply { this[index] = item }
            } else {
                listOf(item) + current
            }
        }
    }

    private fun updateLocalItem(item: NotificationResponse) {
        _notificationsFlow.update { current ->
            val index = current.indexOfFirst { it.id == item.id }
            if (index >= 0) {
                current.toMutableList().apply { this[index] = item }
            } else {
                current
            }
        }
    }
}
