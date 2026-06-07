package com.phuongthanh.effiwork_android.ui.common

import androidx.collection.intIntMapOf
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phuongthanh.effiwork_android.viewmodel.task.Task
import com.phuongthanh.effiwork_android.viewmodel.task.TaskStatus

@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onMoreClick: (() -> Unit)? = null,
    onStatusChange: (TaskStatus) -> Unit,
    canChangeStatus: Boolean = false,
    dropdownMenuContent: (@Composable () -> Unit)? = null
) {
    android.util.Log.d("TaskCardDebug", "TaskCard: ${task.name}, pendingExtensionRequestStatus: ${task.pendingExtensionRequestStatus}, pendingExtensionRequestNewDueDate: ${task.pendingExtensionRequestNewDueDate}")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(task.status, onStatusChange, canChangeStatus)
                if (onMoreClick != null) {
                    Box {
                        IconButton(onClick = onMoreClick) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        dropdownMenuContent?.invoke()
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = task.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = task.assignee,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${task.startDate} - ${task.endDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = task.participants.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (task.pendingExtensionRequestStatus == "PENDING") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF3E0))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Xin gia hạn: ${task.pendingExtensionRequestNewDueDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Medium
                    )
                    if (task.pendingExtensionRequestReason != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${task.pendingExtensionRequestReason})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "TaskCard - Default")
@Composable
private fun TaskCardDefaultPreview() {
    MaterialTheme {
        TaskCard(
            task = Task(
                id = "1",
                name = "Thiết kế giao diện màn hình chính",
                description = "Thiết kế UI/UX cho màn hình chính của ứng dụng. Bao gồm các màn hình: Home, Profile, Settings.",
                status = TaskStatus.IN_PROGRESS,
                assignee = "Phạm Thị Phương Thanh",
                participants = listOf("Trần Văn Hoàng", "Lê Thị Mai"),
                startDate = "2026-05-20",
                endDate = "2026-05-25",
                category = "UI/UX"
            ),
            onClick = {},
            onStatusChange = {}
        )
    }
}

@Preview(showBackground = true, name = "TaskCard - Completed")
@Composable
private fun TaskCardCompletedPreview() {
    MaterialTheme {
        TaskCard(
            task = Task(
                id = "2",
                name = "Review thiết kế",
                description = "Review và feedback thiết kế",
                status = TaskStatus.COMPLETED,
                assignee = "Nguyễn Văn Minh",
                participants = emptyList(),
                startDate = "2026-05-21",
                endDate = "2026-05-24",
                category = "UI/UX"
            ),
            onClick = {},
            onStatusChange = {}
        )
    }
}