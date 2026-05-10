package com.phuongthanh.effiwork_android.data.model.response

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse
)