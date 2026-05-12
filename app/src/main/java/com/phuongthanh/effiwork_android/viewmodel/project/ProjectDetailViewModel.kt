package com.phuongthanh.effiwork_android.viewmodel.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.local.AppPreferences
import com.phuongthanh.effiwork_android.data.model.response.ProjectDetailResponse
import com.phuongthanh.effiwork_android.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProjectDetailUiState {
    data object Idle : ProjectDetailUiState()
    data object Loading : ProjectDetailUiState()
    data object NoProject : ProjectDetailUiState()
    data class Success(val project: ProjectDetailResponse) : ProjectDetailUiState()
    data class Error(val message: String) : ProjectDetailUiState()
}

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProjectDetailUiState>(ProjectDetailUiState.Loading)
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    init {
        checkAndLoadSavedProject()
    }

    private fun checkAndLoadSavedProject() {
        val savedId = appPreferences.getSelectedProjectId()
        if (!savedId.isNullOrEmpty()) {
            loadProject(savedId)
        } else {
            _uiState.value = ProjectDetailUiState.NoProject
        }
    }

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _uiState.value = ProjectDetailUiState.Loading
            when (val result = projectRepository.getProject(projectId)) {
                is ApiResult.Success -> {
                    _uiState.value = ProjectDetailUiState.Success(result.data)
                }
                is ApiResult.Error -> {
                    if (result.message.contains("không thuộc dự án", ignoreCase = true)) {
                        appPreferences.clearSelectedProjectId()
                        _uiState.value = ProjectDetailUiState.NoProject
                    } else {
                        _uiState.value = ProjectDetailUiState.Error(result.message)
                    }
                }
                else -> {}
            }
        }
    }
}