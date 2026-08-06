package org.vexteams.vexnotebooktracker.ui.login

// NavigationEvent.kt
sealed class NavigationEvent {
    data class NavigateToMain(val vexData: Any? = null) : NavigationEvent()
    data class NavigateToLogin(val message: String? = null) : NavigationEvent()
    data class NavigateToSignUp(val message: String? = null) : NavigationEvent()
    data class NavigateToForgotPassword(val email: String? = null) : NavigationEvent()
}