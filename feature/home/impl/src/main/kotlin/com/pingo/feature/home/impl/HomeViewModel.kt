package com.pingo.feature.home.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.notifications.AcessoNotificacoes
import com.pingo.core.notifications.DiagnosticoDaCaptura
import com.pingo.core.notifications.SaudeDaCaptura
import com.pingo.core.notifications.SituacaoDaCaptura
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val acessoNotificacoes: AcessoNotificacoes, private val saudeDaCaptura: SaudeDaCaptura) :
    ViewModel() {
    private val mutableUiState = MutableStateFlow(HomeUiState(capturaAutomatica = acessoNotificacoes.concedido()))

    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            saudeDaCaptura.observar().collect(::aplicarDiagnostico)
        }
    }

    fun atualizarCaptura() {
        mutableUiState.update { it.copy(capturaAutomatica = acessoNotificacoes.concedido()) }
        viewModelScope.launch { aplicarDiagnostico(saudeDaCaptura.atual()) }
    }

    private fun aplicarDiagnostico(diagnostico: DiagnosticoDaCaptura) {
        mutableUiState.update { it.copy(semLeituraHa48h = diagnostico.situacao == SituacaoDaCaptura.SEM_LEITURA) }
    }
}
