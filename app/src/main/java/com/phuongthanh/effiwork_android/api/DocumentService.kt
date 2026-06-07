package com.phuongthanh.effiwork_android.api

import com.phuongthanh.effiwork_android.data.model.request.document.AttachTaskDocumentRequest
import com.phuongthanh.effiwork_android.data.model.request.document.UpdateDocumentRequest
import com.phuongthanh.effiwork_android.data.model.response.ApiResponse
import com.phuongthanh.effiwork_android.data.model.response.DocumentResponse
import com.phuongthanh.effiwork_android.data.model.response.document.TaskAttachmentResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface DocumentService {

    @GET("v1/projects/{projectId}/documents")
    suspend fun listDocuments(
        @Path("projectId") projectId: String,
        @Query("keyword") keyword: String? = null,
        @Query("folderId") folderId: String? = null,
        @Query("visibilityType") visibilityType: String? = null,
        @Query("mineOnly") mineOnly: Boolean? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponse<List<DocumentResponse>>

    @GET("v1/projects/{projectId}/documents/{documentId}")
    suspend fun getDocumentDetail(
        @Path("projectId") projectId: String,
        @Path("documentId") documentId: String
    ): ApiResponse<DocumentResponse>

    @Multipart
    @POST("v1/projects/{projectId}/documents/upload")
    suspend fun uploadDocument(
        @Path("projectId") projectId: String,
        @Part file: MultipartBody.Part,
        @Part("folderId") folderId: RequestBody? = null,
        @Part("visibilityType") visibilityType: RequestBody? = null,
        @Part("fileName") fileName: RequestBody? = null
    ): ApiResponse<DocumentResponse>

    @PATCH("v1/projects/{projectId}/documents/{documentId}")
    suspend fun updateDocument(
        @Path("projectId") projectId: String,
        @Path("documentId") documentId: String,
        @Body request: UpdateDocumentRequest
    ): ApiResponse<DocumentResponse>

    @DELETE("v1/projects/{projectId}/documents/{documentId}")
    suspend fun deleteDocument(
        @Path("projectId") projectId: String,
        @Path("documentId") documentId: String
    ): ApiResponse<Unit>

    @Streaming
    @GET("v1/projects/{projectId}/documents/{documentId}/download")
    suspend fun downloadDocument(
        @Path("projectId") projectId: String,
        @Path("documentId") documentId: String
    ): Response<ResponseBody>

    @Streaming
    @GET("v1/projects/{projectId}/documents/{documentId}/preview")
    suspend fun previewDocument(
        @Path("projectId") projectId: String,
        @Path("documentId") documentId: String
    ): Response<ResponseBody>

    @POST("v1/projects/{projectId}/tasks/{taskId}/attachments")
    suspend fun attachToTask(
        @Path("projectId") projectId: String,
        @Path("taskId") taskId: String,
        @Body request: AttachTaskDocumentRequest
    ): ApiResponse<TaskAttachmentResponse>

    @DELETE("v1/projects/{projectId}/tasks/{taskId}/attachments/{attachmentId}")
    suspend fun detachFromTask(
        @Path("projectId") projectId: String,
        @Path("taskId") taskId: String,
        @Path("attachmentId") attachmentId: String
    ): ApiResponse<Unit>
}
