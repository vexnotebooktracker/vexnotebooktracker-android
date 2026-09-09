package org.vexteams.vexnotebooktracker.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import org.vexteams.vexnotebooktracker.utils.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.vexteams.vexnotebooktracker.models.*
import org.vexteams.vexnotebooktracker.network.*
import org.vexteams.vexnotebooktracker.security.SecureStorage
import org.vexteams.vexnotebooktracker.security.DeviceInfoCollector
import org.vexteams.vexnotebooktracker.security.TokenRefresher
import org.vexteams.vexnotebooktracker.utils.TimeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.security.MessageDigest
import java.util.UUID
import org.vexteams.vexnotebooktracker.network.ForgotPasswordRequest
import org.vexteams.vexnotebooktracker.network.ForgotPasswordResponse

sealed class Resource<T> {
    class Loading<T> : Resource<T>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error<T>(val message: String, val exception: Throwable? = null) : Resource<T>()
}

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vexTeamsApiService: VexTeamsApiService,
    private val secureStorage: SecureStorage,
    private val deviceInfoCollector: DeviceInfoCollector,
    private val tokenRefresher: TokenRefresher
) {

    companion object {
        private const val TAG = "AuthRepository"
    }

    // NEW: Update Profile Method
    suspend fun updateProfile(
        email: String,
        firstName: String,
        lastName: String,
        role: Int,
        teamNoWithLetter: String,
        phone: String? = null,
        currentPassword: String? = null,
        newPassword: String? = null
    ): Flow<Resource<UpdateProfileResponse>> = flow {
        try {
            emit(Resource.Loading())

            DebugLogger.d(TAG, "Starting profile update process")

            // Get session info
            val sessionId = secureStorage.getSessionId()
            val deviceId = deviceInfoCollector.getDeviceId()

            if (sessionId == null) {
                emit(Resource.Error("No active session. Please login again."))
                return@flow
            }

            // Create update profile request
            val updateRequest = UpdateProfileRequest(
                email = email,
                firstName = firstName,
                lastName = lastName,
                teamNoWithLetter = teamNoWithLetter,
                phone = phone,
                currentPassword = currentPassword,
                newPassword = newPassword,
                deviceId = deviceId,
                role = role
            )

            DebugLogger.d(TAG, "Sending update profile request to server")

            // Call update profile API
            val response = vexTeamsApiService.updateProfile(updateRequest)

            if (response.isSuccessful && response.body() != null) {
                val updateResponse = response.body()!!

                if (updateResponse.success) {
                    DebugLogger.d(TAG, "Profile update successful: ${updateResponse.message}")

                    // UPDATED: Save user profile with phone and role
                    secureStorage.saveUserProfile(
                        userId = secureStorage.getUserId(),
                        email = updateResponse.email,
                        teamNumber = updateResponse.teamNoWithLetter,
                        firstName = updateResponse.firstName,
                        lastName = updateResponse.lastName,
                        phone = updateResponse.phone,
                        roleId = role // Use the role that was sent in request
                    )

                    emit(Resource.Success(updateResponse))
                } else {
                    DebugLogger.d(TAG, "Profile update failed: ${updateResponse.message}")
                    emit(Resource.Error(updateResponse.message))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = when (response.code()) {
                    400 -> "Invalid request. Please check your input data."
                    401 -> "Session expired. Please login again."
                    403 -> "Not authorized to update this profile."
                    404 -> "Profile not found."
                    409 -> "Email address is already registered to another account."
                    500 -> "Server error. Please try again later."
                    else -> "Failed to update profile: ${response.code()} - $errorBody"
                }

                DebugLogger.d(TAG, "Update profile API error: $errorMessage")
                emit(Resource.Error(errorMessage))
            }

        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Update profile timeout", e)
            emit(Resource.Error("Request timed out. Please check your internet connection and try again."))
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Update profile network error - no internet", e)
            emit(Resource.Error("No internet connection. Please check your network and try again."))
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Update profile network error", e)
            emit(Resource.Error("Network error. Please check your internet connection and try again."))
        } catch (e: Exception) {
            Log.e(TAG, "Update profile unexpected error", e)
            emit(Resource.Error("An unexpected error occurred. Please try again."))
        }
    }

    // NEW: Delete Profile Method
    suspend fun deleteProfile(confirmationText: String): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())

            DebugLogger.d(TAG, "Starting profile deletion process")

            // Validate confirmation text
            if (confirmationText.trim().uppercase() != "DELETE") {
                emit(Resource.Error("Invalid confirmation. Please type 'DELETE' to confirm."))
                return@flow
            }

            // Get session info
            val sessionId = secureStorage.getSessionId()
            val deviceId = deviceInfoCollector.getDeviceId()

            if (sessionId == null) {
                emit(Resource.Error("No active session. Please login again."))
                return@flow
            }

            // Create delete profile request
            val deleteRequest = DeleteProfileRequest(
                confirmationText = confirmationText.trim().uppercase(),
                sessionId = sessionId,
                deviceId = deviceId,
                reason = "User requested account deletion"
            )

            DebugLogger.d(TAG, "Sending delete profile request to server")

            // Call delete profile API
            val response = vexTeamsApiService.deleteProfile(deleteRequest)

            if (response.isSuccessful && response.body() != null) {
                val deleteResponse = response.body()!!

                if (deleteResponse.success) {
                    DebugLogger.d(TAG, "Profile deletion successful: ${deleteResponse.message}")

                    // Clear all local data immediately
                    secureStorage.clearAll()

                    emit(Resource.Success(deleteResponse.message))
                } else {
                    DebugLogger.d(TAG, "Profile deletion failed: ${deleteResponse.message}")
                    emit(Resource.Error(deleteResponse.message))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = when (response.code()) {
                    400 -> "Invalid request. Please check your confirmation text."
                    401 -> "Session expired. Please login again."
                    403 -> "Not authorized to delete this profile."
                    404 -> "Profile not found."
                    409 -> "Profile cannot be deleted at this time. Please try again later."
                    500 -> "Server error. Please try again later."
                    else -> "Failed to delete profile: ${response.code()} - $errorBody"
                }

                DebugLogger.d(TAG, "Delete profile API error: $errorMessage")
                emit(Resource.Error(errorMessage))
            }

        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Delete profile timeout", e)
            emit(Resource.Error("Request timed out. Please check your internet connection and try again."))
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Delete profile network error - no internet", e)
            emit(Resource.Error("No internet connection. Please check your network and try again."))
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Delete profile network error", e)
            emit(Resource.Error("Network error. Please check your internet connection and try again."))
        } catch (e: Exception) {
            Log.e(TAG, "Delete profile unexpected error", e)
            emit(Resource.Error("An unexpected error occurred. Please try again."))
        }
    }

    // Mobile Sign-Up Method
    suspend fun signUp(
        email: String,
        password: String,
        confirmPassword: String,
        firstName: String,
        lastName: String,
        teamNoWithLetter: String,
        role: Int,
        phone: String? = null
    ): Flow<Resource<MobileSignUpResponse>> = flow {
        try {
            emit(Resource.Loading())

            Log.d(TAG, "Starting mobile sign-up for: $email")

            // Validate device security
            if (!isDeviceSecure()) {
                emit(Resource.Error("Device security check failed"))
                return@flow
            }

            // Create mobile sign-up request with device info
            val signUpRequest = createMobileSignUpRequest(
                email, password, confirmPassword, firstName, lastName, role, teamNoWithLetter, phone
            )

            Log.d(TAG, "Created mobile sign-up request with device info")

            // Call mobile sign-up API
            val response = vexTeamsApiService.mobileSignUp(signUpRequest)

            if (response.isSuccessful && response.body() != null) {
                val signUpResponse = response.body()!!

                if (signUpResponse.success) {
                    Log.d(TAG, "Sign-up successful for: $email")
                    emit(Resource.Success(signUpResponse))
                } else {
                    Log.e(TAG, "Sign-up failed: ${signUpResponse.message}")
                    emit(Resource.Error(signUpResponse.message))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = "Sign-up failed: ${response.code()} - $errorBody"
                Log.e(TAG, errorMessage)
                emit(Resource.Error(errorMessage))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Sign-up error", e)
            emit(Resource.Error("Sign-up error: ${e.message}", e))
        }
    }

    // Email Availability Check
    suspend fun checkEmailAvailability(email: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())

            DebugLogger.d(TAG, "Checking email availability: $email")

            val response = vexTeamsApiService.checkEmailAvailability(email)

            if (response.isSuccessful && response.body() != null) {
                val availabilityResponse = response.body()!!

                Log.d(TAG, "Email availability check result: ${availabilityResponse.available}")
                emit(Resource.Success(availabilityResponse.available))
            } else {
                val errorMessage = "Email availability check failed: ${response.code()}"
                Log.e(TAG, errorMessage)
                emit(Resource.Error(errorMessage))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Email availability check error", e)
            emit(Resource.Error("Email availability check error: ${e.message}", e))
        }
    }

    //  Real JWT Mobile Login
//  Real JWT Mobile Login with improved error handling
    suspend fun mobileLogin(email: String, password: String): Flow<Resource<List<DashboardUrlRecord>>> = flow {
        try {
            emit(Resource.Loading())

            DebugLogger.d(TAG, "Starting mobile JWT login for: $email")

            // Validate device security
            if (!isDeviceSecure()) {
                emit(Resource.Error("Device security check failed"))
                return@flow
            }

            // Create mobile login request
            val loginRequest = createMobileLoginRequest(email, password)

            DebugLogger.d(TAG, "Created mobile login request with device info")

            // Call mobile login API
            val response = vexTeamsApiService.mobileLogin(loginRequest)

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!

                if (authResponse.success) {
                    // Save JWT tokens and session info
                    secureStorage.saveAccessToken(authResponse.accessToken)
                    secureStorage.saveRefreshToken(authResponse.refreshToken)
                    secureStorage.saveSessionInfo(
                        authResponse.sessionId,
                        authResponse.expiresIn,
                        authResponse.slidingExpiration
                    )

                    // UPDATED: Save user profile with phone and roleId
                    secureStorage.saveUserProfile(
                        authResponse.userId, //.toString(),
                        authResponse.email,
                        authResponse.teamNoWithLetter,
                        authResponse.firstName,
                        authResponse.lastName,
                        // ADD: Pass phone and role to saveUserProfile
                        authResponse.phone ?: "",
                        authResponse.roleId ?: 2
                    )

                    // Save device binding
                    secureStorage.saveDeviceBinding(
                        loginRequest.deviceId,
                        loginRequest.appInstanceId,
                        loginRequest.deviceFingerprint
                    )

                    DebugLogger.d(TAG, "JWT authentication successful, fetching dashboard data")

                    // Now fetch VEX Teams data with authenticated session
                    val dashboardResponse = vexTeamsApiService.getDashboardData()

                    if (dashboardResponse.isSuccessful && dashboardResponse.body() != null) {
                        val vexData = dashboardResponse.body()!!

                        // Update last activity with GMT timestamp
                        secureStorage.updateLastActivity()

                        DebugLogger.d(TAG, "Successfully fetched ${vexData.size} VEX dashboard records")
                        emit(Resource.Success(vexData))
                    } else {
                        emit(Resource.Error("Failed to fetch VEX data: ${dashboardResponse.code()}"))
                    }
                } else {
                    // IMPROVED: Handle server response with success=false
                    val errorMessage = authResponse.message.takeIf { !it.isNullOrBlank() }
                        ?: "Login failed. Please check your credentials."
                    Log.e(TAG, "Login failed - server response: $errorMessage")
                    emit(Resource.Error(errorMessage))
                }
            } else {
                // IMPROVED: Handle HTTP error responses with user-friendly messages
                val errorMessage = when (response.code()) {
                    400 -> {
                        // Try to parse error body for more specific message
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Login 400 error body: $errorBody")

                        // Check if it's a validation error or credential issue
                        when {
                            errorBody?.contains("email", ignoreCase = true) == true ->
                                "Please enter a valid email address"
                            errorBody?.contains("password", ignoreCase = true) == true ->
                                "Password is required"
                            errorBody?.contains("invalid", ignoreCase = true) == true ||
                                    errorBody?.contains("credential", ignoreCase = true) == true ->
                                "Invalid email or password. Please try again."
                            else -> "Invalid login information. Please check your email and password."
                        }
                    }
                    401 -> "Invalid email or password. Please check your credentials and try again."
                    403 -> "Your account access has been restricted. Please contact support."
                    404 -> "Account not found. Please check your email or sign up for a new account."
                    429 -> "Too many login attempts. Please wait a few minutes and try again."
                    500 -> "Server error. Please try again later."
                    503 -> "Service temporarily unavailable. Please try again later."
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Login failed with code ${response.code()}: $errorBody")
                        "Login failed. Please check your internet connection and try again."
                    }
                }

                emit(Resource.Error(errorMessage))
            }

        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Login timeout", e)
            emit(Resource.Error("Request timed out. Please check your internet connection and try again."))
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Login network error - no internet", e)
            emit(Resource.Error("No internet connection. Please check your network and try again."))
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Login network error", e)
            emit(Resource.Error("Network error. Please check your internet connection and try again."))
        } catch (e: Exception) {
            Log.e(TAG, "Login unexpected error", e)
            emit(Resource.Error("An unexpected error occurred. Please try again."))
        }
    }

    // Helper Methods for Sign-Up
    private suspend fun createMobileSignUpRequest(
        email: String,
        password: String,
        confirmPassword: String,
        firstName: String,
        lastName: String,
        role: Int,
        teamNoWithLetter: String,
        phone: String?
    ): MobileSignUpRequest {
        val deviceInfo = deviceInfoCollector.getDeviceInfo()
        val deviceId = deviceInfoCollector.getDeviceId()
        val appSignature = deviceInfoCollector.getAppSignature()
        val deviceFingerprint = createDeviceFingerprint(deviceInfo, deviceId, appSignature)

        return MobileSignUpRequest(
            email = email,
            password = password,
            confirmPassword = confirmPassword,
            firstName = firstName,
            lastName = lastName,
            role = role,
            teamNoWithLetter = teamNoWithLetter,
            phone = phone,
            deviceId = deviceId,
            appInstanceId = getAppInstanceId(),
            deviceModel = "${deviceInfo["brand"]} ${deviceInfo["model"]}",
            osVersion = "Android ${deviceInfo["osVersion"]}",
            appVersion = getAppVersion(),
            appSignature = appSignature,
            isDeviceRooted = isDeviceRooted(),
            biometricEnabled = isBiometricAvailable(),
            ipAddress = getCurrentIP(),
            networkType = getNetworkType(),
            deviceFingerprint = deviceFingerprint
        )
    }

    // Session Validation with proper GMT time handling
    suspend fun validateAndRefreshSession(): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())

            // Check local session validity first
            if (!secureStorage.isSessionValid()) {
                DebugLogger.d(TAG, "Local session expired")
                emit(Resource.Error("Session expired locally"))
                return@flow
            }

            val sessionId = secureStorage.getSessionId()
            val deviceId = deviceInfoCollector.getDeviceId()

            if (sessionId == null) {
                emit(Resource.Error("No active session"))
                return@flow
            }

            // Validate device binding
            if (!secureStorage.validateDeviceBinding(deviceId, getAppInstanceId())) {
                secureStorage.recordSecurityEvent("DEVICE_BINDING_VIOLATION", "Device ID mismatch")
                emit(Resource.Error("Device binding violation"))
                return@flow
            }

            // UPDATED: Create session validation request with GMT timestamp
            val validationRequest = SessionValidationRequest(
                sessionId = sessionId,
                deviceId = deviceId,
                appInstanceId = getAppInstanceId(),
                ipAddress = getCurrentIP(),
                lastActivity = TimeUtils.getCurrentTimestamp()  // Use TimeUtils for consistent timestamp
            )

            // Call server validation
            val response = vexTeamsApiService.validateSession(validationRequest)

            if (response.isSuccessful && response.body() != null) {
                val validationResponse = response.body()!!

                when (validationResponse.action) {
                    "continue" -> {
                        // Update last activity for sliding expiration
                        secureStorage.updateLastActivity()

                        // If server provided new token, update it
                        if (validationResponse.shouldRefreshToken && validationResponse.newToken != null) {
                            secureStorage.saveAccessToken(validationResponse.newToken)
                            Log.d(TAG, "Token refreshed during session validation")
                        }

                        emit(Resource.Success(true))
                    }
                    "login_required" -> {
                        secureStorage.clearAuthData()
                        emit(Resource.Error("Session expired - login required"))
                    }
                    "security_violation" -> {
                        secureStorage.recordSecurityEvent("SECURITY_VIOLATION", validationResponse.reason ?: "Unknown")
                        secureStorage.clearAll()
                        emit(Resource.Error("Security violation detected"))
                    }
                    else -> {
                        emit(Resource.Error("Unknown validation response: ${validationResponse.action}"))
                    }
                }
            } else {
                emit(Resource.Error("Session validation failed: ${response.code()}"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Session validation error", e)
            emit(Resource.Error("Session validation error: ${e.message}", e))
        }
    }

    // Token Refresh
    //
    // Delegates to TokenRefresher so this and TokenAuthenticator share one implementation of the
    // auth/mobile/refresh contract. Routine expiry no longer needs this: JwtAuthInterceptor
    // applies the sliding token the API returns, and TokenAuthenticator renews on a 401. This
    // remains for an explicit, user-initiated refresh.
    suspend fun refreshToken(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())

        when (withContext(Dispatchers.IO) { tokenRefresher.refreshBlocking() }) {
            TokenRefresher.Result.SUCCESS -> {
                DebugLogger.d(TAG, "Token refresh successful")
                emit(Resource.Success(true))
            }

            TokenRefresher.Result.NO_CREDENTIALS ->
                emit(Resource.Error("Missing refresh token or session"))

            TokenRefresher.Result.REJECTED ->
                emit(Resource.Error("Session expired - please log in again"))

            TokenRefresher.Result.TRANSIENT_FAILURE ->
                emit(Resource.Error("Token refresh failed - please try again"))
        }
    }

    // Mobile Logout
    suspend fun mobileLogout(): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())

            val sessionId = secureStorage.getSessionId()
            val deviceId = deviceInfoCollector.getDeviceId()

            if (sessionId != null) {
                val logoutRequest = LogoutRequest(
                    sessionId = sessionId,
                    deviceId = deviceId
                )

                try {
                    val response = vexTeamsApiService.mobileLogout(logoutRequest)
                    if (response.isSuccessful) {
                        DebugLogger.d(TAG, "Server logout successful")
                    } else {
                        Log.w(TAG, "Server logout failed: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Server logout error (continuing with local logout)", e)
                }
            }

            // Always clear local data
            secureStorage.clearAll()
            DebugLogger.d(TAG, "Local logout completed")

            emit(Resource.Success(true))

        } catch (e: Exception) {
            Log.e(TAG, "Logout error", e)
            emit(Resource.Error("Logout error: ${e.message}", e))
        }
    }

    // Register URL with GMT time conversion
    suspend fun registerUrl(
        shortUrl: String,
        targetUrl: String,
        startYear: Int, startMonth: Int, startDay: Int, startHour: Int, startMinute: Int,
        endYear: Int, endMonth: Int, endDay: Int, endHour: Int, endMinute: Int,
        redirectionTypeId: Int,
        teamId: Int,
        allowViewingOnMobile: Boolean
    ): Flow<Resource<RegisterUrlResponse>> = flow {
        try {
            emit(Resource.Loading())

            // Convert user input to GMT for server
            val startDateGmt = TimeUtils.userInputToGmt(startYear, startMonth, startDay, startHour, startMinute)
            val endDateGmt = TimeUtils.userInputToGmt(endYear, endMonth, endDay, endHour, endMinute)

            DebugLogger.d(TAG, "Converting times for registration:")
            DebugLogger.d(TAG, "   Start: Local input -> $startDateGmt GMT")
            DebugLogger.d(TAG, "   End: Local input -> $endDateGmt GMT")

            val request = RegisterUrlRequest(
                shortUrl = shortUrl,
                targetUrl = targetUrl,
                startDate = startDateGmt,  // Send GMT to server
                endDate = endDateGmt,      // Send GMT to server
                redirectionTypeId = redirectionTypeId,
                teamId = teamId,
                allowViewingOnMobile = allowViewingOnMobile
            )

            val response = vexTeamsApiService.registerUrl(request)

            if (response.isSuccessful && response.body() != null) {
                val registerResponse = response.body()!!
                DebugLogger.d(TAG, "URL registration successful: ${registerResponse.message}")
                emit(Resource.Success(registerResponse))
            } else {
                val errorBody = response.errorBody()?.string()
                emit(Resource.Error("Registration failed: ${response.code()} - $errorBody"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "URL registration error", e)
            emit(Resource.Error("Registration error: ${e.message}", e))
        }
    }

    // Helper Methods
    private suspend fun createMobileLoginRequest(email: String, password: String): MobileLoginRequest {
        val deviceInfo = deviceInfoCollector.getDeviceInfo()
        val deviceId = deviceInfoCollector.getDeviceId()
        val appSignature = deviceInfoCollector.getAppSignature()
        val deviceFingerprint = createDeviceFingerprint(deviceInfo, deviceId, appSignature)

        return MobileLoginRequest(
            email = email,
            password = password,
            deviceId = deviceId,
            appInstanceId = getAppInstanceId(),
            deviceModel = "${deviceInfo["brand"]} ${deviceInfo["model"]}",
            osVersion = "Android ${deviceInfo["osVersion"]}",
            appVersion = getAppVersion(),
            appSignature = appSignature,
            isDeviceRooted = isDeviceRooted(),
            biometricEnabled = isBiometricAvailable(),
            ipAddress = getCurrentIP(),
            networkType = getNetworkType(),
            deviceFingerprint = deviceFingerprint
        )
    }

    private fun createDeviceFingerprint(
        deviceInfo: Map<String, String>,
        deviceId: String,
        appSignature: String
    ): String {
        val fingerprintData = "${deviceInfo["manufacturer"]}-${deviceInfo["model"]}-" +
                "${deviceInfo["product"]}-$deviceId-$appSignature"

        return MessageDigest.getInstance("SHA-256")
            .digest(fingerprintData.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun isDeviceSecure(): Boolean {
        // Add device security checks here
        // For now, just check if device is not rooted
        return !isDeviceRooted()
    }

    private fun isDeviceRooted(): Boolean {
        // Implement root detection logic
        // This is a simplified check
        return java.io.File("/system/app/Superuser.apk").exists() ||
                java.io.File("/sbin/su").exists() ||
                java.io.File("/system/bin/su").exists() ||
                java.io.File("/system/xbin/su").exists()
    }

    private fun isBiometricAvailable(): Boolean {
        // This would require BiometricManager dependency
        // For now, return false
        return false
    }

    private fun getAppInstanceId(): String {
        // In real implementation, use Firebase Instance ID or similar
        // For now, generate a UUID and persist it
        var instanceId = secureStorage.getBoundAppInstanceId()
        if (instanceId == null) {
            instanceId = UUID.randomUUID().toString()
        }
        return instanceId
    }

    private fun getCurrentIP(): String {
        // This would require network call to external service
        // For now, return placeholder
        return "0.0.0.0"
    }

    private fun getNetworkType(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        return when {
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
            else -> "Unknown"
        }
    }

    private fun getAppVersion(): String {
        return try {
            org.vexteams.vexnotebooktracker.BuildConfig.VERSION_NAME
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    // LEGACY: Keep existing methods for backward compatibility
    suspend fun login(email: String, password: String): Flow<Resource<List<DashboardUrlRecord>>> {
        // Redirect to mobile login
        return mobileLogin(email, password)
    }

    // Forgot Password with better timeout error handling
    suspend fun forgotPassword(email: String): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())

            val request = ForgotPasswordRequest(email = email)
            val response = vexTeamsApiService.forgotPassword(request)

            if (response.isSuccessful) {
                val forgotPasswordResponse = response.body()
                if (forgotPasswordResponse?.success == true) {
                    emit(Resource.Success(forgotPasswordResponse.message))
                } else {
                    emit(Resource.Error(forgotPasswordResponse?.message ?: "Password reset failed"))
                }
            } else {
                // Handle HTTP error responses
                when (response.code()) {
                    404 -> emit(Resource.Error("Email address not found"))
                    400 -> emit(Resource.Error("Invalid email address"))
                    408 -> emit(Resource.Error("Request timeout. The email may still be sent, please check your inbox."))
                    429 -> emit(Resource.Error("Too many requests. Please try again later"))
                    else -> emit(Resource.Error("Password reset failed. Please try again"))
                }
            }
        } catch (e: java.net.SocketTimeoutException) {
            // Handle both socket and connection timeouts
            Log.w(TAG, "Forgot password request timed out", e)
            emit(Resource.Error("Request is taking longer than expected. The reset email may still be sent, please check your inbox in a few minutes."))
        } catch (e: java.io.IOException) {
            // Handle network connection issues (includes ConnectException)
            Log.w(TAG, "Forgot password network error", e)
            when {
                e.message?.contains("timeout", ignoreCase = true) == true -> {
                    emit(Resource.Error("Connection timeout. The email may still be processing, please check your inbox shortly."))
                }
                e.message?.contains("connect", ignoreCase = true) == true -> {
                    emit(Resource.Error("Unable to connect to server. Please check your internet connection."))
                }
                else -> {
                    emit(Resource.Error("Network error. Please check your connection and try again."))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Forgot password error", e)
            emit(Resource.Error("Network error. Please check your connection and try again."))
        }
    }

    // Send Feedback
    suspend fun sendFeedback(feedback: String): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())

            DebugLogger.d(TAG, "Sending feedback to server")

            // Get user info if logged in
            val userEmail = secureStorage.getUserEmail()
            val firstName = secureStorage.getFirstName()
            val lastName = secureStorage.getLastName()
            val userName = if (firstName != null && lastName != null) {
                "$firstName $lastName"
            } else {
                null
            }

            // Get device info
            val deviceInfo = deviceInfoCollector.getDeviceInfo()
            val deviceInfoString = "${deviceInfo["brand"]} ${deviceInfo["model"]} - Android ${deviceInfo["osVersion"]}"
            val appVersion = getAppVersion()

            // Create feedback request
            val request = SendFeedbackRequest(
                feedback = feedback,
                userEmail = userEmail,
                userName = userName,
                deviceInfo = deviceInfoString,
                appVersion = appVersion
            )

            val response = vexTeamsApiService.sendFeedback(request)

            if (response.isSuccessful && response.body() != null) {
                val feedbackResponse = response.body()!!
                if (feedbackResponse.success) {
                    DebugLogger.d(TAG, "Feedback sent successfully: ${feedbackResponse.message}")
                    emit(Resource.Success(feedbackResponse.message))
                } else {
                    DebugLogger.d(TAG, "Feedback sending failed: ${feedbackResponse.message}")
                    emit(Resource.Error(feedbackResponse.message))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = when (response.code()) {
                    400 -> "Invalid feedback. Please try again."
                    429 -> "Too many requests. Please try again later."
                    500 -> "Server error. Please try again later."
                    else -> "Failed to send feedback: ${response.code()} - $errorBody"
                }
                DebugLogger.d(TAG, "Feedback API error: $errorMessage")
                emit(Resource.Error(errorMessage))
            }

        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Feedback timeout", e)
            emit(Resource.Error("Request timed out. Please check your internet connection and try again."))
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Feedback network error - no internet", e)
            emit(Resource.Error("No internet connection. Please check your network and try again."))
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Feedback network error", e)
            emit(Resource.Error("Network error. Please check your internet connection and try again."))
        } catch (e: Exception) {
            Log.e(TAG, "Feedback unexpected error", e)
            emit(Resource.Error("An unexpected error occurred. Please try again."))
        }
    }

    fun logout() {
        secureStorage.clearAll()
    }

    fun isLoggedIn(): Boolean = secureStorage.isLoggedIn()
}