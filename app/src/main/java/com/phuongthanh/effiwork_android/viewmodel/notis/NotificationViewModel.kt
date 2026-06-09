package com.phuongthanh.effiwork_android.viewmodel.notis

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.model.response.NotificationResponse
import com.phuongthanh.effiwork_android.data.repository.NotificationRepository
import com.phuongthanh.effiwork_android.data.repository.ProjectRepository
import com.phuongthanh.effiwork_android.data.socket.NotificationSocketManager
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
        val totalPages: Int,
        val isLoadingMore: Boolean = false
    ) : NotificationUiState()
    data class Error(val message: String) : NotificationUiState()
}

sealed class NotificationEffect {
    data class ShowToast(val message: String) : NotificationEffect()
    object RefreshList : NotificationEffect()
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val projectRepository: ProjectRepository,
    private val notificationSocketManager: NotificationSocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Idle)
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<NotificationEffect>()
    val effect: SharedFlow<NotificationEffect> = _effect.asSharedFlow()

    private val _unreadOnly = MutableStateFlow(false)
    val unreadOnly: StateFlow<Boolean> = _unreadOnly.asStateFlow()

    private val _projectNameMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val projectNameMap: StateFlow<Map<String, String>> = _projectNameMap.asStateFlow()

    private val _newNotificationToShow = MutableSharedFlow<NotificationResponse>(replay = 0, extraBufferCapacity = 16)
    val newNotificationToShow: SharedFlow<NotificationResponse> = _newNotificationToShow.asSharedFlow()

    private var currentPage = 1
    private var totalPages = 1
    private val pageSize = 20

    init {
        loadProjectNames()
        observeNotificationsCache()
        observeSocketEvents()
    }

    private fun observeNotificationsCache() {
        viewModelScope.launch {
            notificationRepository.notificationsFlow.collect { items ->
                val current = _uiState.value
                if (current is NotificationUiState.Success) {
                    _uiState.value = current.copy(notifications = items)
                }
            }
        }
    }

    private fun observeSocketEvents() {
        viewModelScope.launch {
            Log.d(TAG, "observeSocketEvents: collector started, waiting for newNotificationFlow events")
            notificationSocketManager.newNotificationFlow.collect { event ->
                Log.d(TAG, "observeSocketEvents: received event for notification id=${event.notification.id}, projectId=${event.notification.projectId}")
                notificationRepository.upsertNotification(event.notification)
                val emitted = _newNotificationToShow.tryEmit(event.notification)
                Log.d(TAG, "observeSocketEvents: tryEmit to newNotificationToShow -> $emitted")
            }
        }
    }

    private fun loadProjectNames() {
        viewModelScope.launch {
            when (val result = projectRepository.getProjects()) {
                is ApiResult.Success -> {
                    _projectNameMap.value = result.data.data.associate { it.id to it.name }
                }
                is ApiResult.Error -> {
                    Log.w(TAG, "loadProjectNames failed: ${result.message}")
                }
                is ApiResult.Loading -> {}
            }
        }
    }

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
        val currentState = _uiState.value
        if (currentState !is NotificationUiState.Success) return
        if (currentState.isLoadingMore) return
        if (currentState.page >= currentState.totalPages) return

        val nextPage = currentState.page + 1
        _uiState.value = currentState.copy(isLoadingMore = true)

        viewModelScope.launch {
            when (val result = notificationRepository.getNotifications(nextPage, pageSize, _unreadOnly.value)) {
                is ApiResult.Success -> {
                    val data = result.data
                    currentPage = data.page
                    totalPages = data.totalPages
                    val latest = _uiState.value
                    if (latest is NotificationUiState.Success) {
                        _uiState.value = latest.copy(
                            notifications = latest.notifications + data.data,
                            page = currentPage,
                            totalPages = totalPages,
                            isLoadingMore = false
                        )
                    }
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "loadMoreNotifications ERROR: ${result.message}")
                    val latest = _uiState.value
                    if (latest is NotificationUiState.Success) {
                        _uiState.value = latest.copy(isLoadingMore = false)
                    }
                    _effect.emit(NotificationEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
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