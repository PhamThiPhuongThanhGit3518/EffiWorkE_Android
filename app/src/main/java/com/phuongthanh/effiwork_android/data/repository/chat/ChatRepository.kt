package com.phuongthanh.effiwork_android.data.repository.chat

import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.model.request.chat.*
import com.phuongthanh.effiwork_android.data.model.response.chat.*

interface ChatRepository {
    // Messages
    suspend fun getMessages(
        projectId: String,
        conversationId: String,
        page: Int,
        limit: Int,
        type: String?
    ): ApiResult<ListMessagesResponse>

    suspend fun sendMessage(
        projectId: String,
        conversationId: String,
        request: CreateChatMessageRequest
    ): ApiResult<ChatMessageResponse>

    // Conversations
    suspend fun getConversations(
        projectId: String,
        page: Int,
        limit: Int,
        keyword: String?,
        type: String?
    ): ApiResult<ListConversationsResponse>

    suspend fun createPrivateConversation(
        projectId: String,
        targetUserId: String
    ): ApiResult<ChatConversationResponse>

    suspend fun createGroupConversation(
        projectId: String,
        name: String?,
        memberIds: List<String>
    ): ApiResult<ChatConversationResponse>

    suspend fun getConversationDetail(
        projectId: String,
        conversationId: String
    ): ApiResult<ChatConversationDetailResponse>

    suspend fun updateConversation(
        projectId: String,
        conversationId: String,
        request: UpdateChatConversationRequest
    ): ApiResult<ChatConversationResponse>

    suspend fun getSharedFiles(
        projectId: String,
        conversationId: String
    ): ApiResult<List<ChatMessageResponse>>

    suspend fun addMembers(
        projectId: String,
        conversationId: String,
        request: AddMembersRequest
    ): ApiResult<ChatConversationResponse>

    suspend fun removeMembers(
        projectId: String,
        conversationId: String,
        request: RemoveMembersRequest
    ): ApiResult<Unit>

    suspend fun leaveConversation(
        projectId: String,
        conversationId: String
    ): ApiResult<Unit>

    suspend fun markAsRead(
        projectId: String,
        conversationId: String
    ): ApiResult<Unit>
}