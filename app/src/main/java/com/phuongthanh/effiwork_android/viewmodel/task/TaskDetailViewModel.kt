package com.phuongthanh.effiwork_android.viewmodel.task

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "TaskDetailViewModel"

sealed class TaskDetailUiState {
    object Idle : TaskDetailUiState()
    object Loading : TaskDetailUiState()
    data class Success(val taskDetail: TaskDetail) : TaskDetailUiState()
    data class Error(val message: String) : TaskDetailUiState()
}

data class TaskDetail(
    val id: String,
    val title: String,
    val description: String,
    val status: String,
    val groupName: String,
    val assigneeName: String,
    val creatorName: String,
    val startDate: String,
    val endDate: String,
    val createdAt: String,
    val participantNames: List<String>,
    val commentCount: Int,
    val attachmentCount: Int,
    val comments: List<CommentItem>
)

data class CommentItem(
    val id: String,
    val content: String,
    val userName: String,
    val userAvatar: String?,
    val createdAt: String
)

sealed class TaskDetailEffect {
    data class ShowToast(val message: String) : TaskDetailEffect()
    object CommentPosted : TaskDetailEffect()
    object TaskDeleted : TaskDetailEffect()
}

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TaskDetailUiState>(TaskDetailUiState.Idle)
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<TaskDetailEffect>()
    val effect: SharedFlow<TaskDetailEffect> = _effect.asSharedFlow()

    private var currentProjectId: String = ""
    private var currentTaskId: String = ""

    fun loadTaskDetail(projectId: String, taskId: String) {
        currentProjectId = projectId
        currentTaskId = taskId
        viewModelScope.launch {
            _uiState.value = TaskDetailUiState.Loading
            Log.d(TAG, "Loading task detail: projectId=$projectId, taskId=$taskId")

            when (val result = taskRepository.getTaskDetail(projectId, taskId)) {
                is ApiResult.Success -> {
                    val data = result.data
                    Log.d(TAG, "Task detail loaded: ${data.title}")
                    val taskDetail = TaskDetail(
                        id = data.id,
                        title = data.title ?: "",
                        description = data.description ?: "",
                        status = data.status ?: "",
                        groupName = data.group?.name ?: "",
                        assigneeName = data.assignee?.fullName ?: "",
                        creatorName = data.creator?.fullName ?: "",
                        startDate = data.startDate?.take(10) ?: "",
                        endDate = data.endDate?.take(10) ?: "",
                        createdAt = data.createdAt?.take(10) ?: "",
                        participantNames = data.participants?.mapNotNull { it.user?.fullName } ?: emptyList(),
                        commentCount = data.count?.comments ?: 0,
                        attachmentCount = data.count?.attachments ?: 0,
                        comments = data.comments?.map { comment ->
                            CommentItem(
                                id = comment.id,
                                content = comment.content ?: "",
                                userName = comment.user?.fullName ?: "",
                                userAvatar = comment.user?.avatarUrl,
                                createdAt = comment.createdAt?.take(10) ?: ""
                            )
                        } ?: emptyList()
                    )
                    _uiState.value = TaskDetailUiState.Success(taskDetail)
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Failed to load task detail: ${result.message}")
                    _uiState.value = TaskDetailUiState.Error(result.message)
                }
                is ApiResult.Loading -> {
                    _uiState.value = TaskDetailUiState.Loading
                }
            }
        }
    }

    fun postComment(content: String) {
        if (content.isBlank() || currentProjectId.isBlank() || currentTaskId.isBlank()) return

        viewModelScope.launch {
            when (val result = taskRepository.createTaskComment(currentProjectId, currentTaskId, content)) {
                is ApiResult.Success -> {
                    Log.d(TAG, "Comment posted: ${result.data.id}")
                    _effect.emit(TaskDetailEffect.CommentPosted)
                    loadTaskDetail(currentProjectId, currentTaskId)
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Failed to post comment: ${result.message}")
                    _effect.emit(TaskDetailEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun deleteTask() {
        if (currentProjectId.isBlank() || currentTaskId.isBlank()) return

        viewModelScope.launch {
            when (val result = taskRepository.deleteTask(currentProjectId, currentTaskId)) {
                is ApiResult.Success -> {
                    Log.d(TAG, "Task deleted successfully")
                    _effect.emit(TaskDetailEffect.TaskDeleted)
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Failed to delete task: ${result.message}")
                    _effect.emit(TaskDetailEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }
}