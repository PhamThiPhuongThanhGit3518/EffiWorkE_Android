package com.phuongthanh.effiwork_android.data.model.response

import com.google.gson.annotations.SerializedName

data class TaskResponse(
    val id: String,
    val projectId: String,
    @SerializedName("title") val name: String?,
    val description: String?,
    @SerializedName("sectionId") val groupId: String?,
    @SerializedName("section") val group: SectionInfo?,
    val groupName: String?,
    @SerializedName("parentTaskId") val parentTaskId: String?,
    val status: String?,
    val assigneeId: String?,
    val assigneeName: String?,
    @SerializedName("owner") val owner: MemberInfo?,
    @SerializedName("creator") val creator: MemberInfo?,
    val startDate: String?,
    @SerializedName("dueDate") val endDate: String?,
    val reminderTime: String?,
    val participants: List<TaskParticipant>?,
    val subtasks: List<SubtaskResponse>?,
    val createdAt: String?,
    val updatedAt: String?
)

data class TaskParticipant(
    val userId: String,
    val user: TaskParticipantUser?
)

data class TaskParticipantUser(
    val id: String,
    val fullName: String?,
    val email: String?,
    val avatarUrl: String?
)

data class SubtaskResponse(
    val id: String,
    val name: String,
    val isCompleted: Boolean,
    val dueDate: String
)

data class MeetingResponse(
    val id: String,
    val projectId: String,
    val title: String?,
    val content: String?,
    // Server field: hostUserId - Android uses organizerId
    @SerializedName("hostUserId") val organizerId: String?,
    // Server field: hostUser.fullName - Android uses organizerName
    @SerializedName("hostUser") val hostUser: HostUserInfo?,
    // Server field: type - Android uses format
    val type: String?,
    // Server field: meetingTime - Android uses scheduledTime
    @SerializedName("meetingTime") val scheduledTime: String?,
    val status: String?,
    // Server field: note - Android uses notes
    @SerializedName("note") val notes: String?,
    val participants: List<MeetingParticipant>?,
    val attachments: List<Attachment>?,
    val createdAt: String?,
    val updatedAt: String?
)

data class HostUserInfo(
    val id: String,
    val fullName: String?,
    val email: String?,
    val avatarUrl: String?
)

data class MeetingParticipant(
    val userId: String,
    // Server field: user.fullName - Android uses userName
    val user: MeetingParticipantUserInfo?
)

data class MeetingParticipantUserInfo(
    val id: String,
    val fullName: String?,
    val email: String?,
    val avatarUrl: String?
)

data class Attachment(
    val id: String,
    val documentId: String?,
    val createdAt: String?,
    val document: AttachmentDocument?
)

data class AttachmentDocument(
    val id: String,
    val fileName: String?,
    val filePath: String?,
    val mimeType: String?,
    val fileSize: String?
)

data class DocumentResponse(
    val id: String,
    val fileName: String,
    val filePath: String?,
    val mimeType: String?,
    val fileSize: Long,
    val createdAt: String?,
    val projectId: String? = null,
    val folderId: String? = null,
    val visibilityType: String? = null,
    val updatedAt: String? = null,
    val uploadedBy: UploadedByInfo? = null,
    val owner: UploadedByInfo? = null,
    @SerializedName("_count") val count: DocumentCount? = null
)

data class UploadedByInfo(
    val id: String,
    val fullName: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null
)

data class DocumentCount(
    val taskAttachments: Int = 0,
    val meetingAttachments: Int = 0
)

data class SectionInfo(
    val id: String,
    val name: String?,
    val sortOrder: Int?
)

data class SectionResponse(
    val id: String,
    val name: String?,
    val projectId: String?,
    val sortOrder: Int?,
    val createdAt: String?
)

data class MemberInfo(
    val id: String,
    val fullName: String?,
    val email: String?,
    val avatarUrl: String?
)

data class MemberResponse(
    val userId: String,
    val role: String?,
    val user: MemberInfo?
)

data class CommentUser(
    val id: String,
    val fullName: String?,
    val email: String?,
    val avatarUrl: String?
)

data class CommentResponse(
    val id: String,
    val taskId: String,
    val userId: String,
    val content: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val user: CommentUser?
)

data class TaskDocument(
    val id: String,
    val fileName: String?,
    val filePath: String?,
    val mimeType: String?,
    val fileSize: String?
)

data class TaskAttachment(
    val id: String,
    val document: TaskDocument?
)

data class TaskDetailResponse(
    val id: String,
    val projectId: String,
    val title: String?,
    val description: String?,
    @SerializedName("sectionId") val groupId: String?,
    @SerializedName("section") val group: SectionInfo?,
    @SerializedName("parentTaskId") val parentTaskId: String?,
    val status: String?,
    val assigneeId: String?,
    @SerializedName("owner") val assignee: MemberInfo?,
    @SerializedName("creator") val creator: MemberInfo?,
    val startDate: String?,
    @SerializedName("dueDate") val endDate: String?,
    val reminderAt: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val participants: List<TaskParticipantDetail>?,
    val attachments: List<TaskAttachment>?,
    val comments: List<CommentResponse>?,
    @SerializedName("_count") val count: TaskCount?
)

data class TaskParticipantDetail(
    val user: MemberInfo?
)

data class TaskCount(
    val childTasks: Int?,
    val comments: Int?,
    val attachments: Int?,
    val extensionRequests: Int?
)

data class ExtensionRequestResponse(
    val id: String,
    val taskId: String?,
    val requestedById: String?,
    val requestedBy: MemberInfo?,
    val oldDueDate: String?,
    val newDueDate: String?,
    val reason: String?,
    val status: String?,
    val approvedById: String?,
    val approvedBy: MemberInfo?,
    val approvedAt: String?,
    val createdAt: String?
)