package org.vexteams.vexnotebooktracker.ui.contact

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.vexteams.vexnotebooktracker.R
import org.vexteams.vexnotebooktracker.repository.AuthRepository
import org.vexteams.vexnotebooktracker.repository.Resource
import javax.inject.Inject

@HiltViewModel
class ContactViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ContactViewModel"
    }

    private val _uiState = MutableStateFlow(ContactUiState())
    val uiState: StateFlow<ContactUiState> = _uiState.asStateFlow()

    // Get validation limits from resources
    private val minFeedbackLength: Int
        get() = context.resources.getInteger(R.integer.feedback_min_length)

    private val maxFeedbackLength: Int
        get() = context.resources.getInteger(R.integer.feedback_max_length)

    fun sendFeedback(feedback: String) {
        // Validate feedback
        val trimmedFeedback = feedback.trim()

        if (trimmedFeedback.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = context.getString(R.string.feedback_empty)
            )
            return
        }

        if (trimmedFeedback.length < minFeedbackLength) {
            _uiState.value = _uiState.value.copy(
                errorMessage = context.getString(R.string.feedback_too_short, minFeedbackLength)
            )
            return
        }

        if (trimmedFeedback.length > maxFeedbackLength) {
            _uiState.value = _uiState.value.copy(
                errorMessage = context.getString(R.string.feedback_too_long, maxFeedbackLength)
            )
            return
        }

        // Send feedback
        viewModelScope.launch {
            authRepository.sendFeedback(trimmedFeedback).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            errorMessage = null,
                            successMessage = null
                        )
                        Log.d(TAG, "📤 Sending feedback...")
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = context.getString(R.string.feedback_success),
                            feedbackSent = true
                        )
                        Log.d(TAG, "✅ Feedback sent successfully")
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = resource.message
                        )
                        Log.e(TAG, "❌ Failed to send feedback: ${resource.message}")
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

    fun resetFeedbackSent() {
        _uiState.value = _uiState.value.copy(feedbackSent = false)
    }
}

data class ContactUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val feedbackSent: Boolean = false
)