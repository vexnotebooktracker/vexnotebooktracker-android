package org.vexteams.vexnotebooktracker.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import org.vexteams.vexnotebooktracker.security.JwtAuthInterceptor
import org.vexteams.vexnotebooktracker.security.DeviceInfoCollector
import org.vexteams.vexnotebooktracker.security.SecureStorage
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJwtAuthInterceptor(
        secureStorage: SecureStorage,
        deviceInfoCollector: DeviceInfoCollector
    ): JwtAuthInterceptor {
        return JwtAuthInterceptor(secureStorage, deviceInfoCollector)
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (org.vexteams.vexnotebooktracker.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        jwtAuthInterceptor: JwtAuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            // Add JWT interceptor first (before logging)
            .addInterceptor(jwtAuthInterceptor)
            .addInterceptor(loggingInterceptor)
            // Extended timeouts to handle slow operations like email sending
            .connectTimeout(60, TimeUnit.SECONDS)  // Increased from 30 to 60
            .readTimeout(60, TimeUnit.SECONDS)     // Increased from 30 to 60
            .writeTimeout(60, TimeUnit.SECONDS)    // Increased from 30 to 60
            .build()
    }

    @Provides
    @Singleton
    fun provideVexTeamsRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.vexteams.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideVexTeamsApiService(retrofit: Retrofit): VexTeamsApiService {
        return retrofit.create(VexTeamsApiService::class.java)
    }
}