package com.phuongthanh.effiwork_android.ui.screen.document.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import com.phuongthanh.effiwork_android.data.model.response.SectionResponse
import com.phuongthanh.effiwork_android.data.model.response.SubtaskResponse
import com.phuongthanh.effiwork_android.data.model.response.TaskResponse

@Composable
fun ProjectSectionTaskTree(
    sections: List<SectionResponse>,
    allTasks: List<TaskResponse>,
    selectedSectionId: String?,
    selectedTaskId: String?,
    expandedTaskIds: Set<String>,
    onSelectSection: (String?) -> Unit,
    onSelectTask: (TaskResponse) -> Unit,
    onToggleTask: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
    ) {
        SidebarItem(
            icon = Icons.Default.FolderOpen,
            label = "Tài liệu dự án",
            isSelected = selectedSectionId == null && selectedTaskId == null,
            onClick = { onSelectSection(null) }
        )

        sections.forEach { section ->
            val sectionTasks = allTasks.filter { it.groupId == section.id && it.parentTaskId == null }
            SidebarItem(
                icon = Icons.Default.Folder,
                label = section.name ?: "Section",
                isSelected = selectedSectionId == section.id && selectedTaskId == null,
                onClick = { onSelectSection(section.id) }
            )
            sectionTasks.forEach { task ->
                TaskNodeRecursive(
                    task = task,
                    level = 1,
                    selectedTaskId = selectedTaskId,
                    expandedTaskIds = expandedTaskIds,
                    onSelectTask = onSelectTask,
                    onToggleTask = onToggleTask
                )
            }
        }
    }
}

@Composable
private fun TaskNodeRecursive(
    task: TaskResponse,
    level: Int,
    selectedTaskId: String?,
    expandedTaskIds: Set<String>,
    onSelectTask: (TaskResponse) -> Unit,
    onToggleTask: (String) -> Unit
) {
    val subtasks = task.subtasks ?: emptyList()
    val hasChildren = subtasks.isNotEmpty()
    val expanded = task.id in expandedTaskIds

    SidebarItem(
        icon = if (expanded) Icons.Default.FolderOpen else Icons.Default.Folder,
        label = task.name ?: "Task",
        isSelected = selectedTaskId == task.id,
        level = level,
        onClick = { onSelectTask(task) },
        trailing = if (hasChildren) {
            {
                IconButton(onClick = { onToggleTask(task.id) }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else null
    )

    if (expanded && hasChildren) {
        subtasks.forEach { sub ->
            SubtaskNode(
                subtask = sub,
                level = level + 1,
                selectedTaskId = selectedTaskId,
                onSelect = { onSelectTask(sub.toTaskResponse(task)) }
            )
        }
    }
}

@Composable
private fun SubtaskNode(
    subtask: SubtaskResponse,
    level: Int,
    selectedTaskId: String?,
    onSelect: () -> Unit
) {
    SidebarItem(
        icon = Icons.Default.Folder,
        label = subtask.name,
        isSelected = selectedTaskId == subtask.id,
        level = level,
        onClick = onSelect
    )
}

private fun SubtaskResponse.toTaskResponse(parent: TaskResponse): TaskResponse = TaskResponse(
    id = this.id,
    projectId = parent.projectId,
    name = this.name,
    description = null,
    groupId = parent.groupId,
    group = parent.group,
    groupName = parent.groupName,
    parentTaskId = parent.id,
    status = if (this.isCompleted) "DONE" else "TODO",
    assigneeId = null,
    assigneeName = null,
    owner = null,
    creator = null,
    startDate = null,
    endDate = this.dueDate,
    reminderTime = null,
    participants = null,
    subtasks = null,
    createdAt = null,
    updatedAt = null
)

@Preview(showBackground = true, name = "Project Section Task Tree")
@Composable
fun ProjectSectionTaskTreePreview() {
    MaterialTheme {
        Surface {
            ProjectSectionTaskTree(
                sections = listOf(
                    SectionResponse(id = "s1", name = "Phần 1: Backend", projectId = "p1", sortOrder = 0, createdAt = null),
                    SectionResponse(id = "s2", name = "Phần 2: Frontend", projectId = "p1", sortOrder = 1, createdAt = null)
                ),
                allTasks = listOf(
                    TaskResponse(
                        id = "t1", projectId = "p1", name = "Thiết kế API",
                        description = null, groupId = "s1", group = null, groupName = null,
                        parentTaskId = null, status = "TODO", assigneeId = null, assigneeName = null,
                        owner = null, creator = null, startDate = null, endDate = null,
                        reminderTime = null, participants = null,
                        subtasks = listOf(
                            SubtaskResponse(id = "st1", name = "Viết OpenAPI", isCompleted = true, dueDate = "2026-06-10"),
                            SubtaskResponse(id = "st2", name = "Review API", isCompleted = false, dueDate = "2026-06-12")
                        ),
                        createdAt = null, updatedAt = null
                    ),
                    TaskResponse(
                        id = "t2", projectId = "p1", name = "Setup Database",
                        description = null, groupId = "s1", group = null, groupName = null,
                        parentTaskId = null, status = "IN_PROGRESS", assigneeId = null, assigneeName = null,
                        owner = null, creator = null, startDate = null, endDate = null,
                        reminderTime = null, participants = null, subtasks = null,
                        createdAt = null, updatedAt = null
                    )
                ),
                selectedSectionId = "s1",
                selectedTaskId = "t1",
                expandedTaskIds = setOf("t1"),
                onSelectSection = {},
                onSelectTask = {},
                onToggleTask = {}
            )
        }
    }
}

@Composable
private fun SidebarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    level: Int = 0,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (8 + level * 16).dp, top = 2.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
        if (trailing != null) trailing()
    }
}
