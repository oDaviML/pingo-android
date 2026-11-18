package com.pingo.feature.settings.impl

import android.app.Activity
import android.content.Intent
import com.pingo.core.analytics.AnalyticsFacade
import com.pingo.core.auth.AutenticadorDoSistema
import com.pingo.core.auth.AuthRepository
import com.pingo.core.auth.DisponibilidadeAutenticacao
import com.pingo.core.auth.GerenciadorDeBloqueioDoApp
import com.pingo.core.auth.LoginResult
import com.pingo.core.auth.ResultadoAutenticacao
import com.pingo.core.auth.SessionState
import com.pingo.core.data.AccountRepository
import com.pingo.core.data.assinatura.AgendadorDeAvisosDeAssinatura
import com.pingo.core.data.assinatura.AgendadorDeAvisosOpcional
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.network.update.AtualizacaoDisponivel
import com.pingo.core.network.update.EstadoDownloadApk
import com.pingo.core.network.update.MotivoFalhaDownloadApk
import com.pingo.core.network.update.ResultadoVerificacaoAtualizacao
import com.pingo.core.network.update.VerificadorDeAtualizacaoApk
import com.pingo.core.testing.FakeAppDispatchers
import com.pingo.core.testing.TestDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule(UnconfinedTestDispatcher())

    private val accountRepository = FakeAccountRepository()
    private val preferences = FakePreferences()
    private val agendador = FakeAgendador()
    private val authRepository = FakeAuthRepository()
    private val autenticador = FakeAutenticador()
    private val analytics = FakeAnalytics()
    private val verificador = FakeVerificadorAtualizacao()
    private val downloader = FakeDownloaderAtualizacao()
    private val instalador = FakeInstaladorApk()

    private val viewModel = SettingsViewModel(accountRepository)

    @Test
    fun `aviso de assinaturas comeca desligado por padrao`() = runTest(UnconfinedTestDispatcher()) {
        val vm = criarViewModel()

        assertFalse(vm.uiState.value.avisoAssinaturasAtivo)
        assertFalse(vm.uiState.value.permissaoNotificacaoNegada)
    }

    @Test
    fun `ligar e desligar aviso de assinaturas persiste e reconcilia ou cancela`() = runTest(UnconfinedTestDispatcher()) {
        val vm = criarViewModel()

        vm.alternarAvisoAssinaturas(true)

        assertTrue(vm.uiState.value.avisoAssinaturasAtivo)
        assertTrue(preferences.avisoAssinaturasAtivo.first())
        assertFalse(vm.uiState.value.permissaoNotificacaoNegada)
        assertEquals(1, agendador.reconciliacoes)

        vm.alternarAvisoAssinaturas(false)

        assertFalse(vm.uiState.value.avisoAssinaturasAtivo)
        assertFalse(preferences.avisoAssinaturasAtivo.first())
        assertEquals(1, agendador.cancelamentosTotais)
    }

    @Test
    fun `negar permissao de notificacao mantem aviso desligado e sinaliza bloqueio`() = runTest(UnconfinedTestDispatcher()) {
        val vm = criarViewModel()

        vm.aoNegarPermissaoDeNotificacao()

        assertFalse(vm.uiState.value.avisoAssinaturasAtivo)
        assertFalse(preferences.avisoAssinaturasAtivo.first())
        assertTrue(vm.uiState.value.permissaoNotificacaoNegada)
        assertEquals(0, agendador.reconciliacoes)
        assertEquals(1, agendador.cancelamentosTotais)
    }

    @Test
    fun `sincronizar permissao revogada desativa aviso e limpa ao conceder`() = runTest(UnconfinedTestDispatcher()) {
        val vm = criarViewModel()
        vm.alternarAvisoAssinaturas(true)

        vm.sincronizarPermissaoDeNotificacao(concedida = false)

        assertFalse(vm.uiState.value.avisoAssinaturasAtivo)
        assertTrue(vm.uiState.value.permissaoNotificacaoNegada)
        assertFalse(preferences.avisoAssinaturasAtivo.first())
        assertEquals(1, agendador.cancelamentosTotais)

        vm.sincronizarPermissaoDeNotificacao(concedida = true)

        assertFalse(vm.uiState.value.permissaoNotificacaoNegada)
    }

    @Test
    fun `exclusao confirmada conclui e sinaliza conta apagada`() = runTest(UnconfinedTestDispatcher()) {
        viewModel.showDeleteDialog()
        viewModel.deleteAccount()

        assertTrue(viewModel.uiState.value.accountDeleted)
        assertFalse(viewModel.uiState.value.showDeleteDialog)
    }

    @Test
    fun `falha na exclusao mantem o dialogo e permite tentar de novo`() = runTest(UnconfinedTestDispatcher()) {
        accountRepository.failure = IllegalStateException("sem rede")

        viewModel.showDeleteDialog()
        viewModel.deleteAccount()

        assertTrue(viewModel.uiState.value.deleteFailed)
        assertTrue(viewModel.uiState.value.showDeleteDialog)
        assertFalse(viewModel.uiState.value.accountDeleted)

        accountRepository.failure = null
        viewModel.deleteAccount()

        assertTrue(viewModel.uiState.value.accountDeleted)
    }

    @Test
    fun `cancelar fecha o dialogo sem excluir`() = runTest(UnconfinedTestDispatcher()) {
        viewModel.showDeleteDialog()
        viewModel.dismissDeleteDialog()

        assertFalse(viewModel.uiState.value.showDeleteDialog)
        assertFalse(viewModel.uiState.value.accountDeleted)
    }

    @Test
    fun `telemetria comeca desligada e alternancia atualiza analytics`() = runTest(UnconfinedTestDispatcher()) {
        val vm = criarViewModel()

        assertFalse(vm.uiState.value.telemetriaOptIn)

        vm.alternarTelemetria(true)
        assertTrue(vm.uiState.value.telemetriaOptIn)
        assertEquals(listOf(true), analytics.alteracoes)

        vm.alternarTelemetria(false)
        assertFalse(vm.uiState.value.telemetriaOptIn)
        assertEquals(listOf(true, false), analytics.alteracoes)
    }

    @Test
    fun `toggles de revisao por notificacao e alerta de captura parada persistem`() = runTest(UnconfinedTestDispatcher()) {
        val vm = criarViewModel()

        assertTrue(vm.uiState.value.revisarPelaNotificacaoAtivo)
        assertTrue(vm.uiState.value.alertaCapturaParadaAtivo)

        vm.alternarRevisarPelaNotificacao(false)
        vm.alternarAlertaCapturaParada(false)

        assertFalse(preferences.revisarPelaNotificacaoAtivo.first())
        assertFalse(preferences.alertaCapturaParadaAtivo.first())
    }

    @Test
    fun `biometria exige autenticacao e sem cadastro mostra atalho sem ativar`() = runTest(UnconfinedTestDispatcher()) {
        val vm = criarViewModel()
        autenticador.resultado = ResultadoAutenticacao.SemCadastroNoAparelho

        vm.alternarBiometria(true, Activity())

        assertFalse(preferences.biometriaAtiva.first())
        assertTrue(vm.uiState.value.mostrarAtalhoCadastroBiometria)
        assertNotNull(vm.uiState.value.mensagemSeguranca)

        autenticador.resultado = ResultadoAutenticacao.Autenticado
        vm.alternarBiometria(true, Activity())

        assertTrue(preferences.biometriaAtiva.first())
        assertFalse(vm.uiState.value.mostrarAtalhoCadastroBiometria)
    }

    @Test
    fun `selecionar tempo de bloqueio persiste e fecha sheet`() = runTest(UnconfinedTestDispatcher()) {
        val vm = criarViewModel()

        vm.abrirSheetTempoBloqueio()
        assertTrue(vm.uiState.value.mostrarSheetTempoBloqueio)

        vm.selecionarTempoBloqueio(OpcaoBloqueio.IMEDIATO)
        assertEquals(0L, preferences.tempoBloqueioSegundos.first())
        assertFalse(vm.uiState.value.mostrarSheetTempoBloqueio)

        vm.selecionarTempoBloqueio(OpcaoBloqueio.NUNCA)
        assertEquals(-1L, preferences.tempoBloqueioSegundos.first())
    }

    @Test
    fun `confirmar logout encerra sessao sem apagar dados da conta`() = runTest(UnconfinedTestDispatcher()) {
        val vm = criarViewModel()

        vm.solicitarLogout()
        assertTrue(vm.uiState.value.showLogoutDialog)

        vm.confirmarLogout()

        assertTrue(authRepository.logoutChamado)
        assertFalse(accountRepository.deleteChamado)
        assertTrue(vm.uiState.value.loggedOut)
    }

    @Test
    fun `atualizacao apk verifica baixa com progresso e checa permissao de fontes`() = runTest(UnconfinedTestDispatcher()) {
        val vm = criarViewModel()
        val info = AtualizacaoDisponivel(
            "0.3.0",
            "v0.3.0",
            "Beta",
            "Notas",
            "https://github.com/o/r",
            "https://example.com/a.apk",
            "pingo.apk",
        )
        verificador.resultado = ResultadoVerificacaoAtualizacao.NovaVersaoDisponivel(info)
        instalador.podeInstalar = false
        downloader.estados = listOf(EstadoDownloadApk.EmProgresso(50), EstadoDownloadApk.Concluido(File("pingo.apk")))

        vm.verificarAtualizacao()
        assertTrue(vm.uiState.value.estadoAtualizacao is EstadoAtualizacaoAppUi.Disponivel)

        vm.baixarAtualizacao(info)
        val pronto = vm.uiState.value.estadoAtualizacao as EstadoAtualizacaoAppUi.ProntoParaInstalar
        assertTrue(pronto.precisaPermissaoFontes)

        instalador.podeInstalar = true
        vm.atualizarPermissaoInstalacao()
        assertFalse((vm.uiState.value.estadoAtualizacao as EstadoAtualizacaoAppUi.ProntoParaInstalar).precisaPermissaoFontes)
    }

    @Test
    fun `falha de integridade sha256 informa erro de checksum com link fallback`() = runTest(UnconfinedTestDispatcher()) {
        val vm = criarViewModel()
        val info = AtualizacaoDisponivel(
            "0.3.0",
            "v0.3.0",
            "Beta",
            "Notas",
            "https://github.com/o/r",
            "https://example.com/a.apk",
            "pingo.apk",
        )
        downloader.estados = listOf(EstadoDownloadApk.Falha(MotivoFalhaDownloadApk.INTEGRIDADE_INVALIDA))

        vm.baixarAtualizacao(info)

        val falha = vm.uiState.value.estadoAtualizacao as EstadoAtualizacaoAppUi.Falha
        assertTrue(falha.mensagem.contains("SHA-256"))
        assertEquals(info, falha.info)
    }

    private fun criarViewModel(): SettingsViewModel {
        val gerenciador = GerenciadorDeBloqueioDoApp(
            preferences = preferences,
            authRepository = authRepository,
            clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC),
            dispatchers = FakeAppDispatchers(UnconfinedTestDispatcher()),
        )
        return SettingsViewModel(
            accountRepository = accountRepository,
            preferences = preferences,
            agendadorDeAvisos = AgendadorDeAvisosOpcional(Optional.of(agendador)),
            extras = DependenciasExtrasSettings(
                authRepository = authRepository,
                autenticador = autenticador,
                gerenciadorDeBloqueio = gerenciador,
                analytics = analytics,
                atualizacao = AtualizacaoSettingsDeps(
                    verificadorDeAtualizacao = verificador,
                    downloaderDeApk = downloader,
                    gerenciadorDeInstalacao = instalador,
                    informacoesVersaoApp = object : InformacoesVersaoApp {
                        override val versaoNome: String = "0.2.0"
                        override val versaoCodigo: Long = 2L
                    },
                ),
            ),
        )
    }

    private class FakeAccountRepository : AccountRepository {
        var failure: Throwable? = null
        var deleteChamado = false

        override suspend fun recordConsent(version: String) = Unit

        override suspend fun deleteAccount() {
            deleteChamado = true
            failure?.let { throw it }
        }
    }

    private class FakeAgendador : AgendadorDeAvisosDeAssinatura {
        var reconciliacoes = 0
        var cancelamentosTotais = 0

        override suspend fun reconciliarTodas() {
            reconciliacoes++
        }

        override suspend fun cancelarParaAssinatura(assinaturaPublicId: String) = Unit

        override suspend fun cancelarTodos() {
            cancelamentosTotais++
        }
    }

    private class FakeAuthRepository : AuthRepository {
        var logoutChamado = false
        override val session: StateFlow<SessionState> = MutableStateFlow(SessionState.LoggedIn)

        override suspend fun loginWithGoogle(activity: Activity): LoginResult = LoginResult.Cancelled

        override suspend fun logout() {
            logoutChamado = true
        }

        override suspend fun accessToken(forceRefresh: Boolean): String? = null
    }

    private class FakeAutenticador : AutenticadorDoSistema {
        var resultado: ResultadoAutenticacao = ResultadoAutenticacao.Autenticado

        override fun disponibilidade(): DisponibilidadeAutenticacao = DisponibilidadeAutenticacao.DISPONIVEL

        override suspend fun autenticar(
            activity: Activity,
            titulo: String,
            subtitulo: String?,
            descricao: String?,
        ): ResultadoAutenticacao = resultado

        override fun abrirConfiguracoesDeSeguranca(activity: Activity): Boolean = true
    }

    private class FakeAnalytics : AnalyticsFacade {
        val alteracoes = mutableListOf<Boolean>()

        override fun setAnalyticsEnabled(enabled: Boolean) {
            alteracoes += enabled
        }

        override fun logEvent(name: String, attributes: Map<String, String>) = Unit

        override fun recordError(throwable: Throwable) = Unit
    }

    private class FakeVerificadorAtualizacao : VerificadorDeAtualizacaoApk {
        var resultado: ResultadoVerificacaoAtualizacao = ResultadoVerificacaoAtualizacao.Atualizado("0.2.0")

        override suspend fun verificar(versaoInstalada: String): ResultadoVerificacaoAtualizacao = resultado
    }

    private class FakeDownloaderAtualizacao : DownloaderAtualizacaoApk {
        var estados: List<EstadoDownloadApk> = emptyList()

        override fun baixar(atualizacao: AtualizacaoDisponivel): Flow<EstadoDownloadApk> = flowOf(*estados.toTypedArray())
    }

    private class FakeInstaladorApk : GerenciadorInstalacaoApk {
        var podeInstalar = true

        override fun podeInstalarPacotes(): Boolean = podeInstalar

        override fun intentPermissaoFontesDesconhecidas(): Intent = Intent()

        override fun intentInstalacao(arquivoApk: File): Intent = Intent()
    }

    private class FakePreferences : UserPreferencesDataSource {
        private val aviso = MutableStateFlow(false)
        private val biometria = MutableStateFlow(false)
        private val tempo = MutableStateFlow(300L)
        private val revisar = MutableStateFlow(true)
        private val parada = MutableStateFlow(true)
        private val telemetria = MutableStateFlow(false)

        override val syncStatus: Flow<SyncStatus> = flowOf(SyncStatus.IDLE)
        override val avisoAssinaturasAtivo: Flow<Boolean> = aviso
        override val biometriaAtiva: Flow<Boolean> = biometria
        override val tempoBloqueioSegundos: Flow<Long> = tempo
        override val revisarPelaNotificacaoAtivo: Flow<Boolean> = revisar
        override val alertaCapturaParadaAtivo: Flow<Boolean> = parada
        override val telemetriaOptIn: Flow<Boolean> = telemetria

        override suspend fun setAvisoAssinaturasAtivo(ativo: Boolean) {
            aviso.value = ativo
        }

        override suspend fun setBiometriaAtiva(ativo: Boolean) {
            biometria.value = ativo
        }

        override suspend fun setTempoBloqueioSegundos(segundos: Long) {
            tempo.value = segundos
        }

        override suspend fun setRevisarPelaNotificacaoAtivo(ativo: Boolean) {
            revisar.value = ativo
        }

        override suspend fun setAlertaCapturaParadaAtivo(ativo: Boolean) {
            parada.value = ativo
        }

        override suspend fun setTelemetriaOptIn(ativo: Boolean) {
            telemetria.value = ativo
        }

        override suspend fun setSyncStatus(status: SyncStatus) = Unit

        override suspend fun pullCursor(): String? = null

        override suspend fun setPullCursor(cursor: String?) = Unit

        override suspend fun pushCursor(): Instant? = null

        override suspend fun setPushCursor(instant: Instant) = Unit

        override suspend fun lastSyncAt(): Instant? = null

        override suspend fun setLastSyncAt(instant: Instant) = Unit

        override suspend fun deviceId(): String = "dev"

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
