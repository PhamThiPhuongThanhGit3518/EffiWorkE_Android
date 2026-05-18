package com.phuongthanh.effiwork_android.api

import com.phuongthanh.effiwork_android.data.model.request.CreateMeetingRequest
import com.phuongthanh.effiwork_android.data.model.request.CreateSectionRequest
import com.phuongthanh.effiwork_android.data.model.request.CreateTaskRequest
import com.phuongthanh.effiwork_android.data.model.request.UpdateTaskRequest
import com.phuongthanh.effiwork_android.data.model.request.UpdateTaskStatusRequest
import com.phuongthanh.effiwork_android.data.model.response.*
import retrofit2.http.*

interface TaskService {
    @GET("v1/projects/{projectId}/tasks")
    suspend fun getTasks(
        @Path("projectId") projectId: String,
        @Query("sectionId") sectionId: String? = null,
        @Query("status") status: String? = null,
        @Query("assigneeId") assigneeId: String? = null,
        @Query("parentTaskId") parentTaskId: String? = null
    ): ApiResponse<List<TaskResponse>>

    @GET("v1/projects/{projectId}/tasks/{taskId}")
    suspend fun getTaskDetail(
        @Path("projectId") projectId: String,
        @Path("taskId") taskId: String
    ): ApiResponse<TaskDetailResponse>

    @GET("v1/projects/{projectId}/tasks/{taskId}/subtasks")
    suspend fun getSubtasks(
        @Path("projectId") projectId: String,
        @Path("taskId") taskId: String
    ): ApiResponse<List<TaskResponse>>

    @GET("v1/projects/{projectId}/tasks/{taskId}/comments")
    suspend fun getTaskComments(
        @Path("projectId") projectId: String,
        @Path("taskId") taskId: String
    ): ApiResponse<List<CommentResponse>>

    @POST("v1/projects/{projectId}/tasks/{taskId}/comments")
    suspend fun createTaskComment(
        @Path("projectId") projectId: String,
        @Path("taskId") taskId: String,
        @Body request: Map<String, String>
    ): ApiResponse<CommentResponse>

    @GET("v1/projects/{projectId}/sections")
    suspend fun getSections(
        @Path("projectId") projectId: String
    ): ApiResponse<List<SectionResponse>>

    @POST("v1/projects/{projectId}/sections")
    suspend fun createSection(
        @Path("projectId") projectId: String,
        @Body request: CreateSectionRequest
    ): ApiResponse<SectionResponse>

    @GET("v1/projects/{projectId}/members")
    suspend fun getMembers(
        @Path("projectId") projectId: String
    ): ApiResponse<List<MemberResponse>>

    @POST("v1/projects/{projectId}/tasks")
    suspend fun createTask(
        @Path("projectId") projectId: String,
        @Body request: CreateTaskRequest
    ): ApiResponse<TaskResponse>

    @PATCH("v1/projects/{projectId}/tasks/{taskId}/status")
    suspend fun updateTaskStatus(
        @Path("projectId") projectId: String,
        @Path("taskId") taskId: String,
        @Body request: UpdateTaskStatusRequest
    ): ApiResponse<TaskResponse>

    @PATCH("v1/projects/{projectId}/tasks/{taskId}")
    suspend fun updateTask(
        @Path("projectId") projectId: String,
        @Path("taskId") taskId: String,
        @Body request: UpdateTaskRequest
    ): ApiResponse<TaskResponse>

    @DELETE("v1/projects/{projectId}/tasks/{taskId}")
    suspend fun deleteTask(
        @Path("projectId") projectId: String,
        @Path("taskId") taskId: String
    ): ApiResponse<Unit>
}

interface MeetingService {
    @GET("v1/projects/{projectId}/meetings")
    suspend fun getMeetings(
        @Path("projectId") projectId: String,
        @Query("format") format: String? = null
    ): ApiResponse<List<MeetingResponse>>

    @POST("v1/projects/{projectId}/meetings")
    suspend fun createMeeting(
        @Path("projectId") projectId: String,
        @Body request: CreateMeetingRequest
    ): ApiResponse<MeetingResponse>
}