package com.phuongthanh.effiwork_android.viewmodel.document

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.model.request.document.CreateFolderRequest
import com.phuongthanh.effiwork_android.data.model.request.document.UpdateDocumentRequest
import com.phuongthanh.effiwork_android.data.model.response.DocumentResponse
import com.phuongthanh.effiwork_android.data.model.response.TaskDocument
import com.phuongthanh.effiwork_android.data.model.response.TaskResponse
import com.phuongthanh.effiwork_android.data.repository.DocumentRepository
import com.phuongthanh.effiwork_android.data.repository.FolderRepository
import com.phuongthanh.effiwork_android.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentBrowserViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val folderRepository: FolderRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentBrowserUiState())
    val uiState: StateFlow<DocumentBrowserUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<DocumentBrowserEffect>()
    val effect: SharedFlow<DocumentBrowserEffect> = _effect.asSharedFlow()

    private var taskDetailCache: Map<String, List<DocumentResponse>> = emptyMap()
    private var currentProjectId: String? = null

    fun setCurrentUserId(userId: String) {
        _uiState.update { it.copy(currentUserId = userId) }
    }

    fun initialize(projectId: String) {
        currentProjectId = projectId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            loadSectionsAndTasks(projectId)
            loadFolderTree(projectId)
            loadPersonalDocuments(projectId, folderId = null)
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
                            is ApiResult.Error -> emptyList<com.phuongthanh.effiwork_android.data.model.response.TaskResponse>()
                            ApiResult.Loading -> emptyList<com.phuongthanh.effiwork_android.data.model.response.TaskResponse>()
                        }
                    }
                }.awaitAll()
                val allTasks = tasksPerSection.flatten()
                _uiState.update { it.copy(sections = sections, allTasks = allTasks) }
            }
            is ApiResult.Error -> {
                _effect.emit(DocumentBrowserEffect.ShowError(
                    "Lỗi tải sections",
                    sectionsResult.message
                ))
            }
            ApiResult.Loading -> Unit
        }
    }

    fun selectTab(projectId: String, tab: DocumentTab) {
        _uiState.update { it.copy(activeTab = tab, searchQuery = "") }
    }

    fun selectSection(projectId: String, sectionId: String?) {
        _uiState.update { it.copy(selectedSectionId = sectionId, selectedTaskId = null) }
    }

    fun selectTask(projectId: String, task: TaskResponse) {
        _uiState.update { it.copy(selectedTaskId = task.id, selectedSectionId = task.groupId) }
        expandTaskAncestors(task.id)
        loadTaskAttachments(projectId, task.id)
    }

    private fun expandTaskAncestors(taskId: String) {
        val ancestors = mutableSetOf<String>()
        val path = findTaskPath(_uiState.value.allTasks, taskId)
        path.dropLast(1).forEach { ancestors.add(it.id) }
        _uiState.update { it.copy(expandedTaskIds = it.expandedTaskIds + ancestors) }
    }

    fun toggleTaskExpand(taskId: String) {
        _uiState.update {
            val newSet = if (taskId in it.expandedTaskIds) it.expandedTaskIds - taskId
            else it.expandedTaskIds + taskId
            it.copy(expandedTaskIds = newSet)
        }
    }

    private fun loadTaskAttachments(projectId: String, taskId: String) {
        taskDetailCache[taskId]?.let { cached ->
            _uiState.update { it.copy(taskAttachments = cached) }
            return
        }
        viewModelScope.launch {
            when (val result = taskRepository.getTaskDetail(projectId, taskId)) {
                is ApiResult.Success -> {
                    val attachments = result.data.attachments
                        ?.mapNotNull { it.document }
                        ?.map { it.toDocumentResponse() }
                        ?: emptyList()
                    taskDetailCache = taskDetailCache + (taskId to attachments)
                    _uiState.update { it.copy(taskAttachments = attachments) }
                }
                is ApiResult.Error -> {
                    _effect.emit(DocumentBrowserEffect.ShowError(
                        "Lỗi tải tài liệu task",
                        result.message
                    ))
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun onBreadcrumbClick(projectId: String, itemId: String) {
        when (itemId) {
            "project-root", "personal-root" -> {
                _uiState.update { it.copy(selectedSectionId = null, selectedTaskId = null) }
            }
            else -> {
                val isSection = _uiState.value.sections.any { it.id == itemId }
                if (isSection) {
                    _uiState.update { it.copy(selectedSectionId = itemId, selectedTaskId = null) }
                }
            }
        }
    }

    fun selectFolder(projectId: String, folderId: String?) {
        _uiState.update { it.copy(selectedFolderId = folderId) }
        loadPersonalDocuments(projectId, folderId)
    }

    fun toggleFolderExpand(folderId: String) {
        _uiState.update {
            val newSet = if (folderId in it.expandedFolderIds) it.expandedFolderIds - folderId
            else it.expandedFolderIds + folderId
            it.copy(expandedFolderIds = newSet)
        }
    }

    private fun loadFolderTree(projectId: String) {
        viewModelScope.launch {
            when (val result = folderRepository.getFolderTree(projectId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(folderTree = result.data) }
                }
                is ApiResult.Error -> {
                    _effect.emit(DocumentBrowserEffect.ShowError(
                        "Lỗi tải cây thư mục",
                        result.message
                    ))
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun loadPersonalDocuments(projectId: String, folderId: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = documentRepository.listDocuments(
                projectId = projectId,
                folderId = folderId,
                visibilityType = "PERSONAL",
                page = 1,
                limit = 100
            )) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(personalDocuments = result.data, isLoading = false)
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(DocumentBrowserEffect.ShowError(
                        "Lỗi tải tài liệu",
                        result.message
                    ))
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun onSearchChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleViewMode() {
        _uiState.update {
            val newMode = if (it.viewMode == DocumentViewMode.LIST) DocumentViewMode.GRID
            else DocumentViewMode.LIST
            it.copy(viewMode = newMode)
        }
    }

    fun createFolder(projectId: String, name: String) {
        val current = _uiState.value
        viewModelScope.launch {
            val request = CreateFolderRequest(
                name = name,
                parentFolderId = current.selectedFolderId,
                folderType = "PERSONAL"
            )
            when (val result = folderRepository.createFolder(projectId, request)) {
                is ApiResult.Success -> {
                    _effect.emit(DocumentBrowserEffect.FolderCreated)
                    loadFolderTree(projectId)
                }
                is ApiResult.Error -> {
                    _effect.emit(DocumentBrowserEffect.ShowError(
                        "Không tạo được thư mục",
                        result.message
                    ))
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun deleteDocument(projectId: String, document: DocumentResponse) {
        viewModelScope.launch {
            when (val result = documentRepository.deleteDocument(projectId, document.id)) {
                is ApiResult.Success -> {
                    _effect.emit(DocumentBrowserEffect.DocumentDeleted)
                    if (_uiState.value.activeTab == DocumentTab.PERSONAL) {
                        loadPersonalDocuments(projectId, _uiState.value.selectedFolderId)
                    }
                }
                is ApiResult.Error -> {
                    _effect.emit(DocumentBrowserEffect.ShowError(
                        "Không xóa được tài liệu",
                        result.message
                    ))
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun deleteFolder(projectId: String, folderId: String) {
        viewModelScope.launch {
            when (val result = folderRepository.deleteFolder(projectId, folderId)) {
                is ApiResult.Success -> {
                    _effect.emit(DocumentBrowserEffect.DocumentDeleted)
                    if (_uiState.value.selectedFolderId == folderId) {
                        selectFolder(projectId, null)
                    }
                    loadFolderTree(projectId)
                }
                is ApiResult.Error -> {
                    _effect.emit(DocumentBrowserEffect.ShowError(
                        "Không xóa được thư mục",
                        result.message
                    ))
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun renameDocument(projectId: String, documentId: String, newName: String) {
        viewModelScope.launch {
            val request = UpdateDocumentRequest(fileName = newName)
            when (val result = documentRepository.updateDocument(projectId, documentId, request)) {
                is ApiResult.Success -> {
                    _effect.emit(DocumentBrowserEffect.DocumentDeleted)
                    if (_uiState.value.activeTab == DocumentTab.PERSONAL) {
                        loadPersonalDocuments(projectId, _uiState.value.selectedFolderId)
                    }
                }
                is ApiResult.Error -> {
                    _effect.emit(DocumentBrowserEffect.ShowError(
                        "Không đổi tên được",
                        result.message
                    ))
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun onUploadSuccess() {
        val projectId = currentProjectId ?: return
        val current = _uiState.value
        when (current.activeTab) {
            DocumentTab.PERSONAL -> {
                loadPersonalDocuments(projectId, current.selectedFolderId)
            }
            DocumentTab.PROJECT -> {
                val taskId = current.selectedTaskId
                if (taskId != null) {
                    taskDetailCache = taskDetailCache - taskId
                    loadTaskAttachments(projectId, taskId)
                }
            }
        }
    }
}

private fun TaskDocument.toDocumentResponse(): DocumentResponse = DocumentResponse(
    id = this.id,
    fileName = this.fileName ?: "Unknown",
    filePath = this.filePath,
    mimeType = this.mimeType,
    fileSize = this.fileSize?.toLongOrNull() ?: 0L,
    createdAt = null
)
