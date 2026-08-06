package org.vexteams.vexnotebooktracker.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.vexteams.vexnotebooktracker.R
import org.vexteams.vexnotebooktracker.network.DashboardUrlRecord
import org.vexteams.vexnotebooktracker.repository.AuthRepository
import org.vexteams.vexnotebooktracker.repository.Resource
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

    fun login(email: String, password: String) {
        if (!validateInputs(email, password)) return

        viewModelScope.launch {
            authRepository.login(email, password).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            errorMessage = null
                        )
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = null
                        )
                        // ✅ Pass List<DashboardUrlRecord> data to Dashboard
                        _navigationEvent.value = NavigationEvent.NavigateToMain(resource.data)
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = resource.message
                        )
                    }
                }
            }
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        // ✅ Clear any previous errors
        _uiState.value = _uiState.value.copy(errorMessage = null)

        when {
            email.isBlank() -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = context.getString(R.string.error_email_required)
                )
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = context.getString(R.string.error_invalid_email)
                )
                return false
            }
            password.isBlank() -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = context.getString(R.string.error_password_required)
                )
                return false
            }
            password.length < 6 -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = context.getString(R.string.error_password_too_short)
                )
                return false
            }
            // ✅ NEW: Additional validation for common issues
            email.length > 100 -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = context.getString(R.string.error_email_too_long)
                )
                return false
            }
            password.length > 50 -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = context.getString(R.string.error_password_too_long)
                )
                return false
            }
        }
        return true
    }

    // ✅ IMPROVED: Better forgot password validation
    fun forgotPassword(email: String) {
        // ✅ Enhanced validation
        when {
            email.isBlank() -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = context.getString(R.string.error_email_required_first)
                )
                return
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _uiState.value = _uiState.value.copy(
                    errorMessage = context.getString(R.string.error_invalid_email)
                )
                return
            }
        }

        viewModelScope.launch {
            authRepository.forgotPassword(email).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            errorMessage = null
                        )
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = resource.data,
                            errorMessage = null
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = resource.message,
                            successMessage = null
                        )
                    }
                }
            }
        }
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
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)