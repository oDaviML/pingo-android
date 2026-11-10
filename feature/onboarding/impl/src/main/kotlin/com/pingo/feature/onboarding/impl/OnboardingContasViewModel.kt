package com.pingo.feature.onboarding.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.data.ContaRepository
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.NovaConta
import com.pingo.core.model.TAMANHO_ULTIMOS_DIGITOS
import com.pingo.core.model.TipoInstrumento
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
            is OnboardingContasEvent.AlterarApelidoConta -> atualizarEscolha(event.instituicao) {
                it.copy(apelido = event.apelido)
            }

            is OnboardingContasEvent.AlterarTipoConta -> atualizarEscolha(event.instituicao) {
                it.copy(tipoConta = event.tipo)
            }

            is OnboardingContasEvent.AlternarInstrumento -> alternarInstrumento(event.instituicao, event.tipo)
            is OnboardingContasEvent.AlterarApelidoInstrumento -> atualizarInstrumento(event.instituicao, event.tipo) {
                it.copy(apelido = event.apelido)
            }

            is OnboardingContasEvent.AlterarUltimosDigitos -> atualizarInstrumento(event.instituicao, event.tipo) {
                it.copy(ultimosDigitos = apenasDigitos(event.digitos))
            }

            OnboardingContasEvent.Continuar -> continuar()
            OnboardingContasEvent.Pular -> pular()
        }
    }

    private fun alternarBanco(instituicao: InstituicaoFinanceira) {
        mutableUiState.update { state ->
            val escolhas = if (state.escolhas.any { it.instituicao == instituicao }) {
                state.escolhas.filterNot { it.instituicao == instituicao }
            } else {
                state.escolhas + BancoOferecido.de(instituicao).escolhaInicial()
            }

            state.copy(escolhas = escolhas)
        }
    }

    private fun alternarInstrumento(instituicao: InstituicaoFinanceira, tipo: TipoInstrumento) {
        atualizarEscolha(instituicao) { escolha ->
            if (escolha.temInstrumento(tipo)) {
                escolha.removerInstrumento(tipo)
            } else {
                escolha.adicionarInstrumento(tipo)
            }
        }
    }

    private fun atualizarEscolha(instituicao: InstituicaoFinanceira, transformar: (EscolhaBanco) -> EscolhaBanco) {
        mutableUiState.update { state ->
            state.copy(
                escolhas = state.escolhas.map { escolha ->
                    if (escolha.instituicao == instituicao) transformar(escolha) else escolha
                },
            )
        }
    }

    private fun atualizarInstrumento(
        instituicao: InstituicaoFinanceira,
        tipo: TipoInstrumento,
        transformar: (InstrumentoEscolhido) -> InstrumentoEscolhido,
    ) {
        atualizarEscolha(instituicao) { escolha ->
            escolha.copy(
                instrumentos = escolha.instrumentos.map { instrumento ->
                    if (instrumento.tipo == tipo) transformar(instrumento) else instrumento
                },
            )
        }
    }

    private fun continuar() {
        val state = mutableUiState.value
        if (!state.podeContinuar) {
            return
        }

        val contas = state.escolhas.map(EscolhaBanco::paraNovaConta)
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

    private fun apenasDigitos(digitos: String): String = digitos.filter { it in '0'..'9' }.take(TAMANHO_ULTIMOS_DIGITOS)
}
