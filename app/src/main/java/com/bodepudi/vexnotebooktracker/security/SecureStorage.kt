package com.bodepudi.vexnotebooktracker.security

import android.content.Context
import android.util.Log
import com.bodepudi.vexnotebooktracker.utils.DebugLogger
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import com.bodepudi.vexnotebooktracker.utils.TimeUtils
import javax.inject.Inject
import javax.inject.Singleton
import java.util.TimeZone

@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "SecureStorage"

        // JWT & Session keys
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_SESSION_EXPIRES_AT = "session_expires_at"
        private const val KEY_SLIDING_EXPIRATION_MINUTES = "sliding_expiration_minutes"
        private const val KEY_LAST_ACTIVITY = "last_activity"
        private const val KEY_TOKEN_SAVED_AT = "token_saved_at"

        // Device binding keys
        private const val KEY_BOUND_DEVICE_ID = "bound_device_id"
        private const val KEY_BOUND_APP_INSTANCE_ID = "bound_app_instance_id"
        private const val KEY_DEVICE_FINGERPRINT = "device_fingerprint"

        // Legacy keys
        private const val KEY_DEVICE_CERTIFICATE = "device_certificate"

        // User info keys
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_TEAM_NUMBER = "team_number"
        private const val KEY_FIRST_NAME = "first_name"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_PHONE = "user_phone"          // ADD: Phone key
        private const val KEY_ROLE_ID = "user_role_id"      // ADD: Role ID key
        private const val KEY_LOGGED_IN = "logged_in"

        // Security tracking
        private const val KEY_SECURITY_EVENT_PREFIX = "security_event"

        // Time and timezone tracking keys
        private const val KEY_LAST_ACTIVITY_GMT = "last_activity_gmt"
        private const val KEY_SESSION_START_GMT = "session_start_gmt"
        private const val KEY_LOGIN_TIME_LOCAL = "login_time_local"
        private const val KEY_USER_TIMEZONE = "user_timezone"
        private const val KEY_TIMEZONE_OFFSET = "timezone_offset"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            "vex_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w(TAG, "Failed to create encrypted preferences, falling back to regular", e)
        // Fallback to regular SharedPreferences if EncryptedSharedPreferences fails
        context.getSharedPreferences("vex_prefs", Context.MODE_PRIVATE)
    }

    // JWT Token Management with GMT timestamps
    fun saveAccessToken(token: String) {
        val currentTimeGmt = TimeUtils.getCurrentTimestamp()
        sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putLong(KEY_TOKEN_SAVED_AT, System.currentTimeMillis())
            .putString(KEY_LAST_ACTIVITY_GMT, currentTimeGmt)
            .apply()

        DebugLogger.d(TAG, "Access token saved with GMT timestamp: $currentTimeGmt")
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    fun saveRefreshToken(token: String) {
        sharedPreferences.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    // Mobile Session Management with GMT timestamps
    fun saveSessionInfo(sessionId: String, expiresIn: Int, slidingExpiration: Int) {
        val currentTime = System.currentTimeMillis()
        val currentTimeGmt = TimeUtils.getCurrentTimestamp()
        val expirationTime = currentTime + (expiresIn * 1000L) // Convert to milliseconds
        val sessionStartGmt = TimeUtils.getCurrentGmtString()

        sharedPreferences.edit()
            .putString(KEY_SESSION_ID, sessionId)
            .putLong(KEY_SESSION_EXPIRES_AT, expirationTime)
            .putInt(KEY_SLIDING_EXPIRATION_MINUTES, slidingExpiration)
            .putLong(KEY_LAST_ACTIVITY, currentTime)
            .putString(KEY_LAST_ACTIVITY_GMT, currentTimeGmt)
            .putString(KEY_SESSION_START_GMT, sessionStartGmt)
            .apply()

        // Save timezone info for debugging
        saveCurrentTimezoneInfo()

        DebugLogger.d(TAG, "Session saved - ID: $sessionId, Start: $sessionStartGmt GMT, Expires in: ${expiresIn}s")
    }

    fun getSessionId(): String? {
        return sharedPreferences.getString(KEY_SESSION_ID, null)
    }

    // Activity tracking with GMT timestamps
    fun updateLastActivity() {
        val currentTimeGmt = TimeUtils.getCurrentTimestamp()
        sharedPreferences.edit()
            .putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis())
            .putString(KEY_LAST_ACTIVITY_GMT, currentTimeGmt)
            .apply()

        DebugLogger.d(TAG, "Last activity updated: $currentTimeGmt GMT")
    }

    fun isSessionValid(): Boolean {
        val sessionId = getSessionId()
        val lastActivity = getLastActivity()
        val slidingExpirationMinutes = getSlidingExpirationMinutes()

        if (sessionId == null || lastActivity == 0L || slidingExpirationMinutes == 0) {
            DebugLogger.d(TAG, "Session invalid - missing data")
            return false
        }

        val currentTime = System.currentTimeMillis()
        val timeSinceLastActivity = currentTime - lastActivity
        val slidingExpirationMs = slidingExpirationMinutes * 60 * 1000L

        val isValid = timeSinceLastActivity < slidingExpirationMs

        DebugLogger.d(TAG, "Session validity check: ${if (isValid) "Valid" else "Expired"} (${timeSinceLastActivity / 1000}s since last activity)")

        return isValid
    }

    private fun getLastActivity(): Long {
        return sharedPreferences.getLong(KEY_LAST_ACTIVITY, 0L)
    }

    private fun getSlidingExpirationMinutes(): Int {
        return sharedPreferences.getInt(KEY_SLIDING_EXPIRATION_MINUTES, 30) // Default 30 minutes
    }

    // Device Binding
    fun saveDeviceBinding(deviceId: String, appInstanceId: String, deviceFingerprint: String) {
        sharedPreferences.edit()
            .putString(KEY_BOUND_DEVICE_ID, deviceId)
            .putString(KEY_BOUND_APP_INSTANCE_ID, appInstanceId)
            .putString(KEY_DEVICE_FINGERPRINT, deviceFingerprint)
            .apply()

        DebugLogger.d(TAG, "Device binding saved for device: $deviceId")
    }

    fun validateDeviceBinding(deviceId: String, appInstanceId: String): Boolean {
        val boundDeviceId = sharedPreferences.getString(KEY_BOUND_DEVICE_ID, null)
        val boundAppInstanceId = sharedPreferences.getString(KEY_BOUND_APP_INSTANCE_ID, null)

        val isValid = boundDeviceId == deviceId && boundAppInstanceId == appInstanceId

        DebugLogger.d(TAG, "Device binding validation: ${if (isValid) "Valid" else "Invalid"}")

        return isValid
    }

    fun getBoundDeviceId(): String? {
        return sharedPreferences.getString(KEY_BOUND_DEVICE_ID, null)
    }

    fun getBoundAppInstanceId(): String? {
        return sharedPreferences.getString(KEY_BOUND_APP_INSTANCE_ID, null)
    }

    // Keep existing methods for backward compatibility
    fun saveDeviceCertificate(certificate: String) {
        sharedPreferences.edit().putString(KEY_DEVICE_CERTIFICATE, certificate).apply()
    }

    fun getDeviceCertificate(): String? {
        return sharedPreferences.getString(KEY_DEVICE_CERTIFICATE, null)
    }

    fun saveUserInfo(userId: Int, email: String, teamNumber: String) {
        sharedPreferences.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_TEAM_NUMBER, teamNumber)
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()
    }

    // UPDATED: Enhanced User Info for JWT with phone and roleId support
    fun saveUserProfile(
        userId: Int,                    // Keep as Int - no .toString() needed
        email: String,
        teamNumber: String,
        firstName: String? = null,
        lastName: String? = null,
        phone: String? = null,          // ADD: Phone parameter
        roleId: Int? = null             // ADD: Role ID parameter
    ) {
        val loginTimeLocal = TimeUtils.getCurrentLocalTimeString()

        sharedPreferences.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_TEAM_NUMBER, teamNumber)
            .putString(KEY_FIRST_NAME, firstName)
            .putString(KEY_LAST_NAME, lastName)
            .putString(KEY_PHONE, phone ?: "")              // ADD: Save phone
            .putInt(KEY_ROLE_ID, roleId ?: 2)               // ADD: Save roleId (default to 2)
            .putString(KEY_LOGIN_TIME_LOCAL, loginTimeLocal)
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()

        // Save timezone info
        saveCurrentTimezoneInfo()

        DebugLogger.d(TAG, "User profile saved - $email ($teamNumber) role:$roleId phone:${if (phone.isNullOrEmpty()) "none" else "set"} at $loginTimeLocal")
    }

    fun getUserId(): Int {
        return sharedPreferences.getInt(KEY_USER_ID, -1)
    }

    // FIXED: Return getUserId() as String for compatibility
    fun getUserIdString(): String {
        return getUserId().toString()
    }

    fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    fun getTeamNumber(): String? {
        return sharedPreferences.getString(KEY_TEAM_NUMBER, null)
    }

    fun getFirstName(): String? {
        return sharedPreferences.getString(KEY_FIRST_NAME, null)
    }

    fun getLastName(): String? {
        return sharedPreferences.getString(KEY_LAST_NAME, null)
    }

    // ADD: Phone and Role getters
    fun getPhone(): String? {
        val phone = sharedPreferences.getString(KEY_PHONE, null)
        return if (phone.isNullOrEmpty()) null else phone
    }

    fun getRoleId(): Int? {
        val roleId = sharedPreferences.getInt(KEY_ROLE_ID, -1)
        return if (roleId == -1) null else roleId
    }

    // Get login time in local timezone for display
    fun getLoginTimeLocal(): String? {
        return sharedPreferences.getString(KEY_LOGIN_TIME_LOCAL, null)
    }

    // Enhanced Login Status Check
    fun isLoggedIn(): Boolean {
        val hasAccessToken = getAccessToken() != null
        val hasSessionId = getSessionId() != null
        val isSessionValid = isSessionValid()
        val hasUserInfo = sharedPreferences.getBoolean(KEY_LOGGED_IN, false)

        val loggedIn = hasAccessToken && hasSessionId && isSessionValid && hasUserInfo

        DebugLogger.d(TAG, "Login status: ${if (loggedIn) "Logged in" else "Not logged in"}")

        return loggedIn
    }

    // Security Tracking with GMT timestamps
    fun recordSecurityEvent(eventType: String, details: String = "") {
        val timestampGmt = TimeUtils.getCurrentTimestamp()
        val key = "${KEY_SECURITY_EVENT_PREFIX}_${timestampGmt}"
        val eventData = "$eventType|$details|$timestampGmt"

        sharedPreferences.edit()
            .putString(key, eventData)
            .apply()

        Log.w(TAG, "Security event recorded: $eventType - $details")
    }

    fun getSecurityEvents(): List<String> {
        val events = mutableListOf<String>()
        val allPrefs = sharedPreferences.all

        for ((key, value) in allPrefs) {
            if (key.startsWith(KEY_SECURITY_EVENT_PREFIX) && value is String) {
                events.add(value)
            }
        }

        return events.sorted() // Sort by timestamp
    }

    // Timezone and time-related helper methods
    private fun saveCurrentTimezoneInfo() {
        val timeZone = TimeZone.getDefault()
        val offsetMillis = timeZone.getOffset(System.currentTimeMillis())
        val offsetHours = offsetMillis / (1000 * 60 * 60)

        sharedPreferences.edit()
            .putString(KEY_USER_TIMEZONE, timeZone.id)
            .putInt(KEY_TIMEZONE_OFFSET, offsetHours)
            .apply()
    }

    fun getCurrentTimezoneInfo(): String {
        val timeZone = TimeZone.getDefault()
        val offsetMillis = timeZone.getOffset(System.currentTimeMillis())
        val offsetHours = offsetMillis / (1000 * 60 * 60)
        val offsetMinutes = kotlin.math.abs((offsetMillis % (1000 * 60 * 60)) / (1000 * 60))

        return "${timeZone.displayName} (GMT${if (offsetHours >= 0) "+" else ""}$offsetHours:${String.format("%02d", offsetMinutes)})"
    }

    // Get session information for debugging
    fun getSessionInfo(): Map<String, String> {
        val sessionId = getSessionId()
        val sessionStartGmt = sharedPreferences.getString(KEY_SESSION_START_GMT, null)
        val lastActivityGmt = sharedPreferences.getString(KEY_LAST_ACTIVITY_GMT, null)
        val loginTimeLocal = getLoginTimeLocal()
        val timezoneInfo = getCurrentTimezoneInfo()

        return mapOf(
            "sessionId" to (sessionId ?: "None"),
            "sessionStartGmt" to (sessionStartGmt ?: "Unknown"),
            "lastActivityGmt" to (lastActivityGmt ?: "Unknown"),
            "loginTimeLocal" to (loginTimeLocal ?: "Unknown"),
            "timezone" to timezoneInfo,
            "isValid" to isSessionValid().toString()
        )
    }

    // Get last activity timestamps for session validation API
    fun getLastActivityTimestamp(): String {
        return sharedPreferences.getString(KEY_LAST_ACTIVITY_GMT, null)
            ?: TimeUtils.getCurrentTimestamp()
    }

    // Enhanced Clear Method
    fun clearAll() {
        DebugLogger.d(TAG, "Clearing all stored data")
        sharedPreferences.edit().clear().apply()
    }

    fun clearAuthData() {
        DebugLogger.d(TAG, "Clearing authentication data")
        sharedPreferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_SESSION_ID)
            .remove(KEY_SESSION_EXPIRES_AT)
            .remove(KEY_LAST_ACTIVITY)
            .remove(KEY_LAST_ACTIVITY_GMT)
            .remove(KEY_SESSION_START_GMT)
            .remove(KEY_LOGIN_TIME_LOCAL)
            .remove(KEY_LOGGED_IN)
            .apply()
    }
}