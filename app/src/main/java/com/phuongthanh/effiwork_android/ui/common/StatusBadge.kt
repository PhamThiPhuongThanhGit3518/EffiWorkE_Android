package com.phuongthanh.effiwork_android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuongthanh.effiwork_android.viewmodel.task.TaskStatus

@Composable
fun StatusBadge(
    currentStatus: TaskStatus,
    onStatusChange: (TaskStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier.clickable { expanded = true },
            shape = RoundedCornerShape(4.dp),
            color = when (currentStatus) {
                TaskStatus.NOT_STARTED -> Color.Gray.copy(alpha = 0.1f)
                TaskStatus.IN_PROGRESS -> Color(0xFF2196F3).copy(alpha = 0.1f)
                TaskStatus.REVIEW -> Color(0xFFFF9800).copy(alpha = 0.1f)
                TaskStatus.COMPLETED -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                TaskStatus.CANCELLED -> Color(0xFFF44336).copy(alpha = 0.1f)
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentStatus.displayName,
                    fontSize = 12.sp,
                    color = when (currentStatus) {
                        TaskStatus.NOT_STARTED -> Color.Gray
                        TaskStatus.IN_PROGRESS -> Color(0xFF2196F3)
                        TaskStatus.REVIEW -> Color(0xFFFF9800)
                        TaskStatus.COMPLETED -> Color(0xFF4CAF50)
                        TaskStatus.CANCELLED -> Color(0xFFF44336)
                    }
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = when (currentStatus) {
                        TaskStatus.NOT_STARTED -> Color.Gray
                        TaskStatus.IN_PROGRESS -> Color(0xFF2196F3)
                        TaskStatus.REVIEW -> Color(0xFFFF9800)
                        TaskStatus.COMPLETED -> Color(0xFF4CAF50)
                        TaskStatus.CANCELLED -> Color(0xFFF44336)
                    }
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TaskStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (status) {
                                            TaskStatus.NOT_STARTED -> Color.Gray
                                            TaskStatus.IN_PROGRESS -> Color(0xFF2196F3)
                                            TaskStatus.REVIEW -> Color(0xFFFF9800)
                                            TaskStatus.COMPLETED -> Color(0xFF4CAF50)
                                            TaskStatus.CANCELLED -> Color(0xFFF44336)
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(status.displayName)
                        }
                    },
                    onClick = {
                        onStatusChange(status)
                        expanded = false
                    }
                )
            }
        }
    }
}