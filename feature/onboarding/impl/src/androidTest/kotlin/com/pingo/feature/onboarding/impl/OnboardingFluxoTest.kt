package com.pingo.feature.onboarding.impl

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.LoginResult
import com.pingo.core.auth.SessionState
import com.pingo.core.data.AccountRepository
import com.pingo.core.data.ContaRepository
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.NovaConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.ui.PRIVACY_TERM_VERSION
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class OnboardingFluxoTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val contaRepository = FakeContaRepository()
    private val preferencias = FakeUserPreferences()
    private val accountRepository = FakeAccountRepository()

    @Test
    fun fluxoDoPasso1AoPasso7TerminaNaHomeComAContaCriada() {
        mostrarFluxo(FakeAuthRepository(SessionState.LoggedOut))

        composeRule.onNodeWithText("Passo 1 de 8").assertIsDisplayed()
        composeRule.onNodeWithText("Começar").performClick()
        composeRule.onNodeWithText("Continuar com Google").performClick()
        composeRule.onNodeWithText("Passo 3 de 8").assertIsDisplayed()
        composeRule.onNodeWithText("Li e aceito o termo de privacidade (LGPD)").performClick()
        composeRule.onNodeWithText("Aceitar e continuar").performClick()
        composeRule.onNodeWithText("Passo 7 de 8").assertIsDisplayed()
        composeRule.onNodeWithText("Nubank").performClick()
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.onNodeWithText("Home").assertIsDisplayed()

        assertEquals(PRIVACY_TERM_VERSION, accountRepository.consentimento)
        val conta = contaRepository.criadas.single().single()
        assertEquals(InstituicaoFinanceira.NUBANK, conta.instituicao)
        assertEquals(listOf(TipoInstrumento.CREDITO), conta.instrumentos.map { it.tipo })
        assertTrue(preferencias.configuradas)
    }

    @Test
    fun comSessaoAtivaPularOPasso7VaiParaAHomeSemCriarContas() {
        mostrarFluxo(FakeAuthRepository(SessionState.LoggedIn))

        composeRule.onNodeWithText("Passo 3 de 8").assertIsDisplayed()
        composeRule.onNodeWithText("Li e aceito o termo de privacidade (LGPD)").performClick()
        composeRule.onNodeWithText("Aceitar e continuar").performClick()
        composeRule.onNodeWithText("Passo 7 de 8").assertIsDisplayed()
        composeRule.onNodeWithText("Pular").performClick()
        composeRule.onNodeWithText("Home").assertIsDisplayed()

        assertTrue(contaRepository.criadas.isEmpty())
        assertTrue(preferencias.configuradas)
    }

    private fun mostrarFluxo(autenticacao: AuthRepository) {
        val onboarding = OnboardingViewModel(autenticacao, accountRepository)
        val contas = OnboardingContasViewModel(contaRepository, preferencias)

        composeRule.setContent {
            PingoTheme {
                var tela by remember { mutableStateOf(Tela.ONBOARDING) }

                when (tela) {
                    Tela.ONBOARDING -> OnboardingScreen(
                        onContinue = { tela = Tela.CONTAS },
                        viewModel = onboarding,
                    )

                    Tela.CONTAS -> OnboardingContasScreen(
                        onConcluir = { tela = Tela.HOME },
                        viewModel = contas,
                    )

                    Tela.HOME -> PingoText(text = "Home")
                }
            }
        }
    }

    private enum class Tela {
        ONBOARDING,
        CONTAS,
        HOME,
    }

    private class FakeAuthRepository(sessao: SessionState) : AuthRepository {
        override val session: StateFlow<SessionState> = MutableStateFlow(sessao)

        override suspend fun loginWithGoogle(activity: Activity): LoginResult = LoginResult.Success

        override suspend fun logout() = Unit

        override suspend fun accessToken(forceRefresh: Boolean): String? = null
    }

    private class FakeAccountRepository : AccountRepository {
        var consentimento: String? = null

        override suspend fun recordConsent(version: String) {
            consentimento = version
        }

        override suspend fun deleteAccount() = Unit
    }

    private class FakeContaRepository : ContaRepository {
        val criadas = mutableListOf<List<NovaConta>>()

        override fun observeContas(): Flow<List<Conta>> = flowOf(emptyList())

        override suspend fun buscar(publicId: String): Conta? = null

        override suspend fun criarComInstrumentos(contas: List<NovaConta>, agora: Instant) {
            criadas.add(contas)
        }

        override suspend fun salvar(conta: Conta) = Unit

        override suspend fun salvarTodas(contas: List<Conta>) = Unit

        override suspend fun excluir(publicId: String, quando: Instant) = Unit
    }

    private class FakeUserPreferences : UserPreferencesDataSource {
        var configuradas = false

        override val syncStatus: Flow<SyncStatus> = flowOf(SyncStatus.IDLE)

        override suspend fun setSyncStatus(status: SyncStatus) = Unit

        override suspend fun pullCursor(): String? = null

        override suspend fun setPullCursor(cursor: String?) = Unit

        override suspend fun pushCursor(): Instant? = null

        override suspend fun setPushCursor(instant: Instant) = Unit

        override suspend fun lastSyncAt(): Instant? = null

        override suspend fun setLastSyncAt(instant: Instant) = Unit

        override suspend fun deviceId(): String = "dispositivo-teste"

        override suspend fun activatedDeviceId(): String? = null

        override suspend fun setActivatedDeviceId(deviceId: String) = Unit

        override suspend fun consentVersion(): String? = null

        override suspend fun consentAt(): Instant? = null

        override suspend fun setConsent(version: String, instant: Instant) = Unit

        override suspend fun contasConfiguradas(): Boolean = configuradas

        override suspend fun setContasConfiguradas(configuradas: Boolean) {
            this.configuradas = configuradas
        }

        override suspend fun clear() = Unit
    }
}
