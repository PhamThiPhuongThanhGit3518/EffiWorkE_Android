package com.phuongthanh.effiwork_android.api

import com.phuongthanh.effiwork_android.data.model.request.*
import com.phuongthanh.effiwork_android.data.model.response.*
import retrofit2.http.*

interface ProjectService {

    @GET("v1/projects")
    suspend fun getProjects(
        @Query("keyword") keyword: String? = null
    ): ApiResponse<ProjectsListResponse>

    @POST("v1/projects")
    suspend fun createProject(
        @Body request: CreateProjectRequest
    ): ApiResponse<ProjectResponse>

    @GET("v1/projects/{projectId}")
    suspend fun getProject(
        @Path("projectId") projectId: String
    ): ApiResponse<ProjectDetailResponse>

    @PATCH("v1/projects/{projectId}")
    suspend fun updateProject(
        @Path("projectId") projectId: String,
        @Body request: UpdateProjectRequest
    ): ApiResponse<ProjectResponse>

    @GET("v1/projects/{projectId}/summary")
    suspend fun getProjectSummary(
        @Path("projectId") projectId: String
    ): ApiResponse<ProjectSummaryResponse>

    @POST("v1/projects/{projectId}/transfer-admin")
    suspend fun transferAdmin(
        @Path("projectId") projectId: String,
        @Body request: TransferAdminRequest
    ): ApiResponse<Unit>

    @POST("v1/projects/join-by-code")
    suspend fun joinByCode(
        @Body request: JoinByCodeRequest
    ): ApiResponse<Unit>

    @GET("v1/projects/{projectId}/join-requests")
    suspend fun getJoinRequests(
        @Path("projectId") projectId: String
    ): ApiResponse<List<JoinRequestResponse>>

    @POST("v1/projects/{projectId}/join-requests/{requestId}/approve")
    suspend fun approveJoinRequest(
        @Path("projectId") projectId: String,
        @Path("requestId") requestId: String,
        @Body request: ApproveRejectRequest
    ): ApiResponse<Unit>

    @POST("v1/projects/{projectId}/join-requests/{requestId}/reject")
    suspend fun rejectJoinRequest(
        @Path("projectId") projectId: String,
        @Path("requestId") requestId: String,
        @Body request: ApproveRejectRequest
    ): ApiResponse<Unit>

    @DELETE("v1/projects/{projectId}/join-requests/{requestId}/cancel")
    suspend fun cancelJoinRequest(
        @Path("projectId") projectId: String,
        @Path("requestId") requestId: String
    ): ApiResponse<Unit>

    @GET("v1/projects/{projectId}/members")
    suspend fun getProjectMembers(
        @Path("projectId") projectId: String
    ): ApiResponse<List<ProjectMemberResponse>>

    @DELETE("v1/projects/{projectId}/members/{userId}")
    suspend fun removeMember(
        @Path("projectId") projectId: String,
        @Path("userId") userId: String
    ): ApiResponse<Unit>
}
