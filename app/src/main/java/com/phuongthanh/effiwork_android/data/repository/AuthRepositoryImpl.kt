package com.phuongthanh.effiwork_android.data.repository

import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.api.AuthService
import com.phuongthanh.effiwork_android.data.local.TokenManager
import com.phuongthanh.effiwork_android.data.model.request.LoginRequest
import com.phuongthanh.effiwork_android.data.model.request.RefreshTokenRequest
import com.phuongthanh.effiwork_android.data.model.request.RegisterRequest
import com.phuongthanh.effiwork_android.data.model.response.AuthResponse
import com.phuongthanh.effiwork_android.data.model.response.UserResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun login(identifier: String, password: String): ApiResult<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = authService.login(LoginRequest(identifier, password))
                if (response.success && response.data != null) {
                    tokenManager.saveTokens(response.data.accessToken, response.data.refreshToken)
                    ApiResult.Success(response.data)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun register(
        fullName: String,
        email: String,
        phone: String?,
        password: String
    ): ApiResult<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = RegisterRequest(fullName, email, phone, password)
                val response = authService.register(request)
                if (response.success && response.data != null) {
                    tokenManager.saveTokens(response.data.accessToken, response.data.refreshToken)
                    ApiResult.Success(response.data)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun logout(): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val refreshToken = tokenManager.getRefreshToken()
                if (refreshToken != null) {
                    authService.logout(RefreshTokenRequest(refreshToken))
                }
                tokenManager.clearTokens()
                ApiResult.Success(Unit)
            } catch (e: Exception) {
                tokenManager.clearTokens()
                ApiResult.Success(Unit)
            }
        }
    }

    override suspend fun getCurrentUser(): ApiResult<UserResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = authService.getMe()
                if (response.success && response.data != null) {
                    ApiResult.Success(response.data)
                } else {
                    ApiResult.Error(response.message)
                }
            } catch (e: Exception) {
                ApiResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    override fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()
}