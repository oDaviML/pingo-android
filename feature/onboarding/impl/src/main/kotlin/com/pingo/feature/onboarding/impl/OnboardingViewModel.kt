package com.pingo.feature.onboarding.impl

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.LoginResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(OnboardingUiState())

    val uiState: StateFlow<OnboardingUiState> = mutableUiState.asStateFlow()

    fun goToLogin() {
        mutableUiState.update { it.copy(currentStep = LOGIN_STEP) }
    }

    fun loginWithGoogle(activity: Activity) {
        if (mutableUiState.value.login == LoginUiState.Loading) {
            return
        }

        viewModelScope.launch {
            mutableUiState.update { it.copy(login = LoginUiState.Loading) }

            when (authRepository.loginWithGoogle(activity)) {
                LoginResult.Success -> mutableUiState.update {
                    it.copy(login = LoginUiState.Idle, authenticated = true)
                }

                LoginResult.Cancelled -> mutableUiState.update { it.copy(login = LoginUiState.Idle) }

                LoginResult.Failure -> mutableUiState.update { it.copy(login = LoginUiState.Error) }
            }
        }
    }

    private companion object {
        const val LOGIN_STEP = 2
    }
}
