package com.phuongthanh.effiwork_android.ui.screen.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.phuongthanh.effiwork_android.data.model.chat.ChatMessageType
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatMessageResponse
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatUserResponse
import com.phuongthanh.effiwork_android.ui.theme.Blue500

@Composable
fun ChatMessageItem(
    message: ChatMessageResponse,
    isOwnMessage: Boolean,
    modifier: Modifier = Modifier
) {
    val alignment = if (isOwnMessage) Alignment.End else Alignment.Start
    val backgroundColor = if (isOwnMessage) Blue500 else Color.White
    val textColor = if (isOwnMessage) Color.White else Color.Black

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
        ) {
            if (!isOwnMessage) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Blue500.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = message.sender?.fullName?.firstOrNull()?.uppercase() ?: "?",
                            color = Blue500,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                    bottomEnd = if (isOwnMessage) 4.dp else 16.dp
                ),
                color = backgroundColor,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    if (!isOwnMessage && message.sender != null) {
                        Text(
                            text = message.sender.fullName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Blue500,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    when (message.type) {
                        ChatMessageType.TEXT, ChatMessageType.SYSTEM -> {
                            Text(
                                text = message.content ?: "",
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        ChatMessageType.IMAGE -> {
                            Text(
                                text = "[Image]",
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        ChatMessageType.FILE -> {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        tint = textColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = message.document?.fileName ?: "Unknown file",
                                            color = textColor,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        message.document?.fileSize?.let { size ->
                                            Text(
                                                text = formatFileSize(size),
                                                color = if (isOwnMessage) textColor.copy(alpha = 0.7f) else Color.Gray,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatMessageTime(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOwnMessage) textColor.copy(alpha = 0.7f) else Color.Gray
                    )
                }
            }
        }
    }
}

private fun formatMessageTime(timestamp: String?): String {
    if (timestamp == null) return ""
    return try {
        val parts = timestamp.split("T")
        if (parts.size == 2) {
            val dateParts = parts[0].split("-")
            val timePart = parts[1].take(5)
            if (dateParts.size == 3) {
                "${timePart} ${dateParts[2]}/${dateParts[1]}/${dateParts[0]}"
            } else {
                "${timePart} ${parts[0]}"
            }
        } else timestamp
    } catch (e: Exception) {
        timestamp
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

@Preview
@Composable
private fun ChatMessageItemPreview() {
    MaterialTheme {
        val mockMessage = ChatMessageResponse(
            id = "1",
            conversationId = "conv1",
            senderId = "1",
            type = ChatMessageType.TEXT,
            content = "Xin chào! Đây là tin nhắn mẫu.",
            documentId = null,
            createdAt = "2024-01-15T10:30:00",
            updatedAt = null,
            deletedAt = null,
            sender = ChatUserResponse(
                id = "1",
                fullName = "Nguyễn Văn A",
                email = "a@example.com",
                avatarUrl = null
            ),
            document = null
        )

        Column {
            ChatMessageItem(
                message = mockMessage,
                isOwnMessage = false
            )
            ChatMessageItem(
                message = mockMessage,
                isOwnMessage = true
            )
        }
    }
}