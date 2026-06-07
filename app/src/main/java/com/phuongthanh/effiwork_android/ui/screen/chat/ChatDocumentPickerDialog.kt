package com.phuongthanh.effiwork_android.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.data.model.response.DocumentResponse
import com.phuongthanh.effiwork_android.data.model.response.TaskResponse
import com.phuongthanh.effiwork_android.data.model.response.document.FolderNode
import com.phuongthanh.effiwork_android.viewmodel.chat.ChatDocumentPickerViewModel
import com.phuongthanh.effiwork_android.viewmodel.document.DocumentTab

@Composable
fun ChatDocumentPickerDialog(
    projectId: String,
    onDismiss: () -> Unit,
    onSelect: (DocumentResponse) -> Unit,
    viewModel: ChatDocumentPickerViewModel = hiltViewModel()
) {
    LaunchedEffect(projectId) {
        viewModel.initialize(projectId)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Chọn tài liệu cho tin nhắn",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Chọn file từ dự án hoặc tài liệu cá nhân",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                PickerTabs(
                    activeTab = uiState.activeTab,
                    onSelect = viewModel::selectTab
                )

                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Box(
                        modifier = Modifier
                            .width(160.dp)
                            .fillMaxSize()
                            .background(Color(0xFFF7F7F7))
                    ) {
                        if (uiState.activeTab == DocumentTab.PROJECT) {
                            ProjectNavTree(
                                uiState = uiState,
                                onSelectSection = viewModel::selectSection,
                                onSelectTask = viewModel::selectTask
                            )
                        } else {
                            PersonalFolderTree(
                                folders = uiState.folderTree.filter { it.isPersonal() },
                                selectedFolderId = uiState.selectedFolderId,
                                onSelect = viewModel::selectFolder
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                        PickerBreadcrumb(
                            items = if (uiState.activeTab == DocumentTab.PROJECT)
                                buildProjectBreadcrumb(uiState) else buildPersonalBreadcrumb(uiState),
                            onClick = { id ->
                                if (uiState.activeTab == DocumentTab.PROJECT) {
                                    when (id) {
                                        "project-root" -> viewModel.selectSection(null)
                                        "section" -> {
                                            val sid = uiState.selectedSectionId ?: return@PickerBreadcrumb
                                            viewModel.selectTask(TaskResponse(
                                                id = "", projectId = projectId, name = null,
                                                description = null, groupId = sid, group = null,
                                                groupName = null, parentTaskId = null, status = null,
                                                assigneeId = null, assigneeName = null, owner = null,
                                                creator = null, startDate = null, endDate = null,
                                                reminderTime = null, participants = null,
                                                subtasks = null, createdAt = null, updatedAt = null
                                            ))
                                        }
                                        else -> Unit
                                    }
                                } else {
                                    if (id == "personal-root") viewModel.selectFolder(null)
                                }
                            }
                        )
                        PickerSearch(
                            query = uiState.searchQuery,
                            onQueryChange = viewModel::onSearchChange
                        )
                        PickerList(
                            uiState = uiState,
                            onSelectDocument = onSelect,
                            onOpenTask = viewModel::selectTask,
                            onOpenFolder = viewModel::selectFolder
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerTabs(
    activeTab: DocumentTab,
    onSelect: (DocumentTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        TabChip(
            label = "Tài liệu dự án",
            selected = activeTab == DocumentTab.PROJECT,
            onClick = { onSelect(DocumentTab.PROJECT) },
            modifier = Modifier.weight(1f)
        )
        TabChip(
            label = "Tài liệu cá nhân",
            selected = activeTab == DocumentTab.PERSONAL,
            onClick = { onSelect(DocumentTab.PERSONAL) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) Color.White else Color.Transparent
    val fg = if (selected) Color.Black else Color.Gray
    Surface(
        modifier = modifier.clickable { onClick() },
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ProjectNavTree(
    uiState: ChatDocumentPickerViewModel.UiState,
    onSelectSection: (String?) -> Unit,
    onSelectTask: (TaskResponse) -> Unit
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(8.dp)
    ) {
        TreeRow(
            icon = Icons.Default.FolderOpen,
            label = "Tài liệu dự án",
            selected = uiState.selectedSectionId == null,
            onClick = { onSelectSection(null) }
        )
        uiState.sections.forEach { section ->
            TreeRow(
                icon = Icons.Default.Folder,
                label = section.name ?: "Section",
                selected = uiState.selectedSectionId == section.id && uiState.selectedTaskId == null,
                onClick = { onSelectSection(section.id) }
            )
            if (uiState.selectedSectionId == section.id) {
                val sectionTasks = uiState.allTasks.filter { it.groupId == section.id && it.parentTaskId == null }
                sectionTasks.forEach { task ->
                    TaskTreeNode(
                        task = task,
                        depth = 1,
                        uiState = uiState,
                        onSelectTask = onSelectTask
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskTreeNode(
    task: TaskResponse,
    depth: Int,
    uiState: ChatDocumentPickerViewModel.UiState,
    onSelectTask: (TaskResponse) -> Unit
) {
    val subtasks = uiState.subtasksByTaskId[task.id] ?: emptyList()
    val selected = uiState.selectedTaskId == task.id
    TreeRow(
        icon = if (subtasks.isNotEmpty()) Icons.Default.FolderOpen else Icons.Default.Folder,
        label = task.name ?: "Task",
        depth = depth,
        selected = selected,
        onClick = { onSelectTask(task) }
    )
    subtasks.forEach { child ->
        TaskTreeNode(
            task = child,
            depth = depth + 1,
            uiState = uiState,
            onSelectTask = onSelectTask
        )
    }
}

@Composable
private fun TreeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    depth: Int = 0,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Color(0xFFE3F2FD) else Color.Transparent
    val fg = if (selected) Color(0xFF1565C0) else Color.Black
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 12).dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
            .background(bg, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = fg,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PersonalFolderTree(
    folders: List<FolderNode>,
    selectedFolderId: String?,
    onSelect: (String?) -> Unit
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(8.dp)
    ) {
        TreeRow(
            icon = Icons.Default.FolderOpen,
            label = "Tài liệu cá nhân",
            selected = selectedFolderId == null,
            onClick = { onSelect(null) }
        )
        folders.forEach { folder ->
            FolderTreeNode(
                folder = folder,
                depth = 1,
                selectedFolderId = selectedFolderId,
                onSelect = onSelect
            )
        }
    }
}

@Composable
private fun FolderTreeNode(
    folder: FolderNode,
    depth: Int,
    selectedFolderId: String?,
    onSelect: (String?) -> Unit
) {
    val selected = selectedFolderId == folder.id
    TreeRow(
        icon = Icons.Default.Folder,
        label = folder.name,
        depth = depth,
        selected = selected,
        onClick = { onSelect(folder.id) }
    )
    folder.children.forEach { child ->
        FolderTreeNode(
            folder = child,
            depth = depth + 1,
            selectedFolderId = selectedFolderId,
            onSelect = onSelect
        )
    }
}

private data class PickerBreadcrumbData(
    val id: String,
    val label: String
)

@Composable
private fun PickerBreadcrumb(
    items: List<PickerBreadcrumbData>,
    onClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            TextButton(
                onClick = { onClick(item.id) },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    item.label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (index == items.lastIndex) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (index < items.lastIndex) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    null,
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun PickerSearch(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Tìm trong thư mục hiện tại...") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

private sealed class PickerEntry {
    data class Folder(val label: String, val onOpen: () -> Unit) : PickerEntry()
    data class File(val document: DocumentResponse, val onPick: () -> Unit) : PickerEntry()
}

@Composable
private fun PickerList(
    uiState: ChatDocumentPickerViewModel.UiState,
    onSelectDocument: (DocumentResponse) -> Unit,
    onOpenTask: (TaskResponse) -> Unit,
    onOpenFolder: (String?) -> Unit
) {
    val entries = buildEntries(uiState, onOpenTask, onOpenFolder)
    val filtered = if (uiState.searchQuery.isBlank()) entries
    else entries.filter {
        when (it) {
            is PickerEntry.Folder -> it.label.contains(uiState.searchQuery, ignoreCase = true)
            is PickerEntry.File -> it.document.fileName.contains(uiState.searchQuery, ignoreCase = true)
        }
    }

    when {
        uiState.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        filtered.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Thư mục này trống",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        else -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered) { entry ->
                    when (entry) {
                        is PickerEntry.Folder -> EntryRow(
                            icon = Icons.Default.Folder,
                            name = entry.label,
                            description = null,
                            actionLabel = "Mở",
                            onClick = entry.onOpen
                        )
                        is PickerEntry.File -> EntryRow(
                            icon = Icons.Default.Description,
                            name = entry.document.fileName,
                            description = entry.document.mimeType,
                            actionLabel = "Chọn",
                            highlight = true,
                            onClick = {
                                onSelectDocument(entry.document)
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun buildEntries(
    uiState: ChatDocumentPickerViewModel.UiState,
    onOpenTask: (TaskResponse) -> Unit,
    onOpenFolder: (String?) -> Unit
): List<PickerEntry> {
    return if (uiState.activeTab == DocumentTab.PROJECT) {
        val items = mutableListOf<PickerEntry>()
        val task = uiState.allTasks.firstOrNull { it.id == uiState.selectedTaskId }
        if (task != null) {
            val children = uiState.subtasksByTaskId[task.id] ?: emptyList()
            children.forEach { child ->
                items += PickerEntry.Folder(child.name ?: "Task") { onOpenTask(child) }
            }
            val files = uiState.taskAttachments[task.id] ?: emptyList()
            files.forEach { file ->
                items += PickerEntry.File(file) {}
            }
        } else if (uiState.selectedSectionId != null) {
            val sectionTasks = uiState.allTasks
                .filter { it.groupId == uiState.selectedSectionId && it.parentTaskId == null }
            sectionTasks.forEach { task ->
                items += PickerEntry.Folder(task.name ?: "Task") { onOpenTask(task) }
            }
        }
        items
    } else {
        val items = mutableListOf<PickerEntry>()
        val currentFolders = if (uiState.selectedFolderId == null) {
            uiState.folderTree.filter { it.isPersonal() }
        } else {
            val selected = findFolderById(uiState.folderTree, uiState.selectedFolderId)
            selected?.children ?: emptyList()
        }
        currentFolders.forEach { folder ->
            items += PickerEntry.Folder(folder.name) { onOpenFolder(folder.id) }
        }
        uiState.personalDocuments.forEach { doc ->
            items += PickerEntry.File(doc) {}
        }
        items
    }
}

@Composable
private fun EntryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    description: String?,
    actionLabel: String,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
            if (!description.isNullOrBlank()) {
                Text(
                    description,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    if (highlight) Color(0xFFE3F2FD) else Color(0xFFEEEEEE),
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (highlight) {
                Icon(
                    Icons.Default.Check,
                    null,
                    tint = Color(0xFF1565C0),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(actionLabel, color = Color(0xFF1565C0), style = MaterialTheme.typography.labelSmall)
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(actionLabel, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun buildProjectBreadcrumb(
    uiState: ChatDocumentPickerViewModel.UiState
): List<PickerBreadcrumbData> {
    val items = mutableListOf(
        PickerBreadcrumbData("project-root", "Tài liệu dự án")
    )
    val sectionId = uiState.selectedSectionId
    if (sectionId != null) {
        val section = uiState.sections.firstOrNull { it.id == sectionId }
        items += PickerBreadcrumbData("section", section?.name ?: "Section")
    }
    val taskId = uiState.selectedTaskId
    if (taskId != null) {
        val task = findTaskById(uiState, taskId)
        if (task != null) {
            items += PickerBreadcrumbData("task-current", task.name ?: "Task")
        }
    }
    return items
}

private fun findTaskById(
    uiState: ChatDocumentPickerViewModel.UiState,
    taskId: String
): TaskResponse? {
    uiState.allTasks.firstOrNull { it.id == taskId }?.let { return it }
    uiState.subtasksByTaskId.values.forEach { list ->
        list.firstOrNull { it.id == taskId }?.let { return it }
    }
    return null
}

private fun buildPersonalBreadcrumb(
    uiState: ChatDocumentPickerViewModel.UiState
): List<PickerBreadcrumbData> {
    val items = mutableListOf(
        PickerBreadcrumbData("personal-root", "Tài liệu cá nhân")
    )
    val folderId = uiState.selectedFolderId
    if (folderId != null) {
        val path = findFolderPath(uiState.folderTree, folderId)
        path.forEach { folder ->
            items += PickerBreadcrumbData("folder-${folder.id}", folder.name)
        }
    }
    return items
}

private fun findFolderById(folders: List<FolderNode>, folderId: String?): FolderNode? {
    if (folderId == null) return null
    for (folder in folders) {
        if (folder.id == folderId) return folder
        val child = findFolderById(folder.children, folderId)
        if (child != null) return child
    }
    return null
}

private fun findFolderPath(folders: List<FolderNode>, folderId: String?): List<FolderNode> {
    if (folderId == null) return emptyList()
    for (folder in folders) {
        if (folder.id == folderId) return listOf(folder)
        val subPath = findFolderPath(folder.children, folderId)
        if (subPath.isNotEmpty()) return listOf(folder) + subPath
    }
    return emptyList()
}
