package com.phuongthanh.effiwork_android.viewmodel.task

data class TaskAttachmentItem(
    val id: String,
    val documentId: String,
    val fileName: String,
    val fileSize: String?,
    val mimeType: String?,
    val filePath: String?
)
