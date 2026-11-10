package com.bodepudi.vexnotebooktracker.security

import android.util.Log
import com.bodepudi.vexnotebooktracker.network.RefreshTokenRequest
import com.bodepudi.vexnotebooktracker.network.TokenRefreshService
import com.bodepudi.vexnotebooktracker.utils.DebugLogger
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one place that exchanges the stored refresh token for a new access token.
 *
 * Both [TokenAuthenticator] (on a 401) and AuthRepository.refreshToken (for an explicit refresh)
 * go through here, so there is a single implementation of the endpoint contract.
 *
 * Server-side rules this relies on (NotebookTrackerApi):
 *  - auth/mobile/refresh is AllowAnonymous, so it works after the access token has expired.
 *  - The refresh token is only rotated once it is older than 7 days; otherwise the same token is
 *    echoed back. Saving whatever comes back is therefore always correct.
 *  - The refresh token itself is valid for 30 days from session creation. Once past that - or if
 *    the session was deactivated - the endpoint answers 401 and the user has to log in again.
 */
@Singleton
class TokenRefresher @Inject constructor(
    private val refreshService: TokenRefreshService,
    private val secureStorage: SecureStorage,
    private val deviceInfoCollector: DeviceInfoCollector
) {

    companion object {
        private const val TAG = "TokenRefresher"
    }

    enum class Result {
        /** A new access token was obtained and stored. */
        SUCCESS,

        /** Nothing to refresh with - no stored refresh token or session. */
        NO_CREDENTIALS,

        /** The server rejected the refresh token. Only this case justifies forcing a re-login. */
        REJECTED,

        /** Network or server-side failure. The credentials are still presumed good. */
        TRANSIENT_FAILURE
    }

    /**
     * Performs the refresh synchronously. Callers are OkHttp background threads, never the main
     * thread.
     *
     * Synchronized so that several requests failing at once produce a single refresh rather than
     * one per request.
     */
    @Synchronized
    fun refreshBlocking(): Result {
        val refreshToken = secureStorage.getRefreshToken()
        val sessionId = secureStorage.getSessionId()

        if (refreshToken.isNullOrEmpty() || sessionId.isNullOrEmpty()) {
            Log.w(TAG, "No refresh token or session ID stored - cannot refresh")
            return Result.NO_CREDENTIALS
        }

        return try {
            val response = refreshService.refreshToken(
                RefreshTokenRequest(
                    refreshToken = refreshToken,
                    deviceId = deviceInfoCollector.getDeviceId(),
                    sessionId = sessionId
                )
            ).execute()

            val body = response.body()

            when {
                response.isSuccessful && body?.success == true && !body.accessToken.isNullOrEmpty() -> {
                    storeRefreshedSession(body.accessToken, body.refreshToken, body.sessionId ?: sessionId, body.expiresIn, body.slidingExpiration)
                    DebugLogger.d(TAG, "Access token refreshed")
                    Result.SUCCESS
                }

                response.code() == 401 -> {
                    // Refresh token expired (30-day cap), revoked, or its session deactivated.
                    Log.w(TAG, "Refresh token rejected by server - re-login required")
                    Result.REJECTED
                }

                else -> {
                    Log.w(TAG, "Token refresh failed: HTTP ${response.code()}")
                    Result.TRANSIENT_FAILURE
                }
            }
        } catch (e: IOException) {
            // Offline or timed out - the credentials are probably fine, so do not sign the user out.
            Log.w(TAG, "Token refresh failed (network): ${e.message}")
            Result.TRANSIENT_FAILURE
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during token refresh", e)
            Result.TRANSIENT_FAILURE
        }
    }

    private fun storeRefreshedSession(
        accessToken: String,
        refreshToken: String?,
        sessionId: String,
        expiresIn: Int,
        slidingExpiration: Int
    ) {
        secureStorage.saveAccessToken(accessToken)

        if (!refreshToken.isNullOrEmpty()) {
            secureStorage.saveRefreshToken(refreshToken)
        }

        // Only overwrite the session window when the server actually supplied one; a zeroed
        // slidingExpiration would make SecureStorage.isSessionValid() report "missing data".
        if (expiresIn > 0 && slidingExpiration > 0) {
            secureStorage.saveSessionInfo(sessionId, expiresIn, slidingExpiration)
        } else {
            secureStorage.updateLastActivity()
        }
    }
}
