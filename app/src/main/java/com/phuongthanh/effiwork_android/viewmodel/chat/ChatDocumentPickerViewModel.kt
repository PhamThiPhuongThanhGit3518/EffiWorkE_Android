package com.phuongthanh.effiwork_android.viewmodel.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.model.response.DocumentResponse
import com.phuongthanh.effiwork_android.data.model.response.SectionResponse
import com.phuongthanh.effiwork_android.data.model.response.TaskResponse
import com.phuongthanh.effiwork_android.data.model.response.document.FolderNode
import com.phuongthanh.effiwork_android.data.repository.DocumentRepository
import com.phuongthanh.effiwork_android.data.repository.FolderRepository
import com.phuongthanh.effiwork_android.data.repository.TaskRepository
import com.phuongthanh.effiwork_android.viewmodel.document.DocumentTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatDocumentPickerViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val folderRepository: FolderRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    data class UiState(
        val activeTab: DocumentTab = DocumentTab.PROJECT,
        val searchQuery: String = "",
        val isLoading: Boolean = false,

        val sections: List<SectionResponse> = emptyList(),
        val allTasks: List<TaskResponse> = emptyList(),
        val selectedSectionId: String? = null,
        val selectedTaskId: String? = null,
        val subtasksByTaskId: Map<String, List<TaskResponse>> = emptyMap(),
        val taskAttachments: Map<String, List<DocumentResponse>> = emptyMap(),

        val folderTree: List<FolderNode> = emptyList(),
        val selectedFolderId: String? = null,
        val personalDocuments: List<DocumentResponse> = emptyList()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var currentProjectId: String? = null

    fun initialize(projectId: String) {
        if (currentProjectId == projectId && _uiState.value.sections.isNotEmpty()) return
        currentProjectId = projectId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            loadSectionsAndTasks(projectId)
            loadFolderTree(projectId)
            loadPersonalDocuments(projectId, null)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadSectionsAndTasks(projectId: String) = coroutineScope {
        when (val sectionsResult = taskRepository.getTaskGroups(projectId)) {
            is ApiResult.Success -> {
                val sections = sectionsResult.data
                val tasksPerSection = sections.map { section ->
                    async {
                        when (val tasksResult = taskRepository.getTasks(
                            projectId = projectId,
                            sectionId = section.id,
                            parentTaskId = null
                        )) {
                            is ApiResult.Success -> tasksResult.data
                            is ApiResult.Error -> emptyList<TaskResponse>()
                            ApiResult.Loading -> emptyList<TaskResponse>()
                        }
                    }
                }.awaitAll()
                val allTasks = tasksPerSection.flatten()
                _uiState.update { it.copy(sections = sections, allTasks = allTasks) }
            }
            is ApiResult.Error -> Unit
            ApiResult.Loading -> Unit
        }
    }

    fun selectTab(tab: DocumentTab) {
        _uiState.update { it.copy(activeTab = tab, searchQuery = "") }
    }

    fun selectSection(sectionId: String?) {
        _uiState.update {
            it.copy(
                selectedSectionId = sectionId,
                selectedTaskId = null
            )
        }
    }

    fun selectTask(task: TaskResponse) {
        val projectId = currentProjectId ?: return
        _uiState.update { it.copy(selectedTaskId = task.id) }
        loadSubtasks(projectId, task.id)
        loadTaskAttachments(projectId, task.id)
    }

    private fun loadSubtasks(projectId: String, taskId: String) {
        if (_uiState.value.subtasksByTaskId.containsKey(taskId)) return
        viewModelScope.launch {
            when (val result = taskRepository.getSubtasks(projectId, taskId)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(subtasksByTaskId = it.subtasksByTaskId + (taskId to result.data))
                    }
                }
                is ApiResult.Error -> Unit
                ApiResult.Loading -> Unit
            }
        }
    }

    private fun loadTaskAttachments(projectId: String, taskId: String) {
        if (_uiState.value.taskAttachments.containsKey(taskId)) return
        viewModelScope.launch {
            when (val result = taskRepository.getTaskDetail(projectId, taskId)) {
                is ApiResult.Success -> {
                    val attachments = result.data.attachments
                        ?.mapNotNull { it.document }
                        ?.map { it.toDocumentResponse() }
                        ?: emptyList()
                    _uiState.update { it.copy(taskAttachments = it.taskAttachments + (taskId to attachments)) }
                }
                is ApiResult.Error -> Unit
                ApiResult.Loading -> Unit
            }
        }
    }

    fun selectFolder(folderId: String?) {
        val projectId = currentProjectId ?: return
        _uiState.update { it.copy(selectedFolderId = folderId) }
        loadPersonalDocuments(projectId, folderId)
    }

    private fun loadFolderTree(projectId: String) {
        viewModelScope.launch {
            when (val result = folderRepository.getFolderTree(projectId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(folderTree = result.data) }
                }
                is ApiResult.Error -> Unit
                ApiResult.Loading -> Unit
            }
        }
    }

    private fun loadPersonalDocuments(projectId: String, folderId: String?) {
        viewModelScope.launch {
            when (val result = documentRepository.listDocuments(
                projectId = projectId,
                folderId = folderId,
                visibilityType = "PERSONAL",
                page = 1,
                limit = 100
            )) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(personalDocuments = result.data) }
                }
                is ApiResult.Error -> Unit
                ApiResult.Loading -> Unit
            }
        }
    }

    fun onSearchChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
}

private fun com.phuongthanh.effiwork_android.data.model.response.TaskDocument.toDocumentResponse(): DocumentResponse = DocumentResponse(
    id = this.id,
    fileName = this.fileName ?: "Unknown",
    filePath = this.filePath,
    mimeType = this.mimeType,
    fileSize = this.fileSize?.toLongOrNull() ?: 0L,
    createdAt = null
)
