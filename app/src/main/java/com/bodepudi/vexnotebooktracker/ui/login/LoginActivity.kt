package com.bodepudi.vexnotebooktracker.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.bodepudi.vexnotebooktracker.utils.DebugLogger
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.bodepudi.vexnotebooktracker.R
import com.bodepudi.vexnotebooktracker.databinding.ActivityLoginBinding
import com.bodepudi.vexnotebooktracker.ui.dashboard.DashboardActivity
import com.bodepudi.vexnotebooktracker.ui.signup.SignUpActivity  // ✅ NEW IMPORT
import com.bodepudi.vexnotebooktracker.ui.contact.ContactActivity  // ✅ NEW IMPORT
import com.bodepudi.vexnotebooktracker.ui.about.AboutActivity  // ✅ NEW IMPORT
import com.bodepudi.vexnotebooktracker.ui.simplyfoursteps.SimplyFourStepsActivity  // ✅ NEW IMPORT
import com.google.gson.Gson
import com.bodepudi.vexnotebooktracker.network.DashboardUrlRecord
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import com.bodepudi.vexnotebooktracker.utils.WindowInsetsUtils
// import org.vexteams.vexnotebooktracker.BuildConfig  // ✅ ADD THIS LINE

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ✅ TEMPORARY TEST - Add this
        // Log.e("BuildConfig_TEST", "DEBUG = ${BuildConfig.DEBUG}")
        // Log.e("BuildConfig_TEST", "BUILD_TYPE = ${BuildConfig.BUILD_TYPE}")
        // Log.e("BuildConfig_TEST", "API_BASE_URL = ${BuildConfig.API_BASE_URL}")
        try {
            binding = ActivityLoginBinding.inflate(layoutInflater)
            // Android 16 (API 36) enforces edge-to-edge; a dark system bar style keeps the
            // white status bar icons this app has always used over its orange app bar.
            enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
            setContentView(binding.root)

            WindowInsetsUtils.applyStatusBarScrim(binding.statusBarScrim)
            WindowInsetsUtils.applyContentInsets(
                binding.contentContainer,
                handleIme = true,
                includeTopInset = true
            )

            setupClickListeners()
            observeViewModel()

            DebugLogger.d("LoginActivity", "✅ Activity created with VEX API integration!")

        } catch (e: Exception) {
            Log.e("LoginActivity", "❌ Error in onCreate", e)
            showError(getString(R.string.login_activity_error, e.message))
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            DebugLogger.d("LoginActivity", "🔐 Login button clicked - will call VEX API!")
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            viewModel.login(email, password)
        }

        binding.btnForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            // ✅ Check if email is empty before proceeding
            if (email.isEmpty()) {
                binding.etEmail.error = getString(R.string.email_required)
                binding.etEmail.requestFocus()
                Toast.makeText(this, R.string.specify_email_reset, Toast.LENGTH_LONG).show()
            } else {
                viewModel.forgotPassword(email)
            }
        }

        // ✅ UPDATED: Navigate to SignUpActivity instead of showing toast
        binding.btnSignUp.setOnClickListener {
            DebugLogger.d("LoginActivity", "📝 Sign-up button clicked - navigating to SignUpActivity")
            navigateToSignUp()
        }

        // ✅ NEW: Contact Us button
        binding.btnContactUs.setOnClickListener {
            DebugLogger.d("LoginActivity", "📧 Contact Us button clicked")
            navigateToContact()
        }

        // ✅ NEW: About button
        binding.btnAbout.setOnClickListener {
            DebugLogger.d("LoginActivity", "ℹ️ About button clicked")
            navigateToAbout()
        }

        // ✅ NEW: Simply Four Steps button
        binding.btnSimplyFourSteps.setOnClickListener {
            DebugLogger.d("LoginActivity", "📋 Simply Four Steps button clicked")
            navigateToSimplyFourSteps()
        }

        DebugLogger.d("LoginActivity", "✅ Click listeners set up")
    }

    private fun observeViewModel() {
        // Observe UI State
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressIndicator.isVisible = state.isLoading
                binding.btnLogin.isEnabled = !state.isLoading
                binding.btnForgotPassword.isEnabled = !state.isLoading

                if (state.isLoading) {
                    binding.btnLogin.text = getString(R.string.signing_in)
                    // ✅ Clear any previous error messages when starting login
                    clearErrorState()
                } else {
                    binding.btnLogin.text = getString(R.string.sign_in)
                }

                state.errorMessage?.let { message ->
                    showError(message)
                    viewModel.clearError()
                }

                state.successMessage?.let { message ->
                    showSuccess(message)
                    viewModel.clearSuccess()
                }
            }
        }

        // Observe Navigation Events
        lifecycleScope.launch {
            viewModel.navigationEvent.collect { event ->
                event?.let {
                    when (it) {
                        is NavigationEvent.NavigateToMain -> {
                            DebugLogger.d("LoginActivity", "✅ Login successful! Navigating to Dashboard with VEX data")
                            val intent = Intent(this@LoginActivity, DashboardActivity::class.java)

                            // ✅ Handle VEX data safely and pass actual data
                            @Suppress("UNCHECKED_CAST")
                            if (it.vexData is List<*> && (it.vexData as? List<*>)?.firstOrNull() is DashboardUrlRecord) {
                                val urlRecords = it.vexData as List<DashboardUrlRecord>

                                // Pass the actual data as JSON string
                                val gson = Gson()
                                val jsonData = gson.toJson(urlRecords)

                                intent.putExtra("has_vex_data", true)
                                intent.putExtra("vex_data_json", jsonData)
                                intent.putExtra("url_records_count", urlRecords.size)
                                DebugLogger.d("LoginActivity", "VEX data available: ${urlRecords.size} URL records")
                            } else {
                                intent.putExtra("has_vex_data", false)
                                DebugLogger.d("LoginActivity", "No VEX data available")
                            }

                            startActivity(intent)
                            finish()
                        }
                        is NavigationEvent.NavigateToSignUp -> {
                            // TODO: Navigate to SignUpActivity
                            Toast.makeText(this@LoginActivity, R.string.navigate_to_signup, Toast.LENGTH_SHORT).show()
                        }
                        is NavigationEvent.NavigateToLogin -> {
                            // Already on login screen
                            DebugLogger.d("LoginActivity", "Already on login screen")
                        }
                        is NavigationEvent.NavigateToForgotPassword -> {
                            // TODO: Navigate to ForgotPasswordActivity
                            Toast.makeText(this@LoginActivity, R.string.navigate_to_forgot_password, Toast.LENGTH_SHORT).show()
                        }
                    }
                    viewModel.clearNavigationEvent()
                }
            }
        }
    }

    // ✅ NEW: Navigate to Sign-Up Activity
    private fun navigateToSignUp() {
        try {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            Log.d("LoginActivity", "✅ Successfully navigated to SignUpActivity")
        } catch (e: Exception) {
            Log.e("LoginActivity", "❌ Error navigating to SignUpActivity", e)
            Toast.makeText(this, R.string.error_opening_signup, Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ NEW: Navigate to Contact Activity
    private fun navigateToContact() {
        try {
            ContactActivity.start(this)
            Log.d("LoginActivity", "✅ Successfully navigated to ContactActivity")
        } catch (e: Exception) {
            Log.e("LoginActivity", "❌ Error navigating to ContactActivity", e)
            Toast.makeText(this, R.string.error_opening_contact, Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ NEW: Navigate to About Activity
    private fun navigateToAbout() {
        try {
            AboutActivity.start(this)
            Log.d("LoginActivity", "✅ Successfully navigated to AboutActivity")
        } catch (e: Exception) {
            Log.e("LoginActivity", "❌ Error navigating to AboutActivity", e)
            Toast.makeText(this, R.string.error_opening_about, Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ NEW: Navigate to Simply Four Steps Activity
    private fun navigateToSimplyFourSteps() {
        try {
            SimplyFourStepsActivity.start(this)
            Log.d("LoginActivity", "✅ Successfully navigated to SimplyFourStepsActivity")
        } catch (e: Exception) {
            Log.e("LoginActivity", "❌ Error navigating to SimplyFourStepsActivity", e)
            Toast.makeText(this, R.string.error_opening_simply_four_steps, Toast.LENGTH_SHORT).show()
        }
    }

    private fun shakeButton() {
        binding.btnLogin.animate()
            .translationX(25f)
            .setDuration(100)
            .withEndAction {
                binding.btnLogin.animate()
                    .translationX(-25f)
                    .setDuration(100)
                    .withEndAction {
                        binding.btnLogin.animate()
                            .translationX(0f)
                            .setDuration(100)
                    }
            }
    }

    private fun showError(message: String) {
        // Show toast with error icon
        Toast.makeText(this, getString(R.string.error_prefix, message), Toast.LENGTH_LONG).show()

        // Add visual feedback to the form
        binding.etEmail.error = null
        binding.etPassword.error = null

        // Determine which field to highlight based on error message
        when {
            message.contains("email", ignoreCase = true) -> {
                binding.etEmail.error = getString(R.string.check_email)
                binding.etEmail.requestFocus()
            }
            message.contains("password", ignoreCase = true) -> {
                binding.etPassword.error = getString(R.string.check_password)
                binding.etPassword.requestFocus()
            }
            message.contains("credential", ignoreCase = true) ||
                    message.contains("invalid", ignoreCase = true) -> {
                // Highlight both fields for credential errors
                binding.etEmail.error = getString(R.string.invalid_credentials)
                binding.etPassword.error = getString(R.string.invalid_credentials)
                binding.etEmail.requestFocus()
            }
        }

        // Shake the login button for visual feedback
        shakeButton()

        Log.e("LoginActivity", getString(R.string.login_error, message))
    }

    // ✅ NEW: Handle returning from SignUpActivity
    override fun onResume() {
        super.onResume()
        // Check if returning from successful sign-up
        Log.d("LoginActivity", "🔄 Resuming LoginActivity")
    }

    // ✅ NEW: Success message display
    private fun showSuccess(message: String) {
        Toast.makeText(this, getString(R.string.success_prefix, message), Toast.LENGTH_SHORT).show()
        Log.d("LoginActivity", getString(R.string.success_message, message))
    }

    // ✅ NEW: Clear error state
    private fun clearErrorState() {
        binding.etEmail.error = null
        binding.etPassword.error = null
    }
}