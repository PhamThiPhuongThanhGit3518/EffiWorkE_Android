package com.phuongthanh.effiwork_android.api

import com.phuongthanh.effiwork_android.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Thêm cái này nếu bạn muốn dùng chung 1 instance
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    // AuthInterceptor.kt
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        if (path.contains("login") || path.contains("register")) {
            return chain.proceed(originalRequest)
        }

        val token = tokenManager.getAccessToken()
        return if (!token.isNullOrEmpty()) {
            val newRequest = originalRequest.newBuilder()
                .removeHeader("Authorization") // XÓA header cũ trước
                .addHeader("Authorization", "Bearer $token") // Thêm mới
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}