package com.phuongthanh.effiwork_android.data.model.request.document

data class CreateFolderRequest(
    val name: String,
    val parentFolderId: String? = null,
    val folderType: String
)
