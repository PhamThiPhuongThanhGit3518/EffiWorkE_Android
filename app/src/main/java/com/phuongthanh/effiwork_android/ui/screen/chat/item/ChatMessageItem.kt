package com.phuongthanh.effiwork_android.ui.screen.chat.item

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phuongthanh.effiwork_android.data.model.chat.ChatMessageType
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatMessageResponse
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatUserResponse
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import com.phuongthanh.effiwork_android.viewmodel.chat.ChatImagePreviewViewModel

@Composable
fun ChatMessageItem(
    message: ChatMessageResponse,
    isOwnMessage: Boolean,
    projectId: String? = null,
    onDocumentClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val alignment = if (isOwnMessage) Alignment.End else Alignment.Start
    val backgroundColor = if (isOwnMessage) Blue500 else Color.White
    val textColor = if (isOwnMessage) Color.White else Color.Black
    val hasDocument = message.documentId != null &&
        (message.type == ChatMessageType.IMAGE || message.type == ChatMessageType.FILE)

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
                shadowElevation = 1.dp,
                modifier = if (hasDocument && projectId != null) {
                    Modifier.clickable { message.documentId?.let(onDocumentClick) }
                } else Modifier
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
                                text = message.content?.normalizeMessage() ?: "",
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        ChatMessageType.IMAGE -> {
                            val docId = message.documentId
                            if (docId != null && projectId != null) {
                                ChatImageContent(
                                    projectId = projectId,
                                    documentId = docId,
                                    fileName = message.document?.fileName ?: "Image",
                                    isOwnMessage = isOwnMessage
                                )
                            } else {
                                Text(
                                    text = "[Image]",
                                    color = textColor,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            val caption = message.content?.normalizeMessage()
                            if (!caption.isNullOrEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = caption,
                                    color = textColor,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        ChatMessageType.FILE -> {
                            ChatFileContent(
                                fileName = message.document?.fileName ?: "Unknown file",
                                fileSize = message.document?.fileSize,
                                mimeType = message.document?.mimeType,
                                isOwnMessage = isOwnMessage,
                                textColor = textColor
                            )
                            val caption = message.content?.normalizeMessage()
                            if (!caption.isNullOrEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = caption,
                                    color = textColor,
                                    style = MaterialTheme.typography.bodySmall
                                )
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

@Composable
private fun ChatImageContent(
    projectId: String,
    documentId: String,
    fileName: String,
    isOwnMessage: Boolean
) {
    val viewModel: ChatImagePreviewViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bytes = uiState.bytesByDocumentId[documentId]

    LaunchedEffect(documentId) {
        viewModel.loadPreview(projectId, documentId)
    }

    if (bytes != null) {
        val bitmap = remember(bytes) {
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = fileName,
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            ImagePlaceholder(fileName, isOwnMessage)
        }
    } else {
        ImagePlaceholder(fileName, isOwnMessage, isLoading = true)
    }
}

@Composable
private fun ImagePlaceholder(fileName: String, isOwnMessage: Boolean, isLoading: Boolean = false) {
    val textColor = if (isOwnMessage) Color.White else Color.Black
    Box(
        modifier = Modifier
            .size(180.dp, 120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isOwnMessage) Color.White.copy(alpha = 0.15f) else Color(0xFFEEEEEE)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = textColor,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.height(8.dp))
            } else {
                Icon(
                    Icons.Default.Image,
                    null,
                    tint = textColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = fileName,
                color = textColor,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ChatFileContent(
    fileName: String,
    fileSize: Long?,
    mimeType: String?,
    isOwnMessage: Boolean,
    textColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(4.dp)
            .widthIn(max = 240.dp)
    ) {
        Icon(
            Icons.Default.Description,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = fileName,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
            val sub = when {
                mimeType != null && fileSize != null -> "${mimeType} • ${formatFileSize(fileSize)}"
                mimeType != null -> mimeType
                fileSize != null -> formatFileSize(fileSize)
                else -> null
            }
            if (sub != null) {
                Text(
                    text = sub,
                    color = if (isOwnMessage) textColor.copy(alpha = 0.7f) else Color.Gray,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
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
