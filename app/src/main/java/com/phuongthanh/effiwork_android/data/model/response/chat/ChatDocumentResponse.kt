package com.phuongthanh.effiwork_android.data.model.response.chat

data class ChatDocumentResponse(
    val id: String,
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val fileSize: Long
)