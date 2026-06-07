package com.phuongthanh.effiwork_android.ui.screen.document

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.data.model.response.DocumentResponse
import com.phuongthanh.effiwork_android.data.model.response.SectionResponse
import com.phuongthanh.effiwork_android.data.model.response.TaskResponse
import com.phuongthanh.effiwork_android.data.model.response.UploadedByInfo
import com.phuongthanh.effiwork_android.data.model.response.document.FolderNode
import com.phuongthanh.effiwork_android.ui.screen.document.component.DocumentBreadcrumb
import com.phuongthanh.effiwork_android.viewmodel.document.DocumentBrowserEffect
import com.phuongthanh.effiwork_android.viewmodel.document.DocumentBrowserUiState
import com.phuongthanh.effiwork_android.viewmodel.document.DocumentBrowserViewModel
import com.phuongthanh.effiwork_android.viewmodel.document.DocumentGridItem
import com.phuongthanh.effiwork_android.viewmodel.document.DocumentTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentBrowserScreen(
    projectId: String,
    currentUserId: String,
    onBackClick: () -> Unit,
    onPreview: (String) -> Unit,
    viewModel: DocumentBrowserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(currentUserId) {
        viewModel.setCurrentUserId(currentUserId)
    }

    LaunchedEffect(projectId) {
        viewModel.initialize(projectId)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DocumentBrowserEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is DocumentBrowserEffect.ShowError -> {
                    snackbarHostState.showSnackbar(
                        "${effect.message}: ${effect.description ?: ""}"
                    )
                }
                is DocumentBrowserEffect.FolderCreated -> {
                    Toast.makeText(context, "Đã tạo thư mục", Toast.LENGTH_SHORT).show()
                }
                is DocumentBrowserEffect.DocumentDeleted -> {
                    Toast.makeText(context, "Đã xóa", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    DocumentBrowserContent(
        uiState = uiState,
        projectId = projectId,
        currentUserId = currentUserId,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onPreview = onPreview,
        onSelectTab = { viewModel.selectTab(projectId, it) },
        onSelectSection = { viewModel.selectSection(projectId, it) },
        onSelectTask = { viewModel.selectTask(projectId, it) },
        onSelectFolder = { viewModel.selectFolder(projectId, it) },
        onDeleteDocument = { viewModel.deleteDocument(projectId, it) },
        onBreadcrumbClick = { viewModel.onBreadcrumbClick(projectId, it) },
        onUploadSuccess = { viewModel.onUploadSuccess() },
        onCreateFolder = { viewModel.createFolder(projectId, it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentBrowserContent(
    uiState: DocumentBrowserUiState,
    projectId: String,
    currentUserId: String,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onPreview: (String) -> Unit,
    onSelectTab: (DocumentTab) -> Unit,
    onSelectSection: (String?) -> Unit,
    onSelectTask: (TaskResponse) -> Unit,
    onSelectFolder: (String?) -> Unit,
    onDeleteDocument: (DocumentResponse) -> Unit,
    onBreadcrumbClick: (String) -> Unit,
    onUploadSuccess: () -> Unit,
    onCreateFolder: (String) -> Unit
) {
    var showUploadDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Tài liệu", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.activeTab == DocumentTab.PERSONAL) {
                        IconButton(onClick = { showCreateFolderDialog = true }) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "Tạo thư mục")
                        }
                    }
                    IconButton(onClick = { showUploadDialog = true }) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Upload")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = uiState.activeTab.ordinal) {
                Tab(
                    selected = uiState.activeTab == DocumentTab.PROJECT,
                    onClick = { onSelectTab(DocumentTab.PROJECT) },
                    text = { Text("Dự án") },
                    icon = { Icon(Icons.Default.Folder, null) }
                )
                Tab(
                    selected = uiState.activeTab == DocumentTab.PERSONAL,
                    onClick = { onSelectTab(DocumentTab.PERSONAL) },
                    text = { Text("Cá nhân") },
                    icon = { Icon(Icons.Default.Person, null) }
                )
            }

            DocumentBreadcrumb(
                items = when (uiState.activeTab) {
                    DocumentTab.PROJECT -> uiState.projectBreadcrumb()
                    DocumentTab.PERSONAL -> uiState.personalBreadcrumb()
                },
                onItemClick = onBreadcrumbClick
            )

            when (uiState.activeTab) {
                DocumentTab.PROJECT -> ProjectGrid(
                    items = uiState.currentProjectItems,
                    onSelectSection = onSelectSection,
                    onSelectTask = onSelectTask,
                    onPreviewDocument = onPreview
                )
                DocumentTab.PERSONAL -> PersonalGrid(
                    folders = uiState.currentPersonalFolderChildren,
                    documents = uiState.personalDocuments,
                    onSelectFolder = onSelectFolder,
                    onPreviewDocument = onPreview,
                    onDeleteDocument = onDeleteDocument,
                    currentUserId = currentUserId
                )
            }
        }
    }

    if (showUploadDialog) {
        DocumentUploadDialog(
            activeTab = uiState.activeTab,
            selectedFolderId = uiState.selectedFolderId,
            selectedTaskId = uiState.currentTask?.id,
            projectId = projectId,
            onDismiss = { showUploadDialog = false },
            onSuccess = {
                showUploadDialog = false
                onUploadSuccess()
            }
        )
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                onCreateFolder(name)
                showCreateFolderDialog = false
            }
        )
    }
}

@Composable
private fun ProjectGrid(
    items: List<DocumentGridItem>,
    onSelectSection: (String?) -> Unit,
    onSelectTask: (TaskResponse) -> Unit,
    onPreviewDocument: (String) -> Unit
) {
    if (items.isEmpty()) {
        EmptyProjectState()
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { gridItemKey(it) }) { item ->
            when (item) {
                is DocumentGridItem.SectionFolder -> GridFolderCard(
                    name = item.section.name ?: "Section",
                    subtitle = "Section",
                    onClick = { onSelectSection(item.section.id) }
                )
                is DocumentGridItem.TaskFolder -> GridFolderCard(
                    name = item.task.name ?: "Task",
                    subtitle = taskStatusLabel(item.task.status),
                    onClick = { onSelectTask(item.task) }
                )
                is DocumentGridItem.File -> GridFileCard(
                    doc = item.document,
                    onClick = { onPreviewDocument(item.document.id) }
                )
            }
        }
    }
}

@Composable
private fun PersonalGrid(
    folders: List<FolderNode>,
    documents: List<DocumentResponse>,
    onSelectFolder: (String?) -> Unit,
    onPreviewDocument: (String) -> Unit,
    onDeleteDocument: (DocumentResponse) -> Unit,
    currentUserId: String
) {
    if (folders.isEmpty() && documents.isEmpty()) {
        EmptyPersonalState()
        return
    }
    val items = buildList {
        addAll(folders.map { DocumentGridItem.SectionFolder(SectionResponse(
            id = it.id, name = it.name, projectId = it.projectId, sortOrder = null, createdAt = it.createdAt
        )) })
        addAll(documents.map { DocumentGridItem.File(it) })
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { gridItemKey(it) }) { item ->
            when (item) {
                is DocumentGridItem.SectionFolder -> GridFolderCard(
                    name = item.section.name ?: "Folder",
                    subtitle = "Thư mục cá nhân",
                    onClick = { onSelectFolder(item.section.id) }
                )
                is DocumentGridItem.File -> GridFileCard(
                    doc = item.document,
                    onClick = { onPreviewDocument(item.document.id) },
                    onDelete = if (item.document.owner?.id == currentUserId) {
                        { onDeleteDocument(item.document) }
                    } else null
                )
                else -> Unit
            }
        }
    }
}

@Composable
private fun GridFolderCard(
    name: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingIcon: ImageVector? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = trailingIcon ?: Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GridFileCard(
    doc: DocumentResponse,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center)
                )
                if (onDelete != null) {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Xóa",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = doc.fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = formatFileSize(doc.fileSize),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
    if (showDeleteDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Xóa tài liệu?") },
            text = { Text("Xóa \"${doc.fileName}\"? Hành động này không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun EmptyProjectState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Chưa có mục nào ở đây",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyPersonalState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Thư mục trống",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun gridItemKey(item: DocumentGridItem): String = when (item) {
    is DocumentGridItem.SectionFolder -> "section-${item.section.id}"
    is DocumentGridItem.TaskFolder -> "task-${item.task.id}"
    is DocumentGridItem.File -> "file-${item.document.id}"
}

private fun taskStatusLabel(status: String?): String = when (status) {
    "TODO" -> "Chưa làm"
    "IN_PROGRESS" -> "Đang làm"
    "DONE", "COMPLETED" -> "Hoàn thành"
    else -> status ?: "—"
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    return "${if (size >= 10 || unitIndex == 0) size.toInt() else "%.1f".format(size)} ${units[unitIndex]}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Document Browser - Project (root)")
@Composable
fun DocumentBrowserScreenProjectRootPreview() {
    MaterialTheme {
        Surface {
            DocumentBrowserContent(
                uiState = DocumentBrowserUiState(
                    activeTab = DocumentTab.PROJECT,
                    sections = listOf(
                        SectionResponse(id = "s1", name = "Phần 1: Backend", projectId = "p1", sortOrder = 0, createdAt = null),
                        SectionResponse(id = "s2", name = "Phần 2: Frontend", projectId = "p1", sortOrder = 1, createdAt = null)
                    )
                ),
                projectId = "p1",
                currentUserId = "u1",
                snackbarHostState = remember { SnackbarHostState() },
                onBackClick = {},
                onPreview = {},
                onSelectTab = {},
                onSelectSection = {},
                onSelectTask = {},
                onSelectFolder = {},
                onDeleteDocument = {},
                onBreadcrumbClick = {},
                onUploadSuccess = {},
                onCreateFolder = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Document Browser - Task level")
@Composable
fun DocumentBrowserScreenTaskLevelPreview() {
    MaterialTheme {
        Surface {
            DocumentBrowserContent(
                uiState = DocumentBrowserUiState(
                    activeTab = DocumentTab.PROJECT,
                    sections = listOf(
                        SectionResponse(id = "s1", name = "Phần 1: Backend", projectId = "p1", sortOrder = 0, createdAt = null)
                    ),
                    allTasks = listOf(
                        TaskResponse(
                            id = "t1", projectId = "p1", name = "Thiết kế API",
                            description = null, groupId = "s1", group = null, groupName = null,
                            parentTaskId = null, status = "IN_PROGRESS", assigneeId = null, assigneeName = null,
                            owner = null, creator = null, startDate = null, endDate = null,
                            reminderTime = null, participants = null, subtasks = null,
                            createdAt = null, updatedAt = null
                        )
                    ),
                    selectedSectionId = "s1",
                    taskPath = listOf(
                        TaskResponse(
                            id = "t1", projectId = "p1", name = "Thiết kế API",
                            description = null, groupId = "s1", group = null, groupName = null,
                            parentTaskId = null, status = "IN_PROGRESS", assigneeId = null, assigneeName = null,
                            owner = null, creator = null, startDate = null, endDate = null,
                            reminderTime = null, participants = null, subtasks = null,
                            createdAt = null, updatedAt = null
                        )
                    ),
                    subtasksByTaskId = mapOf(
                        "t1" to listOf(
                            TaskResponse(
                                id = "st1", projectId = "p1", name = "Viết OpenAPI",
                                description = null, groupId = "s1", group = null, groupName = null,
                                parentTaskId = "t1", status = "DONE", assigneeId = null, assigneeName = null,
                                owner = null, creator = null, startDate = null, endDate = null,
                                reminderTime = null, participants = null, subtasks = null,
                                createdAt = null, updatedAt = null
                            ),
                            TaskResponse(
                                id = "st2", projectId = "p1", name = "Review API",
                                description = null, groupId = "s1", group = null, groupName = null,
                                parentTaskId = "t1", status = "IN_PROGRESS", assigneeId = null, assigneeName = null,
                                owner = null, creator = null, startDate = null, endDate = null,
                                reminderTime = null, participants = null, subtasks = null,
                                createdAt = null, updatedAt = null
                            )
                        )
                    ),
                    taskAttachments = listOf(
                        DocumentResponse(
                            id = "d1", fileName = "API-design.pdf",
                            filePath = null, mimeType = "application/pdf", fileSize = 540_000,
                            createdAt = "2026-06-02", projectId = "p1", folderId = null,
                            visibilityType = "PROJECT_SHARED", updatedAt = null,
                            uploadedBy = UploadedByInfo(id = "u2", fullName = "Nguyễn B", email = null, avatarUrl = null),
                            owner = null, count = null
                        )
                    )
                ),
                projectId = "p1",
                currentUserId = "u1",
                snackbarHostState = remember { SnackbarHostState() },
                onBackClick = {},
                onPreview = {},
                onSelectTab = {},
                onSelectSection = {},
                onSelectTask = {},
                onSelectFolder = {},
                onDeleteDocument = {},
                onBreadcrumbClick = {},
                onUploadSuccess = {},
                onCreateFolder = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Document Browser - Personal")
@Composable
fun DocumentBrowserScreenPersonalPreview() {
    MaterialTheme {
        Surface {
            DocumentBrowserContent(
                uiState = DocumentBrowserUiState(
                    activeTab = DocumentTab.PERSONAL,
                    folderTree = listOf(
                        FolderNode(
                            id = "f1", projectId = "p1", parentFolderId = null,
                            name = "Hợp đồng", type = "PERSONAL", folderType = "PERSONAL",
                            ownerId = "u1", createdAt = null, updatedAt = null,
                            children = emptyList()
                        )
                    ),
                    selectedFolderId = "f1",
                    expandedFolderIds = setOf("f1"),
                    personalDocuments = listOf(
                        DocumentResponse(
                            id = "d1", fileName = "hop-dong.pdf",
                            filePath = null, mimeType = "application/pdf", fileSize = 245_000,
                            createdAt = "2026-06-01", projectId = null, folderId = "f1",
                            visibilityType = "PERSONAL", updatedAt = null,
                            uploadedBy = null,
                            owner = UploadedByInfo(id = "u1", fullName = "Tôi", email = null, avatarUrl = null),
                            count = null
                        )
                    )
                ),
                projectId = "p1",
                currentUserId = "u1",
                snackbarHostState = remember { SnackbarHostState() },
                onBackClick = {},
                onPreview = {},
                onSelectTab = {},
                onSelectSection = {},
                onSelectTask = {},
                onSelectFolder = {},
                onDeleteDocument = {},
                onBreadcrumbClick = {},
                onUploadSuccess = {},
                onCreateFolder = {}
            )
        }
    }
}
