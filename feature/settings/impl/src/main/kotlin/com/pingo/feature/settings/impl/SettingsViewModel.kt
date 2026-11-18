package com.pingo.feature.settings.impl

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.auth.DisponibilidadeAutenticacao
import com.pingo.core.auth.ResultadoAutenticacao
import com.pingo.core.data.AccountRepository
import com.pingo.core.data.assinatura.AgendadorDeAvisosOpcional
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.model.TemaDoApp
import com.pingo.core.network.update.AtualizacaoDisponivel
import com.pingo.core.network.update.EstadoDownloadApk
import com.pingo.core.network.update.MotivoFalhaDownloadApk
import com.pingo.core.network.update.ResultadoVerificacaoAtualizacao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val preferences: UserPreferencesDataSource,
    private val agendadorDeAvisos: AgendadorDeAvisosOpcional,
    private val extras: DependenciasExtrasSettings,
) : ViewModel() {
    constructor(
        accountRepository: AccountRepository,
        preferences: UserPreferencesDataSource,
        agendadorDeAvisos: AgendadorDeAvisosOpcional,
    ) : this(
        accountRepository = accountRepository,
        preferences = preferences,
        agendadorDeAvisos = agendadorDeAvisos,
        extras = SettingsSemExtras.criar(preferences),
    )

    constructor(accountRepository: AccountRepository) : this(
        accountRepository = accountRepository,
        preferences = SemPreferencias,
        agendadorDeAvisos = AgendadorDeAvisosOpcional(),
        extras = SettingsSemExtras.criar(SemPreferencias),
    )

    private val mutableUiState = MutableStateFlow(
        SettingsUiState(
            disponibilidadeBiometria = extras.autenticador.disponibilidade(),
            versaoApp = extras.atualizacao.informacoesVersaoApp.versaoNome,
            codigoVersaoApp = extras.atualizacao.informacoesVersaoApp.versaoCodigo,
        ),
    )

    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()

    init {
        if (preferences !== SemPreferencias) {
            observarPreferencias()
        }
    }

    fun alternarAvisoAssinaturas(ativo: Boolean) {
        viewModelScope.launch {
            preferences.setAvisoAssinaturasAtivo(ativo)
            mutableUiState.update { it.copy(avisoAssinaturasAtivo = ativo, permissaoNotificacaoNegada = false) }
            if (ativo) agendadorDeAvisos.reconciliarTodas() else agendadorDeAvisos.cancelarTodos()
        }
    }

    fun aoNegarPermissaoDeNotificacao() {
        viewModelScope.launch {
            preferences.setAvisoAssinaturasAtivo(false)
            mutableUiState.update { it.copy(avisoAssinaturasAtivo = false, permissaoNotificacaoNegada = true) }
            agendadorDeAvisos.cancelarTodos()
        }
    }

    fun sincronizarPermissaoDeNotificacao(concedida: Boolean) {
        if (concedida) {
            mutableUiState.update { it.copy(permissaoNotificacaoNegada = false) }
        } else if (mutableUiState.value.avisoAssinaturasAtivo) {
            aoNegarPermissaoDeNotificacao()
        }
    }

    fun alternarRevisarPelaNotificacao(ativo: Boolean) {
        viewModelScope.launch {
            preferences.setRevisarPelaNotificacaoAtivo(ativo)
            mutableUiState.update { it.copy(revisarPelaNotificacaoAtivo = ativo) }
        }
    }

    fun alternarAlertaCapturaParada(ativo: Boolean) {
        viewModelScope.launch {
            preferences.setAlertaCapturaParadaAtivo(ativo)
            mutableUiState.update { it.copy(alertaCapturaParadaAtivo = ativo) }
        }
    }

    fun alternarTelemetria(ativo: Boolean) {
        viewModelScope.launch {
            preferences.setTelemetriaOptIn(ativo)
            extras.analytics.setAnalyticsEnabled(ativo)
            mutableUiState.update { it.copy(telemetriaOptIn = ativo) }
        }
    }

    fun atualizarDisponibilidadeBiometria() {
        val disponibilidade = extras.autenticador.disponibilidade()
        mutableUiState.update { state ->
            state.copy(
                disponibilidadeBiometria = disponibilidade,
                mostrarAtalhoCadastroBiometria = disponibilidade == DisponibilidadeAutenticacao.SEM_CADASTRO_NO_APARELHO &&
                    state.mensagemSeguranca != null,
            )
        }
    }

    fun alternarBiometria(ativo: Boolean, activity: Activity?) {
        if (!ativo) {
            viewModelScope.launch {
                preferences.setBiometriaAtiva(false)
                mutableUiState.update {
                    it.copy(biometriaAtiva = false, mensagemSeguranca = null, mostrarAtalhoCadastroBiometria = false)
                }
            }
            return
        }
        if (activity == null) {
            mutableUiState.update { it.copy(mensagemSeguranca = MENSAGEM_FALHA_BIOMETRIA) }
            return
        }

        viewModelScope.launch {
            extras.gerenciadorDeBloqueio.marcarAutenticacaoEmAndamento(true)
            val resultado = try {
                extras.autenticador.autenticar(
                    activity = activity,
                    titulo = "Confirmar identidade",
                    subtitulo = "Ative o bloqueio do Pingo com biometria ou senha",
                )
            } finally {
                extras.gerenciadorDeBloqueio.marcarAutenticacaoEmAndamento(false)
            }
            aplicarResultadoBiometria(
                resultado = resultado,
                gerenciador = extras.gerenciadorDeBloqueio,
                preferences = preferences,
                mutableUiState = mutableUiState,
            )
        }
    }

    fun abrirConfiguracoesDeSeguranca(activity: Activity) {
        extras.autenticador.abrirConfiguracoesDeSeguranca(activity)
    }

    fun abrirSheetTempoBloqueio() {
        mutableUiState.update { it.copy(mostrarSheetTempoBloqueio = true) }
    }

    fun fecharSheetTempoBloqueio() {
        mutableUiState.update { it.copy(mostrarSheetTempoBloqueio = false) }
    }

    fun selecionarTempoBloqueio(opcao: OpcaoBloqueio) {
        viewModelScope.launch {
            preferences.setTempoBloqueioSegundos(opcao.segundos)
            mutableUiState.update { it.copy(tempoBloqueioSegundos = opcao.segundos, mostrarSheetTempoBloqueio = false) }
        }
    }

    fun abrirSheetTema() {
        mutableUiState.update { it.copy(mostrarSheetTema = true) }
    }

    fun fecharSheetTema() {
        mutableUiState.update { it.copy(mostrarSheetTema = false) }
    }

    fun selecionarTema(tema: TemaDoApp) {
        viewModelScope.launch {
            preferences.setTemaDoApp(tema)
            mutableUiState.update { it.copy(tema = tema, mostrarSheetTema = false) }
        }
    }

    fun verificarAtualizacao() {
        val atual = mutableUiState.value.estadoAtualizacao
        if (atual is EstadoAtualizacaoAppUi.Verificando || atual is EstadoAtualizacaoAppUi.Baixando) {
            return
        }

        viewModelScope.launch {
            mutableUiState.update { it.copy(estadoAtualizacao = EstadoAtualizacaoAppUi.Verificando) }
            val resultado = runCatching {
                extras.atualizacao.verificadorDeAtualizacao.verificar(mutableUiState.value.versaoApp)
            }.getOrElse { ResultadoVerificacaoAtualizacao.Indisponivel }

            mutableUiState.update { it.copy(estadoAtualizacao = mapearResultadoVerificacao(resultado)) }
        }
    }

    fun baixarAtualizacao(info: AtualizacaoDisponivel) {
        if (mutableUiState.value.estadoAtualizacao is EstadoAtualizacaoAppUi.Baixando) {
            return
        }

        viewModelScope.launch {
            extras.atualizacao.downloaderDeApk.baixar(info).collect { estado ->
                val podeInstalar = extras.atualizacao.gerenciadorDeInstalacao.podeInstalarPacotes()
                mutableUiState.update { it.copy(estadoAtualizacao = mapearEstadoDownload(info, estado, podeInstalar)) }
            }
        }
    }

    fun atualizarPermissaoInstalacao() {
        val atual = mutableUiState.value.estadoAtualizacao
        if (atual is EstadoAtualizacaoAppUi.ProntoParaInstalar) {
            val precisa = !extras.atualizacao.gerenciadorDeInstalacao.podeInstalarPacotes()
            mutableUiState.update { it.copy(estadoAtualizacao = atual.copy(precisaPermissaoFontes = precisa)) }
        }
    }

    fun intentPermissaoFontesDesconhecidas() = extras.atualizacao.gerenciadorDeInstalacao.intentPermissaoFontesDesconhecidas()

    fun intentInstalacaoApk(arquivoApk: java.io.File) = extras.atualizacao.gerenciadorDeInstalacao.intentInstalacao(arquivoApk)

    fun solicitarLogout() {
        mutableUiState.update { it.copy(showLogoutDialog = true) }
    }

    fun cancelarLogout() {
        if (!mutableUiState.value.saindoDaConta) {
            mutableUiState.update { it.copy(showLogoutDialog = false) }
        }
    }

    fun confirmarLogout() {
        if (mutableUiState.value.saindoDaConta) {
            return
        }

        viewModelScope.launch {
            mutableUiState.update { it.copy(saindoDaConta = true) }
            extras.authRepository.logout()
            mutableUiState.update { it.copy(saindoDaConta = false, showLogoutDialog = false, loggedOut = true) }
        }
    }

    fun showDeleteDialog() {
        mutableUiState.update { it.copy(showDeleteDialog = true, deleteFailed = false) }
    }

    fun dismissDeleteDialog() {
        if (mutableUiState.value.deleting) {
            return
        }

        mutableUiState.update { it.copy(showDeleteDialog = false, deleteFailed = false) }
    }

    fun deleteAccount() {
        if (mutableUiState.value.deleting) {
            return
        }

        viewModelScope.launch {
            mutableUiState.update { it.copy(deleting = true, deleteFailed = false) }

            runCatching { accountRepository.deleteAccount() }
                .fold(
                    onSuccess = {
                        mutableUiState.update {
                            it.copy(deleting = false, showDeleteDialog = false, accountDeleted = true)
                        }
                    },
                    onFailure = {
                        mutableUiState.update { it.copy(deleting = false, deleteFailed = true) }
                    },
                )
        }
    }

    private fun observarPreferencias() {
        viewModelScope.launch {
            combine(
                combine(
                    preferences.avisoAssinaturasAtivo,
                    preferences.biometriaAtiva,
                    preferences.tempoBloqueioSegundos,
                ) { aviso, biometria, tempo -> Triple(aviso, biometria, tempo) },
                combine(
                    preferences.revisarPelaNotificacaoAtivo,
                    preferences.alertaCapturaParadaAtivo,
                    preferences.telemetriaOptIn,
                ) { revisar, parada, telemetria -> Triple(revisar, parada, telemetria) },
                combine(
                    preferences.nomeUsuario,
                    preferences.emailUsuario,
                    preferences.temaDoApp,
                ) { nome, email, tema -> Triple(nome, email, tema) },
            ) { segurancaPref, notifPref, perfil ->
                PreferenciasSnapshot(segurancaPref, notifPref, perfil)
            }.collect { snap ->
                mutableUiState.update { state ->
                    state.copy(
                        avisoAssinaturasAtivo = snap.seguranca.first,
                        biometriaAtiva = snap.seguranca.second,
                        tempoBloqueioSegundos = snap.seguranca.third,
                        revisarPelaNotificacaoAtivo = snap.notif.first,
                        alertaCapturaParadaAtivo = snap.notif.second,
                        telemetriaOptIn = snap.notif.third,
                        nomeUsuario = snap.perfil.first,
                        emailUsuario = snap.perfil.second,
                        tema = snap.perfil.third,
                    )
                }
            }
        }
    }

    private data class PreferenciasSnapshot(
        val seguranca: Triple<Boolean, Boolean, Long>,
        val notif: Triple<Boolean, Boolean, Boolean>,
        val perfil: Triple<String?, String?, TemaDoApp>,
    )

    private object SemPreferencias : UserPreferencesDataSource {
        override val syncStatus: Flow<SyncStatus> = flowOf(SyncStatus.IDLE)

        override suspend fun setSyncStatus(status: SyncStatus) = Unit

        override suspend fun pullCursor(): String? = null

        override suspend fun setPullCursor(cursor: String?) = Unit

        override suspend fun pushCursor(): Instant? = null

        override suspend fun setPushCursor(instant: Instant) = Unit

        override suspend fun lastSyncAt(): Instant? = null

        override suspend fun setLastSyncAt(instant: Instant) = Unit

        override suspend fun deviceId(): String = ""

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

    private companion object {
        const val MENSAGEM_FALHA_BIOMETRIA = "Não foi possível validar sua identidade. Tente de novo."
    }
}

private suspend fun aplicarResultadoBiometria(
    resultado: ResultadoAutenticacao,
    gerenciador: com.pingo.core.auth.GerenciadorDeBloqueioDoApp,
    preferences: UserPreferencesDataSource,
    mutableUiState: MutableStateFlow<SettingsUiState>,
) {
    when (resultado) {
        ResultadoAutenticacao.Autenticado -> {
            gerenciador.marcarDesbloqueado()
            preferences.setBiometriaAtiva(true)
            mutableUiState.update {
                it.copy(biometriaAtiva = true, mensagemSeguranca = null, mostrarAtalhoCadastroBiometria = false)
            }
        }

        ResultadoAutenticacao.SemCadastroNoAparelho -> mutableUiState.update {
            it.copy(
                biometriaAtiva = false,
                mensagemSeguranca = "Cadastre sua digital ou PIN nas configurações do Android para usar este bloqueio.",
                mostrarAtalhoCadastroBiometria = true,
            )
        }

        ResultadoAutenticacao.Indisponivel -> mutableUiState.update {
            it.copy(
                biometriaAtiva = false,
                mensagemSeguranca = "Este celular não possui biometria nem senha configurável.",
                mostrarAtalhoCadastroBiometria = false,
            )
        }

        ResultadoAutenticacao.Cancelado -> Unit

        is ResultadoAutenticacao.Falha -> mutableUiState.update {
            it.copy(
                biometriaAtiva = false,
                mensagemSeguranca = "Não foi possível validar sua identidade. Tente de novo.",
                mostrarAtalhoCadastroBiometria = false,
            )
        }
    }
}

private fun mapearResultadoVerificacao(resultado: ResultadoVerificacaoAtualizacao): EstadoAtualizacaoAppUi = when (resultado) {
    is ResultadoVerificacaoAtualizacao.Atualizado -> EstadoAtualizacaoAppUi.Atualizado(resultado.versaoAtual)
    is ResultadoVerificacaoAtualizacao.NovaVersaoDisponivel -> EstadoAtualizacaoAppUi.Disponivel(resultado.info)
    ResultadoVerificacaoAtualizacao.SemRede ->
        EstadoAtualizacaoAppUi.Falha("Sem conexão com a internet para verificar atualizações.")
    ResultadoVerificacaoAtualizacao.Indisponivel ->
        EstadoAtualizacaoAppUi.Falha("Não foi possível consultar atualizações agora.")
}

private fun mapearEstadoDownload(info: AtualizacaoDisponivel, estado: EstadoDownloadApk, podeInstalar: Boolean): EstadoAtualizacaoAppUi =
    when (estado) {
        is EstadoDownloadApk.EmProgresso -> EstadoAtualizacaoAppUi.Baixando(info, estado.percentual)
        is EstadoDownloadApk.Concluido -> EstadoAtualizacaoAppUi.ProntoParaInstalar(
            info = info,
            arquivoApk = estado.arquivoApk,
            precisaPermissaoFontes = !podeInstalar,
        )
        is EstadoDownloadApk.Falha -> EstadoAtualizacaoAppUi.Falha(mensagemFalhaDownload(estado.motivo), info)
    }

private fun mensagemFalhaDownload(motivo: MotivoFalhaDownloadApk): String = when (motivo) {
    MotivoFalhaDownloadApk.INTEGRIDADE_INVALIDA ->
        "O arquivo baixado falhou na verificação SHA-256 e foi descartado."
    MotivoFalhaDownloadApk.SEM_REDE ->
        "Conexão perdida durante o download da atualização."
    MotivoFalhaDownloadApk.ERRO_SERVIDOR ->
        "O servidor de releases não respondeu como esperado."
    MotivoFalhaDownloadApk.ERRO_DISCO ->
        "Sem espaço disponível no aparelho para salvar o APK."
    MotivoFalhaDownloadApk.URL_INVALIDA ->
        "Endereço do pacote de atualização inválido."
}
