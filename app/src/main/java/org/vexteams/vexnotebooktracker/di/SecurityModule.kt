package org.vexteams.vexnotebooktracker.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.vexteams.vexnotebooktracker.security.DeviceInfoCollector
import org.vexteams.vexnotebooktracker.security.PlayIntegrityManager
import org.vexteams.vexnotebooktracker.security.SecureStorage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSecureStorage(@ApplicationContext context: Context): SecureStorage {
        return SecureStorage(context)
    }

    @Provides
    @Singleton
    fun provideDeviceInfoCollector(@ApplicationContext context: Context): DeviceInfoCollector {
        return DeviceInfoCollector(context)
    }

    @Provides
    @Singleton
    fun providePlayIntegrityManager(@ApplicationContext context: Context): PlayIntegrityManager {
        return PlayIntegrityManager(context)
    }

    // JWT Auth Interceptor is provided in NetworkModule since it's network-related
}