package org.vexteams.vexnotebooktracker.ui.dashboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import org.vexteams.vexnotebooktracker.utils.DebugLogger
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
// import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import org.vexteams.vexnotebooktracker.databinding.ActivityDashboardBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import org.vexteams.vexnotebooktracker.network.DashboardUrlRecord
// import org.vexteams.vexnotebooktracker.network.UrlViewLogEntry
import org.vexteams.vexnotebooktracker.network.VexTeamsApiService
import org.vexteams.vexnotebooktracker.repository.AuthRepository
import org.vexteams.vexnotebooktracker.repository.Resource
import org.vexteams.vexnotebooktracker.ui.login.LoginActivity
import org.vexteams.vexnotebooktracker.ui.register.RegisterNotebookActivity
import org.vexteams.vexnotebooktracker.utils.TimeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.view.LayoutInflater
import org.vexteams.vexnotebooktracker.R
import org.vexteams.vexnotebooktracker.ui.editprofile.EditProfileActivity
import org.vexteams.vexnotebooktracker.ui.about.AboutActivity
import org.vexteams.vexnotebooktracker.ui.contact.ContactActivity
// import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// Type alias to resolve import conflict
private typealias TableRowData = org.vexteams.vexnotebooktracker.models.TableRowData

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var tableAdapter: TableDataAdapter
    private var dashboardData: List<DashboardUrlRecord> = emptyList()
    private var isReturningFromRegistration = false

    @Inject
    lateinit var vexTeamsApiService: VexTeamsApiService

    @Inject
    lateinit var authRepository: AuthRepository

    companion object {
        private const val REQUEST_CODE_REGISTER = 1001
        private const val CONTACT_EMAIL = "support@vexteams.org"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityDashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupRecyclerView()
            setupClickListeners()
            handleVexData()

            DebugLogger.d("DashboardActivity", "Dashboard created with VEX API table!")

        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error in onCreate", e)
            showError("DashboardActivity Error: ${e.message}")
        }
    }

    private fun setupRecyclerView() {
        tableAdapter = TableDataAdapter { tableRowData ->
            onTableRowClicked(tableRowData)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = tableAdapter
        }

        DebugLogger.d("DashboardActivity", "RecyclerView setup completed")
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            logout()
        }

        // Register button click handler
        binding.btnRegister.setOnClickListener {
            onRegisterClicked()
        }

        // Edit Profile button click handler
        binding.btnEditProfile.setOnClickListener {
            navigateToEditProfile()
        }

        // Delete Profile button click handler
        binding.btnDeleteProfile.setOnClickListener {
            onDeleteProfileClicked()
        }

        // Contact button click handler
        binding.btnContact.setOnClickListener {
            onContactClicked()
        }

        // About button click handler
        binding.btnAbout.setOnClickListener {
            onAboutClicked()
        }

        // ✅ NEW: Refresh FAB button click handler
        binding.fab.setOnClickListener {
            DebugLogger.d("DashboardActivity", "Refresh button clicked - reloading dashboard data")
            refreshDashboardData()
        }
    }

    private fun navigateToEditProfile() {
        try {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
            Log.d("Dashboard", "Navigating to Edit Profile")
        } catch (e: Exception) {
            Log.e("Dashboard", "Error navigating to Edit Profile", e)
            Toast.makeText(this, getString(R.string.error_opening_profile_editor), Toast.LENGTH_SHORT).show()
        }
    }

    // Handle Contact button click
    private fun onContactClicked() {
        try {
            ContactActivity.start(this)
            DebugLogger.d("DashboardActivity", "Opening Contact screen")
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error opening Contact screen", e)
            Toast.makeText(this, getString(R.string.unable_to_open_contact_screen), Toast.LENGTH_SHORT).show()
        }
    }

    // Handle About button click
    private fun onAboutClicked() {
        try {
            AboutActivity.start(this)
            DebugLogger.d("DashboardActivity", "Opening About screen")
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error opening About screen", e)
            Toast.makeText(this, getString(R.string.unable_to_open_about_screen), Toast.LENGTH_SHORT).show()
        }
    }

    // Handle Edit Profile click
    private fun onEditProfileClicked() {
        DebugLogger.d("DashboardActivity", "Edit Profile clicked")
        Toast.makeText(this, getString(R.string.edit_profile_coming_soon), Toast.LENGTH_SHORT).show()
        // TODO: Implement edit profile functionality
        // This will be implemented after delete profile is complete
    }

    // Handle Delete Profile click
    private fun onDeleteProfileClicked() {
        DebugLogger.d("DashboardActivity", "Delete Profile clicked - showing confirmation dialog")
        showDeleteProfileConfirmationDialog()
    }

    // Show Delete Profile Confirmation Dialog
    private fun showDeleteProfileConfirmationDialog() {
        // Create custom layout for confirmation dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_profile_confirmation, null)
        val confirmationInput = dialogView.findViewById<TextInputEditText>(R.id.etConfirmation)
        val confirmationLayout = dialogView.findViewById<TextInputLayout>(R.id.tilConfirmation)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_profile_title))
            .setMessage(getString(R.string.delete_profile_message))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                val confirmationText = confirmationInput.text.toString().trim()
                if (confirmationText.uppercase() == "DELETE") {
                    performDeleteProfile(confirmationText)
                } else {
                    Toast.makeText(this@DashboardActivity, getString(R.string.delete_profile_confirmation_invalid), Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .setIcon(R.drawable.ic_close)
            .create()

        dialog.show()

        // Style the positive button to be red (destructive action)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.red))
    }

    // Perform Delete Profile
    private fun performDeleteProfile(confirmationText: String) {
        DebugLogger.d("DashboardActivity", "Starting profile deletion process")

        lifecycleScope.launch {
            try {
                // Show loading
                binding.progressBar.visibility = android.view.View.VISIBLE
                Toast.makeText(this@DashboardActivity, getString(R.string.delete_profile_processing), Toast.LENGTH_SHORT).show()

                // Call delete profile API
                authRepository.deleteProfile(confirmationText).collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            DebugLogger.d("DashboardActivity", "Deleting profile...")
                        }

                        is Resource.Success -> {
                            DebugLogger.d("DashboardActivity", "Profile deletion successful")

                            // Show success message
                            Toast.makeText(this@DashboardActivity, "${resource.data}", Toast.LENGTH_LONG).show()

                            // Navigate to login screen after successful deletion
                            navigateToLoginAfterDeletion()
                        }

                        is Resource.Error -> {
                            DebugLogger.d("DashboardActivity", "Profile deletion failed: ${resource.message}")
                            Toast.makeText(this@DashboardActivity, "${resource.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }

            } catch (exception: Exception) {
                Log.e("DashboardActivity", "Error deleting profile: ${exception.message}")
                Toast.makeText(this@DashboardActivity, getString(R.string.error_deleting_notebook, exception.message ?: ""), Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    // Navigate to login after profile deletion
    private fun navigateToLoginAfterDeletion() {
        try {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

            DebugLogger.d("DashboardActivity", "Navigated to login screen after profile deletion")

        } catch (exception: Exception) {
            Log.e("DashboardActivity", "Navigation error after profile deletion: ${exception.message}")
            // Force finish activity
            finish()
        }
    }

    private fun handleVexData() {
        val hasVexData = intent.getBooleanExtra("has_vex_data", false)

        if (hasVexData) {
            val jsonData = intent.getStringExtra("vex_data_json")
            if (!jsonData.isNullOrEmpty()) {
                try {
                    val gson = Gson()

                    // Use Array instead of TypeToken to avoid R8 generic type erasure
                    // This approach is R8-safe and doesn't require TypeToken
                    val arrayData = gson.fromJson(jsonData, Array<DashboardUrlRecord>::class.java)
                    dashboardData = arrayData.toList()

                    DebugLogger.d("DashboardActivity", "Received ${dashboardData.size} URL records")
                    displayVexData(dashboardData)

                } catch (exception: Exception) {
                    Log.e("DashboardActivity", "Error parsing VEX data: ${exception.message}")
                    showNoDataState(getString(R.string.error_parsing_data, exception.message ?: ""), showToast = true)
                }
            } else {
                Log.w("DashboardActivity", "Has VEX data flag is true but no JSON data")
                showNoDataState(getString(R.string.no_registered_urls), showToast = false)
            }
        } else {
            Log.w("DashboardActivity", "No VEX API data received")
            showNoDataState(getString(R.string.no_registered_urls), showToast = false)
        }
    }

    private fun displayVexData(data: List<DashboardUrlRecord>) {
        if (data.isNotEmpty()) {
            // Convert DashboardUrlRecord to TableRowData format
            val tableData = convertToTableRowData(data)

            // Update adapter with converted data using ListAdapter method
            tableAdapter.submitList(tableData)

            // Simple message
            val teams = data.map { it.team.trim() }.distinct().sorted()
            val totalViews = data.sumOf { it.noOfViews }

            val message = getString(R.string.loaded_notebooks_format, data.size, teams.size)

            DebugLogger.d("DashboardActivity", "$message")
            DebugLogger.d("DashboardActivity", "Teams: ${teams.joinToString(", ")} | Total Views: $totalViews")

            // Just show basic loading message
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        } else {
            showNoDataState(getString(R.string.no_notebooks_found))
        }
    }

    private fun convertToTableRowData(records: List<DashboardUrlRecord>): List<TableRowData> {
        val tableData = mutableListOf<TableRowData>()

        records.forEach { record ->
            // Add URL row
            tableData.add(
                TableRowData(
                    field = getString(R.string.short_url),
                    value = record.shortUrl,
                    type = "url",
                    urlId = record.urlId
                )
            )

            // ADD: Small spacer between Short URL and Team
            tableData.add(
                TableRowData(
                    field = "",
                    value = "",
                    type = "spacer"
                )
            )

            // Add Team row
            tableData.add(
                TableRowData(
                    field = getString(R.string.team),
                    value = record.team.trim(),
                    type = "team",
                    urlId = record.urlId
                )
            )

            // ADD: Small spacer between Team and Views
            tableData.add(
                TableRowData(
                    field = "",
                    value = "",
                    type = "spacer"
                )
            )

            // CHANGED: Views row - now clickable when > 0
            tableData.add(
                TableRowData(
                    field = getString(R.string.views),
                    value = record.noOfViews.toString(),
                    type = "views",
                    urlId = record.urlId
                )
            )

            // ADD: Small spacer between Views and End Date
            tableData.add(
                TableRowData(
                    field = "",
                    value = "",
                    type = "spacer"
                )
            )

            // UPDATED: Format End Date using GMT->Local conversion
            tableData.add(
                TableRowData(
                    field = getString(R.string.end_date),
                    value = formatEndDate(record.endDate),
                    type = "date",
                    urlId = record.urlId
                )
            )

            // ADD: Small spacer before Delete
            tableData.add(
                TableRowData(
                    field = "",
                    value = "",
                    type = "spacer"
                )
            )

            // CLEAN: Delete option for each record
            tableData.add(
                TableRowData(
                    field = getString(R.string.delete_action),
                    value = "", // EMPTY: No extra text, just the field name and icon
                    type = "delete",
                    urlId = record.urlId
                )
            )

            // Add separator if not last record
            if (record != records.last()) {
                tableData.add(
                    TableRowData(
                        field = getString(R.string.separator_line),
                        value = getString(R.string.separator_line),
                        type = "separator"
                    )
                )
            }
        }

        return tableData
    }

    // UPDATED: Format End Date function with GMT->Local conversion
    private fun formatEndDate(dateString: String): String {
        return try {
            // NEW: Convert GMT time from server to local time for display
            // Server sends in GMT, we convert to local time with same display format
            val localDisplayTime = TimeUtils.gmtToLocalDisplay(dateString)

            DebugLogger.d("DashboardActivity", "Converted '$dateString' (GMT) -> '$localDisplayTime' (Local)")
            localDisplayTime

        } catch (exception: Exception) {
            Log.w("DashboardActivity", "Date formatting failed for: $dateString", exception)
            dateString // Return original if parsing fails
        }
    }

    private fun showNoDataState(reason: String = getString(R.string.no_registered_urls), showToast: Boolean = false) {
        Log.e("DashboardActivity", "No VEX API data received - $reason")

        // Only show toast for actual errors, not for empty data
        if (showToast) {
            Toast.makeText(this, "$reason", Toast.LENGTH_LONG).show()
        }

        // Show empty state in table with user-friendly message
        val emptyData = listOf(
            TableRowData(
                field = getString(R.string.no_data),
                value = reason,
                type = "default"
            )
        )
        tableAdapter.submitList(emptyData)
    }

    private fun onTableRowClicked(tableRowData: TableRowData) {
        when (tableRowData.type) {
            // CHANGED: Views are now clickable instead of URL
            "views" -> {
                if (tableRowData.urlId != null) {
                    val originalRecord = dashboardData.find { it.urlId == tableRowData.urlId }
                    if (originalRecord != null && originalRecord.noOfViews > 0) {
                        startUrlViewLogActivity(tableRowData.urlId, originalRecord.shortUrl)
                    } else {
                        Toast.makeText(this, getString(R.string.no_views_available), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            // NEW: Handle delete clicks
            "delete" -> {
                if (tableRowData.urlId != null) {
                    showDeleteConfirmation(tableRowData.urlId)
                }
            }
        }

        DebugLogger.d("DashboardActivity", "Clicked: ${tableRowData.field} = ${tableRowData.value}")
    }

    // NEW: Show delete confirmation dialog
    private fun showDeleteConfirmation(urlId: Int) {
        val record = dashboardData.find { it.urlId == urlId }
        if (record != null) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_notebook_title))
                .setMessage(getString(R.string.delete_notebook_message, record.shortUrl))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    deleteNotebook(urlId)
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    // COMPLETED: Delete notebook with actual API call
    private fun deleteNotebook(urlId: Int) {
        lifecycleScope.launch {
            try {
                // Show loading
                binding.progressBar.visibility = android.view.View.VISIBLE

                // Make actual API call to delete the URL
                val response = vexTeamsApiService.deleteUrl(urlId)

                if (response.isSuccessful) {
                    val deleteResponse = response.body()

                    if (deleteResponse?.success == true) {
                        // Remove from local data
                        dashboardData = dashboardData.filter { it.urlId != urlId }

                        // Update UI
                        displayVexData(dashboardData)

                        val message = deleteResponse.message ?: getString(R.string.notebook_deleted_successfully)
                        Toast.makeText(this@DashboardActivity, "$message", Toast.LENGTH_SHORT).show()
                        DebugLogger.d("DashboardActivity", "Notebook deleted: URL ID $urlId")
                    } else {
                        val errorMessage = deleteResponse?.message ?: getString(R.string.failed_to_delete_notebook)
                        Toast.makeText(this@DashboardActivity, "$errorMessage", Toast.LENGTH_LONG).show()
                        Log.e("DashboardActivity", "Delete failed: $errorMessage")
                    }
                } else {
                    val errorMessage = getString(R.string.server_error_format, response.code(), response.message())
                    Toast.makeText(this@DashboardActivity, "$errorMessage", Toast.LENGTH_LONG).show()
                    Log.e("DashboardActivity", "Delete API error: $errorMessage")
                }

            } catch (exception: Exception) {
                Log.e("DashboardActivity", "Error deleting notebook: ${exception.message}")
                Toast.makeText(this@DashboardActivity, getString(R.string.error_deleting_notebook, exception.message ?: ""), Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    // MODIFIED: Register new notebook with activity result
    private fun onRegisterClicked() {
        DebugLogger.d("DashboardActivity", "Register new notebook clicked")

        // Set flag to indicate we're going to registration
        isReturningFromRegistration = true

        // Launch RegisterNotebookActivity for result
        val intent = Intent(this, RegisterNotebookActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_REGISTER)
    }

    // Handle result from registration activity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_REGISTER && resultCode == RESULT_OK) {
            DebugLogger.d("DashboardActivity", "Registration completed successfully - refreshing data")
            refreshDashboardData()
        }
    }

    // NEW: Handle result from registration activity
    override fun onResume() {
        super.onResume()

        // Only refresh if we're returning from registration
        if (isReturningFromRegistration) {
            DebugLogger.d("DashboardActivity", "Resuming dashboard after registration - refreshing data")
            refreshDashboardData()
            isReturningFromRegistration = false
        } else {
            DebugLogger.d("DashboardActivity", "Resuming dashboard - no refresh needed")
        }
    }

    // New method to refresh dashboard data
    private fun refreshDashboardData() {
        lifecycleScope.launch {
            try {
                // Show loading indicator
                binding.progressBar.visibility = android.view.View.VISIBLE

                DebugLogger.d("DashboardActivity", "Fetching fresh dashboard data...")

                // Fetch fresh data from API
                val response = vexTeamsApiService.getDashboardData()

                if (response.isSuccessful && response.body() != null) {
                    val freshData = response.body()!!

                    // Update local data
                    dashboardData = freshData

                    // Update UI
                    displayVexData(freshData)

                    DebugLogger.d("DashboardActivity", "Dashboard data refreshed successfully - ${freshData.size} records")
                    Toast.makeText(this@DashboardActivity, getString(R.string.data_refreshed), Toast.LENGTH_SHORT).show()

                } else {
                    Log.e("DashboardActivity", "Failed to refresh data: ${response.code()}")
                    Toast.makeText(this@DashboardActivity, getString(R.string.failed_to_refresh_data), Toast.LENGTH_SHORT).show()
                }

            } catch (exception: Exception) {
                Log.e("DashboardActivity", "Error refreshing dashboard data: ${exception.message}")
                Toast.makeText(this@DashboardActivity, getString(R.string.error_refreshing_data, exception.message ?: ""), Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    // Start the URL View Log Activity
    private fun startUrlViewLogActivity(urlId: Int, shortUrl: String) {
        val title = getString(R.string.view_log_title_format, shortUrl)
        UrlViewLogActivity.start(this, urlId, title)
        DebugLogger.d("DashboardActivity", "Starting UrlViewLogActivity for URL ID: $urlId")
    }

    // Helper function for the adapter to check views count
    fun getViewsForUrlId(urlId: Int): Int {
        return dashboardData.find { it.urlId == urlId }?.noOfViews ?: 0
    }

    // FIXED: Proper logout with API call
    private fun logout() {
        DebugLogger.d("DashboardActivity", "Starting logout process...")

        lifecycleScope.launch {
            try {
                // Show loading
                binding.progressBar.visibility = android.view.View.VISIBLE

                // Call API logout
                authRepository.mobileLogout().collectLatest { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            DebugLogger.d("DashboardActivity", "Logging out from server...")
                        }

                        is Resource.Success -> {
                            DebugLogger.d("DashboardActivity", "Server logout successful")
                            navigateToLogin(getString(R.string.logged_out_successfully))
                        }

                        is Resource.Error -> {
                            Log.w("DashboardActivity", "Server logout error: ${resource.message}")
                            // Still navigate to login even if server logout fails
                            navigateToLogin(getString(R.string.logged_out_server_error))
                        }
                    }
                }

            } catch (exception: Exception) {
                Log.e("DashboardActivity", "Logout error: ${exception.message}")
                // Still navigate to login
                navigateToLogin(getString(R.string.logout_error))
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    // NEW: Helper method for navigation
    private fun navigateToLogin(message: String) {
        try {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            DebugLogger.d("DashboardActivity", "Navigated to login screen")

        } catch (exception: Exception) {
            Log.e("DashboardActivity", "Navigation error: ${exception.message}")
            // Force finish activity
            finish()
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e("DashboardActivity", message)
    }
}