package com.phuongthanh.effiwork_android.data.repository

import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.model.request.document.CreateFolderRequest
import com.phuongthanh.effiwork_android.data.model.request.document.UpdateFolderRequest
import com.phuongthanh.effiwork_android.data.model.response.document.FolderNode

interface FolderRepository {
    suspend fun getFolderTree(projectId: String): ApiResult<List<FolderNode>>
    suspend fun createFolder(projectId: String, request: CreateFolderRequest): ApiResult<FolderNode>
    suspend fun updateFolder(projectId: String, folderId: String, request: UpdateFolderRequest): ApiResult<FolderNode>
    suspend fun deleteFolder(projectId: String, folderId: String): ApiResult<Unit>
}
