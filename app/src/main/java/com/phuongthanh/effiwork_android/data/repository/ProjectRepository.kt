package com.phuongthanh.effiwork_android.data.repository

import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.model.request.*
import com.phuongthanh.effiwork_android.data.model.response.*

interface ProjectRepository {
    suspend fun getProjects(keyword: String? = null): ApiResult<ProjectsListResponse>
    suspend fun createProject(name: String, description: String): ApiResult<ProjectResponse>
    suspend fun getProject(projectId: String): ApiResult<ProjectDetailResponse>
    suspend fun updateProject(projectId: String, name: String, description: String): ApiResult<ProjectResponse>
    suspend fun getProjectSummary(projectId: String): ApiResult<ProjectSummaryResponse>
    suspend fun transferAdmin(projectId: String, targetUserId: String, note: String?): ApiResult<Unit>
    suspend fun joinByCode(projectCode: String, note: String?): ApiResult<Unit>
    suspend fun getJoinRequests(projectId: String): ApiResult<List<JoinRequestResponse>>
    suspend fun approveJoinRequest(projectId: String, requestId: String, note: String?): ApiResult<Unit>
    suspend fun rejectJoinRequest(projectId: String, requestId: String, note: String?): ApiResult<Unit>
    suspend fun cancelJoinRequest(projectId: String, requestId: String): ApiResult<Unit>
    suspend fun getProjectMembers(projectId: String): ApiResult<List<ProjectMemberResponse>>
    suspend fun removeMember(projectId: String, userId: String): ApiResult<Unit>
}
