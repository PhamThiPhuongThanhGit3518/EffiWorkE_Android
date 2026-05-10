package com.phuongthanh.effiwork_android.data.model.response

data class UserResponse(
    val sub: String,
    val email: String,
    val fullName: String,
    val sessionId: String
)