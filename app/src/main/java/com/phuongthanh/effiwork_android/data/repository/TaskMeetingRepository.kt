package com.phuongthanh.effiwork_android.data.repository

import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.model.request.CreateMeetingRequest
import com.phuongthanh.effiwork_android.data.model.request.CreateSectionRequest
import com.phuongthanh.effiwork_android.data.model.request.CreateTaskRequest
import com.phuongthanh.effiwork_android.data.model.request.CreateExtensionRequest
import com.phuongthanh.effiwork_android.data.model.request.ReviewExtensionRequest
import com.phuongthanh.effiwork_android.data.model.request.UpdateTaskRequest
import com.phuongthanh.effiwork_android.data.model.request.UpdateTaskStatusRequest
import com.phuongthanh.effiwork_android.data.model.response.*

interface TaskRepository {
    suspend fun getTasks(projectId: String, sectionId: String? = null, status: String? = null, assigneeId: String? = null, parentTaskId: String? = null): ApiResult<List<TaskResponse>>
    suspend fun getTaskDetail(projectId: String, taskId: String): ApiResult<TaskDetailResponse>
    suspend fun getSubtasks(projectId: String, taskId: String): ApiResult<List<TaskResponse>>
    suspend fun getTaskComments(projectId: String, taskId: String): ApiResult<List<CommentResponse>>
    suspend fun createTaskComment(projectId: String, taskId: String, content: String): ApiResult<CommentResponse>
    suspend fun getTaskGroups(projectId: String): ApiResult<List<SectionResponse>>
    suspend fun createSection(projectId: String, request: CreateSectionRequest): ApiResult<SectionResponse>
    suspend fun getMembers(projectId: String): ApiResult<List<MemberResponse>>
    suspend fun createTask(projectId: String, request: CreateTaskRequest): ApiResult<TaskResponse>
    suspend fun updateTask(projectId: String, taskId: String, request: UpdateTaskRequest): ApiResult<TaskResponse>
    suspend fun deleteTask(projectId: String, taskId: String): ApiResult<Unit>
    suspend fun deleteSubtask(projectId: String, taskId: String, subtaskId: String): ApiResult<Unit>
    suspend fun updateTaskStatus(projectId: String, taskId: String, request: UpdateTaskStatusRequest): ApiResult<TaskResponse>
    suspend fun getExtensionRequests(projectId: String, taskId: String): ApiResult<List<ExtensionRequestResponse>>
    suspend fun createExtensionRequest(projectId: String, taskId: String, request: CreateExtensionRequest): ApiResult<ExtensionRequestResponse>
    suspend fun approveExtensionRequest(projectId: String, taskId: String, requestId: String, note: String?): ApiResult<ExtensionRequestResponse>
    suspend fun rejectExtensionRequest(projectId: String, taskId: String, requestId: String, note: String?): ApiResult<ExtensionRequestResponse>
}

interface MeetingRepository {
    suspend fun getMeetings(projectId: String, format: String? = null): ApiResult<List<MeetingResponse>>
    suspend fun createMeeting(projectId: String, request: CreateMeetingRequest): ApiResult<MeetingResponse>
}