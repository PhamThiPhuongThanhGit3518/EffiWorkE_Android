package com.phuongthanh.effiwork_android.data.model.response.document

import com.phuongthanh.effiwork_android.data.model.response.DocumentResponse

data class TaskAttachmentResponse(
    val id: String,
    val taskId: String,
    val documentId: String,
    val document: DocumentResponse,
    val createdAt: String? = null
)
