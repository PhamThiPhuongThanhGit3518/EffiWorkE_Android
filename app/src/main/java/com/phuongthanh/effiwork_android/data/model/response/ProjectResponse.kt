package com.phuongthanh.effiwork_android.data.model.response

data class ProjectResponse(
    val projectId: String,
    val name: String,
    val description: String,
    val projectCode: String,
    val ownerId: String,
    val ownerName: String,
    val memberCount: Int,
    val createdAt: String,
    val updatedAt: String
)

data class ProjectSummaryResponse(
    val projectId: String,
    val name: String,
    val description: String,
    val ownerName: String,
    val memberCount: Int,
    val taskCount: Int,
    val completedTaskCount: Int
)

data class ProjectMemberResponse(
    val userId: String,
    val fullName: String,
    val email: String,
    val role: String,
    val joinedAt: String
)

data class JoinRequestResponse(
    val requestId: String,
    val userId: String,
    val fullName: String,
    val email: String,
    val note: String?,
    val status: String,
    val createdAt: String
)

data class ProjectsListResponse(
    val projects: List<ProjectResponse>,
    val total: Int,
    val page: Int,
    val pageSize: Int
)

data class ProjectDetailResponse(
    val project: ProjectResponse,
    val summary: ProjectSummaryResponse,
    val members: List<ProjectMemberResponse>
)
