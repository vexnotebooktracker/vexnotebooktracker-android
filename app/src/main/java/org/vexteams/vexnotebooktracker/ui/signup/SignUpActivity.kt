package org.vexteams.vexnotebooktracker.ui.signup

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.vexteams.vexnotebooktracker.R
import org.vexteams.vexnotebooktracker.databinding.ActivitySignUpBinding
import org.vexteams.vexnotebooktracker.ui.login.LoginActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@AndroidEntryPoint
class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private val viewModel: SignUpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Hide the default action bar so our toolbar is visible
            supportActionBar?.hide()

            binding = ActivitySignUpBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupToolbar()
            setupRoleDropdown()
            setupHelpIcons()
            setupValidation()
            setupClickListeners()
            setupBackPressHandler()
            observeViewModel()

            Log.d("SignUpActivity", "✅ Sign-up activity created successfully")

        } catch (e: Exception) {
            Log.e("SignUpActivity", "💥 Error in onCreate", e)
            showError(getString(R.string.error_signup_activity, e.message ?: "Unknown error"))
        }
    }

    private fun setupToolbar() {
        // Hide the default action bar so our toolbar is visible
        supportActionBar?.hide()

        // Set title from string resource
        binding.toolbar.title = getString(R.string.signup_title)
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupBackPressHandler() {
        // Modern way to handle back press (replaces deprecated onBackPressed)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d("SignUpActivity", "🔙 Back pressed - returning to login")
                finish()
            }
        })
    }

    private fun setupRoleDropdown() {
        val roleOptions = arrayOf(
            getString(R.string.role_team_member),
            getString(R.string.role_coach_advisor)
        )

        val adapter = ArrayAdapter(this, R.layout.dropdown_menu_popup_item, roleOptions)
        binding.etRole.setAdapter(adapter)

        // Set default selection
        binding.etRole.setText(roleOptions[0], false)

        // Re-validate team number when role changes
        binding.etRole.setOnItemClickListener { _, _, position, _ ->
            val teamNoWithLetter = binding.etTeamNumber.text.toString().trim()
            if (teamNoWithLetter.isNotEmpty()) {
                val role = if (position == 0) 2 else 3 // 0 = Team Member (2), 1 = Coach (3)
                viewModel.validateTeamNumber(teamNoWithLetter, role)
            }
        }
    }

    private fun setupHelpIcons() {
        // Team Number Help Icon
        binding.ivTeamNumberHelp.setOnClickListener {
            showHelpDialog(
                title = getString(R.string.help_team_number_title),
                message = getString(R.string.help_team_number_message)
            )
        }

        // Phone Number Help Icon
        binding.ivPhoneHelp.setOnClickListener {
            showHelpDialog(
                title = getString(R.string.help_phone_title),
                message = getString(R.string.help_phone_message)
            )
        }

        // Role Help Icon
        binding.ivRoleHelp.setOnClickListener {
            showHelpDialog(
                title = getString(R.string.help_role_title),
                message = getString(R.string.help_role_message)
            )
        }
    }

    private fun showHelpDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.got_it)) { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(R.drawable.ic_help_outline)
            .show()
    }

    private fun setupValidation() {
        // Email validation with availability check
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val email = s.toString().trim()
                if (email.isNotEmpty()) {
                    viewModel.checkEmailAvailability(email)
                }
                validateEmailMatch()
                updateEmailIcon()
            }
        })

        // Confirm email validation
        binding.etConfirmEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateEmailMatch()
                updateConfirmEmailIcon()
            }
        })

        // Password validation
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                viewModel.validatePassword(password)
                validatePasswordMatch()
                updatePasswordRequirements(password)
            }
        })

        // Confirm password validation
        binding.etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePasswordMatch()
                updateConfirmPasswordIcon()
            }
        })

        // First name validation
        binding.etFirstName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val name = s.toString().trim()
                if (name.length >= 2) {
                    binding.tilFirstName.error = null
                } else if (name.isNotEmpty()) {
                    binding.tilFirstName.error = getString(R.string.name_too_short)
                }
            }
        })

        // Last name validation
        binding.etLastName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val name = s.toString().trim()
                if (name.length >= 2) {
                    binding.tilLastName.error = null
                } else if (name.isNotEmpty()) {
                    binding.tilLastName.error = getString(R.string.name_too_short)
                }
            }
        })

        // Team number validation
        binding.etTeamNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val teamNoWithLetter = s.toString().trim()
                if (teamNoWithLetter.isNotEmpty()) {
                    val selectedRole = binding.etRole.text.toString()
                    val role = when (selectedRole) {
                        getString(R.string.role_team_member) -> 2
                        getString(R.string.role_coach_advisor) -> 3
                        else -> 2
                    }
                    viewModel.validateTeamNumber(teamNoWithLetter, role)
                }
            }
        })

        // Phone number validation
        binding.etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val phone = s.toString().trim()
                viewModel.validatePhoneNumber(phone)
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener {
            handleSignUp()
        }

        // "Already have an account" text click
        binding.tvAlreadyHaveAccount.setOnClickListener {
            finish()
        }
    }

    private fun handleSignUp() {
        val email = binding.etEmail.text.toString().trim()
        val confirmEmail = binding.etConfirmEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        val firstName = binding.etFirstName.text.toString().trim()
        val lastName = binding.etLastName.text.toString().trim()
        val selectedRole = binding.etRole.text.toString()
        val role = when (selectedRole) {
            getString(R.string.role_team_member) -> 2
            getString(R.string.role_coach_advisor) -> 3
            else -> 1
        }
        val teamNoWithLetter = binding.etTeamNumber.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        viewModel.signUp(
            email = email,
            confirmEmail = confirmEmail,
            password = password,
            confirmPassword = confirmPassword,
            firstName = firstName,
            lastName = lastName,
            role = role,
            teamNoWithLetter = teamNoWithLetter,
            phone = phone
        )
    }

    private fun observeViewModel() {
        // Observe UI State
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressIndicator.isVisible = state.isLoading
                binding.btnSignUp.isEnabled = !state.isLoading

                if (state.isLoading) {
                    binding.btnSignUp.text = getString(R.string.creating_account)
                } else {
                    binding.btnSignUp.text = getString(R.string.signup_button)
                }

                // Email availability
                binding.tilEmail.error = state.emailError

                // Password validation
                binding.tilPassword.error = state.passwordError

                // Team number validation
                binding.tilTeamNumber.error = state.teamNumberError

                // Phone validation
                binding.tilPhone.error = state.phoneError

                // General error message
                state.errorMessage?.let { message ->
                    Toast.makeText(
                        this@SignUpActivity,
                        getString(R.string.signup_error, message),
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.clearError()
                }

                // Success message
                state.successMessage?.let { message ->
                    Toast.makeText(
                        this@SignUpActivity,
                        getString(R.string.signup_success_toast, message),
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.clearSuccess()
                }
            }
        }

        // Observe Navigation Events
        lifecycleScope.launch {
            viewModel.navigationEvent.collect { event ->
                event?.let {
                    when (it) {
                        is SignUpNavigationEvent.NavigateToLogin -> {
                            Log.d("SignUpActivity", "✅ Sign-up successful! Navigating to login")
                            navigateToLogin()
                        }
                        is SignUpNavigationEvent.NavigateBack -> {
                            Log.d("SignUpActivity", "🔙 Navigating back")
                            finish()
                        }
                    }
                    viewModel.clearNavigationEvent()
                }
            }
        }
    }

    private fun validateEmailMatch() {
        val email = binding.etEmail.text.toString().trim()
        val confirmEmail = binding.etConfirmEmail.text.toString().trim()

        if (confirmEmail.isNotEmpty()) {
            if (email == confirmEmail) {
                binding.tilConfirmEmail.error = null
            } else {
                binding.tilConfirmEmail.error = getString(R.string.email_mismatch)
            }
        }
    }

    private fun validatePasswordMatch() {
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        if (confirmPassword.isNotEmpty()) {
            if (password == confirmPassword) {
                binding.tilConfirmPassword.error = null
            } else {
                binding.tilConfirmPassword.error = getString(R.string.password_mismatch)
            }
        }
    }

    private fun updateEmailIcon() {
        val email = binding.etEmail.text.toString().trim()
        if (viewModel.isValidEmail(email) && viewModel.uiState.value.emailError == null) {
            binding.tilEmail.endIconDrawable = getDrawable(R.drawable.ic_check_circle)
            binding.tilEmail.setEndIconTintList(getColorStateList(R.color.success_green))
        } else {
            binding.tilEmail.endIconDrawable = null
        }
    }

    private fun updateConfirmEmailIcon() {
        val email = binding.etEmail.text.toString().trim()
        val confirmEmail = binding.etConfirmEmail.text.toString().trim()

        if (confirmEmail.isNotEmpty() && email == confirmEmail) {
            binding.tilConfirmEmail.endIconDrawable = getDrawable(R.drawable.ic_check_circle)
            binding.tilConfirmEmail.setEndIconTintList(getColorStateList(R.color.success_green))
        } else {
            binding.tilConfirmEmail.endIconDrawable = null
        }
    }

    private fun updateConfirmPasswordIcon() {
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        if (confirmPassword.isNotEmpty() && password == confirmPassword) {
            binding.tilConfirmPassword.endIconDrawable = getDrawable(R.drawable.ic_check_circle)
            binding.tilConfirmPassword.setEndIconTintList(getColorStateList(R.color.success_green))
        } else {
            binding.tilConfirmPassword.endIconDrawable = null
        }
    }

    private fun updatePasswordRequirements(password: String) {
        val requirements = StringBuilder()

        // Length check
        if (password.length >= 8) {
            requirements.append(getString(R.string.password_req_length_met))
        } else {
            requirements.append(getString(R.string.password_req_length_not_met))
        }

        // Lowercase check
        if (password.any { it.isLowerCase() }) {
            requirements.append(getString(R.string.password_req_lowercase_met))
        } else {
            requirements.append(getString(R.string.password_req_lowercase_not_met))
        }

        // Uppercase check
        if (password.any { it.isUpperCase() }) {
            requirements.append(getString(R.string.password_req_uppercase_met))
        } else {
            requirements.append(getString(R.string.password_req_uppercase_not_met))
        }

        // Number check
        if (password.any { it.isDigit() }) {
            requirements.append(getString(R.string.password_req_number_met))
        } else {
            requirements.append(getString(R.string.password_req_number_not_met))
        }

        // Special character check
        val specialChars = "!@#$%^&*(),.?\":{}|<>"
        if (password.any { it in specialChars }) {
            requirements.append(getString(R.string.password_req_special_met))
        } else {
            requirements.append(getString(R.string.password_req_special_not_met))
        }

        binding.tvPasswordRequirements.text = requirements.toString()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e("SignUpActivity", message)
    }
}