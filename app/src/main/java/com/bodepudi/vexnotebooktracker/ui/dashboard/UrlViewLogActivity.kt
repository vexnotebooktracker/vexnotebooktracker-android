package com.bodepudi.vexnotebooktracker.ui.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.bodepudi.vexnotebooktracker.utils.DebugLogger
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.bodepudi.vexnotebooktracker.R
import com.bodepudi.vexnotebooktracker.databinding.ActivityUrlViewLogBinding
import com.bodepudi.vexnotebooktracker.network.UrlViewLogEntry
import com.bodepudi.vexnotebooktracker.network.VexTeamsApiService
import com.bodepudi.vexnotebooktracker.utils.TimeUtils  // ✅ NEW IMPORT
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import com.bodepudi.vexnotebooktracker.utils.WindowInsetsUtils

@AndroidEntryPoint
class UrlViewLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUrlViewLogBinding
    private lateinit var adapter: UrlViewLogAdapter
    private var urlId: Int = -1
    private var title: String = ""

    @Inject
    lateinit var vexTeamsApiService: VexTeamsApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityUrlViewLogBinding.inflate(layoutInflater)
        // Android 16 (API 36) enforces edge-to-edge; a dark system bar style keeps the
        // white status bar icons this app has always used over its orange app bar.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContentView(binding.root)

        WindowInsetsUtils.applyTopBarInsets(binding.appBarLayout)
        WindowInsetsUtils.applyContentInsets(binding.contentContainer)

        // Get data from intent
        urlId = intent.getIntExtra(EXTRA_URL_ID, -1)
        title = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.url_view_log_default_title)

        DebugLogger.d("UrlViewLogActivity", "🚀 Starting with URL ID: $urlId, Title: $title")

        if (urlId == -1) {
            Toast.makeText(this, R.string.error_invalid_url_id, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupRecyclerView()
        loadUrlViewLog()
    }

    private fun setupUI() {
        // Setup toolbar/action bar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = this@UrlViewLogActivity.title
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = UrlViewLogAdapter(emptyList()) { entry ->
            DebugLogger.d("UrlViewLogActivity", "📋 Entry clicked - LogID: ${entry.logId}, Date: ${entry.date}, IP: ${entry.ipAddress}")

            // When an entry is clicked, load detailed view
            loadUrlViewLogDetail(entry.logId)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@UrlViewLogActivity)
            adapter = this@UrlViewLogActivity.adapter
        }
    }

    private fun loadUrlViewLog() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = android.view.View.VISIBLE
                binding.recyclerView.visibility = android.view.View.GONE
                binding.headerLayout.visibility = android.view.View.GONE

                DebugLogger.d("UrlViewLogActivity", "📡 Fetching view log for URL ID: $urlId")

                val response = vexTeamsApiService.getUrlViewLog(urlId)

                if (response.isSuccessful && response.body() != null) {
                    val viewLogEntries = response.body()!!
                    DebugLogger.d("UrlViewLogActivity", "✅ Received ${viewLogEntries.size} view log entries")

                    // ✅ UPDATED: Convert GMT times from server to local display times
                    val formattedEntries = viewLogEntries.map { entry ->
                        try {
                            // Convert GMT time from server to local time for display
                            val localDisplayTime = TimeUtils.gmtToLocalDisplay(entry.date)
                            DebugLogger.d("UrlViewLogActivity", "📅 Converted '${entry.date}' (GMT) -> '$localDisplayTime' (Local)")

                            entry.copy(date = localDisplayTime)
                        } catch (e: Exception) {
                            Log.w("UrlViewLogActivity", "⚠️ Time conversion failed for entry: ${entry.date}", e)
                            // Fallback to old format method if TimeUtils fails
                            entry.copy(date = formatDateFallback(entry.date))
                        }
                    }

                    // Log each entry's logId for debugging
                    formattedEntries.forEachIndexed { index, entry ->
                        DebugLogger.d("UrlViewLogActivity", "📋 Entry $index: LogID=${entry.logId}, Date=${entry.date}, IP=${entry.ipAddress}")
                    }

                    updateUI(formattedEntries)

                } else {
                    showError(getString(R.string.error_failed_to_load_view_log, response.code()))
                    Log.e("UrlViewLogActivity", "❌ Failed to load view log: ${response.code()}")
                }

            } catch (e: Exception) {
                showError(getString(R.string.error_loading_view_log, e.message ?: "Unknown error"))
                Log.e("UrlViewLogActivity", "❌ Error loading view log", e)
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun updateUI(entries: List<UrlViewLogEntry>) {
        binding.apply {
            headerLayout.visibility = android.view.View.VISIBLE
            recyclerView.visibility = android.view.View.VISIBLE

            // ✅ UPDATED: Show hint text if there are entries, otherwise show count
            tvSubtitle.text = if (entries.isNotEmpty()) {
                getString(R.string.touch_record_for_details_hint)
            } else {
                getString(R.string.no_view_entries_found)
            }

            // Update adapter with new data
            adapter = UrlViewLogAdapter(entries) { entry ->
                DebugLogger.d("UrlViewLogActivity", "🔗 Loading detail for LogID: ${entry.logId}")
                loadUrlViewLogDetail(entry.logId)
            }
            recyclerView.adapter = adapter
        }
    }

    private fun loadUrlViewLogDetail(logId: Int) {
        lifecycleScope.launch {
            try {
                DebugLogger.d("UrlViewLogActivity", "📡 Fetching view log detail for log ID: $logId")

                val response = vexTeamsApiService.getUrlViewLogDetail(logId)

                if (response.isSuccessful && response.body() != null) {
                    val logDetail = response.body()!!
                    DebugLogger.d("UrlViewLogActivity", "✅ Received view log detail for LogID: ${logDetail.logId}")
                    DebugLogger.d("UrlViewLogActivity", "📋 Detail: Date=${logDetail.date}, Referrer=${logDetail.referringUrl}")

                    // ✅ UPDATED: Convert GMT time to local time for detail view
                    val formattedDetail = try {
                        val localDisplayTime = TimeUtils.gmtToLocalDisplay(logDetail.date)
                        DebugLogger.d("UrlViewLogActivity", "📅 Detail converted '${logDetail.date}' (GMT) -> '$localDisplayTime' (Local)")
                        logDetail.copy(date = localDisplayTime)
                    } catch (e: Exception) {
                        Log.w("UrlViewLogActivity", "⚠️ Detail time conversion failed: ${logDetail.date}", e)
                        logDetail.copy(date = formatDateFallback(logDetail.date))
                    }

                    // Show the detailed view in a dialog
                    UrlViewLogDetailDialog.show(this@UrlViewLogActivity, formattedDetail)

                } else {
                    showError(getString(R.string.error_failed_to_load_log_detail, response.code()))
                    Log.e("UrlViewLogActivity", "❌ Failed to load log detail: ${response.code()}")
                }

            } catch (e: Exception) {
                showError(getString(R.string.error_loading_log_detail, e.message ?: "Unknown error"))
                Log.e("UrlViewLogActivity", "❌ Error loading log detail", e)
            }
        }
    }

    // Keep old method as fallback, but prefer TimeUtils
    private fun formatDate(dateString: String): String {
        return try {
            // Try TimeUtils first
            TimeUtils.gmtToLocalDisplay(dateString)
        } catch (e: Exception) {
            Log.w("UrlViewLogActivity", "⚠️ TimeUtils failed, using fallback for: $dateString", e)
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

    private fun showError(message: String) {
        Toast.makeText(this, getString(R.string.error_prefix, message), Toast.LENGTH_LONG).show()
        binding.recyclerView.visibility = android.view.View.VISIBLE
        binding.headerLayout.visibility = android.view.View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        private const val EXTRA_URL_ID = "extra_url_id"
        private const val EXTRA_TITLE = "extra_title"

        fun start(context: Context, urlId: Int, title: String) {
            val intent = Intent(context, UrlViewLogActivity::class.java).apply {
                putExtra(EXTRA_URL_ID, urlId)
                putExtra(EXTRA_TITLE, title)
            }
            context.startActivity(intent)
        }
    }
}