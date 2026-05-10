package com.phuongthanh.effiwork_android.viewmodel.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
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

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProjectsUiState>(ProjectsUiState.Idle)
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    private val _projectDetailState = MutableStateFlow<ProjectDetailUiState>(ProjectDetailUiState.Idle)
    val projectDetailState: StateFlow<ProjectDetailUiState> = _projectDetailState.asStateFlow()

    private val _effect = MutableSharedFlow<ProjectsEffect>()
    val effect: SharedFlow<ProjectsEffect> = _effect.asSharedFlow()

    fun handleIntent(intent: ProjectsIntent) {
        when (intent) {
            is ProjectsIntent.LoadProjects -> loadProjects()
            is ProjectsIntent.SearchProjects -> searchProjects(intent.keyword)
            is ProjectsIntent.CreateProject -> createProject(intent.name, intent.description)
            is ProjectsIntent.LoadProjectDetail -> loadProjectDetail(intent.projectId)
            is ProjectsIntent.UpdateProject -> updateProject(intent.projectId, intent.name, intent.description)
            is ProjectsIntent.TransferAdmin -> transferAdmin(intent.projectId, intent.targetUserId, intent.note)
            is ProjectsIntent.JoinByCode -> joinByCode(intent.projectCode, intent.note)
            is ProjectsIntent.ApproveJoinRequest -> approveJoinRequest(intent.projectId, intent.requestId, intent.note)
            is ProjectsIntent.RejectJoinRequest -> rejectJoinRequest(intent.projectId, intent.requestId, intent.note)
            is ProjectsIntent.RemoveMember -> removeMember(intent.projectId, intent.userId)
        }
    }

    private fun loadProjects() {
        viewModelScope.launch {
            _uiState.value = ProjectsUiState.Loading
            when (val result = projectRepository.getProjects()) {
                is ApiResult.Success -> {
                    _uiState.value = ProjectsUiState.Success(result.data.projects)
                }
                is ApiResult.Error -> {
                    _uiState.value = ProjectsUiState.Error(result.message)
                }
                is ApiResult.Loading -> {
                    _uiState.value = ProjectsUiState.Loading
                }
            }
        }
    }

    private fun searchProjects(keyword: String) {
        viewModelScope.launch {
            _uiState.value = ProjectsUiState.Loading
            when (val result = projectRepository.getProjects(keyword)) {
                is ApiResult.Success -> {
                    _uiState.value = ProjectsUiState.Success(result.data.projects)
                }
                is ApiResult.Error -> {
                    _uiState.value = ProjectsUiState.Error(result.message)
                }
                is ApiResult.Loading -> {
                    _uiState.value = ProjectsUiState.Loading
                }
            }
        }
    }

    private fun createProject(name: String, description: String) {
        viewModelScope.launch {
            _uiState.value = ProjectsUiState.Loading
            when (val result = projectRepository.createProject(name, description)) {
                is ApiResult.Success -> {
                    _effect.emit(ProjectsEffect.ShowToast("Tạo dự án thành công"))
                    loadProjects()
                }
                is ApiResult.Error -> {
                    _uiState.value = ProjectsUiState.Error(result.message)
                }
                is ApiResult.Loading -> {
                    _uiState.value = ProjectsUiState.Loading
                }
            }
        }
    }

    private fun loadProjectDetail(projectId: String) {
        viewModelScope.launch {
            _projectDetailState.value = ProjectDetailUiState.Loading
            when (val result = projectRepository.getProject(projectId)) {
                is ApiResult.Success -> {
                    val data = result.data
                    _projectDetailState.value = ProjectDetailUiState.Success(
                        projectId = data.project.projectId,
                        name = data.project.name,
                        description = data.project.description,
                        ownerName = data.project.ownerName,
                        memberCount = data.summary.memberCount,
                        taskCount = data.summary.taskCount,
                        completedTaskCount = data.summary.completedTaskCount,
                        members = data.members
                    )
                }
                is ApiResult.Error -> {
                    _projectDetailState.value = ProjectDetailUiState.Error(result.message)
                }
                is ApiResult.Loading -> {
                    _projectDetailState.value = ProjectDetailUiState.Loading
                }
            }
        }
    }

    private fun updateProject(projectId: String, name: String, description: String) {
        viewModelScope.launch {
            _projectDetailState.value = ProjectDetailUiState.Loading
            when (val result = projectRepository.updateProject(projectId, name, description)) {
                is ApiResult.Success -> {
                    _effect.emit(ProjectsEffect.ShowToast("Cập nhật dự án thành công"))
                    loadProjectDetail(projectId)
                }
                is ApiResult.Error -> {
                    _projectDetailState.value = ProjectDetailUiState.Error(result.message)
                }
                is ApiResult.Loading -> {
                    _projectDetailState.value = ProjectDetailUiState.Loading
                }
            }
        }
    }

    private fun transferAdmin(projectId: String, targetUserId: String, note: String?) {
        viewModelScope.launch {
            when (val result = projectRepository.transferAdmin(projectId, targetUserId, note)) {
                is ApiResult.Success -> {
                    _effect.emit(ProjectsEffect.ShowToast("Chuyển quyền quản trị thành công"))
                }
                is ApiResult.Error -> {
                    _effect.emit(ProjectsEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    private fun joinByCode(projectCode: String, note: String?) {
        viewModelScope.launch {
            _uiState.value = ProjectsUiState.Loading
            when (val result = projectRepository.joinByCode(projectCode, note)) {
                is ApiResult.Success -> {
                    _effect.emit(ProjectsEffect.ShowToast("Tham gia dự án thành công"))
                    loadProjects()
                }
                is ApiResult.Error -> {
                    _uiState.value = ProjectsUiState.Error(result.message)
                }
                is ApiResult.Loading -> {
                    _uiState.value = ProjectsUiState.Loading
                }
            }
        }
    }

    private fun approveJoinRequest(projectId: String, requestId: String, note: String?) {
        viewModelScope.launch {
            when (val result = projectRepository.approveJoinRequest(projectId, requestId, note)) {
                is ApiResult.Success -> {
                    _effect.emit(ProjectsEffect.ShowToast("Phê duyệt thành công"))
                    loadProjectDetail(projectId)
                }
                is ApiResult.Error -> {
                    _effect.emit(ProjectsEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    private fun rejectJoinRequest(projectId: String, requestId: String, note: String?) {
        viewModelScope.launch {
            when (val result = projectRepository.rejectJoinRequest(projectId, requestId, note)) {
                is ApiResult.Success -> {
                    _effect.emit(ProjectsEffect.ShowToast("Từ chối thành công"))
                    loadProjectDetail(projectId)
                }
                is ApiResult.Error -> {
                    _effect.emit(ProjectsEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    private fun removeMember(projectId: String, userId: String) {
        viewModelScope.launch {
            when (val result = projectRepository.removeMember(projectId, userId)) {
                is ApiResult.Success -> {
                    _effect.emit(ProjectsEffect.ShowToast("Xóa thành viên thành công"))
                    loadProjectDetail(projectId)
                }
                is ApiResult.Error -> {
                    _effect.emit(ProjectsEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }
}
