package com.pingo.feature.onboarding.impl

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.LoginResult
import com.pingo.core.auth.SessionState
import com.pingo.core.data.AccountRepository
import com.pingo.core.ui.PRIVACY_TERM_VERSION
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(initialState())

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
                    it.copy(login = LoginUiState.Idle, currentStep = CONSENT_STEP)
                }

                LoginResult.Cancelled -> mutableUiState.update { it.copy(login = LoginUiState.Idle) }

                LoginResult.Failure -> mutableUiState.update { it.copy(login = LoginUiState.Error) }
            }
        }
    }

    fun toggleConsent(checked: Boolean) {
        mutableUiState.update { state ->
            val consent = if (state.consent == ConsentUiState.Loading) ConsentUiState.Loading else ConsentUiState.Idle
            state.copy(consentChecked = checked, consent = consent)
        }
    }

    fun showTerm() {
        mutableUiState.update { it.copy(showTerm = true) }
    }

    fun dismissTerm() {
        mutableUiState.update { it.copy(showTerm = false) }
    }

    fun acceptConsent() {
        val state = mutableUiState.value

        if (!state.consentChecked || state.consent == ConsentUiState.Loading) {
            return
        }

        viewModelScope.launch {
            mutableUiState.update { it.copy(consent = ConsentUiState.Loading) }

            runCatching { accountRepository.recordConsent(PRIVACY_TERM_VERSION) }
                .fold(
                    onSuccess = { mutableUiState.update { it.copy(consent = ConsentUiState.Idle, completed = true) } },
                    onFailure = { exception ->
                        if (exception is CancellationException) {
                            throw exception
                        }

                        mutableUiState.update { it.copy(consent = ConsentUiState.Error) }
                    },
                )
        }
    }

    private fun initialState(): OnboardingUiState = if (authRepository.session.value == SessionState.LoggedIn) {
        OnboardingUiState(currentStep = CONSENT_STEP)
    } else {
        OnboardingUiState()
    }

    private companion object {
        const val LOGIN_STEP = 2
        const val CONSENT_STEP = 3
    }
}
