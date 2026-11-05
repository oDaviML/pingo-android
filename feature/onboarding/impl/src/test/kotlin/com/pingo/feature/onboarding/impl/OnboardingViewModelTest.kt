package com.pingo.feature.onboarding.impl

import android.app.Activity
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.LoginResult
import com.pingo.core.auth.SessionState
import com.pingo.core.data.AccountRepository
import com.pingo.core.testing.TestDispatcherRule
import com.pingo.core.ui.PRIVACY_TERM_VERSION
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule(UnconfinedTestDispatcher())

    private val authRepository = FakeAuthRepository()
    private val accountRepository = FakeAccountRepository()

    private val viewModel = OnboardingViewModel(authRepository, accountRepository)

    @Test
    fun `inicia nas boas-vindas e avanca para o login`() = runTest {
        assertEquals(1, viewModel.uiState.value.currentStep)

        viewModel.goToLogin()

        assertEquals(2, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `inicia no termo lgpd quando a sessao ja esta ativa`() = runTest {
        val sessaoAtiva = OnboardingViewModel(FakeAuthRepository(SessionState.LoggedIn), accountRepository)

        assertEquals(3, sessaoAtiva.uiState.value.currentStep)
    }

    @Test
    fun `login com sucesso avanca para o termo lgpd`() = runTest {
        authRepository.result = LoginResult.Success
        viewModel.goToLogin()

        viewModel.loginWithGoogle(Activity())

        assertEquals(LoginUiState.Idle, viewModel.uiState.value.login)
        assertEquals(3, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `login cancelado permanece no passo sem autenticar`() = runTest {
        authRepository.result = LoginResult.Cancelled
        viewModel.goToLogin()

        viewModel.loginWithGoogle(Activity())

        assertEquals(LoginUiState.Idle, viewModel.uiState.value.login)
        assertEquals(2, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `falha de login mostra erro e permite tentar de novo`() = runTest {
        authRepository.result = LoginResult.Failure
        viewModel.goToLogin()

        viewModel.loginWithGoogle(Activity())

        assertEquals(LoginUiState.Error, viewModel.uiState.value.login)

        authRepository.result = LoginResult.Success

        viewModel.loginWithGoogle(Activity())

        assertEquals(LoginUiState.Idle, viewModel.uiState.value.login)
        assertEquals(3, viewModel.uiState.value.currentStep)
    }

    @Test
    fun `aceitar o termo registra o consentimento e conclui`() = runTest {
        viewModel.toggleConsent(true)
        viewModel.acceptConsent()

        assertEquals(PRIVACY_TERM_VERSION, accountRepository.recordedVersion)
        assertEquals(ConsentUiState.Idle, viewModel.uiState.value.consent)
        assertTrue(viewModel.uiState.value.completed)
    }

    @Test
    fun `nao registra consentimento sem aceitar o termo`() = runTest {
        viewModel.acceptConsent()

        assertNull(accountRepository.recordedVersion)
        assertFalse(viewModel.uiState.value.completed)
    }

    @Test
    fun `falha ao registrar consentimento mostra erro e permite tentar de novo`() = runTest {
        accountRepository.failure = IllegalStateException("sem rede")
        viewModel.toggleConsent(true)
        viewModel.acceptConsent()

        assertEquals(ConsentUiState.Error, viewModel.uiState.value.consent)
        assertFalse(viewModel.uiState.value.completed)

        accountRepository.failure = null
        viewModel.acceptConsent()

        assertEquals(ConsentUiState.Idle, viewModel.uiState.value.consent)
        assertTrue(viewModel.uiState.value.completed)
    }

    @Test
    fun `alterar o consentimento durante o envio mantem o carregamento`() = runTest {
        accountRepository.gate = CompletableDeferred()
        viewModel.toggleConsent(true)
        viewModel.acceptConsent()

        viewModel.toggleConsent(false)

        assertEquals(ConsentUiState.Loading, viewModel.uiState.value.consent)
        accountRepository.gate?.complete(Unit)
    }

    private class FakeAuthRepository(initialSession: SessionState = SessionState.LoggedOut) : AuthRepository {
        var result: LoginResult = LoginResult.Success

        override val session: StateFlow<SessionState> = MutableStateFlow(initialSession)

        override suspend fun loginWithGoogle(activity: Activity): LoginResult = result

        override suspend fun logout() = Unit

        override suspend fun accessToken(forceRefresh: Boolean): String? = null
    }

    private class FakeAccountRepository : AccountRepository {
        var failure: Throwable? = null
        var recordedVersion: String? = null
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun recordConsent(version: String) {
            gate?.await()
            failure?.let { throw it }
            recordedVersion = version
        }

        override suspend fun deleteAccount() = Unit
    }
}
