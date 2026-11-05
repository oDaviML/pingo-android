package com.pingo.feature.onboarding.impl

import android.app.Activity
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.LoginResult
import com.pingo.core.auth.SessionState
import com.pingo.core.testing.TestDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule(UnconfinedTestDispatcher())

    private val authRepository = FakeAuthRepository()

    private val viewModel = OnboardingViewModel(authRepository)

    @Test
    fun `inicia nas boas-vindas e avanca para o login`() = runTest {
        assertEquals(1, viewModel.uiState.value.currentStep)

        viewModel.goToLogin()

        assertEquals(2, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `login com sucesso autentica e volta ao estado ocioso`() = runTest {
        authRepository.result = LoginResult.Success
        viewModel.goToLogin()

        viewModel.loginWithGoogle(Activity())

        assertEquals(LoginUiState.Idle, viewModel.uiState.value.login)
        assertTrue(viewModel.uiState.value.authenticated)
    }

    @Test
    fun `login cancelado permanece no passo sem autenticar`() = runTest {
        authRepository.result = LoginResult.Cancelled
        viewModel.goToLogin()

        viewModel.loginWithGoogle(Activity())

        assertEquals(LoginUiState.Idle, viewModel.uiState.value.login)
        assertEquals(2, viewModel.uiState.value.currentStep)
        assertFalse(viewModel.uiState.value.authenticated)
    }

    @Test
    fun `falha de login mostra erro e permite tentar de novo`() = runTest {
        authRepository.result = LoginResult.Failure
        viewModel.goToLogin()

        viewModel.loginWithGoogle(Activity())

        assertEquals(LoginUiState.Error, viewModel.uiState.value.login)
        assertFalse(viewModel.uiState.value.authenticated)

        authRepository.result = LoginResult.Success

        viewModel.loginWithGoogle(Activity())

        assertEquals(LoginUiState.Idle, viewModel.uiState.value.login)
        assertTrue(viewModel.uiState.value.authenticated)
    }

    private class FakeAuthRepository : AuthRepository {
        var result: LoginResult = LoginResult.Success

        override val session: StateFlow<SessionState> = MutableStateFlow(SessionState.LoggedOut)

        override suspend fun loginWithGoogle(activity: Activity): LoginResult = result

        override suspend fun logout() = Unit

        override suspend fun accessToken(forceRefresh: Boolean): String? = null
    }
}
