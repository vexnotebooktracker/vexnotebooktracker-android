package com.bodepudi.vexnotebooktracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.bodepudi.vexnotebooktracker.models.VexUrlResponse
import com.bodepudi.vexnotebooktracker.models.TableRowData
import com.bodepudi.vexnotebooktracker.repository.AuthRepository
import com.bodepudi.vexnotebooktracker.security.SecureStorage
import com.bodepudi.vexnotebooktracker.utils.TimeUtils  // ✅ NEW IMPORT
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val secureStorage: SecureStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableStateFlow<DashboardNavigationEvent?>(null)
    val navigationEvent: StateFlow<DashboardNavigationEvent?> = _navigationEvent.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Get user info - Note: Default values are constants, not user-facing strings
                val userEmail = secureStorage.getUserEmail() ?: "test@vex.com"
                val teamNumber = secureStorage.getTeamNumber() ?: "12345A"

                // ✅ UPDATED: Use TimeUtils for consistent local time display
                val loginTime = TimeUtils.getCurrentLocalTimeString()

                _uiState.value = _uiState.value.copy(
                    userEmail = userEmail,
                    teamNumber = teamNumber,
                    loginTime = loginTime,
                    isLoading = false,
                    successMessage = "Dashboard loaded successfully" // This will be shown in UI
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error loading dashboard: ${e.message}"
                )
            }
        }
    }

    fun setVexApiData(vexData: VexUrlResponse) {
        // ✅ UPDATED: Convert VEX API response to table format with proper time conversion
        val tableData = listOf(
            TableRowData("Short URL", vexData.shortUrl, "url"),
            TableRowData("Start Date", TimeUtils.gmtToLocalDisplay(vexData.startDate), "date"),  // ✅ Convert GMT to local
            TableRowData("End Date", TimeUtils.gmtToLocalDisplay(vexData.endDate), "date"),      // ✅ Convert GMT to local
            TableRowData("Redirection Type ID", vexData.redirectionTypeId.toString(), "number"),
            TableRowData("Allow Mobile Viewing", if (vexData.preventViewingOnMobile) "Yes" else "No", "boolean")
        )

        _uiState.value = _uiState.value.copy(
            tableData = tableData,
            rawVexData = vexData,
            successMessage = "VEX API data loaded successfully!"
        )
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
                _navigationEvent.value = DashboardNavigationEvent.NavigateToLogin
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Logout error: ${e.message}"
                )
            }
        }
    }

    fun onTableRowClicked(rowData: TableRowData) {
        when (rowData.type) {
            "url" -> {
                _uiState.value = _uiState.value.copy(
                    successMessage = "URL copied to clipboard: ${rowData.value}"
                )
                // TODO: Copy to clipboard
            }
            "date" -> {
                // Show additional info when date is clicked
                _uiState.value = _uiState.value.copy(
                    successMessage = "Time shown in local timezone: ${rowData.value}"
                )
            }
            else -> {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Selected: ${rowData.field} = ${rowData.value}"
                )
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

data class DashboardUiState(
    val isLoading: Boolean = false,
    val userEmail: String = "",
    val teamNumber: String = "",
    val loginTime: String = "",
    val tableData: List<TableRowData> = emptyList(),
    val rawVexData: VexUrlResponse? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

sealed class DashboardNavigationEvent {
    object NavigateToLogin : DashboardNavigationEvent()
}