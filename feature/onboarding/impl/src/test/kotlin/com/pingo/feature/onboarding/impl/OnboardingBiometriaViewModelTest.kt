package com.pingo.feature.onboarding.impl

import android.app.Activity
import com.pingo.core.auth.AutenticadorDoSistema
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.DisponibilidadeAutenticacao
import com.pingo.core.auth.GerenciadorDeBloqueioDoApp
import com.pingo.core.auth.LoginResult
import com.pingo.core.auth.ResultadoAutenticacao
import com.pingo.core.auth.SessionState
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.testing.FakeAppDispatchers
import com.pingo.core.testing.TestDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingBiometriaViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule(UnconfinedTestDispatcher())

    @Test
    fun `ativar com autenticacao bem sucedida salva preferencia e marca desbloqueado`() = runTest(UnconfinedTestDispatcher()) {
        val prefs = FakePreferences()
        val auth = FakeAuthRepository(SessionState.LoggedIn)
        val gerenciador = GerenciadorDeBloqueioDoApp(
            prefs,
            auth,
            Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC),
            FakeAppDispatchers(UnconfinedTestDispatcher()),
        )
        val autenticador = FakeAutenticador(
            disponibilidadeInicial = DisponibilidadeAutenticacao.DISPONIVEL,
            resultado = ResultadoAutenticacao.Autenticado,
        )
        val viewModel = OnboardingBiometriaViewModel(autenticador, prefs, gerenciador)

        viewModel.ativar(Activity())
        advanceUntilIdle()

        assertTrue(prefs.biometriaAtiva.first())
        assertTrue(prefs.biometriaOnboardingConcluido())
        assertTrue(viewModel.uiState.value.concluido)
        assertFalse(gerenciador.bloqueado.value)
        assertNull(viewModel.uiState.value.mensagemErro)
    }

    @Test
    fun `agora nao desabilita biometria conclui passo oito`() = runTest(UnconfinedTestDispatcher()) {
        val prefs = FakePreferences()
        val gerenciador = GerenciadorDeBloqueioDoApp(
            prefs,
            FakeAuthRepository(SessionState.LoggedIn),
            Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC),
            FakeAppDispatchers(UnconfinedTestDispatcher()),
        )
        val viewModel = OnboardingBiometriaViewModel(FakeAutenticador(), prefs, gerenciador)

        viewModel.pularOuAgoraNao()
        advanceUntilIdle()

        assertFalse(prefs.biometriaAtiva.first())
        assertTrue(prefs.biometriaOnboardingConcluido())
        assertTrue(viewModel.uiState.value.concluido)
    }

    @Test
    fun `sem cadastro no aparelho exibe orientacao sem concluir`() = runTest(UnconfinedTestDispatcher()) {
        val prefs = FakePreferences()
        val gerenciador = GerenciadorDeBloqueioDoApp(
            prefs,
            FakeAuthRepository(SessionState.LoggedIn),
            Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC),
            FakeAppDispatchers(UnconfinedTestDispatcher()),
        )
        val autenticador = FakeAutenticador(
            disponibilidadeInicial = DisponibilidadeAutenticacao.SEM_CADASTRO_NO_APARELHO,
            resultado = ResultadoAutenticacao.SemCadastroNoAparelho,
        )
        val viewModel = OnboardingBiometriaViewModel(autenticador, prefs, gerenciador)

        viewModel.ativar(Activity())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.semCadastroNoAparelho)
        assertFalse(viewModel.uiState.value.concluido)
        assertFalse(prefs.biometriaAtiva.first())
    }

    @Test
    fun `cancelamento no prompt nao ativa biometria e nao exibe erro`() = runTest(UnconfinedTestDispatcher()) {
        val prefs = FakePreferences()
        val gerenciador = GerenciadorDeBloqueioDoApp(
            prefs,
            FakeAuthRepository(SessionState.LoggedIn),
            Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC),
            FakeAppDispatchers(UnconfinedTestDispatcher()),
        )
        val autenticador = FakeAutenticador(resultado = ResultadoAutenticacao.Cancelado)
        val viewModel = OnboardingBiometriaViewModel(autenticador, prefs, gerenciador)

        viewModel.ativar(Activity())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.concluido)
        assertNull(viewModel.uiState.value.mensagemErro)
        assertFalse(prefs.biometriaAtiva.first())
    }

    @Test
    fun `falha de autenticacao informa usuario`() = runTest(UnconfinedTestDispatcher()) {
        val prefs = FakePreferences()
        val gerenciador = GerenciadorDeBloqueioDoApp(
            prefs,
            FakeAuthRepository(SessionState.LoggedIn),
            Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC),
            FakeAppDispatchers(UnconfinedTestDispatcher()),
        )
        val autenticador = FakeAutenticador(resultado = ResultadoAutenticacao.Falha(7, "Bloqueado"))
        val viewModel = OnboardingBiometriaViewModel(autenticador, prefs, gerenciador)

        viewModel.ativar(Activity())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.concluido)
        assertNotNull(viewModel.uiState.value.mensagemErro)
    }

    private class FakeAutenticador(
        private val disponibilidadeInicial: DisponibilidadeAutenticacao = DisponibilidadeAutenticacao.DISPONIVEL,
        private val resultado: ResultadoAutenticacao = ResultadoAutenticacao.Cancelado,
    ) : AutenticadorDoSistema {
        override fun disponibilidade(): DisponibilidadeAutenticacao = disponibilidadeInicial

        override suspend fun autenticar(
            activity: Activity,
            titulo: String,
            subtitulo: String?,
            descricao: String?,
        ): ResultadoAutenticacao = resultado

        override fun abrirConfiguracoesDeSeguranca(activity: Activity): Boolean = true
    }

    private class FakeAuthRepository(inicial: SessionState) : AuthRepository {
        override val session: StateFlow<SessionState> = MutableStateFlow(inicial)

        override suspend fun loginWithGoogle(activity: Activity): LoginResult = LoginResult.Cancelled

        override suspend fun logout() = Unit

        override suspend fun accessToken(forceRefresh: Boolean): String? = null
    }

    private class FakePreferences : UserPreferencesDataSource {
        private val biometriaFlow = MutableStateFlow(false)
        private var onboardingConcluido = false

        override val biometriaAtiva: Flow<Boolean> = biometriaFlow
        override val syncStatus: Flow<SyncStatus> = MutableStateFlow(SyncStatus.IDLE)

        override suspend fun setBiometriaAtiva(ativo: Boolean) {
            biometriaFlow.value = ativo
        }

        override suspend fun biometriaOnboardingConcluido(): Boolean = onboardingConcluido

        override suspend fun setBiometriaOnboardingConcluido(concluido: Boolean) {
            onboardingConcluido = concluido
        }

        override suspend fun setSyncStatus(status: SyncStatus) = Unit

        override suspend fun pullCursor(): String? = null

        override suspend fun setPullCursor(cursor: String?) = Unit

        override suspend fun pushCursor(): Instant? = null

        override suspend fun setPushCursor(instant: Instant) = Unit

        override suspend fun lastSyncAt(): Instant? = null

        override suspend fun setLastSyncAt(instant: Instant) = Unit

        override suspend fun deviceId(): String = "dev-1"

        override suspend fun activatedDeviceId(): String? = null

        override suspend fun setActivatedDeviceId(deviceId: String) = Unit

        override suspend fun consentVersion(): String? = null

        override suspend fun consentAt(): Instant? = null

        override suspend fun setConsent(version: String, instant: Instant) = Unit

        override suspend fun contasConfiguradas(): Boolean = false

        override suspend fun setContasConfiguradas(configuradas: Boolean) = Unit

        override suspend fun contasOnboardingPendentes(): List<String> = emptyList()

        override suspend fun setContasOnboardingPendentes(publicIds: List<String>) = Unit

        override suspend fun clear() = Unit
    }
}
