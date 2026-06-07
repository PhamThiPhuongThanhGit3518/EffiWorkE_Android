package com.phuongthanh.effiwork_android.api

import com.phuongthanh.effiwork_android.data.model.request.document.CreateFolderRequest
import com.phuongthanh.effiwork_android.data.model.request.document.UpdateFolderRequest
import com.phuongthanh.effiwork_android.data.model.response.ApiResponse
import com.phuongthanh.effiwork_android.data.model.response.document.FolderNode
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface FolderService {

    @GET("v1/projects/{projectId}/folders/tree")
    suspend fun getFolderTree(
        @Path("projectId") projectId: String
    ): ApiResponse<List<FolderNode>>

    @POST("v1/projects/{projectId}/folders")
    suspend fun createFolder(
        @Path("projectId") projectId: String,
        @Body request: CreateFolderRequest
    ): ApiResponse<FolderNode>

    @PATCH("v1/projects/{projectId}/folders/{folderId}")
    suspend fun updateFolder(
        @Path("projectId") projectId: String,
        @Path("folderId") folderId: String,
        @Body request: UpdateFolderRequest
    ): ApiResponse<FolderNode>

    @DELETE("v1/projects/{projectId}/folders/{folderId}")
    suspend fun deleteFolder(
        @Path("projectId") projectId: String,
        @Path("folderId") folderId: String
    ): ApiResponse<Unit>
}
