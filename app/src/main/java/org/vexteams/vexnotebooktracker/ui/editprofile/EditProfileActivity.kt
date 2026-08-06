package org.vexteams.vexnotebooktracker.ui.editprofile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.vexteams.vexnotebooktracker.R
import org.vexteams.vexnotebooktracker.databinding.ActivityEditProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@AndroidEntryPoint
class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val viewModel: EditProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityEditProfileBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupToolbar()
            setupRoleDropdown()
            setupHelpIcons()
            setupValidation()
            setupClickListeners()
            observeViewModel()

            Log.d("EditProfileActivity", "✅ Edit profile activity created successfully")

        } catch (e: Exception) {
            Log.e("EditProfileActivity", "💥 Error in onCreate", e)
            showError(getString(R.string.edit_profile_activity_error, e.message))
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRoleDropdown() {
        // Only show roles 2 and 3 (never show Admin role 1)
        val roleOptions = arrayOf(
            getString(R.string.role_team_member),    // Role 2
            getString(R.string.role_coach_advisor)   // Role 3
        )

        val adapter = ArrayAdapter(this, R.layout.dropdown_menu_popup_item, roleOptions)
        binding.etRole.setAdapter(adapter)

        // Don't set default here - let observeViewModel() handle it based on stored roleId

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

        // Password Help Icon
        binding.ivPasswordHelp.setOnClickListener {
            showHelpDialog(
                title = getString(R.string.help_password_title),
                message = getString(R.string.help_password_message)
            )
        }
    }

    private fun showHelpDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.got_it) { dialog, _ ->
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

        // New password validation
        binding.etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                if (password.isNotEmpty()) {
                    viewModel.validatePassword(password)
                    updatePasswordRequirements(password)
                } else {
                    // Clear password requirements if empty
                    binding.cardPasswordRequirements.visibility = View.GONE
                }
                validateNewPasswordMatch()
            }
        })

        // Confirm new password validation
        binding.etConfirmNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateNewPasswordMatch()
                updateConfirmNewPasswordIcon()
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
                    val roleText = binding.etRole.text.toString()
                    val role = if (roleText == getString(R.string.role_team_member)) 2 else 3
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
                if (phone.isNotEmpty()) {
                    viewModel.validatePhoneNumber(phone)
                } else {
                    binding.tilPhone.error = null
                }
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnUpdateProfile.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val confirmEmail = binding.etConfirmEmail.text.toString().trim()
            val firstName = binding.etFirstName.text.toString().trim()
            val lastName = binding.etLastName.text.toString().trim()
            val roleText = binding.etRole.text.toString()
            val role = if (roleText == getString(R.string.role_team_member)) 2 else 3
            val teamNoWithLetter = binding.etTeamNumber.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val currentPassword = binding.etCurrentPassword.text.toString()
            val newPassword = binding.etNewPassword.text.toString()
            val confirmNewPassword = binding.etConfirmNewPassword.text.toString()

            viewModel.updateProfile(
                email = email,
                confirmEmail = confirmEmail,
                firstName = firstName,
                lastName = lastName,
                role = role,
                teamNoWithLetter = teamNoWithLetter,
                phone = phone,
                currentPassword = if (currentPassword.isNotEmpty()) currentPassword else null,
                newPassword = if (newPassword.isNotEmpty()) newPassword else null,
                confirmNewPassword = if (confirmNewPassword.isNotEmpty()) confirmNewPassword else null
            )
        }
    }

    private fun observeViewModel() {
        // Observe UI State
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressIndicator.isVisible = state.isLoading
                binding.btnUpdateProfile.isEnabled = !state.isLoading

                if (state.isLoading) {
                    binding.btnUpdateProfile.text = getString(R.string.updating_profile)
                } else {
                    binding.btnUpdateProfile.text = getString(R.string.update_profile_button)
                }

                // Populate fields with current data
                if (!state.isLoading && binding.etEmail.text.toString().isEmpty()) {
                    binding.etEmail.setText(state.email)
                    binding.etConfirmEmail.setText(state.email)
                    binding.etFirstName.setText(state.firstName)
                    binding.etLastName.setText(state.lastName)
                    binding.etTeamNumber.setText(state.teamNumber)
                    binding.etPhone.setText(state.phone)

                    // Set correct role based on stored roleId
                    val roleText = when (state.roleId) {
                        2 -> getString(R.string.role_team_member)
                        3 -> getString(R.string.role_coach_advisor)
                        else -> getString(R.string.role_team_member) // Default to role 2
                    }
                    binding.etRole.setText(roleText, false)
                }

                // Email availability
                binding.tilEmail.error = state.emailError

                // Password validation
                binding.tilNewPassword.error = state.passwordError

                // Team number validation
                binding.tilTeamNumber.error = state.teamNumberError

                // Phone validation
                binding.tilPhone.error = state.phoneError

                // General error message
                state.errorMessage?.let { message ->
                    Toast.makeText(this@EditProfileActivity, getString(R.string.toast_error_prefix, message), Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }

                // Success message
                state.successMessage?.let { message ->
                    Toast.makeText(this@EditProfileActivity, getString(R.string.toast_success_prefix, message), Toast.LENGTH_SHORT).show()
                    viewModel.clearSuccess()
                }
            }
        }

        // Observe Navigation Events
        lifecycleScope.launch {
            viewModel.navigationEvent.collect { event ->
                event?.let {
                    when (it) {
                        is EditProfileNavigationEvent.NavigateBack -> {
                            Log.d("EditProfileActivity", "✅ Profile update successful! Navigating back")
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

    private fun validateNewPasswordMatch() {
        val newPassword = binding.etNewPassword.text.toString()
        val confirmNewPassword = binding.etConfirmNewPassword.text.toString()

        if (confirmNewPassword.isNotEmpty()) {
            if (newPassword == confirmNewPassword) {
                binding.tilConfirmNewPassword.error = null
            } else {
                binding.tilConfirmNewPassword.error = getString(R.string.password_mismatch)
            }
        }
    }

    private fun updateEmailIcon() {
        val email = binding.etEmail.text.toString().trim()
        if (viewModel.isValidEmail(email) && viewModel.uiState.value.emailError == null) {
            binding.tilEmail.endIconDrawable = getDrawable(R.drawable.ic_check_circle)
            binding.tilEmail.setEndIconTintList(getColorStateList(R.color.green))
        } else {
            binding.tilEmail.endIconDrawable = null
        }
    }

    private fun updateConfirmEmailIcon() {
        val email = binding.etEmail.text.toString().trim()
        val confirmEmail = binding.etConfirmEmail.text.toString().trim()

        if (confirmEmail.isNotEmpty() && email == confirmEmail) {
            binding.tilConfirmEmail.endIconDrawable = getDrawable(R.drawable.ic_check_circle)
            binding.tilConfirmEmail.setEndIconTintList(getColorStateList(R.color.green))
        } else {
            binding.tilConfirmEmail.endIconDrawable = null
        }
    }

    private fun updateConfirmNewPasswordIcon() {
        val newPassword = binding.etNewPassword.text.toString()
        val confirmNewPassword = binding.etConfirmNewPassword.text.toString()

        if (confirmNewPassword.isNotEmpty() && newPassword == confirmNewPassword) {
            binding.tilConfirmNewPassword.endIconDrawable = getDrawable(R.drawable.ic_check_circle)
            binding.tilConfirmNewPassword.setEndIconTintList(getColorStateList(R.color.green))
        } else {
            binding.tilConfirmNewPassword.endIconDrawable = null
        }
    }

    private fun updatePasswordRequirements(password: String) {
        if (password.isEmpty()) {
            binding.cardPasswordRequirements.visibility = View.GONE
            return
        }

        binding.cardPasswordRequirements.visibility = View.VISIBLE

        val requirements = StringBuilder()

        // Length check
        if (password.length >= 8) {
            requirements.append(getString(R.string.password_requirement_length_met))
        } else {
            requirements.append(getString(R.string.password_requirement_length_unmet))
        }

        // Lowercase check
        if (password.any { it.isLowerCase() }) {
            requirements.append(getString(R.string.password_requirement_lowercase_met))
        } else {
            requirements.append(getString(R.string.password_requirement_lowercase_unmet))
        }

        // Uppercase check
        if (password.any { it.isUpperCase() }) {
            requirements.append(getString(R.string.password_requirement_uppercase_met))
        } else {
            requirements.append(getString(R.string.password_requirement_uppercase_unmet))
        }

        // Number check
        if (password.any { it.isDigit() }) {
            requirements.append(getString(R.string.password_requirement_number_met))
        } else {
            requirements.append(getString(R.string.password_requirement_number_unmet))
        }

        // Special character check
        val specialChars = "!@#$%^&*(),.?\":{}|<>"
        if (password.any { it in specialChars }) {
            requirements.append(getString(R.string.password_requirement_special_met))
        } else {
            requirements.append(getString(R.string.password_requirement_special_unmet))
        }

        binding.tvPasswordRequirements.text = requirements.toString()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e("EditProfileActivity", message)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Log.d("EditProfileActivity", "🔙 Back pressed - returning to previous screen")
    }
}