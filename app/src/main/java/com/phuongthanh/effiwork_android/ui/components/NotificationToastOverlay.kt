package com.phuongthanh.effiwork_android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phuongthanh.effiwork_android.data.model.response.NotificationResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

private const val AUTO_DISMISS_MS = 9000L

@Composable
fun NotificationToastOverlay(
    newNotifications: SharedFlow<NotificationResponse>,
    projectNameFor: (String) -> String?,
    modifier: Modifier = Modifier
) {
    android.util.Log.d("NotifToastOverlay", "composing NotificationToastOverlay")
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf<NotificationResponse?>(null) }
    var visible by remember { mutableStateOf(false) }
    var dismissJob by remember { mutableStateOf<Job?>(null) }

    fun show(notification: NotificationResponse) {
        current = notification
        visible = true
        dismissJob?.cancel()
        dismissJob = scope.launch {
            delay(AUTO_DISMISS_MS)
            if (current == notification) {
                visible = false
            }
        }
    }

    fun dismiss() {
        dismissJob?.cancel()
        dismissJob = null
        visible = false
        current = null
    }

    LaunchedEffect(Unit) {
        android.util.Log.d("NotifToastOverlay", "LaunchedEffect started, collecting newNotifications")
        newNotifications.collect { notification ->
            android.util.Log.d("NotifToastOverlay", "received notification id=${notification.id}, projectId=${notification.projectId}, content=${notification.content?.take(40)}")
            show(notification)
            android.util.Log.d("NotifToastOverlay", "after show(): visible=$visible, current!=null=${current != null}")
        }
    }

    AnimatedVisibility(
        visible = visible && current != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        val notification = current ?: return@AnimatedVisibility
        NotificationToastCard(
            projectName = notification.projectId?.let(projectNameFor) ?: "Thông báo",
            content = notification.content ?: notification.message ?: notification.title ?: "",
            onClose = ::dismiss
        )
    }
}

@Composable
private fun NotificationToastCard(
    projectName: String,
    content: String,
    onClose: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .width(320.dp)
            .padding(0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = projectName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = content,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Đóng",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
