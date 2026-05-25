package com.phuongthanh.effiwork_android.viewmodel.task

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.model.request.CreateExtensionRequest
import com.phuongthanh.effiwork_android.data.model.request.CreateTaskRequest
import com.phuongthanh.effiwork_android.data.model.request.ReviewExtensionRequest
import com.phuongthanh.effiwork_android.data.model.request.UpdateTaskRequest
import com.phuongthanh.effiwork_android.data.model.request.UpdateTaskStatusRequest
import com.phuongthanh.effiwork_android.data.model.response.ExtensionRequestResponse
import com.phuongthanh.effiwork_android.data.model.response.TaskDetailResponse
import com.phuongthanh.effiwork_android.data.model.response.TaskResponse
import com.phuongthanh.effiwork_android.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "TaskViewModel"

sealed class TaskUiState {
    object Idle : TaskUiState()
    object Loading : TaskUiState()
    data class Success(val tasks: List<Task>) : TaskUiState()
    data class Error(val message: String) : TaskUiState()
}

data class Task(
    val id: String,
    val name: String,
    val description: String,
    val status: TaskStatus,
    val assignee: String, // assigneeName (người phụ trách)
    val assigneeId: String = "", // ownerId - DÙNG ĐỂ KIỂM TRA QUYỀN
    val creatorId: String = "", // creatorId - người tạo, KHÔNG có quyền đặc biệt
    val participants: List<String>,
    val participantIds: List<String> = emptyList(), // Thêm để lưu participant IDs
    val startDate: String,
    val endDate: String,
    val category: String, // sectionId for update
    val subtasks: List<Subtask> = emptyList(),
    // === THÊM CHO EXTENSION REQUEST ===
    val pendingExtensionRequestId: String? = null,
    val pendingExtensionRequestStatus: String? = null,
    val pendingExtensionRequestNewDueDate: String? = null,
    val pendingExtensionRequestReason: String? = null,
    val pendingExtensionRequestByName: String? = null
)

data class Subtask(
    val id: String,
    val name: String,
    val isCompleted: Boolean,
    val dueDate: String
)

enum class TaskStatus(val displayName: String, val serverValue: String) {
    NOT_STARTED("Chưa bắt đầu", "TODO"),
    IN_PROGRESS("Đang thực hiện", "IN_PROGRESS"),
    REVIEW("Đang review", "REVIEW"),
    COMPLETED("Hoàn thành", "DONE"),
    CANCELLED("Đã hủy", "CANCELLED");

    companion object {
        fun fromString(value: String): TaskStatus {
            return when (value.uppercase()) {
                "TODO", "NOT_STARTED" -> NOT_STARTED
                "IN_PROGRESS" -> IN_PROGRESS
                "REVIEW" -> REVIEW
                "DONE", "COMPLETED" -> COMPLETED
                "CANCELLED" -> CANCELLED
                else -> NOT_STARTED
            }
        }

        fun fromServerValue(value: String): TaskStatus {
            return entries.find { it.serverValue.equals(value, ignoreCase = true) } ?: fromString(value)
        }
    }
}

enum class TaskTab {
    COMMON_TASKS,
    ASSIGNED_TO_ME
}

enum class TaskCategory(val displayName: String) {
    ALL("Tất cả mục"),
    UI_DESIGN("Thiết kế giao diện"),
    FLOW_DESIGN("Thiết kế luồng"),
    CODE_IMPLEMENTATION("Thực hiện code"),
    TESTING("Kiểm thử"),
    DOCUMENTATION("Viết tài liệu")
}

sealed class TaskEffect {
    data class ShowToast(val message: String) : TaskEffect()
    data class TaskCreated(val taskName: String) : TaskEffect()
    data class TaskUpdated(val taskName: String) : TaskEffect()
    data object TaskDeleted : TaskEffect()
}

data class TaskGroup(
    val id: String,
    val name: String
)

data class TaskMember(
    val userId: String,
    val fullName: String
)

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TaskUiState>(TaskUiState.Idle)
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private val _tasksByTab = MutableStateFlow(
        mapOf(
            TaskTab.COMMON_TASKS to emptyList<Task>(),
            TaskTab.ASSIGNED_TO_ME to emptyList<Task>()
        )
    )
    val tasksByTab: StateFlow<Map<TaskTab, List<Task>>> = _tasksByTab.asStateFlow()

    private val _selectedTab = MutableStateFlow(TaskTab.COMMON_TASKS)
    val selectedTab: StateFlow<TaskTab> = _selectedTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow(TaskCategory.ALL)
    val selectedCategory: StateFlow<TaskCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _projectName = MutableStateFlow("NCKH")
    val projectName: StateFlow<String> = _projectName.asStateFlow()

    private val _projectId = MutableStateFlow("")
    val projectId: StateFlow<String> = _projectId.asStateFlow()

    private val _groupId = MutableStateFlow("")
    val groupId: StateFlow<String> = _groupId.asStateFlow()

    private val _effect = MutableSharedFlow<TaskEffect>()
    val effect: SharedFlow<TaskEffect> = _effect.asSharedFlow()

    private val _taskGroups = MutableStateFlow<List<TaskGroup>>(emptyList())
    val taskGroups: StateFlow<List<TaskGroup>> = _taskGroups.asStateFlow()

    private val _taskMembers = MutableStateFlow<List<TaskMember>>(emptyList())
    val taskMembers: StateFlow<List<TaskMember>> = _taskMembers.asStateFlow()

    fun setProjectInfo(projectId: String, projectName: String) {
        _projectId.value = projectId
        _projectName.value = projectName
    }

    fun setGroupId(groupId: String) {
        _groupId.value = groupId
    }

    fun selectTab(tab: TaskTab) {
        if (_selectedTab.value != tab) {
            _selectedTab.value = tab
            val cachedTasks = _tasksByTab.value[tab]
            if (cachedTasks.isNullOrEmpty()) {
                loadTasksForTab(tab)
            } else {
                // Use cached data for this tab
                _uiState.value = TaskUiState.Success(cachedTasks)
            }
        }
    }

    fun refreshCurrentTab() {
        loadTasksForTab(_selectedTab.value)
    }

    fun invalidateAndReloadAllTabs() {
        // Clear cache for both tabs and reload current tab
        _tasksByTab.value = mapOf(
            TaskTab.COMMON_TASKS to emptyList(),
            TaskTab.ASSIGNED_TO_ME to emptyList()
        )
        loadTasksForTab(_selectedTab.value)
    }

    fun loadTasksForTab(tab: TaskTab, sectionId: String? = null) {
        viewModelScope.launch {
            val effectiveSectionId = sectionId ?: _groupId.value.ifBlank { null }
            Log.d(TAG, "loadTasksForTab called: tab=$tab, sectionId=$effectiveSectionId")
            _uiState.value = TaskUiState.Loading
            val projectIdValue = _projectId.value
            Log.d(TAG, "loadTasksForTab projectId=$projectIdValue")
            if (projectIdValue.isBlank()) {
                _uiState.value = TaskUiState.Error("Project ID is required")
                return@launch
            }

            val result = if (tab == TaskTab.ASSIGNED_TO_ME) {
                taskRepository.getAssignedTasks(projectIdValue, effectiveSectionId, "null")
            } else {
                taskRepository.getTasks(projectIdValue, effectiveSectionId, null, null, "null")
            }

            when (result) {
                is ApiResult.Success -> {
                    Log.d(TAG, "Tasks loaded for tab $tab: ${result.data.size}")
                    result.data.forEach { task ->
                        Log.d(TAG, "  Task: id=${task.id}, name=${task.name}, status=${task.status}")
                        Log.d(TAG, "    assigneeName=${task.assigneeName}, owner.fullName=${task.owner?.fullName}")
                        Log.d(TAG, "    participants=${task.participants?.map { it.user?.fullName }}, description=${task.description?.take(50)}")
                    }
                    val tasks = result.data.map { it.toTask() }
                    _tasksByTab.value = _tasksByTab.value + (tab to tasks)
                    _uiState.value = TaskUiState.Success(tasks)
                    Log.d(TAG, "uiState set to Success with ${tasks.size} tasks")
                }
                is ApiResult.Error -> {
                    _uiState.value = TaskUiState.Error(result.message)
                }
                is ApiResult.Loading -> {
                    _uiState.value = TaskUiState.Loading
                }
            }
        }
    }

    fun selectCategory(category: TaskCategory) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadTasks(sectionId: String? = null, parentTaskId: String? = null) {
        loadTasksForTab(TaskTab.COMMON_TASKS, sectionId)
    }

    fun loadTaskDetailForEdit(taskId: String) {
        viewModelScope.launch {
            val projectIdValue = _projectId.value
            if (projectIdValue.isBlank()) return@launch

            when (val result = taskRepository.getTaskDetail(projectIdValue, taskId)) {
                is ApiResult.Success -> {
                    val task = mapTaskDetailToTask(result.data)
                    _uiState.value = TaskUiState.Success(listOf(task))
                }
                is ApiResult.Error -> {
                    _effect.emit(TaskEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    private fun mapTaskDetailToTask(detail: TaskDetailResponse): Task {
        return Task(
            id = detail.id,
            name = detail.title ?: "",
            description = detail.description ?: "",
            status = TaskStatus.fromString(detail.status ?: ""),
            assignee = detail.assignee?.fullName ?: "",
            assigneeId = detail.assigneeId ?: "",
            creatorId = detail.creator?.id ?: "",
            participants = detail.participants?.mapNotNull { it.user?.fullName } ?: emptyList(),
            participantIds = detail.participants?.mapNotNull { it.user?.id } ?: emptyList(),
            startDate = detail.startDate?.take(10) ?: "",
            endDate = detail.endDate?.take(10) ?: "",
            category = detail.groupId ?: "",
            subtasks = emptyList()
        )
    }

    fun loadTaskGroupsForCreate() {
        viewModelScope.launch {
            val projectIdValue = _projectId.value
            if (projectIdValue.isBlank()) return@launch

            when (val result = taskRepository.getTaskGroups(projectIdValue)) {
                is ApiResult.Success -> {
                    _taskGroups.value = result.data.map { TaskGroup(it.id, it.name ?: "") }
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Failed to load task groups: ${result.message}")
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun loadMembersForCreate() {
        viewModelScope.launch {
            val projectIdValue = _projectId.value
            if (projectIdValue.isBlank()) return@launch

            when (val result = taskRepository.getMembers(projectIdValue)) {
                is ApiResult.Success -> {
                    _taskMembers.value = result.data.mapNotNull { member ->
                        member.user?.let { TaskMember(it.id, it.fullName ?: "") }
                    }
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "Failed to load members: ${result.message}")
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun createTask(
        name: String,
        description: String,
        groupId: String?,
        assigneeId: String,
        startDate: String,
        endDate: String,
        reminderTime: String?,
        participantIds: List<String>
    ) {
        viewModelScope.launch {
            _uiState.value = TaskUiState.Loading
            val projectIdValue = _projectId.value

            val request = CreateTaskRequest(
                title = name,
                description = description,
                sectionId = groupId,
                parentTaskId = null,
                ownerId = assigneeId,
                startDate = startDate.takeIf { it.isNotBlank() },
                dueDate = endDate.takeIf { it.isNotBlank() },
                reminderAt = reminderTime?.takeIf { it.isNotBlank() },
                participantIds = participantIds
            )

            when (val result = taskRepository.createTask(projectIdValue, request)) {
                is ApiResult.Success -> {
                    Log.d(TAG, "Create task success: ${result.data.id}, title: ${result.data.name}")
                    _effect.emit(TaskEffect.ShowToast("Tạo công việc thành công"))
                    _effect.emit(TaskEffect.TaskCreated(name))
                    Log.d(TAG, "Calling refreshCurrentTab() to refresh list...")
                    invalidateAndReloadAllTabs()
                }
                is ApiResult.Error -> {
                    _uiState.value = TaskUiState.Error(result.message)
                    _effect.emit(TaskEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {
                    _uiState.value = TaskUiState.Loading
                }
            }
        }
    }

    fun createSubtask(
        name: String,
        description: String,
        parentTaskId: String,
        groupId: String?,
        assigneeId: String,
        startDate: String,
        endDate: String,
        reminderTime: String?,
        participantIds: List<String>
    ) {
        viewModelScope.launch {
            _uiState.value = TaskUiState.Loading
            val projectIdValue = _projectId.value

            Log.d(TAG, "createSubtask: projectId=$projectIdValue, parentTaskId=$parentTaskId")

            val request = CreateTaskRequest(
                title = name,
                description = description,
                sectionId = groupId,
                parentTaskId = parentTaskId,
                ownerId = assigneeId,
                startDate = startDate.takeIf { it.isNotBlank() },
                dueDate = endDate.takeIf { it.isNotBlank() },
                reminderAt = reminderTime?.takeIf { it.isNotBlank() },
                participantIds = participantIds
            )

            when (val result = taskRepository.createTask(projectIdValue, request)) {
                is ApiResult.Success -> {
                    Log.d(TAG, "Create subtask success: ${result.data.id}, title: ${result.data.name}")
                    _effect.emit(TaskEffect.ShowToast("Tạo công việc con thành công"))
                    _effect.emit(TaskEffect.TaskCreated(name))
                    invalidateAndReloadAllTabs()
                }
                is ApiResult.Error -> {
                    _uiState.value = TaskUiState.Error(result.message)
                    _effect.emit(TaskEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {
                    _uiState.value = TaskUiState.Loading
                }
            }
        }
    }

    fun updateTask(
        taskId: String,
        name: String,
        description: String,
        groupId: String?,
        assigneeId: String,
        startDate: String,
        endDate: String,
        reminderTime: String?,
        participantIds: List<String>
    ) {
        viewModelScope.launch {
            _uiState.value = TaskUiState.Loading
            val projectIdValue = _projectId.value
            Log.d(TAG, "updateTask: projectId=$projectIdValue, taskId=$taskId")
            Log.d(TAG, "updateTask: name=$name, groupId=$groupId")
            Log.d(TAG, "updateTask: assigneeId=$assigneeId")

            val request = UpdateTaskRequest(
                title = name.takeIf { it.isNotBlank() },
                description = description.takeIf { it.isNotBlank() },
                sectionId = groupId,
                ownerId = assigneeId.takeIf { it.isNotBlank() },
                startDate = startDate.takeIf { it.isNotBlank() },
                dueDate = endDate.takeIf { it.isNotBlank() },
                reminderAt = reminderTime?.takeIf { it.isNotBlank() },
                participantIds = participantIds.takeIf { it.isNotEmpty() }
            )

            Log.d(TAG, "updateTask: request=$request")
            when (val result = taskRepository.updateTask(projectIdValue, taskId, request)) {
                is ApiResult.Success -> {
                    Log.d(TAG, "Update task success: ${result.data.id}")
                    _effect.emit(TaskEffect.ShowToast("Cập nhật công việc thành công"))
                    _effect.emit(TaskEffect.TaskUpdated(name))
                    invalidateAndReloadAllTabs()
                }
                is ApiResult.Error -> {
                    _uiState.value = TaskUiState.Error(result.message)
                    _effect.emit(TaskEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {
                    _uiState.value = TaskUiState.Loading
                }
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            _uiState.value = TaskUiState.Loading
            val projectIdValue = _projectId.value

            when (val result = taskRepository.deleteTask(projectIdValue, taskId)) {
                is ApiResult.Success -> {
                    Log.d(TAG, "Delete task success")
                    _effect.emit(TaskEffect.ShowToast("Xóa công việc thành công"))
                    _effect.emit(TaskEffect.TaskDeleted)
                    invalidateAndReloadAllTabs()
                }
                is ApiResult.Error -> {
                    _uiState.value = TaskUiState.Error(result.message)
                    _effect.emit(TaskEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {
                    _uiState.value = TaskUiState.Loading
                }
            }
        }
    }

    fun toggleSubtask(taskId: String, subtaskId: String) {
        val currentState = _uiState.value
        if (currentState is TaskUiState.Success) {
            val updatedTasks = currentState.tasks.map { task ->
                if (task.id == taskId) {
                    task.copy(
                        subtasks = task.subtasks.map { subtask ->
                            if (subtask.id == subtaskId) {
                                subtask.copy(isCompleted = !subtask.isCompleted)
                            } else subtask
                        }
                    )
                } else task
            }
            _uiState.value = TaskUiState.Success(updatedTasks)
        }
    }

    fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
        viewModelScope.launch {
            val projectIdValue = _projectId.value
            if (projectIdValue.isBlank()) {
                Log.e(TAG, "updateTaskStatus: projectId is blank!")
                return@launch
            }

            val statusValue = newStatus.serverValue
            Log.d(TAG, "updateTaskStatus: projectId=$projectIdValue, taskId=$taskId, newStatus=$newStatus, serverValue=$statusValue")

            when (val result = taskRepository.updateTaskStatus(projectIdValue, taskId, UpdateTaskStatusRequest(statusValue))) {
                is ApiResult.Success -> {
                    Log.d(TAG, "updateTaskStatus SUCCESS: taskId=$taskId, newStatus=$statusValue")
                    _effect.emit(TaskEffect.ShowToast("Cập nhật trạng thái thành công"))
                    invalidateAndReloadAllTabs()
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "updateTaskStatus ERROR: ${result.message}")
                    _effect.emit(TaskEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun createExtensionRequest(taskId: String, newDueDate: String, reason: String) {
        viewModelScope.launch {
            val projectIdValue = _projectId.value
            if (projectIdValue.isBlank()) {
                Log.e(TAG, "createExtensionRequest: projectId is blank!")
                return@launch
            }
            val request = CreateExtensionRequest(newDueDate = newDueDate, reason = reason)
            Log.d(TAG, "createExtensionRequest: projectId=$projectIdValue, taskId=$taskId, newDueDate=$newDueDate")
            when (val result = taskRepository.createExtensionRequest(projectIdValue, taskId, request)) {
                is ApiResult.Success -> {
                    Log.d(TAG, "createExtensionRequest SUCCESS")
                    _effect.emit(TaskEffect.ShowToast("Đã gửi yêu cầu gia hạn"))
                    invalidateAndReloadAllTabs()
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "createExtensionRequest ERROR: ${result.message}")
                    _effect.emit(TaskEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun approveExtensionRequest(taskId: String, requestId: String, note: String? = null) {
        viewModelScope.launch {
            val projectIdValue = _projectId.value
            if (projectIdValue.isBlank()) {
                Log.e(TAG, "approveExtensionRequest: projectId is blank!")
                return@launch
            }
            Log.d(TAG, "approveExtensionRequest: projectId=$projectIdValue, taskId=$taskId, requestId=$requestId")
            when (val result = taskRepository.approveExtensionRequest(projectIdValue, taskId, requestId, note)) {
                is ApiResult.Success -> {
                    Log.d(TAG, "approveExtensionRequest SUCCESS")
                    _effect.emit(TaskEffect.ShowToast("Đã duyệt yêu cầu gia hạn"))
                    invalidateAndReloadAllTabs()
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "approveExtensionRequest ERROR: ${result.message}")
                    _effect.emit(TaskEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun rejectExtensionRequest(taskId: String, requestId: String, note: String? = null) {
        viewModelScope.launch {
            val projectIdValue = _projectId.value
            if (projectIdValue.isBlank()) {
                Log.e(TAG, "rejectExtensionRequest: projectId is blank!")
                return@launch
            }
            Log.d(TAG, "rejectExtensionRequest: projectId=$projectIdValue, taskId=$taskId, requestId=$requestId")
            when (val result = taskRepository.rejectExtensionRequest(projectIdValue, taskId, requestId, note)) {
                is ApiResult.Success -> {
                    Log.d(TAG, "rejectExtensionRequest SUCCESS")
                    _effect.emit(TaskEffect.ShowToast("Đã từ chối yêu cầu gia hạn"))
                    invalidateAndReloadAllTabs()
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "rejectExtensionRequest ERROR: ${result.message}")
                    _effect.emit(TaskEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun getExtensionRequests(taskId: String, onResult: (List<ExtensionRequestResponse>) -> Unit) {
        viewModelScope.launch {
            val projectIdValue = _projectId.value
            if (projectIdValue.isBlank()) {
                Log.e(TAG, "getExtensionRequests: projectId is blank!")
                return@launch
            }
            Log.d(TAG, "getExtensionRequests: projectId=$projectIdValue, taskId=$taskId")
            when (val result = taskRepository.getExtensionRequests(projectIdValue, taskId)) {
                is ApiResult.Success -> {
                    Log.d(TAG, "getExtensionRequests SUCCESS: ${result.data.size} requests")
                    onResult(result.data)
                }
                is ApiResult.Error -> {
                    Log.e(TAG, "getExtensionRequests ERROR: ${result.message}")
                    _effect.emit(TaskEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    private fun TaskResponse.toTask(): Task {
        val task = Task(
            id = id,
            name = name ?: "",
            description = description ?: "",
            status = TaskStatus.fromString(status ?: ""),
            assignee = assigneeName ?: owner?.fullName ?: "",
            assigneeId = assigneeId ?: owner?.id ?: "",
            creatorId = creator?.id ?: "",
            participants = participants?.mapNotNull { it.user?.fullName } ?: emptyList(),
            participantIds = participants?.mapNotNull { it.userId } ?: emptyList(),
            startDate = startDate?.take(10) ?: "",
            endDate = endDate?.take(10) ?: "",
            category = groupId ?: "",
            subtasks = subtasks?.map { Subtask(it.id, it.name, it.isCompleted, it.dueDate) } ?: emptyList(),
            pendingExtensionRequestId = null,
            pendingExtensionRequestStatus = null,
            pendingExtensionRequestNewDueDate = null,
            pendingExtensionRequestReason = null,
            pendingExtensionRequestByName = null
        )
        Log.d(TAG, "toTask mapped: id=$id, name=$name, assignee=${task.assignee}, assigneeId=${task.assigneeId}, creatorId=${task.creatorId}, participants=${task.participants}, startDate=${startDate}, endDate=${endDate}")
        return task
    }
}