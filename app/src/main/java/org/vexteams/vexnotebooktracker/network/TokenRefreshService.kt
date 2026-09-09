package org.vexteams.vexnotebooktracker.network

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import javax.inject.Qualifier

/**
 * Marks the Retrofit/OkHttp instances used only for token refresh.
 *
 * The refresh call deliberately runs on its own bare client - no [org.vexteams.vexnotebooktracker
 * .security.JwtAuthInterceptor] and no authenticator. That breaks the dependency cycle
 * (AuthRepository -> VexTeamsApiService -> Retrofit -> OkHttp -> authenticator -> AuthRepository)
 * and, just as importantly, stops a 401 from the refresh endpoint itself from re-entering the
 * authenticator and looping.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshClient

/**
 * The single refresh endpoint, POST auth/mobile/refresh.
 *
 * Deliberately a blocking [Call] rather than a suspend function: OkHttp's `Authenticator` runs
 * synchronously on a background OkHttp thread and needs the answer before it can rebuild the
 * request.
 */
interface TokenRefreshService {

    @POST("auth/mobile/refresh")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<RefreshTokenResponse>
}

/**
 * Response shape of auth/mobile/refresh.
 *
 * The API returns its own RefreshTokenResponse here, not the full login payload - there is no
 * userId/email/firstName/lastName/roleId on this endpoint. Modelling it separately keeps Gson from
 * leaving those non-null fields of MobileAuthResponse as null.
 *
 * Every field is optional because a failure response carries only success and message.
 */
data class RefreshTokenResponse(
    @SerializedName("success")
    val success: Boolean = false,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("accessToken")
    val accessToken: String? = null,
    @SerializedName("refreshToken")
    val refreshToken: String? = null,
    @SerializedName("sessionId")
    val sessionId: String? = null,
    @SerializedName("expiresIn")
    val expiresIn: Int = 0,
    @SerializedName("slidingExpiration")
    val slidingExpiration: Int = 0
)
