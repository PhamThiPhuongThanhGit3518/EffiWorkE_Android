package com.phuongthanh.effiwork_android.ui.screen.document.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import com.phuongthanh.effiwork_android.data.model.response.document.FolderNode

@Composable
fun FolderTree(
    folders: List<FolderNode>,
    selectedFolderId: String?,
    expandedIds: Set<String>,
    onSelect: (String?) -> Unit,
    onToggle: (String) -> Unit,
    onDelete: (FolderNode) -> Unit,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
    ) {
        FolderItem(
            folder = null,
            level = 0,
            isSelected = selectedFolderId == null,
            isExpanded = false,
            hasChildren = folders.isNotEmpty(),
            onSelect = { onSelect(null) },
            onToggle = {},
            onDelete = {},
            canDelete = false
        )
        folders.forEach { folder ->
            FolderItemRecursive(
                folder = folder,
                level = 1,
                selectedFolderId = selectedFolderId,
                expandedIds = expandedIds,
                onSelect = onSelect,
                onToggle = onToggle,
                onDelete = onDelete,
                currentUserId = currentUserId
            )
        }
    }
}

@Composable
private fun FolderItemRecursive(
    folder: FolderNode,
    level: Int,
    selectedFolderId: String?,
    expandedIds: Set<String>,
    onSelect: (String?) -> Unit,
    onToggle: (String) -> Unit,
    onDelete: (FolderNode) -> Unit,
    currentUserId: String
) {
    val hasChildren = folder.children.isNotEmpty()
    val expanded = folder.id in expandedIds
    val canDelete = folder.isManageable(currentUserId)

    FolderItem(
        folder = folder,
        level = level,
        isSelected = selectedFolderId == folder.id,
        isExpanded = expanded,
        hasChildren = hasChildren,
        onSelect = { onSelect(folder.id) },
        onToggle = { onToggle(folder.id) },
        onDelete = { onDelete(folder) },
        canDelete = canDelete
    )

    if (expanded && hasChildren) {
        folder.children.forEach { child ->
            FolderItemRecursive(
                folder = child,
                level = level + 1,
                selectedFolderId = selectedFolderId,
                expandedIds = expandedIds,
                onSelect = onSelect,
                onToggle = onToggle,
                onDelete = onDelete,
                currentUserId = currentUserId
            )
        }
    }
}

@Composable
private fun FolderItem(
    folder: FolderNode?,
    level: Int,
    isSelected: Boolean,
    isExpanded: Boolean,
    hasChildren: Boolean,
    onSelect: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (8 + level * 16).dp, top = 2.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onSelect() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasChildren) {
            IconButton(onClick = onToggle, modifier = Modifier.size(20.dp)) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else {
            Spacer(Modifier.width(20.dp))
        }
        Icon(
            if (isExpanded || folder == null) Icons.Default.FolderOpen else Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = folder?.name ?: "Tài liệu cá nhân",
            modifier = Modifier.weight(1f),
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (folder != null && canDelete) {
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Xóa",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Folder Tree")
@Composable
fun FolderTreePreview() {
    MaterialTheme {
        Surface {
            FolderTree(
                folders = listOf(
                    FolderNode(
                        id = "f1", projectId = "p1", parentFolderId = null,
                        name = "Hợp đồng", type = "PERSONAL", folderType = "PERSONAL",
                        ownerId = "u1", createdAt = null, updatedAt = null,
                        children = listOf(
                            FolderNode(
                                id = "f1-1", projectId = "p1", parentFolderId = "f1",
                                name = "Khách hàng A", type = "PERSONAL", folderType = "PERSONAL",
                                ownerId = "u1", createdAt = null, updatedAt = null, children = emptyList()
                            )
                        )
                    ),
                    FolderNode(
                        id = "f2", projectId = "p1", parentFolderId = null,
                        name = "Báo cáo", type = "PERSONAL", folderType = "PERSONAL",
                        ownerId = "u1", createdAt = null, updatedAt = null, children = emptyList()
                    )
                ),
                selectedFolderId = "f1-1",
                expandedIds = setOf("f1"),
                onSelect = {},
                onToggle = {},
                onDelete = {},
                currentUserId = "u1"
            )
        }
    }
}
