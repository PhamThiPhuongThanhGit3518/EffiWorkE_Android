package com.phuongthanh.effiwork_android.data.repository

import android.util.Log
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.api.AuthInterceptor
import com.phuongthanh.effiwork_android.api.ChatbotService
import com.phuongthanh.effiwork_android.data.model.chatbot.ChatStreamEvent
import com.phuongthanh.effiwork_android.data.model.chatbot.ChatbotHistoryResponse
import com.phuongthanh.effiwork_android.data.model.response.ApiResponse
import com.phuongthanh.effiwork_android.di.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatbotRepository @Inject constructor(
    private val chatbotService: ChatbotService,
    private val okHttpClient: OkHttpClient,
    private val authInterceptor: AuthInterceptor
) {

    companion object {
        private const val TAG = "ChatbotRepo"
    }

    suspend fun getHistory(projectId: String): ApiResult<ChatbotHistoryResponse> = withContext(Dispatchers.IO) {
        Log.d(TAG, "getHistory projectId=$projectId")
        runCatching { chatbotService.getHistory(projectId) }
            .fold(
                onSuccess = { response: ApiResponse<ChatbotHistoryResponse> ->
                    Log.d(TAG, "getHistory success=${response.success} dataNull=${response.data == null} msgCount=${response.data?.messages?.size}")
                    if (response.success && response.data != null) {
                        ApiResult.Success(response.data)
                    } else {
                        ApiResult.Error(response.message)
                    }
                },
                onFailure = {
                    Log.e(TAG, "getHistory failed: ${it.message}", it)
                    ApiResult.Error(it.message ?: "Không thể tải lịch sử hội thoại")
                }
            )
    }

    suspend fun reset(projectId: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        runCatching { chatbotService.resetConversation(projectId) }
            .fold(
                onSuccess = { ApiResult.Success(Unit) },
                onFailure = { ApiResult.Error(it.message ?: "Không thể xóa hội thoại") }
            )
    }

    fun streamMessage(projectId: String, content: String): Flow<ChatStreamEvent> = callbackFlow {
        val authHeader = authInterceptor.authHeaderValue()
        val encodedContent = java.net.URLEncoder.encode(content, "UTF-8")
        val url = "${NetworkModule.BASE_URL}v1/projects/$projectId/chatbot/stream?content=$encodedContent"
        Log.d(TAG, "streamMessage START url=$url authHeaderNull=${authHeader == null}")

        val builder = Request.Builder()
            .url(url)
            .addHeader("Accept", "text/event-stream")
            .get()

        if (!authHeader.isNullOrEmpty()) {
            builder.addHeader("Authorization", authHeader)
        }

        val factory = EventSources.createFactory(okHttpClient)
        val eventSource: EventSource = factory.newEventSource(builder.build(), object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Log.d(TAG, "SSE onOpen code=${response.code} ct=${response.header("Content-Type")}")
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                Log.d(TAG, "SSE onEvent type=$type data=$data")
                val event = parseEvent(type, data)
                if (event != null) {
                    Log.d(TAG, "SSE parsed -> $event")
                    trySend(event)
                } else {
                    Log.w(TAG, "SSE parse FAILED type=$type data=$data")
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                val msg = t?.message
                    ?: response?.message
                    ?: "Kết nối thất bại (HTTP ${response?.code ?: "?"})"
                Log.e(TAG, "SSE onFailure msg=$msg code=${response?.code}", t)
                trySend(ChatStreamEvent.Error(code = "stream_failure", message = msg))
                close(t)
            }

            override fun onClosed(eventSource: EventSource) {
                Log.d(TAG, "SSE onClosed")
                close()
            }
        })

        awaitClose {
            Log.d(TAG, "streamMessage awaitClose -> eventSource.cancel()")
            eventSource.cancel()
        }
    }.flowOn(Dispatchers.IO)

    private fun parseEvent(type: String?, data: String): ChatStreamEvent? {
        return runCatching {
            val (effectiveType, innerJson) = unwrapPayload(type, data)
            val obj = JSONObject(innerJson)
            when (effectiveType) {
                "start" -> ChatStreamEvent.Start(messageId = obj.optString("messageId"))
                "token" -> ChatStreamEvent.Token(text = obj.optString("text"))
                "done" -> ChatStreamEvent.Done(
                    messageId = obj.optString("messageId"),
                    content = obj.optString("content")
                )
                "error" -> ChatStreamEvent.Error(
                    code = obj.optString("code", "unknown"),
                    message = obj.optString("message", "Lỗi không xác định")
                )
                else -> null
            }
        }.getOrNull()
    }

    private fun unwrapPayload(type: String?, data: String): Pair<String?, String> {
        val outer = runCatching { JSONObject(data) }.getOrNull() ?: return type to data
        val nestedType = outer.opt("type").takeIf { it != null && it != JSONObject.NULL }?.toString()
        val nestedData = outer.opt("data").takeIf { it != null && it != JSONObject.NULL }?.toString()
        return if (nestedType != null && nestedData != null) {
            val effectiveType = type ?: nestedType
            val innerJson = if (nestedData.startsWith("{")) nestedData else data
            effectiveType to innerJson
        } else {
            type to data
        }
    }
}
