package com.phuongthanh.effiwork_android.api

import com.phuongthanh.effiwork_android.data.model.chatbot.ChatbotHistoryResponse
import com.phuongthanh.effiwork_android.data.model.response.ApiResponse
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path

interface ChatbotService {
    @GET("v1/projects/{projectId}/chatbot/history")
    suspend fun getHistory(
        @Path("projectId") projectId: String
    ): ApiResponse<ChatbotHistoryResponse>

    @DELETE("v1/projects/{projectId}/chatbot/conversation")
    suspend fun resetConversation(
        @Path("projectId") projectId: String
    ): ApiResponse<Unit>
}
