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
    val organizerId: String?,
    val organizerName: String?,
    val format: String?,
    val scheduledTime: String?,
    val status: String?,
    val notes: String?,
    val participants: List<MeetingParticipant>?,
    val attachments: List<Attachment>?,
    val createdAt: String?,
    val updatedAt: String?
)

data class MeetingParticipant(
    val userId: String,
    val userName: String?
)

data class Attachment(
    val id: String,
    val fileName: String,
    val fileUrl: String,
    val fileSize: Long
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