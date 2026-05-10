package com.phuongthanh.effiwork_android.viewmodel.projects

import com.phuongthanh.effiwork_android.data.model.response.ProjectMemberResponse

sealed class ProjectsUiState {
    object Idle : ProjectsUiState()
    object Loading : ProjectsUiState()
    data class Success(val projects: List<com.phuongthanh.effiwork_android.data.model.response.ProjectResponse>) : ProjectsUiState()
    data class Error(val message: String) : ProjectsUiState()
}

sealed class ProjectDetailUiState {
    object Idle : ProjectDetailUiState()
    object Loading : ProjectDetailUiState()
    data class Success(
        val projectId: String,
        val name: String,
        val description: String,
        val ownerName: String,
        val memberCount: Int,
        val taskCount: Int,
        val completedTaskCount: Int,
        val members: List<ProjectMemberResponse>
    ) : ProjectDetailUiState()
    data class Error(val message: String) : ProjectDetailUiState()
}

sealed class ProjectsEffect {
    data class ShowToast(val message: String) : ProjectsEffect()
    data class NavigateToProjectDetail(val projectId: String) : ProjectsEffect()
}

sealed class ProjectsIntent {
    object LoadProjects : ProjectsIntent()
    data class SearchProjects(val keyword: String) : ProjectsIntent()
    data class CreateProject(val name: String, val description: String) : ProjectsIntent()
    data class LoadProjectDetail(val projectId: String) : ProjectsIntent()
    data class UpdateProject(val projectId: String, val name: String, val description: String) : ProjectsIntent()
    data class TransferAdmin(val projectId: String, val targetUserId: String, val note: String?) : ProjectsIntent()
    data class JoinByCode(val projectCode: String, val note: String?) : ProjectsIntent()
    data class ApproveJoinRequest(val projectId: String, val requestId: String, val note: String?) : ProjectsIntent()
    data class RejectJoinRequest(val projectId: String, val requestId: String, val note: String?) : ProjectsIntent()
    data class RemoveMember(val projectId: String, val userId: String) : ProjectsIntent()
}
