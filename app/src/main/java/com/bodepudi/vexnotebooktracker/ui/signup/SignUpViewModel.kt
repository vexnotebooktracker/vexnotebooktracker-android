package com.bodepudi.vexnotebooktracker.ui.signup

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.bodepudi.vexnotebooktracker.R
import com.bodepudi.vexnotebooktracker.repository.AuthRepository
import com.bodepudi.vexnotebooktracker.repository.Resource
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "SignUpViewModel"
    }

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<SignUpNavigationEvent?>(null)
    val navigationEvent: StateFlow<SignUpNavigationEvent?> = _navigationEvent.asStateFlow()

    private var emailCheckJob: Job? = null

    // Get constants from resources
    private val emailCheckDelay: Long
        get() = context.resources.getInteger(R.integer.email_check_delay_ms).toLong()

    private val maxEmailLength: Int
        get() = context.resources.getInteger(R.integer.email_max_length)

    private val minPasswordLength: Int
        get() = context.resources.getInteger(R.integer.password_min_length)

    private val maxPasswordLength: Int
        get() = context.resources.getInteger(R.integer.password_max_length)

    private val minNameLength: Int
        get() = context.resources.getInteger(R.integer.name_min_length)

    fun signUp(
        email: String,
        confirmEmail: String,
        password: String,
        confirmPassword: String,
        firstName: String,
        lastName: String,
        role: Int,
        teamNoWithLetter: String,
        phone: String
    ) {
        if (!validateAllInputs(email, confirmEmail, password, confirmPassword, firstName, lastName, teamNoWithLetter, phone, role)) {
            return
        }

        viewModelScope.launch {
            authRepository.signUp(
                email = email,
                password = password,
                confirmPassword = confirmPassword,
                firstName = firstName,
                lastName = lastName,
                teamNoWithLetter = teamNoWithLetter,
                role = role,
                phone = phone.ifEmpty { null }
            ).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            errorMessage = null
                        )
                        Log.d(TAG, "📤 Sign-up in progress...")
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = context.getString(R.string.signup_success_message)
                        )
                        Log.d(TAG, "✅ Sign-up successful")
                        _navigationEvent.value = SignUpNavigationEvent.NavigateToLogin
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = resource.message
                        )
                        Log.e(TAG, "❌ Sign-up failed: ${resource.message}")
                    }
                }
            }
        }
    }

    fun checkEmailAvailability(email: String) {
        if (!isValidEmail(email)) {
            _uiState.value = _uiState.value.copy(
                emailError = context.getString(R.string.email_invalid_format)
            )
            return
        }

        if (email.length > maxEmailLength) {
            _uiState.value = _uiState.value.copy(
                emailError = context.getString(R.string.email_too_long_signup)
            )
            return
        }

        // Cancel previous job and start new one (debouncing)
        emailCheckJob?.cancel()
        emailCheckJob = viewModelScope.launch {
            delay(emailCheckDelay)

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
                                    emailError = context.getString(R.string.email_already_registered_short)
                                )
                                Log.d(TAG, "❌ Email not available: $email")
                            }
                        }
                        is Resource.Error -> {
                            _uiState.value = _uiState.value.copy(
                                emailError = context.getString(R.string.email_availability_check_failed_short)
                            )
                            Log.e(TAG, "❌ Email check error: ${resource.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Email availability check failed", e)
                _uiState.value = _uiState.value.copy(
                    emailError = context.getString(R.string.email_availability_check_failed_short)
                )
            }
        }
    }

    fun validatePassword(password: String): Boolean {
        val errors = mutableListOf<String>()

        if (password.length > maxPasswordLength) {
            errors.add(context.getString(R.string.password_too_long_signup))
        }

        if (password.length < minPasswordLength) {
            errors.add(context.getString(R.string.password_too_short_signup))
        }

        if (!password.any { it.isLowerCase() }) {
            errors.add(context.getString(R.string.password_missing_lowercase_short))
        }

        if (!password.any { it.isUpperCase() }) {
            errors.add(context.getString(R.string.password_missing_uppercase_short))
        }

        if (!password.any { it.isDigit() }) {
            errors.add(context.getString(R.string.password_missing_number_short))
        }

        val specialChars = "!@#$%^&*(),.?\":{}|<>"
        if (!password.any { it in specialChars }) {
            errors.add(context.getString(R.string.password_missing_special_short))
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
                2 -> context.getString(R.string.team_number_format_team_member)
                3 -> context.getString(R.string.team_number_format_coach)
                else -> context.getString(R.string.team_number_format_default)
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
            return true // Phone is optional
        }

        // Format: +countryCode phoneNumber (e.g., +1 1234567890)
        val phoneRegex = Regex("^\\+\\d{1,3}\\s\\d{10,15}$")
        val isValid = phoneRegex.matches(phone)

        if (!isValid) {
            _uiState.value = _uiState.value.copy(
                phoneError = context.getString(R.string.phone_format_invalid)
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
        password: String,
        confirmPassword: String,
        firstName: String,
        lastName: String,
        teamNoWithLetter: String,
        phone: String,
        role: Int
    ): Boolean {
        val errors = mutableListOf<String>()

        // Email validation
        if (email.isBlank()) {
            errors.add(context.getString(R.string.email_required))
        } else if (!isValidEmail(email)) {
            errors.add(context.getString(R.string.email_invalid_format))
        } else if (email.length > maxEmailLength) {
            errors.add(context.getString(R.string.email_too_long))
        }

        // Confirm email validation
        if (confirmEmail.isBlank()) {
            errors.add(context.getString(R.string.email_confirmation_required_short))
        } else if (email != confirmEmail) {
            errors.add(context.getString(R.string.emails_do_not_match_short))
        }

        // Password validation
        if (password.isBlank()) {
            errors.add(context.getString(R.string.password_required))
        } else if (!validatePassword(password)) {
            errors.add(context.getString(R.string.password_not_meet_requirements))
        }

        // Confirm password validation
        if (confirmPassword.isBlank()) {
            errors.add(context.getString(R.string.password_confirmation_required_short))
        } else if (password != confirmPassword) {
            errors.add(context.getString(R.string.passwords_do_not_match_short))
        }

        // First name validation
        if (firstName.isBlank()) {
            errors.add(context.getString(R.string.first_name_required_short))
        } else if (firstName.length < minNameLength) {
            errors.add(context.getString(R.string.first_name_too_short))
        }

        // Last name validation
        if (lastName.isBlank()) {
            errors.add(context.getString(R.string.last_name_required_short))
        } else if (lastName.length < minNameLength) {
            errors.add(context.getString(R.string.last_name_too_short))
        }

        // Team number validation
        if (!validateTeamNumber(teamNoWithLetter, role)) {
            errors.add(context.getString(R.string.team_number_format_invalid))
        }

        // Phone validation (optional)
        if (phone.isNotEmpty() && !validatePhoneNumber(phone)) {
            errors.add(context.getString(R.string.phone_format_invalid_short))
        }

        // Check for email availability error
        if (_uiState.value.emailError != null) {
            errors.add(context.getString(R.string.email_issue, _uiState.value.emailError))
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

data class SignUpUiState(
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val teamNumberError: String? = null,
    val phoneError: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

sealed class SignUpNavigationEvent {
    object NavigateToLogin : SignUpNavigationEvent()
    object NavigateBack : SignUpNavigationEvent()
}