package com.phuongthanh.effiwork_android.data.repository

import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.api.ProjectService
import com.phuongthanh.effiwork_android.data.model.request.*
import com.phuongthanh.effiwork_android.data.model.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectService: ProjectService
) : ProjectRepository {

    override suspend fun getProjects(keyword: String?): ApiResult<ProjectsListResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = projectService.getProjects(keyword)
                ApiResult.Success(response)
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun createProject(name: String, description: String): ApiResult<ProjectResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = projectService.createProject(CreateProjectRequest(name, description))
                if (response.success && response.data != null) {
                    ApiResult.Success(response.data)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun getProject(projectId: String): ApiResult<ProjectDetailResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = projectService.getProject(projectId)
                if (response.success && response.data != null) {
                    ApiResult.Success(response.data)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun updateProject(projectId: String, name: String, description: String): ApiResult<ProjectResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = projectService.updateProject(projectId, UpdateProjectRequest(name, description))
                if (response.success && response.data != null) {
                    ApiResult.Success(response.data)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun getProjectSummary(projectId: String): ApiResult<ProjectSummaryResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = projectService.getProjectSummary(projectId)
                if (response.success && response.data != null) {
                    ApiResult.Success(response.data)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun transferAdmin(projectId: String, targetUserId: String, note: String?): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = projectService.transferAdmin(projectId, TransferAdminRequest(targetUserId, note))
                if (response.success) {
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun joinByCode(projectCode: String, note: String?): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = projectService.joinByCode(JoinByCodeRequest(projectCode, note))
                if (response.success) {
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun getJoinRequests(projectId: String): ApiResult<List<JoinRequestResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = projectService.getJoinRequests(projectId)
                if (response.success && response.data != null) {
                    ApiResult.Success(response.data)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun approveJoinRequest(projectId: String, requestId: String, note: String?): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = projectService.approveJoinRequest(projectId, requestId, ApproveRejectRequest(note))
                if (response.success) {
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun rejectJoinRequest(projectId: String, requestId: String, note: String?): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = projectService.rejectJoinRequest(projectId, requestId, ApproveRejectRequest(note))
                if (response.success) {
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun cancelJoinRequest(projectId: String, requestId: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = projectService.cancelJoinRequest(projectId, requestId)
                if (response.success) {
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun getProjectMembers(projectId: String): ApiResult<List<ProjectMemberResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = projectService.getProjectMembers(projectId)
                if (response.success && response.data != null) {
                    ApiResult.Success(response.data)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun removeMember(projectId: String, userId: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = projectService.removeMember(projectId, userId)
                if (response.success) {
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }
}
