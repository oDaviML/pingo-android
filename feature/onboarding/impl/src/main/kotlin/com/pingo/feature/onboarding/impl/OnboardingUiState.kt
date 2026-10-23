package com.pingo.feature.onboarding.impl

import androidx.compose.runtime.Immutable

@Immutable
data class OnboardingUiState(
    val currentStep: Int = 2,
    val totalSteps: Int = 8,
    val login: LoginUiState = LoginUiState.Idle,
    val authenticated: Boolean = false,
)

enum class LoginUiState {
    Idle,
    Loading,
    Error,
}
