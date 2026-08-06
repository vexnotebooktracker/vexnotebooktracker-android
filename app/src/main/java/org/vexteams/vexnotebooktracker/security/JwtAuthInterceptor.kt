// NEW FILE: security/JwtAuthInterceptor.kt
// Location: app/src/main/java/org/vexteams/vexnotebooktracker/security/JwtAuthInterceptor.kt
package org.vexteams.vexnotebooktracker.security

import android.util.Log
import org.vexteams.vexnotebooktracker.utils.DebugLogger
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import org.vexteams.vexnotebooktracker.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JwtAuthInterceptor @Inject constructor(
    private val secureStorage: SecureStorage,
    private val deviceInfoCollector: DeviceInfoCollector
) : Interceptor {

    companion object {
        private const val TAG = "JwtAuthInterceptor"
        private val EXCLUDED_PATHS = listOf(
            "/auth/mobile/login",
            "/auth/mobile/refresh",
            "/auth/register"
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()

        // Skip auth for login and registration endpoints
        if (EXCLUDED_PATHS.any { url.contains(it) }) {
            DebugLogger.d(TAG, "Skipping auth for: $url")
            return chain.proceed(originalRequest)
        }

        // Get current access token
        val accessToken = secureStorage.getAccessToken()

        if (accessToken == null) {
            Log.w(TAG, "No access token available for: $url")
            return chain.proceed(originalRequest)
        }

        // Add JWT token to request
        val authenticatedRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("X-Device-ID", deviceInfoCollector.getDeviceId())
            .addHeader("X-App-Version", getAppVersion())
            .build()

        DebugLogger.d(TAG, "Added JWT token to request: $url")

        // Execute request
        val response = chain.proceed(authenticatedRequest)

        // Handle token expiration (401 Unauthorized)
        if (response.code == 401) {
            Log.w(TAG, "Received 401 - attempting token refresh")

            // Try to refresh token
            val refreshed = attemptTokenRefresh()

            if (refreshed) {
                // Retry original request with new token
                val newToken = secureStorage.getAccessToken()
                if (newToken != null) {
                    val retryRequest = originalRequest.newBuilder()
                        .removeHeader("Authorization")
                        .addHeader("Authorization", "Bearer $newToken")
                        .addHeader("X-Device-ID", deviceInfoCollector.getDeviceId())
                        .addHeader("X-App-Version", getAppVersion())
                        .build()

                    response.close() // Close original response
                    DebugLogger.d(TAG, "Retrying request with refreshed token")
                    return chain.proceed(retryRequest)
                }
            } else {
                Log.e(TAG, "Token refresh failed - user needs to login again")
                // Clear invalid tokens
                secureStorage.clearAll()
            }
        }

        return response
    }

    private fun attemptTokenRefresh(): Boolean {
        return try {
            val refreshToken = secureStorage.getRefreshToken()
            val sessionId = secureStorage.getSessionId()

            if (refreshToken == null || sessionId == null) {
                Log.w(TAG, "Missing refresh token or session ID")
                return false
            }

            // Note: This is a simplified approach. In production, you'd want to:
            // 1. Inject AuthRepository properly or use a separate refresh service
            // 2. Handle this asynchronously to avoid blocking the interceptor
            // 3. Implement proper synchronization for concurrent requests

            runBlocking {
                // This is a placeholder - you'll need to implement proper refresh logic
                // by either injecting AuthRepository or creating a separate RefreshService
                DebugLogger.d(TAG, "Token refresh would be attempted here")
                // For now, return false to force re-login
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during token refresh", e)
            false
        }
    }

    private fun getAppVersion(): String {
        return try {
            // Get app version from BuildConfig or PackageManager
            org.vexteams.vexnotebooktracker.BuildConfig.VERSION_NAME
        } catch (e: Exception) {
            "unknown"
        }
    }
}