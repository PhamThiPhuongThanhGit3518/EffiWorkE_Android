package com.phuongthanh.effiwork_android.viewmodel.notis

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.model.response.NotificationResponse
import com.phuongthanh.effiwork_android.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "NotificationViewModel"

sealed class NotificationUiState {
    object Idle : NotificationUiState()
    object Loading : NotificationUiState()
    data class Success(
        val notifications: List<NotificationResponse>,
        val page: Int,
        val totalPages: Int
    ) : NotificationUiState()
    data class Error(val message: String) : NotificationUiState()
}

sealed class NotificationEffect {
    data class ShowToast(val message: String) : NotificationEffect()
    object RefreshList : NotificationEffect()
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Idle)
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<NotificationEffect>()
    val effect: SharedFlow<NotificationEffect> = _effect.asSharedFlow()

    private val _unreadOnly = MutableStateFlow(false)
    val unreadOnly: StateFlow<Boolean> = _unreadOnly.asStateFlow()

    private var currentPage = 1
    private var totalPages = 1
    private val pageSize = 20

    fun loadNotifications(refresh: Boolean = false) {
        if (refresh) {
            currentPage = 1
        }

        viewModelScope.launch {
            _uiState.value = NotificationUiState.Loading
            Log.d(TAG, "loadNotifications: page=$currentPage, unreadOnly=${_unreadOnly.value}")
            when (val result = notificationRepository.getNotifications(currentPage, pageSize, _unreadOnly.value)) {
                is ApiResult.Success -> {
                    val data = result.data
                    currentPage = data.page
                    totalPages = data.totalPages
                    Log.d(TAG, "loadNotifications SUCCESS: notifications count=${data.data.size}, page=$currentPage, totalPages=$totalPages")
                    _uiState.value = NotificationUiState.Success(
                        notifications = data.data,
                        page = currentPage,
                        totalPages = totalPages
                    )
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "loadNotifications ERROR: ${result.message}")
                    _uiState.value = NotificationUiState.Error(result.message)
                }
                is ApiResult.Loading -> {
                    _uiState.value = NotificationUiState.Loading
                }
            }
        }
    }

    fun loadMoreNotifications() {
        if (currentPage >= totalPages) return
        currentPage++
        loadNotifications(refresh = false)
    }

    fun toggleUnreadFilter() {
        _unreadOnly.value = !_unreadOnly.value
        loadNotifications(refresh = true)
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            when (val result = notificationRepository.markAsRead(notificationId)) {
                is ApiResult.Success -> {
                    _effect.emit(NotificationEffect.ShowToast("Marked as read"))
                    loadNotifications(refresh = true)
                }
                is ApiResult.Error -> {
                    _effect.emit(NotificationEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun markAsUnread(notificationId: String) {
        viewModelScope.launch {
            when (val result = notificationRepository.markAsUnread(notificationId)) {
                is ApiResult.Success -> {
                    _effect.emit(NotificationEffect.ShowToast("Marked as unread"))
                    loadNotifications(refresh = true)
                }
                is ApiResult.Error -> {
                    _effect.emit(NotificationEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            when (val result = notificationRepository.markAllAsRead()) {
                is ApiResult.Success -> {
                    _effect.emit(NotificationEffect.ShowToast("All notifications marked as read"))
                    loadNotifications(refresh = true)
                }
                is ApiResult.Error -> {
                    _effect.emit(NotificationEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun saveFcmToken(token: String, deviceName: String? = null) {
        viewModelScope.launch {
            when (val result = notificationRepository.saveFcmToken(token, deviceName)) {
                is ApiResult.Success -> {
                }
                is ApiResult.Error -> {
                    _effect.emit(NotificationEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun refresh() {
        loadNotifications(refresh = true)
    }
}