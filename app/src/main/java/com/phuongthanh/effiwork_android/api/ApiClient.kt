package com.phuongthanh.effiwork_android.api

import com.phuongthanh.effiwork_android.BuildConfig
import com.phuongthanh.effiwork_android.data.local.TokenManagerHolder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = BuildConfig.API_BASE_URL

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private fun createOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun createRetrofit(authInterceptor: AuthInterceptor): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient(authInterceptor))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authService: AuthService by lazy {
        val tm = TokenManagerHolder.tokenManager
            ?: throw IllegalStateException("TokenManager not initialized. Ensure AppModule provides TokenManager before accessing ApiClient.")
        val authInterceptor = AuthInterceptor(tm)
        createRetrofit(authInterceptor).create(AuthService::class.java)
    }
}