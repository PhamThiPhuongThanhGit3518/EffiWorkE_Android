package com.phuongthanh.effiwork_android.data.model.request

import com.google.gson.annotations.SerializedName

data class CreateTaskRequest(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("sectionId")
    val sectionId: String?,
    @SerializedName("parentTaskId")
    val parentTaskId: String?,
    @SerializedName("ownerId")
    val ownerId: String,
    @SerializedName("startDate")
    val startDate: String?,
    @SerializedName("dueDate")
    val dueDate: String?,
    @SerializedName("reminderAt")
    val reminderAt: String?,
    @SerializedName("participantIds")
    val participantIds: List<String>
)

data class UpdateTaskStatusRequest(
    @SerializedName("status")
    val status: String
)

data class UpdateTaskRequest(
    @SerializedName("title")
    val title: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("sectionId")
    val sectionId: String?,
    @SerializedName("ownerId")
    val ownerId: String?,
    @SerializedName("startDate")
    val startDate: String?,
    @SerializedName("dueDate")
    val dueDate: String?,
    @SerializedName("reminderAt")
    val reminderAt: String?,
    @SerializedName("participantIds")
    val participantIds: List<String>?
)

data class CreateMeetingRequest(
    @SerializedName("title")
    val title: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("meetingTime")
    val meetingTime: String,
    @SerializedName("hostUserId")
    val hostUserId: String,
    @SerializedName("content")
    val content: String?,
    @SerializedName("note")
    val note: String?,
    @SerializedName("participantIds")
    val participantIds: List<String>,
    @SerializedName("attachmentDocumentIds")
    val attachmentDocumentIds: List<String>?
)

data class UpdateMeetingRequest(
    @SerializedName("title")
    val title: String?,
    @SerializedName("type")
    val type: String?,
    @SerializedName("meetingTime")
    val meetingTime: String?,
    @SerializedName("hostUserId")
    val hostUserId: String?,
    @SerializedName("content")
    val content: String?,
    @SerializedName("note")
    val note: String?,
    @SerializedName("participantIds")
    val participantIds: List<String>?,
    @SerializedName("attachmentDocumentIds")
    val attachmentDocumentIds: List<String>?
)

data class CreateExtensionRequest(
    @SerializedName("newDueDate")
    val newDueDate: String,
    @SerializedName("reason")
    val reason: String
)

data class ReviewExtensionRequest(
    @SerializedName("note")
    val note: String?
)