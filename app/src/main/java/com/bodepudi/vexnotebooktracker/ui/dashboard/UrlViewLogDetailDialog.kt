package com.bodepudi.vexnotebooktracker.ui.dashboard

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.bodepudi.vexnotebooktracker.utils.DebugLogger
import android.view.LayoutInflater
import android.view.Window
import com.bodepudi.vexnotebooktracker.R
import com.bodepudi.vexnotebooktracker.databinding.DialogUrlViewLogDetailBinding
import com.bodepudi.vexnotebooktracker.network.UrlViewLogDetail
import com.bodepudi.vexnotebooktracker.utils.TimeUtils
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
            tvDeviceType.text = joinValues(logDetail.deviceType, logDetail.operatingSystem)
            tvBrowserName.text = joinValues(logDetail.browserName, logDetail.browserVersion)
            tvReferringUrl.text = logDetail.referringUrl.ifEmpty { context.getString(R.string.direct_access) }
        }
    }

    /** Renders "<first>, <second>", skipping either part when the server sent it empty. */
    private fun joinValues(first: String, second: String): String =
        listOf(first, second).filter { it.isNotBlank() }.joinToString(", ")

    /**
     * The API sends this timestamp without a zone; the app parses it as GMT and renders it in the
     * device's local timezone. The old "(GMT-4:00)" suffix showed the *local* offset, which read
     * as though the value itself were GMT, so it has been dropped - the value shown is local.
     */
    private fun formatDate(dateString: String): String {
        return try {
            val date = TimeUtils.parseGmtTime(dateString)

            if (date != null) {
                val detailedFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault()).apply {
                    timeZone = TimeZone.getDefault()
                }

                val detailedLocal = detailedFormat.format(date)

                DebugLogger.d("UrlViewLogDetailDialog", "Detail converted '${dateString}' -> '$detailedLocal' (local)")

                detailedLocal
            } else {
                // Fallback to TimeUtils display format
                TimeUtils.gmtToLocalDisplay(dateString)
            }

        } catch (e: Exception) {
            Log.w("UrlViewLogDetailDialog", "TimeUtils failed, using fallback for: $dateString", e)
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