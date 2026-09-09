package org.vexteams.vexnotebooktracker.security

import android.util.Log
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.vexteams.vexnotebooktracker.utils.DebugLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renews the access token when the API answers 401, then replays the failed request.
 *
 * An OkHttp [Authenticator] rather than an interceptor: OkHttp only invokes it on a 401, hands it
 * the failed response, and exposes [Response.priorResponse] so repeated failures can be detected
 * and the retry loop cut off.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val secureStorage: SecureStorage,
    private val tokenRefresher: TokenRefresher
) : Authenticator {

    companion object {
        private const val TAG = "TokenAuthenticator"

        /** One refresh, one replay. Beyond that the 401 is real. */
        private const val MAX_ATTEMPTS = 2

        /**
         * Endpoints that never carry an access token. A 401 from any of these is a genuine
         * credential failure, not an expired token.
         */
        private val EXCLUDED_PATHS = listOf(
            "/auth/mobile/login",
            "/auth/mobile/refresh",
            "/auth/mobile/signup",
            "/auth/register"
        )
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath

        if (EXCLUDED_PATHS.any { path.contains(it) }) {
            DebugLogger.d(TAG, "401 on unauthenticated endpoint - not refreshing: $path")
            return null
        }

        if (attemptCount(response) >= MAX_ATTEMPTS) {
            Log.w(TAG, "Still 401 after refresh and replay - giving up on $path")
            return null
        }

        // The token this particular request was sent with. If it no longer matches what is
        // stored, another thread has already refreshed while this request was in flight.
        val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")

        synchronized(this) {
            val currentToken = secureStorage.getAccessToken()

            if (!currentToken.isNullOrEmpty() && currentToken != failedToken) {
                DebugLogger.d(TAG, "Token already refreshed by another request - replaying")
                return response.request.signedWith(currentToken)
            }

            return when (tokenRefresher.refreshBlocking()) {
                TokenRefresher.Result.SUCCESS -> {
                    val newToken = secureStorage.getAccessToken()
                    if (newToken.isNullOrEmpty()) {
                        Log.e(TAG, "Refresh reported success but no token was stored")
                        null
                    } else {
                        DebugLogger.d(TAG, "Replaying $path with refreshed token")
                        response.request.signedWith(newToken)
                    }
                }

                TokenRefresher.Result.REJECTED,
                TokenRefresher.Result.NO_CREDENTIALS -> {
                    // The refresh token is gone or the server refused it - the session is over.
                    // clearAuthData() rather than clearAll(): the device binding and cached
                    // profile stay put so the next login is not treated as a brand new device.
                    Log.w(TAG, "Session cannot be renewed - clearing stored credentials")
                    secureStorage.clearAuthData()
                    null
                }

                TokenRefresher.Result.TRANSIENT_FAILURE -> {
                    // Offline or a server error. Keep the credentials; the next call can retry.
                    Log.w(TAG, "Token refresh failed transiently - keeping credentials")
                    null
                }
            }
        }
    }

    /** Number of times this request has been attempted, including the original. */
    private fun attemptCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    /**
     * Swaps in the new bearer token. Only the Authorization header is touched: the authenticator
     * runs below the application interceptors, so JwtAuthInterceptor does not run again for the
     * replay, but the X-Device-ID / X-App-Version headers it already added are still on the
     * request and must not be duplicated.
     */
    private fun Request.signedWith(token: String): Request = newBuilder()
        .removeHeader("Authorization")
        .addHeader("Authorization", "Bearer $token")
        .build()
}
