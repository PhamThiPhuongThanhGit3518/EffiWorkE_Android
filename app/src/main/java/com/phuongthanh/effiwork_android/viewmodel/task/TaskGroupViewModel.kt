package com.phuongthanh.effiwork_android.viewmodel.task

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.model.request.CreateSectionRequest
import com.phuongthanh.effiwork_android.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "TaskGroupViewModel"

sealed class TaskGroupUiState {
    object Idle : TaskGroupUiState()
    object Loading : TaskGroupUiState()
    data class Success(val groups: List<TaskGroupItem>) : TaskGroupUiState()
    data class Error(val message: String) : TaskGroupUiState()
}

data class TaskGroupItem(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val taskCount: Int
)

@HiltViewModel
class TaskGroupViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TaskGroupUiState>(TaskGroupUiState.Idle)
    val uiState: StateFlow<TaskGroupUiState> = _uiState.asStateFlow()

    private val _projectId = MutableStateFlow("")
    val projectId: StateFlow<String> = _projectId.asStateFlow()

    private val _projectName = MutableStateFlow("")
    val projectName: StateFlow<String> = _projectName.asStateFlow()

    fun setProjectInfo(projectId: String, projectName: String) {
        _projectId.value = projectId
        _projectName.value = projectName
    }

    fun loadTaskGroups() {
        viewModelScope.launch {
            _uiState.value = TaskGroupUiState.Loading
            val projectIdValue = _projectId.value
            if (projectIdValue.isBlank()) {
                _uiState.value = TaskGroupUiState.Error("Project ID is required")
                return@launch
            }

            when (val sectionsResult = taskRepository.getTaskGroups(projectIdValue)) {
                is ApiResult.Success -> {
                    Log.d(TAG, "Sections loaded: ${sectionsResult.data.size}")
                    sectionsResult.data.forEach { Log.d(TAG, "  Section: id=${it.id}, name=${it.name}") }
                    when (val tasksResult = taskRepository.getTasks(projectIdValue)) {
                        is ApiResult.Success -> {
                            Log.d(TAG, "Tasks loaded: ${tasksResult.data.size}")
                            tasksResult.data.forEach { Log.d(TAG, "  Task: id=${it.id}, groupId=${it.groupId}, name=${it.name}") }
                            val tasksBySection = tasksResult.data
                                .filter { it.groupId != null }
                                .groupBy { it.groupId }
                            Log.d(TAG, "Tasks by section: ${tasksBySection.keys}")

                            val groups = sectionsResult.data.map { section ->
                                val count = tasksBySection[section.id]?.size ?: 0
                                Log.d(TAG, "Section ${section.name} has $count tasks")
                                TaskGroupItem(
                                    id = section.id,
                                    name = section.name ?: "",
                                    icon = Icons.Default.Folder,
                                    color = Color(0xFF2196F3),
                                    taskCount = count
                                )
                            }
                            _uiState.value = TaskGroupUiState.Success(groups)
                        }
                        is ApiResult.Error -> {
                            val groups = sectionsResult.data.map { section ->
                                TaskGroupItem(
                                    id = section.id,
                                    name = section.name ?: "",
                                    icon = Icons.Default.Folder,
                                    color = Color(0xFF2196F3),
                                    taskCount = 0
                                )
                            }
                            _uiState.value = TaskGroupUiState.Success(groups)
                        }
                        is ApiResult.Loading -> {
                            _uiState.value = TaskGroupUiState.Loading
                        }
                    }
                }
                is ApiResult.Error -> {
                    _uiState.value = TaskGroupUiState.Error(sectionsResult.message)
                }
                is ApiResult.Loading -> {
                    _uiState.value = TaskGroupUiState.Loading
                }
            }
        }
    }

    fun createTaskGroup(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val projectIdValue = _projectId.value
            if (projectIdValue.isBlank()) {
                onError("Project ID is required")
                return@launch
            }

            val request = CreateSectionRequest(name = name)
            when (val result = taskRepository.createSection(projectIdValue, request)) {
                is ApiResult.Success -> {
                    Log.d(TAG, "createSection success: ${result.data.id}, name=${result.data.name}")
                    loadTaskGroups()
                    onSuccess()
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "createSection failed: ${result.message}")
                    onError(result.message)
                }
                is ApiResult.Loading -> {}
            }
        }
    }
}