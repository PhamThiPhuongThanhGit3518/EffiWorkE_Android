package com.phuongthanh.effiwork_android.di

import android.content.Context
import android.content.SharedPreferences
import com.phuongthanh.effiwork_android.data.local.AppPreferences
import com.phuongthanh.effiwork_android.data.local.TokenManager
import com.phuongthanh.effiwork_android.data.repository.AuthRepository
import com.phuongthanh.effiwork_android.data.repository.AuthRepositoryImpl
import com.phuongthanh.effiwork_android.data.repository.DocumentRepository
import com.phuongthanh.effiwork_android.data.repository.DocumentRepositoryImpl
import com.phuongthanh.effiwork_android.data.repository.MeetingRepository
import com.phuongthanh.effiwork_android.data.repository.MeetingRepositoryImpl
import com.phuongthanh.effiwork_android.data.repository.ProjectRepository
import com.phuongthanh.effiwork_android.data.repository.ProjectRepositoryImpl
import com.phuongthanh.effiwork_android.data.repository.TaskRepository
import com.phuongthanh.effiwork_android.data.repository.TaskRepositoryImpl
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
        return TokenManager(sharedPreferences)
    }

    @Provides
    @Singleton
    fun provideAppPreferences(sharedPreferences: SharedPreferences): AppPreferences {
        return AppPreferences(sharedPreferences)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        authRepository: AuthRepositoryImpl
    ): AuthRepository {
        return authRepository
    }

    @Provides
    @Singleton
    fun provideProjectRepository(
        projectRepository: ProjectRepositoryImpl
    ): ProjectRepository {
        return projectRepository
    }

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskRepositoryImpl: TaskRepositoryImpl
    ): TaskRepository {
        return taskRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideMeetingRepository(
        meetingRepositoryImpl: MeetingRepositoryImpl
    ): MeetingRepository {
        return meetingRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideDocumentRepository(
        documentRepositoryImpl: DocumentRepositoryImpl
    ): DocumentRepository {
        return documentRepositoryImpl
    }
}
