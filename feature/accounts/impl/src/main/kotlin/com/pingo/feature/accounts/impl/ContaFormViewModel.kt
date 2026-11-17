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
            is ContaFormEvent.Formulario -> alterarFormulario(event)
            ContaFormEvent.Salvar -> salvar()
            ContaFormEvent.TentarDeNovo -> tentarCarregarDeNovo()
            ContaFormEvent.SolicitarArquivamentoDaConta -> solicitarArquivamentoDaConta()
            is ContaFormEvent.SolicitarArquivamentoDoInstrumento -> solicitarArquivamentoDoInstrumento(event.chave)
            ContaFormEvent.ConfirmarArquivamento -> confirmarArquivamento()
            ContaFormEvent.CancelarArquivamento -> cancelarArquivamento()
            ContaFormEvent.ReativarConta -> reativarConta()
            is ContaFormEvent.ReativarInstrumento -> reativarInstrumento(event.chave)
        }
    }

    private fun tentarCarregarDeNovo() {
        val atual = mutableUiState.value
        if (!atual.falhaDeLeitura || atual.carregando) {
            return
        }

        contaPublicId?.let(::carregar)
    }

    private fun cancelarArquivamento() {
        if (mutableUiState.value.processandoArquivamento || mutableUiState.value.salvando) {
            return
        }

        mutableUiState.update { it.copy(dialogo = null) }
    }

    private fun alterarFormulario(event: ContaFormEvent.Formulario) {
        if (!mutableUiState.value.formularioEditavel) {
            return
        }

        mutableUiState.update { state ->
            state.copy(formulario = state.formulario.aplicar(event.evento), falhouAoSalvar = false)
        }
    }

    private fun carregar(publicId: String) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(carregando = true, falhaDeLeitura = false) }

            runCatching {
                val conta = contaRepository.buscar(publicId)
                    ?: throw IllegalArgumentException("Conta inexistente")
                val instrumentos = instrumentoRepository.observePorConta(publicId).first()

                conta to ContaFormulario.de(conta, instrumentos)
            }.fold(
                onSuccess = { (conta, formulario) ->
                    mutableUiState.update {
                        it.copy(
                            carregando = false,
                            falhaDeLeitura = false,
                            formulario = formulario,
                            contaArquivada = conta.arquivada,
                        )
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

    private fun solicitarArquivamentoDaConta() {
        val atual = mutableUiState.value
        if (!podeArquivar(atual)) {
            return
        }

        mutableUiState.update { it.copy(dialogo = DialogoDeArquivamento.Conta, falhouAoArquivar = false) }
    }

    private fun solicitarArquivamentoDoInstrumento(chave: String) {
        val atual = mutableUiState.value
        val instrumento = atual.formulario.instrumentos.firstOrNull { it.chave == chave }

        if (!podeArquivar(atual) || instrumento == null || instrumento.arquivado) {
            return
        }

        mutableUiState.update {
            it.copy(dialogo = DialogoDeArquivamento.Instrumento(chave = chave, apelido = instrumento.apelido), falhouAoArquivar = false)
        }
    }

    private fun podeArquivar(atual: ContaFormUiState): Boolean = !contaNaoEditavel(atual) && atual.formularioEditavel

    private fun contaNaoEditavel(atual: ContaFormUiState): Boolean = contaPublicId == null || !atual.edicao || atual.contaArquivada

    private fun confirmarArquivamento() {
        val atual = mutableUiState.value
        val dialogo = atual.dialogo ?: return
        val publicId = contaPublicId ?: return
        if (atual.salvando || atual.processandoArquivamento) {
            return
        }

        mutableUiState.update { it.copy(processandoArquivamento = true, falhouAoArquivar = false) }

        viewModelScope.launch {
            runCatching {
                when (dialogo) {
                    DialogoDeArquivamento.Conta -> contaRepository.arquivar(publicId, clock.instant())
                    is DialogoDeArquivamento.Instrumento -> instrumentoRepository.arquivar(dialogo.chave, clock.instant())
                }
            }.fold(
                onSuccess = {
                    mutableUiState.update { state ->
                        state.copy(
                            processandoArquivamento = false,
                            dialogo = null,
                            contaArquivada = state.contaArquivada || dialogo == DialogoDeArquivamento.Conta,
                            concluido = state.concluido || dialogo == DialogoDeArquivamento.Conta,
                            formulario = state.formulario.comInstrumentosArquivados(dialogo),
                        )
                    }
                },
                onFailure = { exception ->
                    if (exception is CancellationException) {
                        throw exception
                    }

                    mutableUiState.update { it.copy(processandoArquivamento = false, dialogo = null, falhouAoArquivar = true) }
                },
            )
        }
    }

    private fun reativarConta() {
        val publicId = contaPublicId ?: return
        val atual = mutableUiState.value
        if (!atual.contaArquivada || atual.salvando || atual.processandoArquivamento) {
            return
        }

        mutableUiState.update { it.copy(processandoArquivamento = true, falhouAoArquivar = false) }

        viewModelScope.launch {
            runCatching { contaRepository.reativar(publicId, clock.instant()) }.fold(
                onSuccess = {
                    mutableUiState.update { it.copy(processandoArquivamento = false, contaArquivada = false) }
                    carregar(publicId)
                },
                onFailure = { exception ->
                    if (exception is CancellationException) {
                        throw exception
                    }

                    mutableUiState.update { it.copy(processandoArquivamento = false, falhouAoArquivar = true) }
                },
            )
        }
    }

    private fun reativarInstrumento(chave: String) {
        val instrumento = mutableUiState.value.formulario.instrumentos.firstOrNull { it.chave == chave }
        if (instrumento == null || !instrumento.arquivado || !mutableUiState.value.formularioEditavel) {
            return
        }

        mutableUiState.update { it.copy(processandoArquivamento = true, falhouAoArquivar = false) }

        viewModelScope.launch {
            runCatching { instrumentoRepository.reativar(chave, clock.instant()) }.fold(
                onSuccess = {
                    mutableUiState.update { state ->
                        state.copy(
                            processandoArquivamento = false,
                            formulario = state.formulario.comInstrumentoReativado(chave),
                        )
                    }
                },
                onFailure = { exception ->
                    if (exception is CancellationException) {
                        throw exception
                    }

                    mutableUiState.update { it.copy(processandoArquivamento = false, falhouAoArquivar = true) }
                },
            )
        }
    }

    private fun ContaFormulario.comInstrumentosArquivados(dialogo: DialogoDeArquivamento): ContaFormulario = when (dialogo) {
        DialogoDeArquivamento.Conta -> copy(instrumentos = instrumentos.map { it.copy(arquivado = true) })
        is DialogoDeArquivamento.Instrumento ->
            copy(instrumentos = instrumentos.map { if (it.chave == dialogo.chave) it.copy(arquivado = true) else it })
    }

    private fun ContaFormulario.comInstrumentoReativado(chave: String): ContaFormulario =
        copy(instrumentos = instrumentos.map { if (it.chave == chave) it.copy(arquivado = false) else it })

    private suspend fun persistir(formulario: ContaFormulario) {
        if (formulario.editando) {
            contaRepository.salvarCadastro(formulario.paraEdicao(), clock.instant())
        } else {
            contaRepository.criarComInstrumentos(listOf(formulario.paraNovaConta()), clock.instant())
        }
    }
}
