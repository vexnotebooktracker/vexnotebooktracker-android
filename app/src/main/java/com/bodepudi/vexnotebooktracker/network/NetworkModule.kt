package com.bodepudi.vexnotebooktracker.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.bodepudi.vexnotebooktracker.security.JwtAuthInterceptor
import com.bodepudi.vexnotebooktracker.security.TokenAuthenticator
import com.bodepudi.vexnotebooktracker.security.DeviceInfoCollector
import com.bodepudi.vexnotebooktracker.security.SecureStorage
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Shared by the main client and the refresh client so the two can never drift onto different
     * hosts - a refresh against a different server than the API calls would fail confusingly.
     */
    private const val BASE_URL = "https://api.vexteams.org/"

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
            level = if (com.bodepudi.vexnotebooktracker.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Refresh client - no auth interceptor, no authenticator.
    //
    // Keeping the refresh call on its own client breaks the dependency cycle that would
    // otherwise exist (AuthRepository -> VexTeamsApiService -> Retrofit -> OkHttp ->
    // TokenAuthenticator -> AuthRepository) and guarantees that a 401 from the refresh endpoint
    // cannot re-enter the authenticator.
    // ---------------------------------------------------------------------------------------

    @Provides
    @Singleton
    @RefreshClient
    fun provideRefreshOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @RefreshClient
    fun provideRefreshRetrofit(@RefreshClient okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTokenRefreshService(@RefreshClient retrofit: Retrofit): TokenRefreshService {
        return retrofit.create(TokenRefreshService::class.java)
    }

    // ---------------------------------------------------------------------------------------
    // Main client
    // ---------------------------------------------------------------------------------------

    @Provides
    @Singleton
    fun provideOkHttpClient(
        jwtAuthInterceptor: JwtAuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            // Add JWT interceptor first (before logging)
            .addInterceptor(jwtAuthInterceptor)
            .addInterceptor(loggingInterceptor)
            // Renews the access token and replays the request when the API answers 401.
            .authenticator(tokenAuthenticator)
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
            .baseUrl(BASE_URL)
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
