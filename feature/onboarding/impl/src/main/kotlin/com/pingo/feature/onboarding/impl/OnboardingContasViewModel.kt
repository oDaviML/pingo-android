package com.pingo.feature.onboarding.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.data.CadastroSalvo
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.InstrumentoRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class OnboardingContasViewModel @Inject constructor(
    private val contaRepository: ContaRepository,
    private val instrumentoRepository: InstrumentoRepository,
    private val preferences: UserPreferencesDataSource,
    private val clock: Clock,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(OnboardingContasUiState(recuperando = true))

    val uiState: StateFlow<OnboardingContasUiState> = mutableUiState.asStateFlow()

    private var contasPendentes: List<String> = emptyList()

    init {
        recuperarContasPendentes()
    }

    fun onEvent(event: OnboardingContasEvent) {
        if (!mutableUiState.value.formularioEditavel) {
            return
        }

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

        val formularios = state.contas
        concluirPasso { salvarContas(formularios) }
    }

    private fun pular() {
        concluirPasso(::concluirPreferencias)
    }

    private suspend fun salvarContas(formularios: List<ContaFormulario>) {
        val agora = clock.instant()
        val existentes = formularios.filter(ContaFormulario::editando)
        val novas = formularios.filterNot(ContaFormulario::editando)

        existentes.forEach { contaRepository.salvarCadastro(it.paraEdicao(), agora) }
        val idsCriados = criarContasNovas(novas = novas, agora = agora)
        arquivarContasRemovidas(mantidas = existentes.mapNotNull(ContaFormulario::publicId) + idsCriados, agora = agora)

        concluirPreferencias()
    }

    private suspend fun criarContasNovas(novas: List<ContaFormulario>, agora: Instant): List<String> {
        if (novas.isEmpty()) {
            return emptyList()
        }

        val publicIds = novas.map { UUID.randomUUID().toString() }
        preferences.setContasOnboardingPendentes(contasPendentes + publicIds)

        val criadas = contaRepository.criarComInstrumentos(
            contas = novas.map(ContaFormulario::paraNovaConta),
            agora = agora,
            publicIds = publicIds,
        )

        contasPendentes = contasPendentes + criadas.map { it.conta.publicId }
        atualizarContasCriadas(novas = novas, criadas = criadas)

        return criadas.map { it.conta.publicId }
    }

    private fun atualizarContasCriadas(novas: List<ContaFormulario>, criadas: List<CadastroSalvo>) {
        val substituicoes = novas.zip(criadas).associate { (formulario, criada) ->
            formulario.instituicao to ContaFormulario.de(criada.conta, criada.instrumentos)
        }

        mutableUiState.update { state ->
            state.copy(contas = state.contas.map { substituicoes[it.instituicao] ?: it })
        }
    }

    private suspend fun arquivarContasRemovidas(mantidas: List<String>, agora: Instant) {
        val mantidasSet = mantidas.toSet()

        contasPendentes.filterNot { it in mantidasSet }.forEach { publicId ->
            contaRepository.arquivar(publicId, agora)
        }
    }

    private suspend fun concluirPreferencias() {
        preferences.setContasConfiguradas(true)
        preferences.setContasOnboardingPendentes(emptyList())
        contasPendentes = emptyList()
    }

    private fun concluirPasso(salvar: suspend () -> Unit) {
        mutableUiState.update { it.copy(salvamento = SalvamentoUiState.Loading) }

        viewModelScope.launch {
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

    private fun recuperarContasPendentes() {
        viewModelScope.launch {
            try {
                recuperarContasPendentesDeFalha()
            } finally {
                mutableUiState.update { it.copy(recuperando = false) }
            }
        }
    }

    private suspend fun recuperarContasPendentesDeFalha() {
        val ids = runCatching { preferences.contasOnboardingPendentes() }.getOrDefault(emptyList())
        if (ids.isEmpty()) {
            return
        }

        if (runCatching { preferences.contasConfiguradas() }.getOrDefault(false)) {
            runCatching { preferences.setContasOnboardingPendentes(emptyList()) }
            return
        }

        contasPendentes = ids
        val formularios = ids.mapNotNull { publicId -> recuperarFormulario(publicId) }

        if (formularios.isNotEmpty()) {
            mutableUiState.update { state ->
                if (state.contas.isEmpty()) state.copy(contas = formularios) else state
            }
        }
    }

    private suspend fun recuperarFormulario(publicId: String): ContaFormulario? = runCatching {
        val conta = contaRepository.buscar(publicId)
        val instrumentos = instrumentoRepository.observePorConta(publicId).first()

        conta?.let { ContaFormulario.de(it, instrumentos) }
    }.getOrNull()
}
