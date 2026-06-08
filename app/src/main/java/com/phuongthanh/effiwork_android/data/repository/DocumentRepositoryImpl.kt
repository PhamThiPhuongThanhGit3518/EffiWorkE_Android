package com.phuongthanh.effiwork_android.data.repository

import android.util.Log
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.api.DocumentService
import com.phuongthanh.effiwork_android.data.model.request.document.AttachTaskDocumentRequest
import com.phuongthanh.effiwork_android.data.model.request.document.UpdateDocumentRequest
import com.phuongthanh.effiwork_android.data.model.response.DocumentResponse
import com.phuongthanh.effiwork_android.data.model.response.document.TaskAttachmentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DocumentRepository"

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val documentService: DocumentService
) : DocumentRepository {

    override suspend fun uploadDocument(
        projectId: String,
        fileName: String,
        fileBytes: ByteArray
    ): ApiResult<DocumentResponse> = uploadDocument(
        projectId = projectId,
        fileName = fileName,
        fileBytes = fileBytes,
        mimeType = "application/octet-stream",
        folderId = null,
        visibilityType = null,
        customFileName = null
    )

    override suspend fun uploadDocument(
        projectId: String,
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String?,
        folderId: String?,
        visibilityType: String?,
        customFileName: String?
    ): ApiResult<DocumentResponse> = withContext(Dispatchers.IO) {
        try {
            val mediaType = (mimeType ?: "application/octet-stream").toMediaTypeOrNull()
            val filePartBody = fileBytes.toRequestBody(mediaType)
            val filePart = MultipartBody.Part.createFormData("file", fileName, filePartBody)

            val folderIdPart = folderId?.toRequestBody("text/plain".toMediaTypeOrNull())
            val visibilityTypePart = visibilityType?.toRequestBody("text/plain".toMediaTypeOrNull())
            val customFileNamePart = customFileName?.toRequestBody("text/plain".toMediaTypeOrNull())

            Log.d(TAG, "[uploadDocument] projectId=$projectId, fileName=$fileName, bytes=${fileBytes.size}, mimeType=$mediaType")
            val response = documentService.uploadDocument(
                projectId = projectId,
                file = filePart,
                folderId = folderIdPart,
                visibilityType = visibilityTypePart,
                fileName = customFileNamePart
            )
            Log.d(TAG, "[uploadDocument] response.success=${response.success}, data=${response.data}, message=${response.message}")
            if (response.success && response.data != null) {
                val d = response.data
                Log.d(TAG, "[uploadDocument] data.id=${d.id}, fileName=${d.fileName}, fileSize=${d.fileSize}, filePath=${d.filePath}, mimeType=${d.mimeType}")
                ApiResult.Success(d)
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "uploadDocument EXCEPTION: ${e.message}", e)
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun listDocuments(
        projectId: String,
        keyword: String?,
        folderId: String?,
        visibilityType: String?,
        mineOnly: Boolean?,
        page: Int,
        limit: Int
    ): ApiResult<List<DocumentResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = documentService.listDocuments(
                projectId, keyword, folderId, visibilityType, mineOnly, page, limit
            )
            if (response.success && response.data != null) {
                ApiResult.Success(response.data)
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "listDocuments EXCEPTION: ${e.message}", e)
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun getDocumentDetail(
        projectId: String,
        documentId: String
    ): ApiResult<DocumentResponse> = withContext(Dispatchers.IO) {
        try {
            val response = documentService.getDocumentDetail(projectId, documentId)
            if (response.success && response.data != null) {
                ApiResult.Success(response.data)
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun updateDocument(
        projectId: String,
        documentId: String,
        request: UpdateDocumentRequest
    ): ApiResult<DocumentResponse> = withContext(Dispatchers.IO) {
        try {
            val response = documentService.updateDocument(projectId, documentId, request)
            if (response.success && response.data != null) {
                ApiResult.Success(response.data)
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun deleteDocument(
        projectId: String,
        documentId: String
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = documentService.deleteDocument(projectId, documentId)
            if (response.success) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun downloadDocument(
        projectId: String,
        documentId: String
    ): ApiResult<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val response = documentService.downloadDocument(projectId, documentId)
            if (response.isSuccessful) {
                val bytes = response.body()?.bytes() ?: ByteArray(0)
                ApiResult.Success(bytes)
            } else {
                ApiResult.Error("Download failed: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun downloadDocumentToFile(
        projectId: String,
        documentId: String,
        dest: File
    ): ApiResult<File> = withContext(Dispatchers.IO) {
        try {
            val response = documentService.downloadDocument(projectId, documentId)
            if (response.isSuccessful) {
                val body = response.body() ?: return@withContext ApiResult.Error("Empty body")
                body.byteStream().use { input ->
                    dest.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                ApiResult.Success(dest)
            } else {
                ApiResult.Error("Download failed: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun previewDocument(
        projectId: String,
        documentId: String
    ): ApiResult<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val response = documentService.previewDocument(projectId, documentId)
            if (response.isSuccessful) {
                val bytes = response.body()?.bytes() ?: ByteArray(0)
                ApiResult.Success(bytes)
            } else {
                ApiResult.Error("Preview failed: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun attachToTask(
        projectId: String,
        taskId: String,
        documentId: String
    ): ApiResult<TaskAttachmentResponse> = withContext(Dispatchers.IO) {
        try {
            val response = documentService.attachToTask(
                projectId, taskId, AttachTaskDocumentRequest(documentId)
            )
            if (response.success && response.data != null) {
                ApiResult.Success(response.data)
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun detachFromTask(
        projectId: String,
        taskId: String,
        attachmentId: String
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = documentService.detachFromTask(projectId, taskId, attachmentId)
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
