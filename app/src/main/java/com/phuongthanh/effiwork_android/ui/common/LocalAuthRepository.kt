package com.phuongthanh.effiwork_android.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.phuongthanh.effiwork_android.data.repository.AuthRepository
import com.phuongthanh.effiwork_android.di.AppModule
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthRepositoryEntryPoint {
    fun authRepository(): AuthRepository
}

@Composable
fun rememberAuthRepository(): AuthRepository {
    val context = LocalContext.current
    return remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AuthRepositoryEntryPoint::class.java
        ).authRepository()
    }
}