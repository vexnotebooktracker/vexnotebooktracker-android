package org.vexteams.vexnotebooktracker.ui.contact

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.vexteams.vexnotebooktracker.R
import org.vexteams.vexnotebooktracker.databinding.ActivityContactBinding
import org.vexteams.vexnotebooktracker.utils.DebugLogger

@AndroidEntryPoint
class ContactActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactBinding
    private val viewModel: ContactViewModel by viewModels()

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, ContactActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Hide the default action bar so our toolbar is visible
            supportActionBar?.hide()

            binding = ActivityContactBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupToolbar()
            setupClickListeners()
            setupBackPressHandler()
            observeViewModel()

            DebugLogger.d("ContactActivity", "📧 Contact screen initialized")

        } catch (e: Exception) {
            Log.e("ContactActivity", "💥 Error in onCreate", e)
            Toast.makeText(
                this,
                getString(R.string.error_loading_contact, e.message ?: "Unknown error"),
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    private fun setupToolbar() {
        // Set title from string resource
        binding.toolbar.title = getString(R.string.contact_title)
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)

        binding.toolbar.setNavigationOnClickListener {
            DebugLogger.d("ContactActivity", "⬅️ Back button clicked")
            finish()
        }
    }

    private fun setupBackPressHandler() {
        // Modern way to handle back press (replaces deprecated onBackPressed)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                DebugLogger.d("ContactActivity", "⬅️ System back button pressed")
                finish()
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnSendFeedback.setOnClickListener {
            val feedback = binding.etFeedback.text.toString()
            viewModel.sendFeedback(feedback)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                // Update loading state
                binding.progressIndicator.isVisible = state.isLoading
                binding.btnSendFeedback.isEnabled = !state.isLoading

                // Update button text from string resources
                if (state.isLoading) {
                    binding.btnSendFeedback.text = getString(R.string.sending_feedback)
                } else {
                    binding.btnSendFeedback.text = getString(R.string.send_feedback_button)
                }

                // Show error message
                state.errorMessage?.let { message ->
                    binding.tilFeedback.error = message
                    Toast.makeText(
                        this@ContactActivity,
                        getString(R.string.feedback_error, message),
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.clearError()
                }

                // Show success message and hide input/button
                state.successMessage?.let { message ->
                    binding.tilFeedback.error = null

                    // Hide the input field and button
                    binding.tilFeedback.isVisible = false
                    binding.btnSendFeedback.isVisible = false

                    // Show the success message card
                    binding.cardSuccessMessage.isVisible = true
                    binding.tvSuccessMessage.text = message

                    viewModel.clearSuccess()

                    // Clear the feedback text field
                    if (state.feedbackSent) {
                        binding.etFeedback.text?.clear()
                        viewModel.resetFeedbackSent()
                    }
                }
            }
        }
    }
}