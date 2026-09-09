package org.vexteams.vexnotebooktracker.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field
import retrofit2.http.Path
import retrofit2.http.Header
import retrofit2.http.Query
import org.vexteams.vexnotebooktracker.models.VexUrlResponse
import org.vexteams.vexnotebooktracker.models.*
import com.google.gson.annotations.SerializedName
import retrofit2.http.HTTP

interface VexTeamsApiService {

    // Mobile Sign-Up Endpoints
    @POST("auth/mobile/signup")
    suspend fun mobileSignUp(@Body request: MobileSignUpRequest): Response<MobileSignUpResponse>

    // Anonymous Email Availability Check (No Authentication Required)
    @GET("auth/check-email-availability")
    suspend fun checkEmailAvailability(@Query("email") email: String): Response<EmailAvailabilityResponse>

    // Mobile JWT Authentication Endpoints
    @POST("auth/mobile/login")
    suspend fun mobileLogin(@Body request: MobileLoginRequest): Response<MobileAuthResponse>

    // auth/mobile/refresh deliberately lives on TokenRefreshService instead, on a client with no
    // auth interceptor. It also returns RefreshTokenResponse, not MobileAuthResponse - the
    // endpoint sends no userId/email/firstName/lastName/roleId.

    @POST("auth/mobile/validate-session")
    suspend fun validateSession(@Body request: SessionValidationRequest): Response<SessionValidationResponse>

    @POST("auth/mobile/logout")
    suspend fun mobileLogout(@Body request: LogoutRequest): Response<LogoutResponse>

    @POST("auth/mobile/logout-all")
    suspend fun logoutAllDevices(): Response<LogoutResponse>

    // ✅ NEW: Update Profile Endpoint
    @PUT("auth/mobile/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UpdateProfileResponse>

    // Delete Profile Endpoint
    @HTTP(method = "DELETE", path = "auth/mobile/profile", hasBody = true)
    suspend fun deleteProfile(@Body request: DeleteProfileRequest): Response<DeleteProfileResponse>

    // VEX Teams API endpoints
    @GET("url")
    suspend fun getDashboardData(): Response<List<DashboardUrlRecord>>

    @GET("urlViewLog/{urlId}")
    suspend fun getUrlViewLog(@Path("urlId") urlId: Int): Response<List<UrlViewLogEntry>>

    @GET("urlViewLogEntry/{logId}")
    suspend fun getUrlViewLogDetail(@Path("logId") logId: Int): Response<UrlViewLogDetail>

    // Check Short URL availability
    @GET("shortUrl/{shortUrl}")
    suspend fun checkShortUrl(@Path("shortUrl") shortUrl: String): Response<ShortUrlCheckResponse>

    // Get redirection types
    @GET("RedirectionType")
    suspend fun getRedirectionTypes(): Response<List<RedirectionType>>

    // Get teams
    @GET("team")
    suspend fun getTeams(): Response<List<Team>>

    // Register new URL/Notebook
    @POST("url")
    suspend fun registerUrl(@Body request: RegisterUrlRequest): Response<RegisterUrlResponse>

    @POST("auth/mobile/forgotpassword")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<ForgotPasswordResponse>

    // Send Feedback Endpoint
    @POST("feedback")
    suspend fun sendFeedback(@Body request: SendFeedbackRequest): Response<SendFeedbackResponse>

    @DELETE("url/{urlId}")
    suspend fun deleteUrl(@Path("urlId") urlId: Int): Response<DeleteUrlResponse>

    // Keep existing registration endpoints for backward compatibility
    @POST("auth/register")
    @FormUrlEncoded
    suspend fun registerUser(
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("confirmPassword") confirmPassword: String,
        @Field("firstName") firstName: String,
        @Field("lastName") lastName: String,
        @Field("teamNumber") teamNumber: String
    ): Response<AuthResponse>

    // Alternative with request body
    @POST("auth/register")
    suspend fun registerUser(
        @Body request: RegisterUserRequest
    ): Response<AuthResponse>
}

// Update Profile Request/Response Models
data class UpdateProfileRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("firstName")
    val firstName: String,
    @SerializedName("lastName")
    val lastName: String,
    @SerializedName("teamNoWithLetter")
    val teamNoWithLetter: String,
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("currentPassword")
    val currentPassword: String? = null,
    @SerializedName("newPassword")
    val newPassword: String? = null,
    @SerializedName("deviceId")
    val deviceId: String? = null,
    @SerializedName("roleId")
    val role: Int
)

data class UpdateProfileResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("firstName")
    val firstName: String,
    @SerializedName("lastName")
    val lastName: String,
    @SerializedName("teamNoWithLetter")
    val teamNoWithLetter: String,
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("roleId")
    val roleId: Int
)

// Delete Profile Request/Response Models
data class DeleteProfileRequest(
    @SerializedName("confirmationText")
    val confirmationText: String,
    @SerializedName("sessionId")
    val sessionId: String,
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("reason") // not used.
    val reason: String? = null // not used.
)

data class DeleteProfileResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("deletedAt")
    val deletedAt: String? = null
)

// Mobile JWT request/response models
data class RefreshTokenRequest(
    @SerializedName("refreshToken")
    val refreshToken: String,
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("sessionId")
    val sessionId: String
)

data class LogoutRequest(
    @SerializedName("sessionId")
    val sessionId: String,
    @SerializedName("deviceId")
    val deviceId: String
)

data class LogoutResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String
)

data class RegisterUserRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("confirmPassword")
    val confirmPassword: String,
    @SerializedName("firstName")
    val firstName: String,
    @SerializedName("lastName")
    val lastName: String,
    @SerializedName("teamNumber")
    val teamNumber: String,
    @SerializedName("role")
    val role: Int? = null,
    @SerializedName("phone")
    val phone: String? = null
)

data class AuthResponse(
    @SerializedName("token")
    val token: String,
    @SerializedName("user")
    val user: UserData,
    @SerializedName("message")
    val message: String? = null
)

data class UserData(
    @SerializedName("id")
    val id: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("firstName")
    val firstName: String,
    @SerializedName("lastName")
    val lastName: String,
    @SerializedName("teamNumber")
    val teamNumber: String
)

// Redirection Type model to match actual API response
data class RedirectionType(
    @SerializedName("redirectionTypeId")
    val redirectionTypeId: Int,
    @SerializedName("redirectionTypeName")
    val redirectionTypeName: String
)

// Team model to match actual API response
data class Team(
    @SerializedName("teamId")
    val teamId: Int,
    @SerializedName("teamName")
    val teamName: String
)

// Register URL Request
data class RegisterUrlRequest(
    @SerializedName("shortUrl")
    val shortUrl: String,
    @SerializedName("targetUrl")
    val targetUrl: String,
    @SerializedName("startDate")
    val startDate: String,           // Format: "YYYY-MM-DD HH:mm:ss"
    @SerializedName("endDate")
    val endDate: String,             // Format: "YYYY-MM-DD HH:mm:ss"
    @SerializedName("redirectionTypeId")
    val redirectionTypeId: Int,
    @SerializedName("teamId")
    val teamId: Int,
    @SerializedName("allowViewingOnMobile")
    val allowViewingOnMobile: Boolean  // true = allow, false = prevent
)

// Register URL Response
data class RegisterUrlResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("urlId")
    val urlId: Int? = null,
    @SerializedName("shortUrl")
    val shortUrl: String? = null
)

// Delete URL Response
data class DeleteUrlResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("urlId")
    val urlId: Int? = null
)

// Model that matches the new API response structure
data class DashboardUrlRecord(
    @SerializedName("urlId")
    val urlId: Int,
    @SerializedName("team")
    val team: String,
    @SerializedName("shortUrl")
    val shortUrl: String,
    @SerializedName("targetUrl")
    val targetUrl: String,
    @SerializedName("startDate")
    val startDate: String,
    @SerializedName("endDate")
    val endDate: String,
    @SerializedName("redirectionType")
    val redirectionType: String,
    @SerializedName("allowMobileView")
    val allowMobileView: String,       // "Yes" or "No"
    @SerializedName("noOfViews")
    val noOfViews: Int
) : java.io.Serializable

// URL View Log Entry (for urlViewLog/{urlId} API)
data class UrlViewLogEntry(
    @SerializedName("logId")
    val logId: Int,
    @SerializedName("date")
    val date: String,        // Format: 'MMM-dd hh:mm'
    @SerializedName("ipAddress")
    val ipAddress: String,
    @SerializedName("hostName")
    val hostName: String,
    @SerializedName("deviceType")
    val deviceType: String
) : java.io.Serializable

// URL View Log Detail (for urlViewLogEntry/{logId} API)
data class UrlViewLogDetail(
    @SerializedName("logId")
    val logId: Int,
    @SerializedName("date")
    val date: String,               // Format: 'yyyy-MM-dd hh:mm:ss'
    @SerializedName("referringUrl")
    val referringUrl: String,
    @SerializedName("ipAddress")
    val ipAddress: String,
    @SerializedName("hostName")
    val hostName: String,
    @SerializedName("deviceType")
    val deviceType: String,
    @SerializedName("operatingSystem")
    val operatingSystem: String,
    @SerializedName("browserName")
    val browserName: String,
    @SerializedName("browserVersion")
    val browserVersion: String
) : java.io.Serializable

// Add this near the other data classes:
data class ShortUrlCheckResponse(
    @SerializedName("shortUrl")
    val shortUrl: String,
    @SerializedName("available")
    val available: Boolean,
    @SerializedName("message")
    val message: String
)

data class ForgotPasswordRequest(
    @SerializedName("email")
    val email: String
)

data class ForgotPasswordResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String
)

// Send Feedback Request/Response Models
data class SendFeedbackRequest(
    @SerializedName("feedback")
    val feedback: String,
    @SerializedName("userEmail")
    val userEmail: String? = null,
    @SerializedName("userName")
    val userName: String? = null,
    @SerializedName("deviceInfo")
    val deviceInfo: String? = null,
    @SerializedName("appVersion")
    val appVersion: String? = null
)

data class SendFeedbackResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String
)