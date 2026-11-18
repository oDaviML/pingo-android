package com.pingo.feature.accounts.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.fatura.CompletarDatasDoCartaoUseCase
import com.pingo.core.data.fatura.ResultadoCompletarDatasCartao
import com.pingo.core.data.projecao.ContasFinanceiroProjection
import com.pingo.core.data.voucher.AjustarSaldoVoucherUseCase
import com.pingo.core.data.voucher.MaterializarRecargasVencidasUseCase
import com.pingo.core.data.voucher.ResultadoAjusteVoucher
import com.pingo.core.data.voucher.ValorAjusteSaldoVoucher
import com.pingo.core.model.fatura.CalendarioDeFatura
import com.pingo.core.model.fatura.FaturasDoCartao
import com.pingo.core.model.organizacao.ContaComIndicadores
import com.pingo.core.model.organizacao.ResumoContasFinanceiro
import com.pingo.core.model.voucher.DetalheVoucher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel internal constructor(
    private val contasFinanceiroProjection: ContasFinanceiroProjection,
    private val completarDatas: (suspend (String, Int, Int) -> ResultadoCompletarDatasCartao)?,
    private val ajustarSaldo: (suspend (String, ValorAjusteSaldoVoucher, String?) -> ResultadoAjusteVoucher)?,
    private val materializarRecargas: (suspend () -> Int)?,
    private val clock: Clock,
) : ViewModel() {
    @Inject
    constructor(
        contasFinanceiroProjection: ContasFinanceiroProjection,
        completarDatasDoCartaoUseCase: CompletarDatasDoCartaoUseCase,
        ajustarSaldoVoucherUseCase: AjustarSaldoVoucherUseCase,
        materializarRecargasVencidasUseCase: MaterializarRecargasVencidasUseCase,
        clock: Clock,
    ) : this(
        contasFinanceiroProjection = contasFinanceiroProjection,
        completarDatas = { id, fechamento, vencimento -> completarDatasDoCartaoUseCase(id, fechamento, vencimento) },
        ajustarSaldo = { id, entrada, observacao -> ajustarSaldoVoucherUseCase(id, entrada, observacao) },
        materializarRecargas = { materializarRecargasVencidasUseCase() },
        clock = clock,
    )

    constructor(
        contaRepository: ContaRepository,
        instrumentoRepository: InstrumentoRepository,
    ) : this(
        contasFinanceiroProjection = ProjecaoFallbackDeContas(contaRepository, instrumentoRepository),
        completarDatas = null,
        ajustarSaldo = null,
        materializarRecargas = null,
        clock = Clock.systemDefaultZone(),
    )

    private val tentativa = MutableStateFlow(0)
    private val mutableUiState = MutableStateFlow(AccountsUiState(hoje = hojeAtual()))

    val uiState: StateFlow<AccountsUiState> = mutableUiState.asStateFlow()

    init {
        observar()
        materializarComSeguranca()
    }

    fun tentarDeNovo() {
        tentativa.update { it + 1 }
        materializarComSeguranca()
    }

    fun atualizar() {
        materializarComSeguranca()
    }

    fun onEvent(event: AccountsEvent) {
        when (event) {
            is AccountsEvent.AbrirCompletarDatas -> abrirCompletarDatas(event.instrumentoPublicId, event.tituloCartao)
            is AccountsEvent.AlterarFechamentoModal -> atualizarModalDatas { it.copy(fechamentoDia = event.valor, erro = null) }
            is AccountsEvent.AlterarVencimentoModal -> atualizarModalDatas { it.copy(vencimentoDia = event.valor, erro = null) }
            AccountsEvent.FecharModalCompletarDatas -> mutableUiState.update { it.copy(modalCompletarDatas = null) }
            AccountsEvent.SalvarDatasCartao -> salvarDatasCartao()
            is AccountsEvent.AbrirAjustarSaldo -> abrirAjustarSaldo(
                instrumentoPublicId = event.instrumentoPublicId,
                tituloVoucher = event.tituloVoucher,
                saldoAtualCentavos = event.saldoAtualCentavos,
            )
            is AccountsEvent.AlterarModoAjuste -> atualizarModalSaldo { it.copy(modo = event.modo, erro = null) }
            is AccountsEvent.AlterarValorAjuste -> atualizarModalSaldo { it.copy(valorTexto = event.valor, erro = null) }
            is AccountsEvent.AlterarObservacaoAjuste -> atualizarModalSaldo { it.copy(observacao = event.observacao, erro = null) }
            AccountsEvent.FecharModalAjustarSaldo -> mutableUiState.update { it.copy(modalAjustarSaldo = null) }
            AccountsEvent.SalvarAjusteVoucher -> salvarAjusteVoucher()
        }
    }

    private fun materializarComSeguranca() {
        val acao = materializarRecargas ?: return
        viewModelScope.launch {
            runCatching { acao() }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observar() {
        viewModelScope.launch {
            tentativa
                .flatMapLatest {
                    mutableUiState.update { it.copy(carregando = true, falha = false, hoje = hojeAtual()) }
                    contasFinanceiroProjection.observeContas()
                        .catch {
                            mutableUiState.update { it.copy(carregando = false, falha = true) }
                        }
                }
                .collect { resumo ->
                    mutableUiState.update {
                        it.copy(
                            carregando = false,
                            falha = false,
                            grupos = agruparContasFinanceiro(resumo.contasAtivas),
                            arquivadas = contasArquivadasFinanceiro(resumo.contasArquivadas),
                            hoje = hojeAtual(),
                        )
                    }
                }
        }
    }

    private fun abrirCompletarDatas(instrumentoPublicId: String, tituloCartao: String) {
        mutableUiState.update {
            it.copy(
                modalCompletarDatas = ModalCompletarDatasUiState(
                    instrumentoPublicId = instrumentoPublicId,
                    tituloCartao = tituloCartao,
                ),
            )
        }
    }

    private fun abrirAjustarSaldo(instrumentoPublicId: String, tituloVoucher: String, saldoAtualCentavos: Long) {
        mutableUiState.update {
            it.copy(
                modalAjustarSaldo = ModalAjustarSaldoUiState(
                    instrumentoPublicId = instrumentoPublicId,
                    tituloVoucher = tituloVoucher,
                    saldoAtualCentavos = saldoAtualCentavos,
                ),
            )
        }
    }

    private fun atualizarModalDatas(transformar: (ModalCompletarDatasUiState) -> ModalCompletarDatasUiState) {
        mutableUiState.update { estado ->
            estado.copy(modalCompletarDatas = estado.modalCompletarDatas?.let(transformar))
        }
    }

    private fun atualizarModalSaldo(transformar: (ModalAjustarSaldoUiState) -> ModalAjustarSaldoUiState) {
        mutableUiState.update { estado ->
            estado.copy(modalAjustarSaldo = estado.modalAjustarSaldo?.let(transformar))
        }
    }

    private fun salvarDatasCartao() {
        val modal = mutableUiState.value.modalCompletarDatas ?: return
        val fechamento = modal.fechamentoInt
        val vencimento = modal.vencimentoInt
        val executor = completarDatas
        val dadosProntos = modal.podeSalvar && fechamento != null && vencimento != null
        if (!dadosProntos || executor == null) {
            return
        }
        viewModelScope.launch {
            atualizarModalDatas { it.copy(salvando = true, erro = null) }
            val resultado = runCatching { executor(modal.instrumentoPublicId, fechamento!!, vencimento!!) }
            aplicarResultadoDatas(resultado)
        }
    }

    private fun aplicarResultadoDatas(resultado: Result<ResultadoCompletarDatasCartao>) {
        val valor = resultado.getOrNull()
        when (valor) {
            is ResultadoCompletarDatasCartao.Sucesso -> mutableUiState.update { it.copy(modalCompletarDatas = null) }
            ResultadoCompletarDatasCartao.DadosInvalidos ->
                atualizarModalDatas { it.copy(salvando = false, erro = "Informe dias válidos entre 1 e 31.") }
            ResultadoCompletarDatasCartao.NaoEncontrado ->
                atualizarModalDatas { it.copy(salvando = false, erro = "Cartão não encontrado.") }
            null ->
                atualizarModalDatas { it.copy(salvando = false, erro = "Não foi possível salvar as datas. Tente de novo.") }
        }
    }

    private fun salvarAjusteVoucher() {
        val modal = mutableUiState.value.modalAjustarSaldo ?: return
        val entrada = modal.entradaCalculada
        val executor = ajustarSaldo
        if (!modal.podeSalvar || entrada == null || executor == null) {
            return
        }
        viewModelScope.launch {
            atualizarModalSaldo { it.copy(salvando = true, erro = null) }
            val resultado = runCatching { executor(modal.instrumentoPublicId, entrada, modal.observacao) }
            aplicarResultadoAjuste(resultado)
        }
    }

    private fun aplicarResultadoAjuste(resultado: Result<ResultadoAjusteVoucher>) {
        when (resultado.getOrNull()) {
            is ResultadoAjusteVoucher.Aplicado -> mutableUiState.update { it.copy(modalAjustarSaldo = null) }
            ResultadoAjusteVoucher.ValorInvalido ->
                atualizarModalSaldo { it.copy(salvando = false, erro = "O saldo final não pode ficar negativo.") }
            ResultadoAjusteVoucher.NaoEncontrado ->
                atualizarModalSaldo { it.copy(salvando = false, erro = "Benefício não encontrado.") }
            null ->
                atualizarModalSaldo { it.copy(salvando = false, erro = "Não foi possível ajustar o saldo. Tente de novo.") }
        }
    }

    private fun hojeAtual(): LocalDate = ZonedDateTime.ofInstant(clock.instant(), CalendarioDeFatura.ZONA_SAO_PAULO).toLocalDate()
}

private class ProjecaoFallbackDeContas(
    private val contaRepository: ContaRepository,
    private val instrumentoRepository: InstrumentoRepository,
) : ContasFinanceiroProjection {
    override fun observeContas(): Flow<ResumoContasFinanceiro> = combine(
        contaRepository.observeContas(),
        instrumentoRepository.observeInstrumentos(),
    ) { contas, instrumentos ->
        val porConta = instrumentos.filter { it.excluidoEm == null }.groupBy { it.contaPublicId }
        val ativas = contas.filter { !it.arquivada && it.excluidoEm == null }.map { conta ->
            ContaComIndicadores(
                conta = conta,
                instrumentos = porConta[conta.publicId].orEmpty()
                    .filter { !it.arquivado }
                    .map { it.indicadorPadrao() },
            )
        }
        val arquivadas = contas.filter { it.arquivada && it.excluidoEm == null }.map { conta ->
            ContaComIndicadores(
                conta = conta,
                instrumentos = porConta[conta.publicId].orEmpty().map { it.indicadorPadrao() },
            )
        }
        ResumoContasFinanceiro(contasAtivas = ativas, contasArquivadas = arquivadas)
    }

    override fun observeFaturasDoCartao(instrumentoPublicId: String): Flow<FaturasDoCartao?> = flowOf(null)

    override fun observeDetalheVoucher(instrumentoPublicId: String): Flow<DetalheVoucher?> = flowOf(null)
}
