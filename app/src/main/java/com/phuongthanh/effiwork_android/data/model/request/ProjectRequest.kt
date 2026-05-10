package com.phuongthanh.effiwork_android.data.model.request

data class CreateProjectRequest(
    val name: String,
    val description: String
)

data class UpdateProjectRequest(
    val name: String,
    val description: String
)

data class TransferAdminRequest(
    val targetUserId: String,
    val note: String?
)

data class JoinByCodeRequest(
    val projectCode: String,
    val note: String?
)

data class ApproveRejectRequest(
    val note: String?
)
