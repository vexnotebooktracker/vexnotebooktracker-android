package org.vexteams.vexnotebooktracker.ui.register

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.util.Log
import org.vexteams.vexnotebooktracker.utils.DebugLogger
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
// import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.vexteams.vexnotebooktracker.R
import org.vexteams.vexnotebooktracker.databinding.ActivityRegisterNotebookBinding
import org.vexteams.vexnotebooktracker.network.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import org.vexteams.vexnotebooktracker.utils.WindowInsetsUtils

@AndroidEntryPoint
class RegisterNotebookActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterNotebookBinding

    @Inject
    lateinit var vexTeamsApiService: VexTeamsApiService

    private var redirectionTypes: List<RedirectionType> = emptyList()
    private var teams: List<Team> = emptyList()
    private var selectedRedirectionTypeId: Int? = null
    private var selectedTeamId: Int? = null

    // Date formatter for display (uses device's local timezone)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    // API format uses UTC timezone - converts local time to UTC before sending
    private val apiDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, RegisterNotebookActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityRegisterNotebookBinding.inflate(layoutInflater)
            // Android 16 (API 36) enforces edge-to-edge; a dark system bar style keeps the
            // white status bar icons this app has always used over its orange app bar.
            enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
            setContentView(binding.root)

            WindowInsetsUtils.applyTopBarInsets(binding.appBarLayout)
            WindowInsetsUtils.applyContentInsets(binding.contentContainer, handleIme = true)

            setupToolbar()
            setupHelpIcons()
            setupClickListeners()
            setupValidation()
            setupDefaultValues()
            loadFormData()

            DebugLogger.d("RegisterNotebookActivity", "Registration screen initialized")

        } catch (e: Exception) {
            Log.e("RegisterNotebookActivity", "Error in onCreate", e)
            showError(getString(R.string.registration_screen_error, e.message))
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupHelpIcons() {
        binding.ivRedirectionTypeHelp.setOnClickListener {
            openHelpUrl(getString(R.string.redirection_help_url))
        }
    }

    private fun openHelpUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            DebugLogger.d("RegisterNotebookActivity", "Opened help URL: $url")
        } catch (e: Exception) {
            Log.e("RegisterNotebookActivity", "Failed to open help URL: ${e.message}")
            Toast.makeText(this, getString(R.string.error_open_help), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnRegister.setOnClickListener { validateAndSubmit() }

        binding.etStartDate.setOnClickListener {
            showDateTimePicker { dateTime -> binding.etStartDate.setText(dateTime) }
        }
        binding.etEndDate.setOnClickListener {
            showDateTimePicker { dateTime -> binding.etEndDate.setText(dateTime) }
        }

        binding.tilStartDate.setEndIconOnClickListener { binding.etStartDate.performClick() }
        binding.tilEndDate.setEndIconOnClickListener { binding.etEndDate.performClick() }
    }

    private fun setupDefaultValues() {
        // Pre-populate targetUrl with https://
        binding.etNotebookUrl.setText("https://")

        // Pre-populate dates
        val calendar = Calendar.getInstance()

        // Set start date to today at 11 PM (local time)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startDate = dateTimeFormat.format(calendar.time)
        binding.etStartDate.setText(startDate)

        // Set end date to 14 days from today at 8 PM (local time)
        calendar.add(Calendar.DAY_OF_MONTH, 14)
        calendar.set(Calendar.HOUR_OF_DAY, 20)
        val endDate = dateTimeFormat.format(calendar.time)
        binding.etEndDate.setText(endDate)

        DebugLogger.d("RegisterNotebookActivity", "Default values set (Local Time): Start=$startDate, End=$endDate")
    }

    private fun setupValidation() {
        // Input filter to allow only alphanumeric, underscore, and hyphen
        val shortUrlFilter = InputFilter { source, start, end, _, _, _ -> // last 3 params dest, dstart and dend are not used, so defined as _
            for (i in start until end) {
                val char = source[i]
                if (!char.isLetterOrDigit() && char != '_' && char != '-') {
                    // Invalid character - reject it
                    return@InputFilter ""
                }
            }
            // All characters are valid - accept them
            null
        }

        // Apply filter to prevent invalid characters
        binding.etShortUrl.filters = arrayOf(
            shortUrlFilter,
            InputFilter.LengthFilter(resources.getInteger(R.integer.short_url_max_length))  // Also enforce max length at input level
        )

        binding.etShortUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val shortUrl = s.toString().trim()
                if (shortUrl.isNotEmpty()) validateShortUrl(shortUrl)
                else clearShortUrlValidation()
            }
        })

        // Notebook URL validation
        binding.etNotebookUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateNotebookUrl(s.toString().trim())
            }
        })
    }

    private fun validateShortUrl(shortUrl: String) {
        // Character validation is handled by InputFilter, so we only need length checks
        val minLength = resources.getInteger(R.integer.short_url_min_length)
        val maxLength = resources.getInteger(R.integer.short_url_max_length)

        if (shortUrl.length < minLength) {
            binding.tilShortUrl.error = getString(R.string.short_url_too_short, minLength)
            binding.tilShortUrl.setEndIconDrawable(R.drawable.ic_error)
            return
        }
        if (shortUrl.length > maxLength) {
            binding.tilShortUrl.error = getString(R.string.short_url_too_long, maxLength)
            binding.tilShortUrl.setEndIconDrawable(R.drawable.ic_error)
            return
        }

        // Clear error if validation passes
        binding.tilShortUrl.error = null
        binding.tilShortUrl.setEndIconDrawable(R.drawable.ic_check)

        // Server-side availability check disabled - no longer needed after the database changes
        // checkShortUrlAvailability(shortUrl)
    }

    private fun clearShortUrlValidation() {
        binding.tilShortUrl.error = null
        binding.tilShortUrl.setEndIconDrawable(R.drawable.ic_help)
    }

    // Short URL availability lookup is no longer needed after the database changes.
    // Kept commented out in case the check has to be restored later.
    /*
    private fun checkShortUrlAvailability(shortUrl: String) {
        lifecycleScope.launch {
            try {
                val response = vexTeamsApiService.checkShortUrl(shortUrl)
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.available == true) {
                        binding.tilShortUrl.error = null
                        binding.tilShortUrl.setEndIconDrawable(R.drawable.ic_check)
                    } else {
                        binding.tilShortUrl.error =
                            result?.message ?: getString(R.string.short_url_unavailable)
                        binding.tilShortUrl.setEndIconDrawable(R.drawable.ic_error)
                    }
                }
            } catch (e: Exception) {
                Log.w("RegisterNotebookActivity", "Error checking short URL: ${e.message}")
            }
        }
    }
    */

    private fun validateNotebookUrl(url: String) {
        if (url.isEmpty()) {
            binding.tilNotebookUrl.error = null
            return
        }

        when {
            !url.startsWith("https://") ->
                binding.tilNotebookUrl.error = getString(R.string.url_must_start_https)
            url.length < 10 ->
                binding.tilNotebookUrl.error = getString(R.string.url_incomplete)
            else -> binding.tilNotebookUrl.error = null
        }
    }

    private fun loadFormData() {
        binding.progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            try {
                // First load redirection types
                val redirectionTypesResponse = vexTeamsApiService.getRedirectionTypes()
                if (redirectionTypesResponse.isSuccessful) {
                    redirectionTypes = redirectionTypesResponse.body() ?: emptyList()
                    setupRedirectionTypeDropdown()
                }

                // Then load teams
                val teamsResponse = vexTeamsApiService.getTeams()
                if (teamsResponse.isSuccessful) {
                    teams = teamsResponse.body() ?: emptyList()
                    setupTeamDropdown()
                }

                DebugLogger.d(
                    "RegisterNotebookActivity",
                    "Form data loaded: ${redirectionTypes.size} types, ${teams.size} teams"
                )

            } catch (e: Exception) {
                Log.e("RegisterNotebookActivity", "Error loading form data: ${e.message}")
                showError(getString(R.string.registration_error, e.message ?: ""))
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun setupRedirectionTypeDropdown() {
        val adapter = ArrayAdapter(
            this,
            R.layout.dropdown_menu_popup_item,  // ✅ Use custom layout with proper styling
            redirectionTypes.map { it.redirectionTypeName }
        )
        binding.actvRedirectionType.setAdapter(adapter)
        binding.actvRedirectionType.setOnItemClickListener { _, _, position, _ ->
            selectedRedirectionTypeId = redirectionTypes[position].redirectionTypeId
            DebugLogger.d(
                "RegisterNotebookActivity",
                "Selected redirection type: ${redirectionTypes[position].redirectionTypeName}"
            )
        }

        if (redirectionTypes.isNotEmpty()) {
            binding.actvRedirectionType.setText(redirectionTypes[0].redirectionTypeName, false)
            selectedRedirectionTypeId = redirectionTypes[0].redirectionTypeId
            DebugLogger.d(
                "RegisterNotebookActivity",
                "Default redirection type set: ${redirectionTypes[0].redirectionTypeName}"
            )
        }
    }

    private fun setupTeamDropdown() {
        val adapter = ArrayAdapter(
            this,
            R.layout.dropdown_menu_popup_item,  // ✅ Use custom layout with proper styling
            teams.map { it.teamName }
        )
        binding.actvTeam.setAdapter(adapter)
        binding.actvTeam.setOnItemClickListener { _, _, position, _ ->
            selectedTeamId = teams[position].teamId
            DebugLogger.d("RegisterNotebookActivity", "Selected team: ${teams[position].teamName}")
        }

        // Set first option as default
        if (teams.isNotEmpty()) {
            binding.actvTeam.setText(teams[0].teamName, false)
            selectedTeamId = teams[0].teamId
            DebugLogger.d("RegisterNotebookActivity", "Default team set: ${teams[0].teamName}")
        }
    }

    private fun showDateTimePicker(onDateTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            TimePickerDialog(this, { _, hourOfDay, minute ->
                calendar.set(year, month, dayOfMonth, hourOfDay, minute)
                val formattedDateTime = dateTimeFormat.format(calendar.time)
                onDateTimeSelected(formattedDateTime)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun validateAndSubmit() {
        val shortUrl = binding.etShortUrl.text.toString().trim()
        val notebookUrl = binding.etNotebookUrl.text.toString().trim()
        val startDate = binding.etStartDate.text.toString().trim()
        val endDate = binding.etEndDate.text.toString().trim()
        val preventMobile = binding.cbPreventMobileViewing.isChecked

        var hasErrors = false

        if (shortUrl.isEmpty()) {
            binding.tilShortUrl.error = getString(R.string.short_url_required)
            hasErrors = true
        }

        if (notebookUrl.isEmpty()) {
            binding.tilNotebookUrl.error = getString(R.string.notebook_url_required)
            hasErrors = true
        } else if (!notebookUrl.startsWith("https://")) {
            binding.tilNotebookUrl.error = getString(R.string.url_must_start_https)
            hasErrors = true
        }

        if (startDate.isEmpty()) {
            binding.tilStartDate.error = getString(R.string.start_date_required)
            hasErrors = true
        }

        if (endDate.isEmpty()) {
            binding.tilEndDate.error = getString(R.string.end_date_required)
            hasErrors = true
        }

        if (selectedRedirectionTypeId == null) {
            binding.tilRedirectionType.error = getString(R.string.redirection_type_required)
            hasErrors = true
        } else {
            binding.tilRedirectionType.error = null
        }

        if (selectedTeamId == null) {
            binding.tilTeam.error = getString(R.string.team_required)
            hasErrors = true
        } else {
            binding.tilTeam.error = null
        }

        if (hasErrors) {
            Toast.makeText(this, getString(R.string.fix_errors_above), Toast.LENGTH_LONG).show()
            return
        }

        submitRegistration(shortUrl, notebookUrl, startDate, endDate, selectedRedirectionTypeId!!, selectedTeamId!!, preventMobile)
    }

    private fun submitRegistration(
        shortUrl: String,
        notebookUrl: String,
        startDate: String,
        endDate: String,
        redirectionTypeId: Int,
        teamId: Int,
        preventMobile: Boolean
    ) {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnRegister.isEnabled = false

        lifecycleScope.launch {
            try {
                // Convert dates from local time to UTC for API
                val startDateTime = convertToApiDateTime(startDate)
                val endDateTime = convertToApiDateTime(endDate)

                DebugLogger.d("RegisterNotebookActivity", "Local times: Start=$startDate, End=$endDate")
                DebugLogger.d("RegisterNotebookActivity", "UTC times: Start=$startDateTime, End=$endDateTime")

                val request = RegisterUrlRequest(
                    shortUrl = shortUrl,
                    targetUrl = notebookUrl,
                    startDate = startDateTime,  // Now in UTC
                    endDate = endDateTime,      // Now in UTC
                    redirectionTypeId = redirectionTypeId,
                    teamId = teamId,
                    allowViewingOnMobile = !preventMobile
                )

                val response = vexTeamsApiService.registerUrl(request)

                if (response.isSuccessful) {
                    val registerResponse = response.body()
                    if (registerResponse?.success == true) {
                        Toast.makeText(
                            this@RegisterNotebookActivity,
                            getString(R.string.registration_success),
                            Toast.LENGTH_LONG
                        ).show()
                        DebugLogger.d("RegisterNotebookActivity", "Registration successful: ${registerResponse.shortUrl}")
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        showError(registerResponse?.message ?: getString(R.string.registration_failed))
                    }
                } else {
                    // Parse error response body to get the actual server error message
                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string()
                        if (errorBody != null) {
                            // Parse JSON to extract message field
                            val jsonObject = org.json.JSONObject(errorBody)
                            jsonObject.optString("message", null)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("RegisterNotebookActivity", "Failed to parse error body", e)
                        null
                    }

                    // Show parsed message or fall back to generic error
                    showError(errorMessage ?: getString(R.string.server_error, response.code(), response.message()))
                }

            } catch (e: Exception) {
                Log.e("RegisterNotebookActivity", "Registration error: ${e.message}")
                showError(getString(R.string.registration_error, e.message ?: ""))
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnRegister.isEnabled = true
            }
        }
    }

    /**
     * Converts local date/time string to UTC format for API
     * Input: "2025-01-15 23:00" (local time)
     * Output: "2025-01-16 04:00:00" (UTC time, assuming EST timezone)
     */
    private fun convertToApiDateTime(dateTime: String): String {
        return try {
            // Parse the date in local timezone (what user entered)
            val date = dateTimeFormat.parse(dateTime)
            if (date != null) {
                // Format in UTC timezone (what API expects)
                apiDateTimeFormat.format(date)
            } else {
                Log.e("RegisterNotebookActivity", "Failed to parse date: $dateTime")
                "$dateTime:00"
            }
        } catch (e: Exception) {
            Log.e("RegisterNotebookActivity", "Date conversion failed for: $dateTime", e)
            "$dateTime:00"  // Fallback: add seconds if missing
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, "$message", Toast.LENGTH_LONG).show()
        Log.e("RegisterNotebookActivity", message)
    }
}