package com.phuongthanh.effiwork_android.data.model.request

import com.google.gson.annotations.SerializedName

data class CreateSectionRequest(
    @SerializedName("name")
    val name: String,
    @SerializedName("sortOrder")
    val sortOrder: Int? = null
)