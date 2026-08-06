package org.vexteams.vexnotebooktracker.ui.dashboard

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import org.vexteams.vexnotebooktracker.utils.DebugLogger
import android.view.LayoutInflater
import android.view.Window
import org.vexteams.vexnotebooktracker.R
import org.vexteams.vexnotebooktracker.databinding.DialogUrlViewLogDetailBinding
import org.vexteams.vexnotebooktracker.network.UrlViewLogDetail
import org.vexteams.vexnotebooktracker.utils.TimeUtils
import java.text.SimpleDateFormat
import java.util.*

class UrlViewLogDetailDialog(
    context: Context,
    private val logDetail: UrlViewLogDetail
) : Dialog(context) {

    private lateinit var binding: DialogUrlViewLogDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = DialogUrlViewLogDetailBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        setupDialog()
        populateData()
    }

    private fun setupDialog() {
        binding.tvTitle.text = context.getString(R.string.view_log_detail_title)

        // Remove sub-heading completely
        binding.tvSubtitle.visibility = android.view.View.GONE

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // Make dialog take up most of the screen
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            (context.resources.displayMetrics.heightPixels * 0.8).toInt()
        )
    }

    private fun populateData() {
        binding.apply {
            // Use TimeUtils for GMT to Local conversion, with fallback
            tvDate.text = formatDate(logDetail.date)
            tvIpAddress.text = logDetail.ipAddress
            tvHostName.text = logDetail.hostName
            tvDeviceType.text = logDetail.deviceType
            tvOperatingSystem.text = logDetail.operatingSystem
            tvBrowserName.text = logDetail.browserName
            tvBrowserVersion.text = logDetail.browserVersion
            tvReferringUrl.text = logDetail.referringUrl.ifEmpty { context.getString(R.string.direct_access) }
        }
    }

    // Use TimeUtils for GMT to Local conversion with detailed formatting
    private fun formatDate(dateString: String): String {
        return try {
            // Try TimeUtils first for GMT->Local conversion
            val date = TimeUtils.parseGmtTime(dateString)

            if (date != null) {
                // Use a more detailed format for the detail view, in local timezone
                val detailedFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault()).apply {
                    timeZone = TimeZone.getDefault()
                }

                val detailedLocal = detailedFormat.format(date)

                // Add timezone info for clarity
                val timeZone = TimeZone.getDefault()
                val offsetMillis = timeZone.getOffset(System.currentTimeMillis())
                val offsetHours = offsetMillis / (1000 * 60 * 60)
                val offsetMinutes = kotlin.math.abs((offsetMillis % (1000 * 60 * 60)) / (1000 * 60))
                val tzInfo = context.getString(
                    R.string.timezone_format,
                    if (offsetHours >= 0) "+" else "",
                    offsetHours,
                    offsetMinutes
                )

                DebugLogger.d("UrlViewLogDetailDialog", "📅 Detail converted '${dateString}' (GMT) -> '$detailedLocal' ($tzInfo)")

                context.getString(R.string.date_with_timezone, detailedLocal, tzInfo)
            } else {
                // Fallback to TimeUtils display format
                TimeUtils.gmtToLocalDisplay(dateString)
            }

        } catch (e: Exception) {
            Log.w("UrlViewLogDetailDialog", "⚠️ TimeUtils failed, using fallback for: $dateString", e)
            formatDateFallback(dateString)
        }
    }

    // Keep your original logic as fallback
    private fun formatDateFallback(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())

            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            Log.w("DateFormat", "⚠️ Date formatting failed for: $dateString")
            dateString // Return original if parsing fails
        }
    }

    companion object {
        fun show(context: Context, logDetail: UrlViewLogDetail) {
            UrlViewLogDetailDialog(context, logDetail).show()
        }
    }
}