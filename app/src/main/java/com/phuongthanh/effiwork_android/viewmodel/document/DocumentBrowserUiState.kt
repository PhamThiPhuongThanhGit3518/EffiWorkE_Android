package com.phuongthanh.effiwork_android.viewmodel.document

import com.phuongthanh.effiwork_android.data.model.response.DocumentResponse
import com.phuongthanh.effiwork_android.data.model.response.SectionResponse
import com.phuongthanh.effiwork_android.data.model.response.TaskResponse
import com.phuongthanh.effiwork_android.data.model.response.document.FolderNode

enum class DocumentTab { PROJECT, PERSONAL }
enum class DocumentViewMode { LIST, GRID }

sealed class DocumentGridItem {
    data class SectionFolder(val section: SectionResponse) : DocumentGridItem()
    data class TaskFolder(val task: TaskResponse) : DocumentGridItem()
    data class File(val document: DocumentResponse) : DocumentGridItem()
}

data class DocumentBrowserUiState(
    val activeTab: DocumentTab = DocumentTab.PROJECT,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentUserId: String = "",

    val sections: List<SectionResponse> = emptyList(),
    val allTasks: List<TaskResponse> = emptyList(),
    val selectedSectionId: String? = null,
    val taskPath: List<TaskResponse> = emptyList(),
    val subtasksByTaskId: Map<String, List<TaskResponse>> = emptyMap(),
    val taskAttachments: List<DocumentResponse> = emptyList(),

    val folderTree: List<FolderNode> = emptyList(),
    val selectedFolderId: String? = null,
    val expandedFolderIds: Set<String> = emptySet(),
    val personalDocuments: List<DocumentResponse> = emptyList()
) {
    val currentTask: TaskResponse?
        get() = taskPath.lastOrNull()

    val currentTaskChildren: List<TaskResponse>
        get() = currentTask?.id?.let { subtasksByTaskId[it] } ?: emptyList()

    val currentProjectItems: List<DocumentGridItem>
        get() {
            val items = mutableListOf<DocumentGridItem>()
            val task = currentTask
            if (task != null) {
                currentTaskChildren.forEach { items += DocumentGridItem.TaskFolder(it) }
                items += taskAttachments.map { DocumentGridItem.File(it) }
            } else if (selectedSectionId != null) {
                allTasks
                    .filter { it.groupId == selectedSectionId && it.parentTaskId == null }
                    .forEach { items += DocumentGridItem.TaskFolder(it) }
            } else {
                sections.forEach { items += DocumentGridItem.SectionFolder(it) }
            }
            return items
        }

    fun projectBreadcrumb(): List<BreadcrumbItem> {
        val items = mutableListOf(
            BreadcrumbItem(
                id = "project-root",
                label = "Tài liệu dự án",
                isCurrent = selectedSectionId == null
            )
        )
        if (selectedSectionId != null) {
            val section = sections.firstOrNull { it.id == selectedSectionId }
            items.add(
                BreadcrumbItem(
                    id = selectedSectionId,
                    label = section?.name ?: "Section",
                    isCurrent = taskPath.isEmpty()
                )
            )
        }
        taskPath.forEachIndexed { index, task ->
            items.add(
                BreadcrumbItem(
                    id = task.id,
                    label = task.name ?: "Task",
                    isCurrent = index == taskPath.lastIndex
                )
            )
        }
        return items
    }

    fun personalBreadcrumb(): List<BreadcrumbItem> {
        val items = mutableListOf(
            BreadcrumbItem(
                id = "personal-root",
                label = "Tài liệu cá nhân",
                isCurrent = selectedFolderId == null
            )
        )
        if (selectedFolderId != null) {
            val folderPath = findFolderPath(folderTree, selectedFolderId)
            folderPath.forEach { folder ->
                items.add(
                    BreadcrumbItem(
                        id = folder.id,
                        label = folder.name,
                        isCurrent = folder.id == selectedFolderId
                    )
                )
            }
        }
        return items
    }

    val currentPersonalFolderChildren: List<FolderNode>
        get() {
            if (selectedFolderId == null) {
                return folderTree.filter { it.isPersonal() }
            }
            val selected = findFolderById(folderTree, selectedFolderId) ?: return emptyList()
            return selected.children
        }
}

data class BreadcrumbItem(
    val id: String,
    val label: String,
    val isCurrent: Boolean = false
)

internal fun findFolderById(folders: List<FolderNode>, folderId: String?): FolderNode? {
    if (folderId == null) return null
    for (folder in folders) {
        if (folder.id == folderId) return folder
        val child = findFolderById(folder.children, folderId)
        if (child != null) return child
    }
    return null
}

internal fun findFolderPath(folders: List<FolderNode>, folderId: String?): List<FolderNode> {
    if (folderId == null) return emptyList()
    for (folder in folders) {
        if (folder.id == folderId) return listOf(folder)
        val subPath = findFolderPath(folder.children, folderId)
        if (subPath.isNotEmpty()) return listOf(folder) + subPath
    }
    return emptyList()
}
