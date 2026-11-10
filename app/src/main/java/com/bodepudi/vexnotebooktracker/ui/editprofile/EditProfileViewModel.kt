package com.bodepudi.vexnotebooktracker.ui.editprofile

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.bodepudi.vexnotebooktracker.R
import com.bodepudi.vexnotebooktracker.repository.AuthRepository
import com.bodepudi.vexnotebooktracker.repository.Resource
import com.bodepudi.vexnotebooktracker.security.SecureStorage
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val secureStorage: SecureStorage,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "EditProfileViewModel"
        private const val EMAIL_CHECK_DELAY = 500L
    }

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<EditProfileNavigationEvent?>(null)
    val navigationEvent: StateFlow<EditProfileNavigationEvent?> = _navigationEvent.asStateFlow()

    private var emailCheckJob: Job? = null
    private var originalEmail: String = ""

    init {
        loadCurrentProfile()
    }

    private fun loadCurrentProfile() {
        viewModelScope.launch {
            try {
                val email = secureStorage.getUserEmail() ?: ""
                val firstName = secureStorage.getFirstName() ?: ""
                val lastName = secureStorage.getLastName() ?: ""
                val teamNumber = secureStorage.getTeamNumber() ?: ""
                val phone = secureStorage.getPhone() ?: ""
                val roleId = secureStorage.getRoleId() ?: 2

                originalEmail = email

                _uiState.value = _uiState.value.copy(
                    email = email,
                    firstName = firstName,
                    lastName = lastName,
                    teamNumber = teamNumber,
                    phone = phone,
                    roleId = roleId,
                    isLoading = false
                )

                Log.d(TAG, "✅ Loaded current profile for: $email")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading profile", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = context.getString(R.string.profile_update_failed_load)
                )
            }
        }
    }

    fun updateProfile(
        email: String,
        confirmEmail: String,
        firstName: String,
        lastName: String,
        role: Int,
        teamNoWithLetter: String,
        phone: String,
        currentPassword: String? = null,
        newPassword: String? = null,
        confirmNewPassword: String? = null
    ) {
        if (!validateAllInputs(
                email, confirmEmail, firstName, lastName, teamNoWithLetter, phone,
                currentPassword, newPassword, confirmNewPassword, role
            )) {
            return
        }

        viewModelScope.launch {
            authRepository.updateProfile(
                email = email,
                firstName = firstName,
                lastName = lastName,
                role = role,
                teamNoWithLetter = teamNoWithLetter,
                phone = phone.ifEmpty { null },
                currentPassword = currentPassword,
                newPassword = newPassword
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            errorMessage = null
                        )
                        Log.d(TAG, "⏳ Profile update in progress...")
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = context.getString(R.string.profile_update_success)
                        )
                        Log.d(TAG, "✅ Profile update successful")
                        _navigationEvent.value = EditProfileNavigationEvent.NavigateBack
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = resource.message
                        )
                        Log.e(TAG, "❌ Profile update failed: ${resource.message}")
                    }
                }
            }
        }
    }

    fun checkEmailAvailability(email: String) {
        // Skip check if email hasn't changed
        if (email == originalEmail) {
            _uiState.value = _uiState.value.copy(emailError = null)
            return
        }

        if (!isValidEmail(email)) {
            _uiState.value = _uiState.value.copy(
                emailError = context.getString(R.string.email_invalid_format)
            )
            return
        }

        if (email.length > 249) {
            _uiState.value = _uiState.value.copy(
                emailError = context.getString(R.string.email_too_long_249)
            )
            return
        }

        // Cancel previous job and start new one (debouncing)
        emailCheckJob?.cancel()
        emailCheckJob = viewModelScope.launch {
            delay(EMAIL_CHECK_DELAY)

            try {
                authRepository.checkEmailAvailability(email).collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            _uiState.value = _uiState.value.copy(emailError = null)
                        }
                        is Resource.Success -> {
                            val isAvailable = resource.data
                            if (isAvailable) {
                                _uiState.value = _uiState.value.copy(emailError = null)
                                Log.d(TAG, "✅ Email available: $email")
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    emailError = context.getString(R.string.email_already_registered)
                                )
                                Log.d(TAG, "❌ Email not available: $email")
                            }
                        }
                        is Resource.Error -> {
                            _uiState.value = _uiState.value.copy(
                                emailError = context.getString(R.string.email_check_unavailable)
                            )
                            Log.e(TAG, "❌ Email check error: ${resource.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Email availability check failed", e)
                _uiState.value = _uiState.value.copy(
                    emailError = context.getString(R.string.email_check_unavailable)
                )
            }
        }
    }

    fun validatePassword(password: String): Boolean {
        val errors = mutableListOf<String>()

        if (password.length > 32) {
            errors.add(context.getString(R.string.password_too_long))
        }

        if (password.length < 8) {
            errors.add(context.getString(R.string.password_too_short))
        }

        if (!password.any { it.isLowerCase() }) {
            errors.add(context.getString(R.string.password_missing_lowercase))
        }

        if (!password.any { it.isUpperCase() }) {
            errors.add(context.getString(R.string.password_missing_uppercase))
        }

        if (!password.any { it.isDigit() }) {
            errors.add(context.getString(R.string.password_missing_number))
        }

        val specialChars = "!@#$%^&*(),.?\":{}|<>"
        if (!password.any { it in specialChars }) {
            errors.add(context.getString(R.string.password_missing_special))
        }

        val errorMessage = if (errors.isNotEmpty()) errors.joinToString(", ") else null
        _uiState.value = _uiState.value.copy(passwordError = errorMessage)

        return errors.isEmpty()
    }

    fun validateTeamNumber(teamNoWithLetter: String, roleId: Int? = null): Boolean {
        if (teamNoWithLetter.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                teamNumberError = context.getString(R.string.team_number_required)
            )
            return false
        }

        // Role-based validation:
        // roleId 2 (Team Member/Captain): Must end with letter (e.g., 123A)
        // roleId 3 (Coach/Advisor): Letter is optional (e.g., 123 or 123A)
        val teamNumberRegex = when (roleId) {
            2 -> Regex("^\\d+[A-Z]$")  // Letter required for team members
            3 -> Regex("^\\d+[A-Z]?$") // Letter optional for coaches
            else -> Regex("^\\d+[A-Z]$") // Default: optional letter
        }

        val isValid = teamNumberRegex.matches(teamNoWithLetter)

        if (!isValid) {
            val errorMessage = when (roleId) {
                2 -> context.getString(R.string.team_number_error_member)
                3 -> context.getString(R.string.team_number_error_coach)
                else -> context.getString(R.string.team_number_error_default)
            }
            _uiState.value = _uiState.value.copy(teamNumberError = errorMessage)
        } else {
            _uiState.value = _uiState.value.copy(teamNumberError = null)
        }

        return isValid
    }

    fun validatePhoneNumber(phone: String): Boolean {
        if (phone.isEmpty()) {
            _uiState.value = _uiState.value.copy(phoneError = null)
            return true
        }

        val phoneRegex = Regex("^\\+\\d{1,3}\\s\\d{10,15}$")
        val isValid = phoneRegex.matches(phone)

        if (!isValid) {
            _uiState.value = _uiState.value.copy(
                phoneError = context.getString(R.string.phone_invalid_format)
            )
        } else {
            _uiState.value = _uiState.value.copy(phoneError = null)
        }

        return isValid
    }

    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun validateAllInputs(
        email: String,
        confirmEmail: String,
        firstName: String,
        lastName: String,
        teamNoWithLetter: String,
        phone: String,
        currentPassword: String?,
        newPassword: String?,
        confirmNewPassword: String?,
        role: Int
    ): Boolean {
        val errors = mutableListOf<String>()

        // Email validation
        if (email.isBlank()) {
            errors.add(context.getString(R.string.error_email_required))
        } else if (!isValidEmail(email)) {
            errors.add(context.getString(R.string.email_invalid_format))
        } else if (email.length > 249) {
            errors.add(context.getString(R.string.error_email_too_long))
        }

        // Confirm email validation
        if (confirmEmail.isBlank()) {
            errors.add(context.getString(R.string.error_email_confirmation_required))
        } else if (email != confirmEmail) {
            errors.add(context.getString(R.string.error_emails_do_not_match))
        }

        // First name validation
        if (firstName.isBlank()) {
            errors.add(context.getString(R.string.error_first_name_required))
        } else if (firstName.length < 2) {
            errors.add(context.getString(R.string.error_first_name_too_short))
        }

        // Last name validation
        if (lastName.isBlank()) {
            errors.add(context.getString(R.string.error_last_name_required))
        } else if (lastName.length < 2) {
            errors.add(context.getString(R.string.error_last_name_too_short))
        }

        // Team number validation
        if (!validateTeamNumber(teamNoWithLetter, role)) {
            errors.add(context.getString(R.string.error_team_number_invalid))
        }

        // Phone validation (optional)
        if (phone.isNotEmpty() && !validatePhoneNumber(phone)) {
            errors.add(context.getString(R.string.error_phone_invalid))
        }

        // Password validation (only if changing password)
        if (!newPassword.isNullOrEmpty()) {
            if (currentPassword.isNullOrEmpty()) {
                errors.add(context.getString(R.string.error_current_password_required))
            }

            if (!validatePassword(newPassword)) {
                errors.add(context.getString(R.string.error_new_password_requirements))
            }

            if (confirmNewPassword.isNullOrEmpty()) {
                errors.add(context.getString(R.string.error_password_confirmation_required))
            } else if (newPassword != confirmNewPassword) {
                errors.add(context.getString(R.string.error_new_passwords_do_not_match))
            }
        }

        // Check for email availability error
        if (_uiState.value.emailError != null) {
            errors.add(context.getString(R.string.error_email_issue, _uiState.value.emailError))
        }

        if (errors.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = context.getString(R.string.validation_errors_prefix, errors.joinToString(", "))
            )
            Log.d(TAG, "❌ Validation failed: ${errors.joinToString(", ")}")
            return false
        }

        Log.d(TAG, "✅ All validations passed")
        return true
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun clearNavigationEvent() {
        _navigationEvent.value = null
    }

    override fun onCleared() {
        super.onCleared()
        emailCheckJob?.cancel()
    }
}

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val teamNumber: String = "",
    val phone: String = "",
    val roleId: Int = 2,
    val emailError: String? = null,
    val passwordError: String? = null,
    val teamNumberError: String? = null,
    val phoneError: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

sealed class EditProfileNavigationEvent {
    object NavigateBack : EditProfileNavigationEvent()
}