package com.phuongthanh.effiwork_android.data.repository.chat

import android.util.Log
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.api.ChatService
import com.phuongthanh.effiwork_android.data.model.request.chat.*
import com.phuongthanh.effiwork_android.data.model.response.chat.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ChatRepository"

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatService: ChatService
) : ChatRepository {

    override suspend fun getMessages(
        projectId: String,
        conversationId: String,
        page: Int,
        limit: Int,
        type: String?
    ): ApiResult<ListMessagesResponse> = withContext(Dispatchers.IO) {
        try {
            val response = chatService.listMessages(projectId, conversationId, page, limit, type)
            if (response.success) {
                // Web API returns { success, message, data: [], meta: {} } directly
                ApiResult.Success(
                    ListMessagesResponse(
                        items = response.data,
                        meta = response.meta
                    )
                )
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getMessages error", e)
            ApiResult.Error(e.message ?: "Failed to get messages")
        }
    }

    override suspend fun sendMessage(
        projectId: String,
        conversationId: String,
        request: CreateChatMessageRequest
    ): ApiResult<ChatMessageResponse> = withContext(Dispatchers.IO) {
        try {
            val response = chatService.sendMessage(projectId, conversationId, request)
            if (response.success && response.data != null) {
                ApiResult.Success(response.data)
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage error", e)
            ApiResult.Error(e.message ?: "Failed to send message")
        }
    }

    override suspend fun getConversations(
        projectId: String,
        page: Int,
        limit: Int,
        keyword: String?,
        type: String?
    ): ApiResult<ListConversationsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = chatService.listConversations(projectId, page, limit, keyword, type)
            Log.d(TAG, "📥 listConversations response: success=${response.success}, data=${response.data?.size} items")
            if (response.success && response.data != null) {
                ApiResult.Success(ListConversationsResponse(
                    items = response.data,
                    meta = PaginationMeta(page = page, limit = limit, total = response.data.size, totalPages = 1)
                ))
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getConversations error", e)
            ApiResult.Error(e.message ?: "Failed to get conversations")
        }
    }

    override suspend fun createPrivateConversation(
        projectId: String,
        targetUserId: String
    ): ApiResult<ChatConversationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = chatService.createPrivateConversation(
                projectId,
                CreatePrivateConversationRequest(targetUserId)
            )
            if (response.success && response.data != null) {
                ApiResult.Success(response.data)
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "createPrivateConversation error", e)
            ApiResult.Error(e.message ?: "Failed to create conversation")
        }
    }

    override suspend fun createGroupConversation(
        projectId: String,
        name: String?,
        memberIds: List<String>
    ): ApiResult<ChatConversationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = chatService.createGroupConversation(
                projectId,
                CreateGroupConversationRequest(name, memberIds)
            )
            if (response.success && response.data != null) {
                ApiResult.Success(response.data)
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "createGroupConversation error", e)
            ApiResult.Error(e.message ?: "Failed to create group conversation")
        }
    }

    override suspend fun getConversationDetail(
        projectId: String,
        conversationId: String
    ): ApiResult<ChatConversationDetailResponse> = withContext(Dispatchers.IO) {
        try {
            val response = chatService.getConversationDetail(projectId, conversationId)
            if (response.success && response.data != null) {
                ApiResult.Success(response.data)
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getConversationDetail error", e)
            ApiResult.Error(e.message ?: "Failed to get conversation detail")
        }
    }

    override suspend fun updateConversation(
        projectId: String,
        conversationId: String,
        request: UpdateChatConversationRequest
    ): ApiResult<ChatConversationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = chatService.updateConversation(projectId, conversationId, request)
            if (response.success && response.data != null) {
                ApiResult.Success(response.data)
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateConversation error", e)
            ApiResult.Error(e.message ?: "Failed to update conversation")
        }
    }

    override suspend fun getSharedFiles(
        projectId: String,
        conversationId: String
    ): ApiResult<List<ChatMessageResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = chatService.getSharedFiles(projectId, conversationId)
            if (response.success && response.data != null) {
                ApiResult.Success(response.data)
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "getSharedFiles error", e)
            ApiResult.Error(e.message ?: "Failed to get shared files")
        }
    }

    override suspend fun addMembers(
        projectId: String,
        conversationId: String,
        request: AddMembersRequest
    ): ApiResult<ChatConversationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = chatService.addMembers(projectId, conversationId, request)
            if (response.success && response.data != null) {
                ApiResult.Success(response.data)
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "addMembers error", e)
            ApiResult.Error(e.message ?: "Failed to add members")
        }
    }

    override suspend fun removeMembers(
        projectId: String,
        conversationId: String,
        request: RemoveMembersRequest
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = chatService.removeMembers(projectId, conversationId, request)
            if (response.success) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "removeMembers error", e)
            ApiResult.Error(e.message ?: "Failed to remove members")
        }
    }

    override suspend fun leaveConversation(
        projectId: String,
        conversationId: String
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = chatService.leaveConversation(projectId, conversationId)
            if (response.success) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "leaveConversation error", e)
            ApiResult.Error(e.message ?: "Failed to leave conversation")
        }
    }

    override suspend fun markAsRead(
        projectId: String,
        conversationId: String
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = chatService.markAsRead(projectId, conversationId)
            if (response.success) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(response.message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "markAsRead error", e)
            ApiResult.Error(e.message ?: "Failed to mark as read")
        }
    }
}