package com.bodepudi.vexnotebooktracker.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.bodepudi.vexnotebooktracker.R
import com.bodepudi.vexnotebooktracker.repository.AuthRepository
import com.bodepudi.vexnotebooktracker.security.SecureStorage
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val secureStorage: SecureStorage,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<MainNavigationEvent?>(null)
    val navigationEvent: StateFlow<MainNavigationEvent?> = _navigationEvent.asStateFlow()

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            try {
                val userEmail = secureStorage.getUserEmail() ?: context.getString(R.string.default_email)
                val teamNumber = secureStorage.getTeamNumber() ?: context.getString(R.string.default_team_number)
                val loginTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

                _uiState.value = _uiState.value.copy(
                    userEmail = userEmail,
                    teamNumber = teamNumber,
                    loginTime = loginTime,
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = context.getString(R.string.error_loading_user_info1, e.message)
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Clear stored data
                authRepository.logout()

                _uiState.value = _uiState.value.copy(isLoading = false)
                _navigationEvent.value = MainNavigationEvent.NavigateToLogin

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = context.getString(R.string.error_logout1, e.message)
                )
            }
        }
    }

    fun onWelcomeCardClicked() {
        _uiState.value = _uiState.value.copy(
            successMessage = context.getString(R.string.welcome_card_clicked)
        )
    }

    fun onUserInfoCardClicked() {
        _navigationEvent.value = MainNavigationEvent.NavigateToProfile
    }

    fun onFeaturesCardClicked() {
        _navigationEvent.value = MainNavigationEvent.NavigateToFeatures
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

data class MainUiState(
    val isLoading: Boolean = true,
    val userEmail: String = "",
    val teamNumber: String = "",
    val loginTime: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

sealed class MainNavigationEvent {
    object NavigateToLogin : MainNavigationEvent()
    object NavigateToProfile : MainNavigationEvent()
    object NavigateToFeatures : MainNavigationEvent()
}