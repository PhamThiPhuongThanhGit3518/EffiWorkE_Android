package com.phuongthanh.effiwork_android.data.model.response

data class UserResponse(
    val id: String,        // Sửa 'sub' thành 'id' cho khớp với JSON API trả về
    val email: String,
    val fullName: String?,
    val phone: String?,    // Thêm dòng này
    val avatarUrl: String?,
    val status: String?,
    val createdAt: String?,
    val updatedAt: String?
)