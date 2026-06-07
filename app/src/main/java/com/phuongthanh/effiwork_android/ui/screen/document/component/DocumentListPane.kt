package com.phuongthanh.effiwork_android.ui.screen.document.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phuongthanh.effiwork_android.data.model.response.DocumentResponse
import com.phuongthanh.effiwork_android.viewmodel.document.DocumentBrowserUiState
import com.phuongthanh.effiwork_android.viewmodel.document.DocumentTab
import com.phuongthanh.effiwork_android.viewmodel.document.DocumentViewMode

@Composable
fun DocumentListPane(
    uiState: DocumentBrowserUiState,
    currentUserId: String,
    onPreview: (DocumentResponse) -> Unit,
    onDelete: (DocumentResponse) -> Unit,
    onRename: (DocumentResponse, String) -> Unit
) {
    val documents = when (uiState.activeTab) {
        DocumentTab.PROJECT -> uiState.taskAttachments
        DocumentTab.PERSONAL -> uiState.personalDocuments
    }

    val filtered = remember(documents, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) documents
        else documents.filter { it.fileName.contains(uiState.searchQuery, ignoreCase = true) }
    }

    if (filtered.isEmpty()) {
        EmptyState(tab = uiState.activeTab)
        return
    }

    when (uiState.viewMode) {
        DocumentViewMode.LIST -> LazyColumn(modifier = Modifier.fillMaxSize()) {
            listItems(filtered) { doc ->
                DocumentListItem(
                    doc = doc,
                    canManage = when (uiState.activeTab) {
                        DocumentTab.PERSONAL -> doc.owner?.id == currentUserId
                        DocumentTab.PROJECT -> doc.uploadedBy?.id == currentUserId
                    },
                    onClick = { onPreview(doc) },
                    onDelete = { onDelete(doc) },
                    onRename = { newName -> onRename(doc, newName) }
                )
                Divider()
            }
        }
        DocumentViewMode.GRID -> LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp)
        ) {
            gridItems(filtered) { doc ->
                DocumentGridItem(
                    doc = doc,
                    canManage = when (uiState.activeTab) {
                        DocumentTab.PERSONAL -> doc.owner?.id == currentUserId
                        DocumentTab.PROJECT -> doc.uploadedBy?.id == currentUserId
                    },
                    onClick = { onPreview(doc) },
                    onDelete = { onDelete(doc) }
                )
            }
        }
    }
}

@Composable
private fun DocumentListItem(
    doc: DocumentResponse,
    canManage: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.InsertDriveFile, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                doc.fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                "${formatFileSize(doc.fileSize)} • ${doc.uploadedBy?.fullName ?: "Unknown"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (canManage) {
            IconButton(onClick = { showRenameDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Đổi tên")
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showRenameDialog) {
        RenameDialog(
            currentName = doc.fileName,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                onRename(newName)
                showRenameDialog = false
            }
        )
    }
    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            fileName = doc.fileName,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            }
        )
    }
}

@Composable
private fun DocumentGridItem(
    doc: DocumentResponse,
    canManage: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.InsertDriveFile,
                null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                doc.fileName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
            if (canManage) {
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, "Xóa", modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            fileName = doc.fileName,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            }
        )
    }
}

@Composable
private fun EmptyState(tab: DocumentTab) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.FolderOpen, null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (tab == DocumentTab.PROJECT) "Chưa có tài liệu nào trong task này"
                else "Thư mục trống",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RenameDialog(currentName: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đổi tên tài liệu") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
private fun ConfirmDeleteDialog(fileName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xóa tài liệu?") },
        text = { Text("Xóa \"$fileName\"? Hành động này không thể hoàn tác.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Xóa", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
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

@Preview(showBackground = true, name = "Document List - Personal")
@Composable
fun DocumentListPanePersonalPreview() {
    androidx.compose.material3.MaterialTheme {
        androidx.compose.material3.Surface {
            DocumentListPane(
                uiState = DocumentBrowserUiState(
                    activeTab = DocumentTab.PERSONAL,
                    personalDocuments = listOf(
                        DocumentResponse(
                            id = "d1", fileName = "hop-dong-khach-hang.pdf",
                            filePath = null, mimeType = "application/pdf", fileSize = 245_000,
                            createdAt = "2026-06-01",
                            projectId = null, folderId = null, visibilityType = "PERSONAL",
                            updatedAt = null,
                            uploadedBy = null,
                            owner = com.phuongthanh.effiwork_android.data.model.response.UploadedByInfo(
                                id = "u1", fullName = "Nguyễn Văn A", email = null, avatarUrl = null
                            ),
                            count = null
                        ),
                        DocumentResponse(
                            id = "d2", fileName = "bao-cao-thang-5.xlsx",
                            filePath = null, mimeType = "application/vnd.ms-excel", fileSize = 89_500,
                            createdAt = "2026-05-30",
                            projectId = null, folderId = null, visibilityType = "PERSONAL",
                            updatedAt = null,
                            uploadedBy = null,
                            owner = com.phuongthanh.effiwork_android.data.model.response.UploadedByInfo(
                                id = "u1", fullName = "Nguyễn Văn A", email = null, avatarUrl = null
                            ),
                            count = null
                        )
                    )
                ),
                currentUserId = "u1",
                onPreview = {},
                onDelete = {},
                onRename = { _, _ -> }
            )
        }
    }
}

@Preview(showBackground = true, name = "Document List - Empty (Project)")
@Composable
fun DocumentListPaneEmptyPreview() {
    androidx.compose.material3.MaterialTheme {
        androidx.compose.material3.Surface {
            DocumentListPane(
                uiState = DocumentBrowserUiState(activeTab = DocumentTab.PROJECT),
                currentUserId = "u1",
                onPreview = {},
                onDelete = {},
                onRename = { _, _ -> }
            )
        }
    }
}
