package com.phuongthanh.effiwork_android.api

import com.phuongthanh.effiwork_android.data.model.request.LoginRequest
import com.phuongthanh.effiwork_android.data.model.request.RegisterRequest
import com.phuongthanh.effiwork_android.data.model.request.RefreshTokenRequest
import com.phuongthanh.effiwork_android.data.model.response.ApiResponse
import com.phuongthanh.effiwork_android.data.model.response.AuthResponse
import com.phuongthanh.effiwork_android.data.model.response.UserResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthService {
    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>

    @POST("v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthResponse>

    @POST("v1/auth/logout")
    suspend fun logout(@Body request: RefreshTokenRequest): ApiResponse<Unit>

    @GET("v1/auth/me")
    suspend fun getMe(): ApiResponse<UserResponse>
}
