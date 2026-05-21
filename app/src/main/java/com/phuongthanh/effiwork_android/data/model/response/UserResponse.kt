package com.phuongthanh.effiwork_android.data.model.response

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("sub") val id: String,
    val email: String,
    val fullName: String?,
    val phone: String?,
    val avatarUrl: String?,
    val status: String?,
    val createdAt: String?,
    val updatedAt: String?
)