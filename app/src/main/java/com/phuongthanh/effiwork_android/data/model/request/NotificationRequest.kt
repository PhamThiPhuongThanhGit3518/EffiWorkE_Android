package com.phuongthanh.effiwork_android.data.model.request

import com.google.gson.annotations.SerializedName

data class SaveFcmTokenRequest(
    @SerializedName("token")
    val token: String,
    @SerializedName("deviceName")
    val deviceName: String?
)