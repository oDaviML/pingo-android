package com.pingo.feature.accounts.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.ui.contas.ContaFormulario
import com.pingo.core.ui.contas.aplicar
import com.pingo.feature.accounts.api.AccountsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class ContaFormViewModel @Inject constructor(
    private val contaRepository: ContaRepository,
    private val instrumentoRepository: InstrumentoRepository,
    private val clock: Clock,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val contaPublicId: String? = savedStateHandle[AccountsRoute.ARG_CONTA_ID]

    private val mutableUiState = MutableStateFlow(
        ContaFormUiState(
            carregando = contaPublicId != null,
            edicao = contaPublicId != null,
        ),
    )

    val uiState: StateFlow<ContaFormUiState> = mutableUiState.asStateFlow()

    init {
        contaPublicId?.let(::carregar)
    }

    fun onEvent(event: ContaFormEvent) {
        when (event) {
            is ContaFormEvent.Formulario -> mutableUiState.update { state ->
                state.copy(formulario = state.formulario.aplicar(event.evento), falhouAoSalvar = false)
            }

            ContaFormEvent.Salvar -> salvar()
            ContaFormEvent.TentarDeNovo -> contaPublicId?.let(::carregar)
        }
    }

    private fun carregar(publicId: String) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(carregando = true, falhaDeLeitura = false) }

            runCatching {
                val conta = contaRepository.buscar(publicId)
                    ?: throw IllegalArgumentException("Conta inexistente")
                val instrumentos = instrumentoRepository.observePorConta(publicId).first()

                ContaFormulario.de(conta, instrumentos)
            }.fold(
                onSuccess = { formulario ->
                    mutableUiState.update {
                        it.copy(carregando = false, falhaDeLeitura = false, formulario = formulario)
                    }
                },
                onFailure = { exception ->
                    if (exception is CancellationException) {
                        throw exception
                    }

                    mutableUiState.update { it.copy(carregando = false, falhaDeLeitura = true) }
                },
            )
        }
    }

    private fun salvar() {
        val atual = mutableUiState.value
        if (!atual.podeSalvar) {
            return
        }

        mutableUiState.update { it.copy(salvando = true, falhouAoSalvar = false) }

        viewModelScope.launch {
            runCatching { persistir(atual.formulario) }.fold(
                onSuccess = {
                    mutableUiState.update { it.copy(salvando = false, concluido = true) }
                },
                onFailure = { exception ->
                    if (exception is CancellationException) {
                        throw exception
                    }

                    mutableUiState.update { it.copy(salvando = false, falhouAoSalvar = true) }
                },
            )
        }
    }

    private suspend fun persistir(formulario: ContaFormulario) {
        if (formulario.editando) {
            contaRepository.salvarCadastro(formulario.paraEdicao(), clock.instant())
        } else {
            contaRepository.criarComInstrumentos(listOf(formulario.paraNovaConta()), clock.instant())
        }
    }
}
