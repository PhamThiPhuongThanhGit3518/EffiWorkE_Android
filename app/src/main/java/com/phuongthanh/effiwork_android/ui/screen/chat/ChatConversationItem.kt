package com.phuongthanh.effiwork_android.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phuongthanh.effiwork_android.data.model.chat.ChatConversationType
import com.phuongthanh.effiwork_android.data.model.chat.ChatMessageType
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatConversationMemberResponse
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatConversationResponse
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatMessageResponse
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatUserResponse
import com.phuongthanh.effiwork_android.ui.theme.Blue500
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ChatConversationItem(
    conversation: ChatConversationResponse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = conversation.name ?: conversation.members
        ?.firstOrNull()?.user?.fullName
        ?: "Unknown"
    val lastMessage = conversation.messages?.firstOrNull()
    val unreadCount = conversation.unreadCount

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Blue500.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (conversation.type == ChatConversationType.GROUP)
                        Icons.Default.Group else Icons.Default.Person,
                    contentDescription = null,
                    tint = Blue500,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = Blue500) {
                            Text(if (unreadCount > 99) "99+" else unreadCount.toString())
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastMessage?.content ?: "No messages yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (conversation.type == ChatConversationType.GROUP) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.Group,
                    contentDescription = "Group",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun ChatConversationItemPreview() {
    MaterialTheme {
        val mockConversation = ChatConversationResponse(
            id = "1",
            projectId = "p1",
            type = ChatConversationType.PRIVATE,
            name = null,
            createdById = "1",
            lastMessageId = null,
            lastMessageAt = null,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = null,
            unreadCount = 3,
            createdBy = null,
            members = listOf(
                ChatConversationMemberResponse(
                    id = "m1",
                    conversationId = "1",
                    userId = "1",
                    joinedAt = "2024-01-01T00:00:00Z",
                    user = ChatUserResponse(
                        id = "1",
                        fullName = "Nguyễn Văn A",
                        email = "a@example.com",
                        avatarUrl = null
                    )
                )
            ),
            messages = listOf(
                ChatMessageResponse(
                    id = "1",
                    conversationId = "1",
                    senderId = "1",
                    type = ChatMessageType.TEXT,
                    content = "Xin chào bạn!",
                    documentId = null,
                    createdAt = "2024-01-15T10:30:00Z",
                    updatedAt = null,
                    deletedAt = null,
                    sender = null,
                    document = null
                )
            ),
            _count = null
        )
        ChatConversationItem(
            conversation = mockConversation,
            onClick = {}
        )
    }
}