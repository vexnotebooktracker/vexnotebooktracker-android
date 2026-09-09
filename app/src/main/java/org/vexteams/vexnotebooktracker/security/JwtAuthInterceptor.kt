// security/JwtAuthInterceptor.kt
// Location: app/src/main/java/org/vexteams/vexnotebooktracker/security/JwtAuthInterceptor.kt
package org.vexteams.vexnotebooktracker.security

import android.util.Log
import org.vexteams.vexnotebooktracker.utils.DebugLogger
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches the JWT and device headers to every authenticated request, and picks up the sliding
 * access token the API hands back.
 *
 * Renewal on a 401 is not handled here - that is [TokenAuthenticator]'s job. OkHttp invokes an
 * Authenticator only on a 401 and tracks the retry chain, which an interceptor cannot do.
 */
@Singleton
class JwtAuthInterceptor @Inject constructor(
    private val secureStorage: SecureStorage,
    private val deviceInfoCollector: DeviceInfoCollector
) : Interceptor {

    companion object {
        private const val TAG = "JwtAuthInterceptor"

        /** Response headers the API uses to hand back a renewed access token. */
        private const val HEADER_NEW_ACCESS_TOKEN = "X-New-Access-Token"
        private const val HEADER_TOKEN_REFRESH = "X-Token-Refresh"

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

        val response = chain.proceed(authenticatedRequest)

        captureSlidingToken(response)

        // A 401 that reaches here is passed straight through to TokenAuthenticator, which
        // refreshes the token and replays the request.
        return response
    }

    /**
     * Stores the renewed access token the API sends once a session is within five minutes of its
     * 30-minute inactivity timeout.
     *
     * The server mints it in JwtAuthAttribute and BaseApiController returns it as
     * X-New-Access-Token / X-Token-Refresh on both success and error responses. Applying it keeps
     * an actively used session alive indefinitely, so the 401 path is rarely reached at all.
     */
    private fun captureSlidingToken(response: Response) {
        val newToken = response.header(HEADER_NEW_ACCESS_TOKEN)
        if (newToken.isNullOrEmpty()) return

        if (newToken == secureStorage.getAccessToken()) return

        secureStorage.saveAccessToken(newToken)
        // The server only issues this because it just extended the session server-side; mirror
        // that locally so SecureStorage.isSessionValid() agrees with the server.
        secureStorage.updateLastActivity()

        DebugLogger.d(
            TAG,
            "Applied sliding access token from $HEADER_NEW_ACCESS_TOKEN " +
                "(${HEADER_TOKEN_REFRESH}=${response.header(HEADER_TOKEN_REFRESH)})"
        )
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
