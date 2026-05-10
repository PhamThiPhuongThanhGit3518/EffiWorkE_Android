package com.phuongthanh.effiwork_android.di

import android.content.Context
import android.content.SharedPreferences
import com.phuongthanh.effiwork_android.api.AuthService
import com.phuongthanh.effiwork_android.api.ApiClient
import com.phuongthanh.effiwork_android.data.local.TokenManager
import com.phuongthanh.effiwork_android.data.local.TokenManagerHolder
import com.phuongthanh.effiwork_android.data.repository.AuthRepository
import com.phuongthanh.effiwork_android.data.repository.AuthRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("efiwork_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideTokenManager(sharedPreferences: SharedPreferences): TokenManager {
        val tokenManager = TokenManager(sharedPreferences)
        TokenManagerHolder.tokenManager = tokenManager
        return tokenManager
    }

    @Provides
    @Singleton
    fun provideAuthService(): AuthService {
        return ApiClient.authService
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        authService: AuthService,
        tokenManager: TokenManager
    ): AuthRepository {
        return AuthRepositoryImpl(authService, tokenManager)
    }
}