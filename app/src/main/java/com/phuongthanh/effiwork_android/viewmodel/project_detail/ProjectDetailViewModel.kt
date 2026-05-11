package com.phuongthanh.effiwork_android.viewmodel.project_detail

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

    fun loadProject(projectId: String) {
        android.util.Log.d("ProjectDetail", "loadProject called with projectId: $projectId")
        viewModelScope.launch {
            _uiState.value = ProjectDetailUiState.Loading
            android.util.Log.d("ProjectDetail", "Calling repository.getProject($projectId)")
            when (val result = projectRepository.getProject(projectId)) {
                is ApiResult.Success -> {
                    android.util.Log.d("ProjectDetail", "API Success! Data: ${result.data}")
                    android.util.Log.d("ProjectDetail", "name: ${result.data.name}")
                    android.util.Log.d("ProjectDetail", "projectCode: ${result.data.projectCode}")
                    android.util.Log.d("ProjectDetail", "memberCount: ${result.data.memberCount}")
                    appPreferences.saveSelectedProjectId(projectId)
                    _uiState.value = ProjectDetailUiState.Success(result.data)
                }
                is ApiResult.Error -> {
                    android.util.Log.e("ProjectDetail", "API Error: ${result.message}")
                    _uiState.value = ProjectDetailUiState.Error(result.message)
                }
                is ApiResult.Loading -> {
                    _uiState.value = ProjectDetailUiState.Loading
                }
            }
        }
    }

    fun selectProjectAndLoad(projectId: String) {
        appPreferences.saveSelectedProjectId(projectId)
        loadProject(projectId)
    }
}