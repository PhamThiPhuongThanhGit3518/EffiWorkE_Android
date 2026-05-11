package com.phuongthanh.effiwork_android.viewmodel.project_setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.model.response.JoinRequestResponse
import com.phuongthanh.effiwork_android.data.model.response.ProjectDetailResponse
import com.phuongthanh.effiwork_android.data.model.response.ProjectMemberResponse
import com.phuongthanh.effiwork_android.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProjectSettingUiState {
    data object Loading : ProjectSettingUiState()
    data class Success(
        val project: ProjectDetailResponse,
        val members: List<ProjectMemberResponse>,
        val joinRequests: List<JoinRequestResponse>
    ) : ProjectSettingUiState()
    data class Error(val message: String) : ProjectSettingUiState()
}

sealed class ProjectSettingEffect {
    data class ShowToast(val message: String) : ProjectSettingEffect()
}

@HiltViewModel
class ProjectSettingViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProjectSettingUiState>(ProjectSettingUiState.Loading)
    val uiState: StateFlow<ProjectSettingUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ProjectSettingEffect>()
    val effect: SharedFlow<ProjectSettingEffect> = _effect.asSharedFlow()

    fun loadProjectSettings(projectId: String) {
        viewModelScope.launch {
            _uiState.value = ProjectSettingUiState.Loading

            val projectResult = projectRepository.getProject(projectId)
            if (projectResult !is ApiResult.Success) {
                _uiState.value = ProjectSettingUiState.Error("Không thể tải thông tin dự án")
                return@launch
            }
            val project = projectResult.data

            val membersResult = projectRepository.getProjectMembers(projectId)
            val members = if (membersResult is ApiResult.Success) membersResult.data else emptyList()

            val requestsResult = projectRepository.getJoinRequests(projectId)
            val requests = if (requestsResult is ApiResult.Success) requestsResult.data else emptyList()

            _uiState.value = ProjectSettingUiState.Success(project, members, requests)
        }
    }

    fun approveJoinRequest(projectId: String, requestId: String) {
        viewModelScope.launch {
            when (val result = projectRepository.approveJoinRequest(projectId, requestId, null)) {
                is ApiResult.Success -> {
                    _effect.emit(ProjectSettingEffect.ShowToast("Phê duyệt thành công"))
                    loadProjectSettings(projectId)
                }
                is ApiResult.Error -> {
                    _effect.emit(ProjectSettingEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun rejectJoinRequest(projectId: String, requestId: String) {
        viewModelScope.launch {
            when (val result = projectRepository.rejectJoinRequest(projectId, requestId, null)) {
                is ApiResult.Success -> {
                    _effect.emit(ProjectSettingEffect.ShowToast("Từ chối thành công"))
                    loadProjectSettings(projectId)
                }
                is ApiResult.Error -> {
                    _effect.emit(ProjectSettingEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun removeMember(projectId: String, userId: String) {
        viewModelScope.launch {
            when (val result = projectRepository.removeMember(projectId, userId)) {
                is ApiResult.Success -> {
                    _effect.emit(ProjectSettingEffect.ShowToast("Xóa thành viên thành công"))
                    loadProjectSettings(projectId)
                }
                is ApiResult.Error -> {
                    _effect.emit(ProjectSettingEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }
}