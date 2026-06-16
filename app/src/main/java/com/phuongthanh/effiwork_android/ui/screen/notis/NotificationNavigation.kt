package com.phuongthanh.effiwork_android.ui.screen.notis

import com.phuongthanh.effiwork_android.data.model.response.NotificationResponse

private val QUOTED_NAME_REGEX = Regex("\"([^\"]+)\"")

fun extractConversationName(notification: NotificationResponse): String? {
    val source = notification.content ?: notification.message ?: return null
    val match = QUOTED_NAME_REGEX.find(source) ?: return null
    val name = match.groupValues.getOrNull(1)?.trim().orEmpty()
    return name.takeIf { it.isNotEmpty() }
}

fun resolveNotificationNavigation(
    notification: NotificationResponse,
    onNavigateToTaskDetail: (projectId: String, taskId: String) -> Unit,
    onNavigateToMeetingDetail: (projectId: String, meetingId: String) -> Unit,
    onNavigateToChat: (projectId: String, conversationId: String, conversationName: String) -> Unit,
    onNavigateToProjectSetting: (projectId: String) -> Unit,
    onNavigateToDocument: (projectId: String) -> Unit,
    onNavigateToProject: (projectId: String) -> Unit
) {
    val projectId = notification.projectId ?: notification.data?.projectId
    val relatedType = notification.relatedType?.uppercase()
    val relatedId = notification.relatedId
    val data = notification.data

    when {
        projectId != null && relatedType == "TASK" && !relatedId.isNullOrBlank() -> {
            onNavigateToTaskDetail(projectId, relatedId)
        }
        projectId != null && relatedType == "MEETING" && !relatedId.isNullOrBlank() -> {
            onNavigateToMeetingDetail(projectId, relatedId)
        }
        projectId != null && relatedType == "CHAT_CONVERSATION" && !relatedId.isNullOrBlank() -> {
            val name = extractConversationName(notification) ?: "Hội thoại"
            onNavigateToChat(projectId, relatedId, name)
        }
        projectId != null && (relatedType == "PROJECT_JOIN_REQUEST" || relatedType == "PROJECT_MEMBER") -> {
            onNavigateToProjectSetting(projectId)
        }
        projectId != null && relatedType == "DOCUMENT" -> {
            onNavigateToDocument(projectId)
        }
        data?.taskId != null && data.projectId != null -> {
            onNavigateToTaskDetail(data.projectId, data.taskId)
        }
        data?.meetingId != null && data.projectId != null -> {
            onNavigateToMeetingDetail(data.projectId, data.meetingId)
        }
        projectId != null -> {
            onNavigateToProject(projectId)
        }
    }
}
