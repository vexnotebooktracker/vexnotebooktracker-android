package org.vexteams.vexnotebooktracker.ui.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import org.vexteams.vexnotebooktracker.utils.DebugLogger
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.vexteams.vexnotebooktracker.R
import org.vexteams.vexnotebooktracker.databinding.ActivityMainBinding
import org.vexteams.vexnotebooktracker.ui.login.LoginActivity

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()  // Hilt ViewModel injection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupMaterialUI()
            setupClickListeners()
            observeViewModel()

            DebugLogger.d("MainActivity", "🎉 MainActivity created with ViewModel + Hilt!")

        } catch (e: Exception) {
            Log.e("MainActivity", "💥 Error in onCreate", e)
            showError(getString(R.string.error_main_activity, e.message))
        }
    }

    private fun setupMaterialUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        binding.toolbar.title = getString(R.string.app_name)

        DebugLogger.d("MainActivity", "🎨 Material UI setup with ViewModel")
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            DebugLogger.d("MainActivity", "🔥 Logout button clicked (ViewModel)!")
            viewModel.logout()  // ✅ Using ViewModel
        }

        binding.cardWelcome.setOnClickListener {
            viewModel.onWelcomeCardClicked()  // ✅ Using ViewModel
        }

        binding.cardUserInfo.setOnClickListener {
            viewModel.onUserInfoCardClicked()  // ✅ Using ViewModel
        }

        binding.cardFeatures.setOnClickListener {
            viewModel.onFeaturesCardClicked()  // ✅ Using ViewModel
        }

        DebugLogger.d("MainActivity", "🎯 Click listeners set up with ViewModel")
    }

    private fun observeViewModel() {
        // Observe UI State
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (!state.isLoading) {
                    val userInfoText = getString(
                        R.string.user_info_display,
                        state.userEmail,
                        state.teamNumber,
                        state.loginTime
                    )
                    binding.tvUserInfo.text = userInfoText
                }

                state.errorMessage?.let { message ->
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }

                state.successMessage?.let { message ->
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    viewModel.clearSuccess()
                }
            }
        }

        // Observe Navigation Events
        lifecycleScope.launch {
            viewModel.navigationEvent.collect { event ->
                event?.let {
                    when (it) {
                        is MainNavigationEvent.NavigateToLogin -> {
                            DebugLogger.d("MainActivity", "🚪 Navigating to LoginActivity via ViewModel!")
                            val intent = Intent(this@MainActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                        is MainNavigationEvent.NavigateToProfile -> {
                            Toast.makeText(this@MainActivity, R.string.profile_coming_soon, Toast.LENGTH_SHORT).show()
                        }
                        is MainNavigationEvent.NavigateToFeatures -> {
                            Toast.makeText(this@MainActivity, R.string.features_coming_soon, Toast.LENGTH_SHORT).show()
                        }
                    }
                    viewModel.clearNavigationEvent()
                }
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e("MainActivity", "❌ $message")
    }
}