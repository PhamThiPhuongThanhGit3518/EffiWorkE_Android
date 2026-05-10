package com.phuongthanh.effiwork_android.data.repository

import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.model.response.AuthResponse
import com.phuongthanh.effiwork_android.data.model.response.UserResponse

interface AuthRepository {
    suspend fun login(identifier: String, password: String): ApiResult<AuthResponse>
    suspend fun register(fullName: String, email: String, phone: String?, password: String): ApiResult<AuthResponse>
    suspend fun logout(): ApiResult<Unit>
    suspend fun getCurrentUser(): ApiResult<UserResponse>
    fun isLoggedIn(): Boolean
}