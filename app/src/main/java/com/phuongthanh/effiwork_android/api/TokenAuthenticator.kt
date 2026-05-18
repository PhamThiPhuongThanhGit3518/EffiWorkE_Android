package com.phuongthanh.effiwork_android.api

import com.phuongthanh.effiwork_android.data.local.TokenManager
import com.phuongthanh.effiwork_android.data.model.request.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authServiceProvider: Provider<AuthService>
) : Authenticator {

    companion object {
        private const val HEADER_RETRY = "X-Refresh-Attempt"
    }

    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        // 1. Nếu đã thử refresh cho request này rồi → không retry nữa (tránh loop)
        if (response.request.header(HEADER_RETRY) != null) {
            return null
        }

        // 2. Lấy refresh token
        val refreshToken = tokenManager.getRefreshToken()
        if (refreshToken == null) {
            return null
        }

        // 3. Kiểm tra token trên request gốc có khác token hiện tại trong storage không
        // Nếu khác → có thread khác vừa refresh thành công → dùng token mới luôn
        val requestToken = response.request.header("Authorization")?.replace("Bearer ", "")
        val currentToken = tokenManager.getAccessToken()
        if (requestToken != currentToken && currentToken != null) {
            return response.request.newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .header(HEADER_RETRY, "true")
                .build()
        }

        // 4. Synchronized - chỉ 1 thread được refresh, các thread khác đợi
        synchronized(refreshLock) {
            // Đọc lại token sau khi có lock - phòng trường hợp thread khác vừa refresh xong
            val latestAccessToken = tokenManager.getAccessToken()
            val latestRefreshToken = tokenManager.getRefreshToken()

            // Double-check: nếu token đã thay đổi (thread khác refresh thành công) → dùng token mới
            if (latestAccessToken != null && requestToken != latestAccessToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $latestAccessToken")
                    .header(HEADER_RETRY, "true")
                    .build()
            }

            // 5. Thực hiện refresh token (chỉ 1 thread chạy đến đây)
            return runBlocking {
                try {
                    val authService = authServiceProvider.get()
                    val refreshResponse = authService.refreshToken(RefreshTokenRequest(latestRefreshToken!!))

                    if (refreshResponse.success && refreshResponse.data != null) {
                        val newAccessToken = refreshResponse.data.accessToken
                        val newRefreshToken = refreshResponse.data.refreshToken
                        tokenManager.saveTokens(newAccessToken, newRefreshToken)

                        // Retry request với token mới
                        response.request.newBuilder()
                            .header("Authorization", "Bearer $newAccessToken")
                            .header(HEADER_RETRY, "true")
                            .build()
                    } else {
                        tokenManager.clearTokens()
                        null
                    }
                } catch (e: Exception) {
                    tokenManager.clearTokens()
                    null
                }
            }
        }
    }
}