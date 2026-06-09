package com.phuongthanh.effiwork_android.data.socket

import android.util.Log
import com.phuongthanh.effiwork_android.data.local.TokenManager
import com.phuongthanh.effiwork_android.data.model.response.NotificationData
import com.phuongthanh.effiwork_android.data.model.response.NotificationResponse
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
class NotificationSocketManager @Inject constructor(
    private val tokenManager: TokenManager
) {
    companion object {
        private const val TAG = "NotificationSocketMgr"
        private const val SOCKET_URL = "https://effiwork-api.phuongthanhphuongthanh.id.vn"
    }

    private var socket: Socket? = null

    private val _newNotificationFlow = MutableSharedFlow<NotificationNewEvent>(replay = 1, extraBufferCapacity = 64)
    val newNotificationFlow: SharedFlow<NotificationNewEvent> = _newNotificationFlow.asSharedFlow()

    private val _connectionStateFlow = MutableSharedFlow<ConnectionState>(replay = 1)
    val connectionStateFlow: SharedFlow<ConnectionState> = _connectionStateFlow.asSharedFlow()

    enum class ConnectionState { CONNECTING, CONNECTED, DISCONNECTED, ERROR }

    fun connect() {
        if (socket != null && socket!!.connected()) {
            return
        }

        val token = tokenManager.getAccessToken() ?: run {
            Log.w(TAG, "connect: no access token, skipping")
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

            on(Socket.EVENT_CONNECT_ERROR) { _ ->
                _connectionStateFlow.tryEmit(ConnectionState.ERROR)
            }

            on("notification:new") { args ->
                if (args.isNotEmpty()) {
                    try {
                        val data = args[0] as JSONObject
                        val notification = parseNotification(data)
                        _newNotificationFlow.tryEmit(NotificationNewEvent(notification))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing notification:new event", e)
                    }
                } else {
                    Log.w(TAG, "notification:new received but args is empty")
                }
            }
        }

        _connectionStateFlow.tryEmit(ConnectionState.CONNECTING)
        socket?.connect()
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

    private fun parseNotification(json: JSONObject): NotificationResponse {
        return NotificationResponse(
            id = json.optString("id"),
            userId = json.optString("userId", null),
            title = json.optString("title", null),
            message = json.optString("message", null),
            content = json.optString("content", null),
            type = json.optString("type", null),
            isRead = if (json.has("isRead")) json.optBoolean("isRead") else null,
            readAt = json.optString("readAt", null),
            data = json.optJSONObject("data")?.let { parseData(it) },
            createdAt = json.optString("createdAt", null),
            updatedAt = json.optString("updatedAt", null),
            projectId = json.optString("projectId", null),
            relatedType = json.optString("relatedType", null),
            relatedId = json.optString("relatedId", null)
        )
    }

    private fun parseData(json: JSONObject): NotificationData {
        return NotificationData(
            taskId = json.optString("taskId", null),
            projectId = json.optString("projectId", null),
            meetingId = json.optString("meetingId", null),
            commentId = json.optString("commentId", null),
            senderId = json.optString("senderId", null),
            senderName = json.optString("senderName", null),
            avatarUrl = json.optString("avatarUrl", null)
        )
    }
}
