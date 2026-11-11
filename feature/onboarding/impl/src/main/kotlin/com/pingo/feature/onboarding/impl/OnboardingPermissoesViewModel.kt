package com.pingo.feature.onboarding.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.datastore.PermissaoDoPingo
import com.pingo.core.datastore.PermissoesDataSource
import com.pingo.core.notifications.AcessoNotificacoes
import com.pingo.core.notifications.NotificacoesDoSistema
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingPermissoesViewModel @Inject constructor(
    private val acessoNotificacoes: AcessoNotificacoes,
    private val notificacoesDoSistema: NotificacoesDoSistema,
    private val permissoes: PermissoesDataSource,
    fabricanteDoAparelho: FabricanteDoAparelho,
    private val bateriaDoAparelho: BateriaDoAparelho,
) : ViewModel() {
    private val fabricanteDetectado = fabricanteDoAparelho.fabricante()
    private val mutableUiState = MutableStateFlow(
        OnboardingPermissoesUiState(
            acessoConcedido = acessoNotificacoes.concedido(),
            notificacoesHabilitadas = notificacoesDoSistema.habilitadas(),
            fabricanteDetectado = fabricanteDetectado,
            fabricanteSelecionado = fabricanteDetectado,
            semRestricaoDeBateria = bateriaDoAparelho.semRestricaoDeBateria(),
        ),
    )

    val uiState: StateFlow<OnboardingPermissoesUiState> = mutableUiState.asStateFlow()

    fun onEvent(event: OnboardingPermissoesEvent) {
        when (event) {
            OnboardingPermissoesEvent.AbrirAjustesDeNotificacoes -> mutableUiState.update { it.copy(solicitouAjustes = true) }
            OnboardingPermissoesEvent.NegarAcesso -> mutableUiState.update { it.copy(modoManual = true) }
            OnboardingPermissoesEvent.ContinuarSemAcesso -> registrarAcesso(PermissaoDoPingo.NEGADA)
            OnboardingPermissoesEvent.ContinuarComAcesso -> registrarAcesso(PermissaoDoPingo.PERMITIDA)
            OnboardingPermissoesEvent.PedirNotificacoes -> Unit
            OnboardingPermissoesEvent.CorrigirNotificacoes -> Unit
            OnboardingPermissoesEvent.PularNotificacoes -> registrarNotificacoes(
                permissao = PermissaoDoPingo.NAO_PERGUNTADA,
                habilitadas = notificacoesDoSistema.habilitadas(),
                avancar = true,
            )
            is OnboardingPermissoesEvent.SelecionarFabricante -> mutableUiState.update {
                it.copy(fabricanteSelecionado = event.fabricante)
            }

            OnboardingPermissoesEvent.AbrirAjustesDeBateria -> Unit
            OnboardingPermissoesEvent.AbrirInicioAutomatico -> Unit
            OnboardingPermissoesEvent.Concluir -> mutableUiState.update { it.copy(concluido = true) }
            OnboardingPermissoesEvent.Pular -> pularPasso()
        }
    }

    fun atualizarEstadosDoSistema() {
        val acessoConcedido = acessoNotificacoes.concedido()
        val notificacoesHabilitadas = notificacoesDoSistema.habilitadas()
        val semRestricaoDeBateria = bateriaDoAparelho.semRestricaoDeBateria()
        val solicitouAjustes = mutableUiState.value.solicitouAjustes

        mutableUiState.update { state ->
            state.copy(
                acessoConcedido = acessoConcedido,
                modoManual = !acessoConcedido && (state.modoManual || state.solicitouAjustes),
                notificacoesHabilitadas = notificacoesHabilitadas,
                semRestricaoDeBateria = semRestricaoDeBateria,
            )
        }

        if (acessoConcedido && solicitouAjustes) {
            viewModelScope.launch { runCatching { permissoes.registrarAcessoAsNotificacoes(PermissaoDoPingo.PERMITIDA) } }
        }
    }

    fun registrarNotificacoesDoSistema(habilitadas: Boolean) {
        registrarNotificacoes(
            permissao = if (habilitadas) PermissaoDoPingo.PERMITIDA else PermissaoDoPingo.NEGADA,
            habilitadas = habilitadas,
            avancar = true,
        )
    }

    private fun registrarAcesso(permissao: PermissaoDoPingo) {
        viewModelScope.launch {
            runCatching { permissoes.registrarAcessoAsNotificacoes(permissao) }
            val acessoConcedido = acessoNotificacoes.concedido()
            mutableUiState.update { state ->
                state.copy(
                    passo = PassoDePermissoes.NOTIFICACOES_DO_PINGO,
                    acessoConcedido = acessoConcedido,
                    modoManual = !acessoConcedido,
                )
            }
        }
    }

    private fun registrarNotificacoes(permissao: PermissaoDoPingo, habilitadas: Boolean, avancar: Boolean) {
        viewModelScope.launch {
            runCatching { permissoes.registrarNotificacoesDoPingo(permissao) }
            mutableUiState.update { state ->
                state.copy(
                    notificacoesHabilitadas = habilitadas,
                    passo = if (avancar && state.passo == PassoDePermissoes.NOTIFICACOES_DO_PINGO) {
                        PassoDePermissoes.BATERIA
                    } else {
                        state.passo
                    },
                )
            }
        }
    }

    private fun pularPasso() {
        when (mutableUiState.value.passo) {
            PassoDePermissoes.ACESSO_AS_NOTIFICACOES -> {
                mutableUiState.update { it.copy(modoManual = true) }
                registrarAcesso(PermissaoDoPingo.NEGADA)
            }

            PassoDePermissoes.NOTIFICACOES_DO_PINGO -> registrarNotificacoes(
                permissao = PermissaoDoPingo.NAO_PERGUNTADA,
                habilitadas = notificacoesDoSistema.habilitadas(),
                avancar = true,
            )
            PassoDePermissoes.BATERIA -> mutableUiState.update { it.copy(concluido = true) }
        }
    }
}
