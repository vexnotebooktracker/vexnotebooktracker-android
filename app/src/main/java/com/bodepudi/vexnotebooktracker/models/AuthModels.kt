package com.bodepudi.vexnotebooktracker.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.gson.annotations.SerializedName

// Enhanced Mobile Sign-Up Request
@Parcelize
data class MobileSignUpRequest(
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
    @SerializedName("role")
    val role: Int,                 // 2 = Team Member/Captain, 3 = School/Coach/Advisor
    @SerializedName("teamNoWithLetter")
    val teamNoWithLetter: String,
    @SerializedName("phone")
    val phone: String? = null,     // Optional

    // Mobile device identification (same as login)
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("appInstanceId")
    val appInstanceId: String,
    @SerializedName("deviceModel")
    val deviceModel: String,
    @SerializedName("osVersion")
    val osVersion: String,
    @SerializedName("appVersion")
    val appVersion: String,

    // Security info
    @SerializedName("appSignature")
    val appSignature: String,
    @SerializedName("isDeviceRooted")
    val isDeviceRooted: Boolean,
    @SerializedName("biometricEnabled")
    val biometricEnabled: Boolean,

    // Network info
    @SerializedName("ipAddress")
    val ipAddress: String,
    @SerializedName("networkType")
    val networkType: String,
    @SerializedName("deviceFingerprint")
    val deviceFingerprint: String
) : Parcelable

// Sign-Up Response
@Parcelize
data class MobileSignUpResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("userId")
    val userId: Int? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("requiresEmailVerification")
    val requiresEmailVerification: Boolean = false,
    @SerializedName("verificationEmailSent")
    val verificationEmailSent: Boolean = false
) : Parcelable

// Email Availability Check
@Parcelize
data class EmailAvailabilityRequest(
    @SerializedName("email")
    val email: String
) : Parcelable

@Parcelize
data class EmailAvailabilityResponse(
    @SerializedName("email")
    val email: String? = null, // .NET is returning this at this time.
    @SerializedName("available")
    val available: Boolean,
    @SerializedName("message")
    val message: String
) : Parcelable

// Mobile-specific JWT login request
@Parcelize
data class MobileLoginRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,

    // Mobile device identification
    @SerializedName("deviceId")
    val deviceId: String,           // Android ID
    @SerializedName("appInstanceId")
    val appInstanceId: String,      // Firebase Instance ID or similar
    @SerializedName("deviceModel")
    val deviceModel: String,        // "Samsung Galaxy S23"
    @SerializedName("osVersion")
    val osVersion: String,          // "Android 13"
    @SerializedName("appVersion")
    val appVersion: String,         // "1.0.0"

    // Security info
    @SerializedName("appSignature")
    val appSignature: String,       // App signing certificate hash
    @SerializedName("isDeviceRooted")
    val isDeviceRooted: Boolean,    // Jailbreak/root detection
    @SerializedName("biometricEnabled")
    val biometricEnabled: Boolean,  // Touch/Face ID available

    // Network info
    @SerializedName("ipAddress")
    val ipAddress: String,
    @SerializedName("networkType")
    val networkType: String,        // "WiFi" or "Cellular"
    @SerializedName("deviceFingerprint")
    val deviceFingerprint: String   // Combined device characteristics
) : Parcelable

// JWT response with mobile session
@Parcelize
data class MobileAuthResponse(
    @SerializedName("accessToken")
    val accessToken: String,        // JWT token
    @SerializedName("refreshToken")
    val refreshToken: String,       // For token refresh
    @SerializedName("sessionId")
    val sessionId: String,          // Server-side session ID
    @SerializedName("expiresIn")
    val expiresIn: Int,            // Token expiration in seconds
    @SerializedName("slidingExpiration")
    val slidingExpiration: Int,     // Sliding window in minutes

    // User info
    @SerializedName("userId")
    val userId: Int,
    @SerializedName("email")
    val email: String,
    @SerializedName("teamNoWithLetter")
    val teamNoWithLetter: String,
    @SerializedName("firstName")
    val firstName: String?,
    @SerializedName("lastName")
    val lastName: String?,
    // ADD: Phone and role fields
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("roleId")
    val roleId: Int? = null,

    // Response metadata
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String
) : Parcelable

// Session validation request
@Parcelize
data class SessionValidationRequest(
    @SerializedName("sessionId")
    val sessionId: String,
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("appInstanceId")
    val appInstanceId: String,
    @SerializedName("ipAddress")
    val ipAddress: String,
    @SerializedName("lastActivity")
    val lastActivity: String       // ISO timestamp
) : Parcelable

// Session validation response
@Parcelize
data class SessionValidationResponse(
    @SerializedName("isValid")
    val isValid: Boolean,
    @SerializedName("shouldRefreshToken")
    val shouldRefreshToken: Boolean,
    @SerializedName("newToken")
    val newToken: String?,
    @SerializedName("expiresIn")
    val expiresIn: Int?,
    @SerializedName("reason")
    val reason: String?,
    @SerializedName("action")
    val action: String             // "continue", "login_required", "security_violation"
) : Parcelable

// Device security validation
@Parcelize
data class DeviceSecurityInfo(
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("deviceFingerprint")
    val deviceFingerprint: String,
    @SerializedName("isRooted")
    val isRooted: Boolean,
    @SerializedName("appSignature")
    val appSignature: String,
    @SerializedName("lastKnownIP")
    val lastKnownIP: String,
    @SerializedName("consecutiveFailures")
    val consecutiveFailures: Int
) : Parcelable

// Original models for backward compatibility
@Parcelize
data class LoginRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("deviceCertificate")
    val deviceCertificate: String
) : Parcelable

@Parcelize
data class SignUpRequest(
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
    @SerializedName("deviceCertificate")
    val deviceCertificate: String
) : Parcelable

@Parcelize
data class ForgotPasswordRequest(
    @SerializedName("email")
    val email: String
) : Parcelable

@Parcelize
data class DeviceRegistrationRequest(
    @SerializedName("deviceId")
    val deviceId: String,
    @SerializedName("integrityToken")
    val integrityToken: String,
    @SerializedName("nonce")
    val nonce: String,
    @SerializedName("appSignature")
    val appSignature: String
) : Parcelable

@Parcelize
data class AuthResponse(
    @SerializedName("accessToken")
    val accessToken: String,
    @SerializedName("refreshToken")
    val refreshToken: String,
    @SerializedName("userId")
    val userId: Int,
    @SerializedName("email")
    val email: String,
    @SerializedName("teamNumber")
    val teamNumber: String,
    @SerializedName("expiresAt")
    val expiresAt: String,
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String
) : Parcelable

@Parcelize
data class DeviceRegistrationResponse(
    @SerializedName("deviceCertificate")
    val deviceCertificate: String,
    @SerializedName("expiresAt")
    val expiresAt: String,
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String
) : Parcelable

// Keep generic response model without @Parcelize
data class ApiResponse<T>(
    @SerializedName("data")
    val data: T?,
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("errors")
    val errors: List<String>? = null
)

@Parcelize
data class UserProfile(
    @SerializedName("id")
    val id: Int,
    @SerializedName("email")
    val email: String,
    @SerializedName("firstName")
    val firstName: String,
    @SerializedName("lastName")
    val lastName: String,
    @SerializedName("teamNumber")
    val teamNumber: String,
    @SerializedName("createdAt")
    val createdAt: String
) : Parcelable