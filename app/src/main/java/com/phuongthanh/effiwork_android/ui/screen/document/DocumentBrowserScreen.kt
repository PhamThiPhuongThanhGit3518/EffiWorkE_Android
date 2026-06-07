package com.phuongthanh.effiwork_android.ui.screen.document

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.phuongthanh.effiwork_android.ui.screen.document.component.DocumentListPane
import com.phuongthanh.effiwork_android.ui.screen.document.component.FolderTree
import com.phuongthanh.effiwork_android.ui.screen.document.component.ProjectSectionTaskTree
import com.phuongthanh.effiwork_android.viewmodel.document.DocumentBrowserEffect
import com.phuongthanh.effiwork_android.viewmodel.document.DocumentBrowserUiState
import com.phuongthanh.effiwork_android.viewmodel.document.DocumentBrowserViewModel
import com.phuongthanh.effiwork_android.viewmodel.document.DocumentTab
import com.phuongthanh.effiwork_android.viewmodel.document.DocumentViewMode

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
        onToggleTask = { viewModel.toggleTaskExpand(it) },
        onSelectFolder = { viewModel.selectFolder(projectId, it) },
        onToggleFolder = { viewModel.toggleFolderExpand(it) },
        onDeleteFolder = { viewModel.deleteFolder(projectId, it) },
        onBreadcrumbClick = { viewModel.onBreadcrumbClick(projectId, it) },
        onDeleteDocument = { viewModel.deleteDocument(projectId, it) },
        onRenameDocument = { doc, newName -> viewModel.renameDocument(projectId, doc.id, newName) },
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
    onToggleTask: (String) -> Unit,
    onSelectFolder: (String?) -> Unit,
    onToggleFolder: (String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onBreadcrumbClick: (String) -> Unit,
    onDeleteDocument: (DocumentResponse) -> Unit,
    onRenameDocument: (DocumentResponse, String) -> Unit,
    onUploadSuccess: () -> Unit,
    onCreateFolder: (String) -> Unit
) {
    var showUploadDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    Scaffold(
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

            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .fillMaxHeight()
                ) {
                    when (uiState.activeTab) {
                        DocumentTab.PROJECT -> ProjectSectionTaskTree(
                            sections = uiState.sections,
                            allTasks = uiState.allTasks,
                            selectedSectionId = uiState.selectedSectionId,
                            selectedTaskId = uiState.selectedTaskId,
                            expandedTaskIds = uiState.expandedTaskIds,
                            onSelectSection = onSelectSection,
                            onSelectTask = onSelectTask,
                            onToggleTask = onToggleTask
                        )
                        DocumentTab.PERSONAL -> FolderTree(
                            folders = uiState.folderTree.filter { it.isPersonal() },
                            selectedFolderId = uiState.selectedFolderId,
                            expandedIds = uiState.expandedFolderIds,
                            onSelect = onSelectFolder,
                            onToggle = onToggleFolder,
                            onDelete = { folder -> onDeleteFolder(folder.id) },
                            currentUserId = currentUserId
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    DocumentBreadcrumb(
                        items = when (uiState.activeTab) {
                            DocumentTab.PROJECT -> uiState.projectBreadcrumb()
                            DocumentTab.PERSONAL -> uiState.personalBreadcrumb()
                        },
                        onItemClick = onBreadcrumbClick
                    )
                    DocumentListPane(
                        uiState = uiState,
                        currentUserId = currentUserId,
                        onPreview = { doc -> onPreview(doc.id) },
                        onDelete = onDeleteDocument,
                        onRename = onRenameDocument
                    )
                }
            }
        }
    }

    if (showUploadDialog) {
        DocumentUploadDialog(
            activeTab = uiState.activeTab,
            selectedFolderId = uiState.selectedFolderId,
            selectedTaskId = uiState.selectedTaskId,
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Document Browser - Personal")
@Composable
fun DocumentBrowserScreenPersonalPreview() {
    MaterialTheme {
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
            onToggleTask = {},
            onSelectFolder = {},
            onToggleFolder = {},
            onDeleteFolder = {},
            onBreadcrumbClick = {},
            onDeleteDocument = {},
            onRenameDocument = { _, _ -> },
            onUploadSuccess = {},
            onCreateFolder = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Document Browser - Project")
@Composable
fun DocumentBrowserScreenProjectPreview() {
    MaterialTheme {
        DocumentBrowserContent(
            uiState = DocumentBrowserUiState(
                activeTab = DocumentTab.PROJECT,
                viewMode = DocumentViewMode.LIST,
                sections = listOf(
                    SectionResponse(id = "s1", name = "Phần 1: Backend", projectId = "p1", sortOrder = 0, createdAt = null)
                ),
                allTasks = listOf(
                    TaskResponse(
                        id = "t1", projectId = "p1", name = "Thiết kế API",
                        description = null, groupId = "s1", group = null, groupName = null,
                        parentTaskId = null, status = "TODO", assigneeId = null, assigneeName = null,
                        owner = null, creator = null, startDate = null, endDate = null,
                        reminderTime = null, participants = null, subtasks = null,
                        createdAt = null, updatedAt = null
                    )
                ),
                selectedSectionId = "s1",
                selectedTaskId = "t1",
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
            onToggleTask = {},
            onSelectFolder = {},
            onToggleFolder = {},
            onDeleteFolder = {},
            onBreadcrumbClick = {},
            onDeleteDocument = {},
            onRenameDocument = { _, _ -> },
            onUploadSuccess = {},
            onCreateFolder = {}
        )
    }
}
