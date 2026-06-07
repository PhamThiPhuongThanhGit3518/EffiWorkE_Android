package com.phuongthanh.effiwork_android.data.model.request.document

data class UpdateDocumentRequest(
    val fileName: String? = null,
    val folderId: String? = null,
    val visibilityType: String? = null
)
