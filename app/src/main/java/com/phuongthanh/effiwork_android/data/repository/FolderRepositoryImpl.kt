package com.phuongthanh.effiwork_android.data.repository

import android.util.Log
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.api.FolderService
import com.phuongthanh.effiwork_android.data.model.request.document.CreateFolderRequest
import com.phuongthanh.effiwork_android.data.model.request.document.UpdateFolderRequest
import com.phuongthanh.effiwork_android.data.model.response.document.FolderNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FolderRepository"

@Singleton
class FolderRepositoryImpl @Inject constructor(
    private val folderService: FolderService
) : FolderRepository {

    override suspend fun getFolderTree(projectId: String): ApiResult<List<FolderNode>> =
        withContext(Dispatchers.IO) {
            try {
                val response = folderService.getFolderTree(projectId)
                if (response.success && response.data != null) {
                    ApiResult.Success(response.data)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "getFolderTree EXCEPTION: ${e.message}", e)
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }

    override suspend fun createFolder(
        projectId: String,
        request: CreateFolderRequest
    ): ApiResult<FolderNode> = withContext(Dispatchers.IO) {
        try {
            val response = folderService.createFolder(projectId, request)
            if (response.success && response.data != null) {
                ApiResult.Success(response.data)
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun updateFolder(
        projectId: String,
        folderId: String,
        request: UpdateFolderRequest
    ): ApiResult<FolderNode> = withContext(Dispatchers.IO) {
        try {
            val response = folderService.updateFolder(projectId, folderId, request)
            if (response.success && response.data != null) {
                ApiResult.Success(response.data)
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun deleteFolder(
        projectId: String,
        folderId: String
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = folderService.deleteFolder(projectId, folderId)
            if (response.success) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }
}
