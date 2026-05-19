package com.phuongthanh.effiwork_android.data.repository

import android.util.Log
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.api.MeetingService
import com.phuongthanh.effiwork_android.api.TaskService
import com.phuongthanh.effiwork_android.data.model.request.CreateMeetingRequest
import com.phuongthanh.effiwork_android.data.model.request.CreateSectionRequest
import com.phuongthanh.effiwork_android.data.model.request.CreateTaskRequest
import com.phuongthanh.effiwork_android.data.model.request.UpdateTaskRequest
import com.phuongthanh.effiwork_android.data.model.request.UpdateTaskStatusRequest
import com.phuongthanh.effiwork_android.data.model.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TaskRepository"

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskService: TaskService
) : TaskRepository {

    override suspend fun getTasks(projectId: String, sectionId: String?, status: String?, assigneeId: String?, parentTaskId: String?): ApiResult<List<TaskResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "getTasks called: projectId=$projectId, parentTaskId=$parentTaskId")
                val response = taskService.getTasks(projectId, sectionId, status, assigneeId, parentTaskId)
                Log.d(TAG, "getTasks response: success=${response.success}, data=${response.data?.size} items")
                response.data?.forEachIndexed { index, task ->
                    Log.d(TAG, "  Task[$index]: id=${task.id}, name=${task.name}, status=${task.status}")
                    Log.d(TAG, "    assigneeId=${task.assigneeId}, assigneeName=${task.assigneeName}")
                    Log.d(TAG, "    owner=${task.owner}, owner.fullName=${task.owner?.fullName}")
                    Log.d(TAG, "    creator=${task.creator}, creator.fullName=${task.creator?.fullName}")
                    Log.d(TAG, "    startDate=${task.startDate}, endDate=${task.endDate}")
                    Log.d(TAG, "    participants=${task.participants?.map { "${it.userId}:${it.user?.fullName}" }}")
                }
                if (response.success && response.data != null) {
                    ApiResult.Success(response.data)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "getTasks EXCEPTION: ${e.message}", e)
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun createTask(projectId: String, request: CreateTaskRequest): ApiResult<TaskResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "createTask called: projectId=$projectId, title=${request.title}, sectionId=${request.sectionId}, ownerId=${request.ownerId}")
                val response = taskService.createTask(projectId, request)
                Log.d(TAG, "createTask response: success=${response.success}, message=${response.message}, data=${response.data}")
                if (response.success && response.data != null) {
                    Log.d(TAG, "createTask SUCCESS: id=${response.data.id}")
                    ApiResult.Success(response.data)
                } else {
                    Log.e(TAG, "createTask FAILED: ${response.message}")
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "createTask EXCEPTION: ${e.message}", e)
                if (e is retrofit2.HttpException) {
                    try {
                        val errorBody = e.response()?.errorBody()?.string()
                        Log.e(TAG, "createTask ERROR BODY: $errorBody")
                        val message = errorBody?.let {
                            if (it.contains("message")) {
                                val msgMatch = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(it)
                                msgMatch?.groupValues?.get(1) ?: it
                            } else it
                        } ?: e.message()
                        ApiResult.Error(message, e.code())
                    } catch (ignored: Exception) {
                        ApiResult.Error(e.message ?: "Unknown error", e.code())
                    }
                } else {
                    ApiResult.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    override suspend fun updateTask(projectId: String, taskId: String, request: UpdateTaskRequest): ApiResult<TaskResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "updateTask called: projectId=$projectId, taskId=$taskId")
                Log.d(TAG, "  title=${request.title}, description=${request.description}")
                Log.d(TAG, "  sectionId=${request.sectionId}, ownerId=${request.ownerId}")
                Log.d(TAG, "  startDate=${request.startDate}, dueDate=${request.dueDate}")
                Log.d(TAG, "  participantIds=${request.participantIds}")

                val response = taskService.updateTask(projectId, taskId, request)
                Log.d(TAG, "updateTask response: success=${response.success}, message=${response.message}")
                if (response.success && response.data != null) {
                    ApiResult.Success(response.data)
                } else {
                    Log.e(TAG, "updateTask failed: ${response.message}")
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateTask EXCEPTION: ${e.message}", e)
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun deleteTask(projectId: String, taskId: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "deleteTask called: projectId=$projectId, taskId=$taskId")
                val response = taskService.deleteTask(projectId, taskId)
                Log.d(TAG, "deleteTask response: success=${response.success}")
                if (response.success) {
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "deleteTask EXCEPTION: ${e.message}", e)
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun deleteSubtask(projectId: String, taskId: String, subtaskId: String): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "deleteSubtask called: projectId=$projectId, taskId=$taskId, subtaskId=$subtaskId")
                val response = taskService.deleteTask(projectId, subtaskId)
                Log.d(TAG, "deleteSubtask response: success=${response.success}")
                if (response.success) {
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "deleteSubtask EXCEPTION: ${e.message}", e)
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun updateTaskStatus(projectId: String, taskId: String, request: UpdateTaskStatusRequest): ApiResult<TaskResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "updateTaskStatus: projectId=$projectId, taskId=$taskId, request=$request")
                val response = taskService.updateTaskStatus(projectId, taskId, request)
                Log.d(TAG, "updateTaskStatus response: success=${response.success}, message=${response.message}")
                Log.d(TAG, "updateTaskStatus response data: ${response.data}")
                if (response.success && response.data != null) {
                    ApiResult.Success(response.data)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateTaskStatus EXCEPTION: ${e.message}", e)
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun getTaskGroups(projectId: String): ApiResult<List<SectionResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = taskService.getSections(projectId)
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

    override suspend fun createSection(projectId: String, request: CreateSectionRequest): ApiResult<SectionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "createSection called: projectId=$projectId, name=${request.name}")
                val response = taskService.createSection(projectId, request)
                Log.d(TAG, "createSection response: success=${response.success}, message=${response.message}, data=${response.data}")
                if (response.success && response.data != null) {
                    Log.d(TAG, "createSection SUCCESS: id=${response.data.id}")
                    ApiResult.Success(response.data)
                } else {
                    Log.e(TAG, "createSection FAILED: ${response.message}")
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "createSection EXCEPTION: ${e.message}", e)
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun getMembers(projectId: String): ApiResult<List<MemberResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = taskService.getMembers(projectId)
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

    override suspend fun getTaskDetail(projectId: String, taskId: String): ApiResult<TaskDetailResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = taskService.getTaskDetail(projectId, taskId)
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

    override suspend fun getSubtasks(projectId: String, taskId: String): ApiResult<List<TaskResponse>> {
        return getTasks(projectId, null, null, null, taskId)
    }

    override suspend fun getTaskComments(projectId: String, taskId: String): ApiResult<List<CommentResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = taskService.getTaskComments(projectId, taskId)
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

    override suspend fun createTaskComment(projectId: String, taskId: String, content: String): ApiResult<CommentResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = taskService.createTaskComment(projectId, taskId, mapOf("content" to content))
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
}

@Singleton
class MeetingRepositoryImpl @Inject constructor(
    private val meetingService: MeetingService
) : MeetingRepository {

    override suspend fun getMeetings(projectId: String, format: String?): ApiResult<List<MeetingResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = meetingService.getMeetings(projectId, format)
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

    override suspend fun createMeeting(projectId: String, request: CreateMeetingRequest): ApiResult<MeetingResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = meetingService.createMeeting(projectId, request)
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
}