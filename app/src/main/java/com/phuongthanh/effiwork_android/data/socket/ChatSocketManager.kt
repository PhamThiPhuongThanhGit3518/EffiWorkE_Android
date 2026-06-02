package com.phuongthanh.effiwork_android.data.socket

import android.util.Log
import com.phuongthanh.effiwork_android.data.local.TokenManager
import com.phuongthanh.effiwork_android.data.model.chat.ChatMessageType
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatMessageResponse
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatUserResponse
import com.phuongthanh.effiwork_android.data.model.response.chat.ChatConversationResponse
import com.phuongthanh.effiwork_android.data.model.chat.ChatConversationType
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatSocketManager @Inject constructor(
    private val tokenManager: TokenManager
) {
    companion object {
        private const val TAG = "ChatSocketManager"
        private const val SOCKET_URL = "https://effiwork-api.phuongthanhphuongthanh.id.vn"
    }

    private var socket: Socket? = null

    private val _newMessageFlow = MutableSharedFlow<NewMessageEvent>(replay = 1, extraBufferCapacity = 64)
    val newMessageFlow: SharedFlow<NewMessageEvent> = _newMessageFlow.asSharedFlow()

    private val _conversationUpdatedFlow = MutableSharedFlow<ConversationUpdatedEvent>(replay = 1, extraBufferCapacity = 64)
    val conversationUpdatedFlow: SharedFlow<ConversationUpdatedEvent> = _conversationUpdatedFlow.asSharedFlow()

    private val _connectionStateFlow = MutableSharedFlow<ConnectionState>(replay = 1)
    val connectionStateFlow: SharedFlow<ConnectionState> = _connectionStateFlow.asSharedFlow()

    enum class ConnectionState { CONNECTING, CONNECTED, DISCONNECTED, ERROR }

    fun connect() {
        if (socket != null && socket!!.connected()) {
            return
        }

        val token = tokenManager.getAccessToken() ?: run {
            return
        }

        val options = IO.Options().apply {
            transports = arrayOf(WebSocket.NAME)
            auth = mapOf("token" to token)
            reconnection = true
            reconnectionAttempts = 5
            reconnectionDelay = 1000
        }

        socket = IO.socket(SOCKET_URL, options).apply {
            on(Socket.EVENT_CONNECT) {
                _connectionStateFlow.tryEmit(ConnectionState.CONNECTED)
            }

            on(Socket.EVENT_DISCONNECT) {
                _connectionStateFlow.tryEmit(ConnectionState.DISCONNECTED)
            }

            on(Socket.EVENT_CONNECT_ERROR) { args ->
                _connectionStateFlow.tryEmit(ConnectionState.ERROR)
            }

            on("chat:message:new") { args ->
                if (args.isNotEmpty()) {
                    try {
                        val data = args[0] as JSONObject
                        val message = parseMessage(data)
                        _newMessageFlow.tryEmit(NewMessageEvent(message))
                    } catch (e: Exception) {
                        android.util.Log.e("SocketDebug", "Error parsing new message event", e)
                    }
                } else {
                    android.util.Log.w("SocketDebug", "chat:message:new received but args is empty")
                }
            }

            on("chat:conversation:updated") { args ->
                if (args.isNotEmpty()) {
                    try {
                        val data = args[0] as JSONObject
                        val conversation = parseConversation(data)
                        _conversationUpdatedFlow.tryEmit(ConversationUpdatedEvent(conversation))
                    } catch (e: Exception) {
                        android.util.Log.e("SocketDebug", "Error parsing conversation updated event", e)
                    }
                } else {
                    android.util.Log.w("SocketDebug", "chat:conversation:updated received but args is empty")
                }
            }
        }

        _connectionStateFlow.tryEmit(ConnectionState.CONNECTING)
        socket?.connect()
    }

    fun joinConversation(projectId: String, conversationId: String) {
        val payload = JSONObject().apply {
            put("projectId", projectId)
            put("conversationId", conversationId)
        }
        socket?.emit("chat:join-conversation", payload)
    }

    fun leaveConversation(conversationId: String) {
        val payload = JSONObject().apply {
            put("conversationId", conversationId)
        }
        socket?.emit("chat:leave-conversation", payload)
    }

    fun sendMessage(projectId: String, conversationId: String, type: ChatMessageType, content: String?, documentId: String?) {
        val payload = JSONObject().apply {
            put("projectId", projectId)
            put("conversationId", conversationId)
            put("message", JSONObject().apply {
                put("type", type.name)
                content?.let { put("content", it) }
                documentId?.let { put("documentId", it) }
            })
        }
        Log.d(TAG, "Sending message via socket: $payload")
        socket?.emit("chat:send-message", payload)
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }

    suspend fun waitForConnection() {
        if (socket?.connected() == true) return

        connectionStateFlow
            .filter { it == ConnectionState.CONNECTED }
            .first()
    }

    fun isConnected(): Boolean = socket?.connected() == true

    private fun parseMessage(json: JSONObject): ChatMessageResponse {
        return ChatMessageResponse(
            id = json.optString("id"),
            conversationId = json.optString("conversationId"),
            senderId = json.optString("senderId"),
            type = if (json.has("type")) ChatMessageType.valueOf(json.optString("type")) else ChatMessageType.TEXT,
            content = json.optString("content", null),
            documentId = json.optString("documentId", null),
            createdAt = json.optString("createdAt"),
            updatedAt = json.optString("updatedAt", null),
            deletedAt = json.optString("deletedAt", null),
            sender = json.optJSONObject("sender")?.let { parseUser(it) },
            document = json.optJSONObject("document")?.let { parseDocument(it) }
        )
    }

    private fun parseUser(json: JSONObject): ChatUserResponse {
        return ChatUserResponse(
            id = json.optString("id"),
            fullName = json.optString("fullName"),
            email = json.optString("email"),
            avatarUrl = json.optString("avatarUrl", null)
        )
    }

    private fun parseDocument(json: JSONObject): com.phuongthanh.effiwork_android.data.model.response.chat.ChatDocumentResponse {
        return com.phuongthanh.effiwork_android.data.model.response.chat.ChatDocumentResponse(
            id = json.optString("id"),
            fileName = json.optString("fileName"),
            filePath = json.optString("filePath"),
            mimeType = json.optString("mimeType"),
            fileSize = json.optLong("fileSize", 0)
        )
    }

    private fun parseConversation(json: JSONObject): ChatConversationResponse {
        return ChatConversationResponse(
            id = json.optString("id"),
            projectId = json.optString("projectId"),
            name = json.optString("name", null),
            type = if (json.has("type")) ChatConversationType.valueOf(json.optString("type")) else ChatConversationType.PRIVATE,
            createdById = json.optString("createdById"),
            lastMessageId = json.optString("lastMessageId", null),
            lastMessageAt = json.optString("lastMessageAt", null),
            createdAt = json.optString("createdAt"),
            updatedAt = json.optString("updatedAt", null),
            unreadCount = json.optInt("unreadCount", 0),
            createdBy = json.optJSONObject("createdBy")?.let { parseUser(it) },
            members = null,
            messages = null,
            _count = null
        )
    }
}