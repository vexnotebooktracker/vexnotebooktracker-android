package org.vexteams.vexnotebooktracker.utils

import android.util.Log
import org.vexteams.vexnotebooktracker.utils.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for handling time conversions between GMT/UTC and local time
 *
 * Server Protocol:
 * - Always send times to server in GMT/UTC
 * - Always receive times from server in GMT/UTC
 * - Display times in local timezone with existing format
 *
 * ✅ PERFORMANCE OPTIMIZATIONS:
 * - Added async methods for background thread processing
 * - Cached date formatters to avoid recreation
 * - Added batch processing for multiple timestamps
 */
object TimeUtils {

    private const val TAG = "TimeUtils"

    // Server expects this format in GMT/UTC
    private const val SERVER_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"

    // Server may also send ISO format
    private const val SERVER_ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"

    // Display format (user-friendly, in local time)
    private const val DISPLAY_DATE_FORMAT = "yyyy-MM-dd HH:mm"

    // 12-hour display variant, for values where AM/PM removes ambiguity
    private const val DISPLAY_DATE_FORMAT_12H = "yyyy-MM-dd hh:mm a"
    private const val DISPLAY_TIME_FORMAT = "HH:mm:ss"

    // ✅ OPTIMIZATION: Cache date formatters to avoid recreation
    private val serverDateFormat by lazy {
        SimpleDateFormat(SERVER_DATE_FORMAT, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }
    }

    private val serverIsoFormat by lazy {
        SimpleDateFormat(SERVER_ISO_FORMAT, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }
    }

    private val displayDateFormat by lazy {
        SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    private val displayDateFormat12h by lazy {
        SimpleDateFormat(DISPLAY_DATE_FORMAT_12H, Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    private val displayTimeFormat by lazy {
        SimpleDateFormat(DISPLAY_TIME_FORMAT, Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    /**
     * Convert local time to GMT for sending to server
     * @param localTimeString Time in local timezone (format: "yyyy-MM-dd HH:mm:ss")
     * @return Time in GMT timezone (format: "yyyy-MM-dd HH:mm:ss")
     */
    fun localToGmt(localTimeString: String): String {
        return try {
            // Parse as local time
            val localFormat = SimpleDateFormat(SERVER_DATE_FORMAT, Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }
            val date = localFormat.parse(localTimeString)

            // Format as GMT
            serverDateFormat.format(date ?: Date())
        } catch (e: Exception) {
            Log.e(TAG, "Error converting local time to GMT: $localTimeString", e)
            localTimeString // Return original if conversion fails
        }
    }

    /**
     * Convert current local Date to GMT string for server
     * @param date Local Date object
     * @return GMT time string (format: "yyyy-MM-dd HH:mm:ss")
     */
    fun dateToGmtString(date: Date = Date()): String {
        return serverDateFormat.format(date)
    }

    /**
     * Convert GMT time from server to local time for display
     * @param gmtTimeString Time from server in GMT (format: "yyyy-MM-dd HH:mm:ss" or "yyyy-MM-dd'T'HH:mm:ss")
     * @return Time in local timezone (format: "yyyy-MM-dd HH:mm")
     */
    fun gmtToLocalDisplay(gmtTimeString: String): String {
        return try {
            // Try to parse with different server formats
            val date = when {
                gmtTimeString.contains("T") -> serverIsoFormat.parse(gmtTimeString)
                else -> serverDateFormat.parse(gmtTimeString)
            }

            // Format as local time for display
            displayDateFormat.format(date ?: Date())
        } catch (e: Exception) {
            Log.e(TAG, "Error converting GMT time to local display: $gmtTimeString", e)
            gmtTimeString // Return original if conversion fails
        }
    }

    /**
     * Same conversion as [gmtToLocalDisplay] but rendered in 12-hour form with AM/PM
     * (for example "2026-09-08 08:30 PM"), for values where 24-hour time reads ambiguously.
     */
    fun gmtToLocalDisplay12h(gmtTimeString: String): String {
        return try {
            val date = when {
                gmtTimeString.contains("T") -> serverIsoFormat.parse(gmtTimeString)
                else -> serverDateFormat.parse(gmtTimeString)
            }

            displayDateFormat12h.format(date ?: Date())
        } catch (e: Exception) {
            Log.e(TAG, "Error converting GMT time to 12-hour local display: $gmtTimeString", e)
            gmtToLocalDisplay(gmtTimeString)
        }
    }

    // ✅ NEW: Async version for background thread processing
    /**
     * Convert GMT time to local display on background thread
     * @param gmtTimeString Time from server in GMT
     * @return Time in local timezone for display
     */
    suspend fun gmtToLocalDisplayAsync(gmtTimeString: String): String =
        withContext(Dispatchers.Default) {
            gmtToLocalDisplay(gmtTimeString)
        }

    // ✅ NEW: Batch processing for multiple timestamps
    /**
     * Convert multiple GMT timestamps to local display on background thread
     * @param gmtTimestamps List of GMT timestamp strings
     * @return List of local display time strings
     */
    suspend fun convertTimestampsAsync(gmtTimestamps: List<String>): List<String> =
        withContext(Dispatchers.Default) {
            gmtTimestamps.map { timestamp ->
                gmtToLocalDisplay(timestamp)
            }
        }

    // ✅ NEW: For single conversion with caching for repeated calls
    private val conversionCache = mutableMapOf<String, String>()

    /**
     * Convert GMT to local with caching for repeated calls
     * @param gmtTimeString GMT timestamp from server
     * @return Cached or newly converted local display time
     */
    fun gmtToLocalDisplayCached(gmtTimeString: String): String {
        return conversionCache.getOrPut(gmtTimeString) {
            gmtToLocalDisplay(gmtTimeString)
        }
    }

    // ✅ NEW: Clear cache when memory is needed
    fun clearConversionCache() {
        conversionCache.clear()
    }

    /**
     * Get current time as GMT string for server API calls
     * @return Current time in GMT (format: "yyyy-MM-dd HH:mm:ss")
     */
    fun getCurrentGmtString(): String {
        return serverDateFormat.format(Date())
    }

    /**
     * Get current time as local display string
     * @return Current time in local timezone (format: "HH:mm:ss")
     */
    fun getCurrentLocalTimeString(): String {
        return displayTimeFormat.format(Date())
    }

    /**
     * Convert user input datetime to GMT for server
     * Used when user selects date/time in UI
     * @param year Selected year
     * @param month Selected month (0-based)
     * @param day Selected day
     * @param hour Selected hour (24-hour format)
     * @param minute Selected minute
     * @return GMT time string for server
     */
    fun userInputToGmt(year: Int, month: Int, day: Int, hour: Int, minute: Int): String {
        return try {
            val calendar = Calendar.getInstance(TimeZone.getDefault()).apply {
                set(year, month, day, hour, minute, 0)
                set(Calendar.MILLISECOND, 0)
            }

            serverDateFormat.format(calendar.time)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting user input to GMT", e)
            getCurrentGmtString()
        }
    }

    /**
     * ✅ SIMPLIFIED: Parse GMT time from server to Date object
     * Just add ":00" if seconds are missing - much simpler!
     */
    fun parseGmtTime(gmtTimeString: String): Date? {
        return try {
            val normalizedTimeString = when {
                gmtTimeString.contains("T") -> {
                    // ISO format: keep as-is
                    gmtTimeString
                }
                gmtTimeString.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$")) -> {
                    // Missing seconds: add ":00"
                    "$gmtTimeString:00"
                }
                else -> {
                    // Already has seconds or other format: keep as-is
                    gmtTimeString
                }
            }

            DebugLogger.d(TAG, "Normalized '$gmtTimeString' -> '$normalizedTimeString'")

            when {
                normalizedTimeString.contains("T") -> serverIsoFormat.parse(normalizedTimeString)
                else -> serverDateFormat.parse(normalizedTimeString)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing GMT time: $gmtTimeString", e)
            null
        }
    }

    /**
     * Check if a GMT time string from server represents a future time
     * @param gmtTimeString GMT time string from server
     * @return true if the time is in the future
     */
    fun isFutureTime(gmtTimeString: String): Boolean {
        val date = parseGmtTime(gmtTimeString)
        return date?.after(Date()) ?: false
    }

    /**
     * Get timestamp for API calls (milliseconds since epoch)
     * @return Current timestamp as string
     */
    fun getCurrentTimestamp(): String {
        return System.currentTimeMillis().toString()
    }

    /**
     * Format timestamp for logging/debugging
     * @param timestamp Timestamp in milliseconds
     * @return Formatted local time string
     */
    fun formatTimestampForLogging(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }.format(Date(timestamp))
    }
}