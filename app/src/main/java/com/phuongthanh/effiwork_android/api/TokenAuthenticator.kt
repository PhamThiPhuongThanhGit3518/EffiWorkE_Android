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

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("X-Refresh-Attempt") != null) {
            return null
        }

        val refreshToken = tokenManager.getRefreshToken()
        if (refreshToken == null) {
            return null
        }

        return runBlocking {
            try {
                val authService = authServiceProvider.get()
                val refreshResponse = authService.refreshToken(RefreshTokenRequest(refreshToken))
                if (refreshResponse.success && refreshResponse.data != null) {
                    val newAccessToken = refreshResponse.data.accessToken
                    val newRefreshToken = refreshResponse.data.refreshToken
                    tokenManager.saveTokens(newAccessToken, newRefreshToken)

                    response.request.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .header("X-Refresh-Attempt", "true")
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