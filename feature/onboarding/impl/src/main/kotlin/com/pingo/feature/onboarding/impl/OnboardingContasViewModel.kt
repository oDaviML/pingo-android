package com.pingo.feature.onboarding.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.data.ContaRepository
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.NovaConta
import com.pingo.core.ui.contas.ContaFormulario
import com.pingo.core.ui.contas.aplicar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class OnboardingContasViewModel @Inject constructor(
    private val contaRepository: ContaRepository,
    private val preferences: UserPreferencesDataSource,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(OnboardingContasUiState())

    val uiState: StateFlow<OnboardingContasUiState> = mutableUiState.asStateFlow()

    private var contasSalvas = false

    fun onEvent(event: OnboardingContasEvent) {
        when (event) {
            is OnboardingContasEvent.AlternarBanco -> alternarBanco(event.instituicao)
            is OnboardingContasEvent.AlterarFormulario -> alterarFormulario(event.instituicao, event)
            OnboardingContasEvent.Continuar -> continuar()
            OnboardingContasEvent.Pular -> pular()
        }
    }

    private fun alternarBanco(instituicao: InstituicaoFinanceira) {
        mutableUiState.update { state ->
            val contas = if (state.contas.any { it.instituicao == instituicao }) {
                state.contas.filterNot { it.instituicao == instituicao }
            } else {
                state.contas + BancoOferecido.de(instituicao).escolhaInicial()
            }

            state.copy(contas = contas)
        }
    }

    private fun alterarFormulario(instituicao: InstituicaoFinanceira, evento: OnboardingContasEvent.AlterarFormulario) {
        mutableUiState.update { state ->
            state.copy(
                contas = state.contas.map { conta ->
                    if (conta.instituicao == instituicao) conta.aplicar(evento.evento) else conta
                },
            )
        }
    }

    private fun continuar() {
        val state = mutableUiState.value
        if (!state.podeContinuar) {
            return
        }

        val contas = state.contas.map(ContaFormulario::paraNovaConta)
        concluirPasso { salvarContas(contas) }
    }

    private fun pular() {
        if (mutableUiState.value.salvamento == SalvamentoUiState.Loading) {
            return
        }

        concluirPasso { preferences.setContasConfiguradas(true) }
    }

    private suspend fun salvarContas(contas: List<NovaConta>) {
        if (!contasSalvas) {
            contaRepository.criarComInstrumentos(contas, Instant.now())
            contasSalvas = true
        }

        preferences.setContasConfiguradas(true)
    }

    private fun concluirPasso(salvar: suspend () -> Unit) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(salvamento = SalvamentoUiState.Loading) }

            runCatching { salvar() }.fold(
                onSuccess = {
                    mutableUiState.update { it.copy(salvamento = SalvamentoUiState.Idle, concluido = true) }
                },
                onFailure = { exception ->
                    if (exception is CancellationException) {
                        throw exception
                    }

                    mutableUiState.update { it.copy(salvamento = SalvamentoUiState.Error) }
                },
            )
        }
    }
}
