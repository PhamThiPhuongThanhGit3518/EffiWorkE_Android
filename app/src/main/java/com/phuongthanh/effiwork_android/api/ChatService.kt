package com.phuongthanh.effiwork_android.api

import com.phuongthanh.effiwork_android.data.model.request.chat.*
import com.phuongthanh.effiwork_android.data.model.response.ApiResponse
import com.phuongthanh.effiwork_android.data.model.response.chat.*
import retrofit2.http.*

interface ChatService {

    // === Messages ===
    @GET("v1/projects/{projectId}/chat/conversations/{conversationId}/messages")
    suspend fun listMessages(
        @Path("projectId") projectId: String,
        @Path("conversationId") conversationId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30,
        @Query("type") type: String? = null
    ): WebListMessagesResponse

    @POST("v1/projects/{projectId}/chat/conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Path("projectId") projectId: String,
        @Path("conversationId") conversationId: String,
        @Body request: CreateChatMessageRequest
    ): ApiResponse<ChatMessageResponse>

    // === Conversations ===
    @GET("v1/projects/{projectId}/chat/conversations")
    suspend fun listConversations(
        @Path("projectId") projectId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("keyword") keyword: String? = null,
        @Query("type") type: String? = null
    ): ApiResponse<List<ChatConversationResponse>>

    @POST("v1/projects/{projectId}/chat/conversations/private")
    suspend fun createPrivateConversation(
        @Path("projectId") projectId: String,
        @Body request: CreatePrivateConversationRequest
    ): ApiResponse<ChatConversationResponse>

    @POST("v1/projects/{projectId}/chat/conversations/group")
    suspend fun createGroupConversation(
        @Path("projectId") projectId: String,
        @Body request: CreateGroupConversationRequest
    ): ApiResponse<ChatConversationResponse>

    @GET("v1/projects/{projectId}/chat/conversations/{conversationId}")
    suspend fun getConversationDetail(
        @Path("projectId") projectId: String,
        @Path("conversationId") conversationId: String
    ): ApiResponse<ChatConversationDetailResponse>

    @PATCH("v1/projects/{projectId}/chat/conversations/{conversationId}")
    suspend fun updateConversation(
        @Path("projectId") projectId: String,
        @Path("conversationId") conversationId: String,
        @Body request: UpdateChatConversationRequest
    ): ApiResponse<ChatConversationResponse>

    @GET("v1/projects/{projectId}/chat/conversations/{conversationId}/files")
    suspend fun getSharedFiles(
        @Path("projectId") projectId: String,
        @Path("conversationId") conversationId: String
    ): ApiResponse<List<ChatMessageResponse>>

    @DELETE("v1/projects/{projectId}/chat/conversations/{conversationId}/leave")
    suspend fun leaveConversation(
        @Path("projectId") projectId: String,
        @Path("conversationId") conversationId: String
    ): ApiResponse<Unit>

    @PATCH("v1/projects/{projectId}/chat/conversations/{conversationId}/read")
    suspend fun markAsRead(
        @Path("projectId") projectId: String,
        @Path("conversationId") conversationId: String
    ): ApiResponse<Unit>
}