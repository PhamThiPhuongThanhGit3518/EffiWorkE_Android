package com.phuongthanh.effiwork_android.data.model.response

import com.google.gson.annotations.SerializedName

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

data class ProjectsCount(
    val members: Int,
    val tasks: Int,
    val meetings: Int
)

data class ProjectsApiData(
    val id: String,
    val projectCode: String,
    val name: String,
    val description: String,
    val status: String,
    val createdById: String,
    val currentAdminId: String,
    val createdAt: String,
    val updatedAt: String,
    val _count: ProjectsCount?
)

data class ProjectsMeta(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int
)

data class ProjectsListResponse(
    val data: List<ProjectsApiData>,
    val meta: ProjectsMeta
)

data class ProjectDetailResponse(
    val project: ProjectResponse,
    val summary: ProjectSummaryResponse,
    val members: List<ProjectMemberResponse>
)

fun ProjectsApiData.toProjectResponse(): ProjectResponse = ProjectResponse(
    projectId = id,
    name = name,
    description = description,
    projectCode = projectCode,
    ownerId = createdById,
    ownerName = "",
    memberCount = _count?.members ?: 0,
    createdAt = createdAt,
    updatedAt = updatedAt
)
