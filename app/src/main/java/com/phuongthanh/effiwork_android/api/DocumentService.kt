package com.phuongthanh.effiwork_android.api

import com.phuongthanh.effiwork_android.data.model.response.ApiResponse
import com.phuongthanh.effiwork_android.data.model.response.DocumentResponse
import okhttp3.MultipartBody
import retrofit2.http.*

interface DocumentService {
    @Multipart
    @POST("v1/projects/{projectId}/documents/upload")
    suspend fun uploadDocument(
        @Path("projectId") projectId: String,
        @Part file: MultipartBody.Part
    ): retrofit2.Response<ApiResponse<DocumentResponse>>

    @GET("v1/projects/{projectId}/documents")
    suspend fun getDocuments(
        @Path("projectId") projectId: String
    ): retrofit2.Response<ApiResponse<List<DocumentResponse>>>
}