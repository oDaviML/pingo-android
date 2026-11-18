package com.pingo.feature.transactions.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.relatorio.AgregadorDeRelatorioMensal
import com.pingo.feature.transactions.api.TransactionsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val lancamentoRepository: LancamentoRepository,
    contaRepository: ContaRepository,
    instrumentoRepository: InstrumentoRepository,
    categoriaRepository: CategoriaRepository,
    private val clock: Clock,
) : ViewModel() {
    constructor(
        lancamentoRepository: LancamentoRepository,
        contaRepository: ContaRepository,
        instrumentoRepository: InstrumentoRepository,
        categoriaRepository: CategoriaRepository,
        clock: Clock,
    ) : this(
        savedStateHandle = SavedStateHandle(),
        lancamentoRepository = lancamentoRepository,
        contaRepository = contaRepository,
        instrumentoRepository = instrumentoRepository,
        categoriaRepository = categoriaRepository,
        clock = clock,
    )

    private val filtrosIniciais = filtrosDeDrillDown(savedStateHandle)
    private val zonaCalendario = if (filtrosIniciais != null) AgregadorDeRelatorioMensal.ZONA_SAO_PAULO else clock.zone
    private val controle = MutableStateFlow(ControleDaTela(filtros = filtrosIniciais ?: FiltrosDeLancamentos()))
    private val dados = MutableStateFlow(BaseDeDados())
    private val mutableUiState = MutableStateFlow(TransactionsUiState())

    val uiState: StateFlow<TransactionsUiState> = mutableUiState.asStateFlow()

    private var mesEscolhidoPeloUsuario = filtrosIniciais != null

    init {
        viewModelScope.launch {
            combine(
                lancamentoRepository.observeLancamentos(),
                contaRepository.observeContas(),
                instrumentoRepository.observeInstrumentos(),
                categoriaRepository.observeCategorias(),
            ) { lancamentos, contas, instrumentos, categorias ->
                BaseDeDados(
                    carregado = true,
                    lancamentos = lancamentos,
                    contas = contas,
                    instrumentos = instrumentos,
                    categorias = categorias,
                )
            }.collect(::aplicarBase)
        }
        viewModelScope.launch {
            controle.collect { recompor() }
        }
    }

    fun onEvent(evento: TransactionsEvent) {
        when (evento) {
            is TransactionsEvent.BuscaAlterada -> controle.update { it.copy(busca = evento.texto) }
            TransactionsEvent.AbrirFiltros -> controle.update { it.copy(filtrosVisiveis = true, troca = null) }
            TransactionsEvent.FecharFiltros -> controle.update { it.copy(filtrosVisiveis = false) }
            is TransactionsEvent.SelecionarMes,
            is TransactionsEvent.SelecionarTipo,
            is TransactionsEvent.SelecionarVinculo,
            is TransactionsEvent.SelecionarCategoria,
            is TransactionsEvent.SelecionarStatus,
            -> selecionarFiltro(evento)
            TransactionsEvent.LimparFiltros -> {
                mesEscolhidoPeloUsuario = true
                controle.update { it.copy(busca = "", filtros = FiltrosDeLancamentos()) }
            }
            is TransactionsEvent.Excluir -> excluir(evento.publicId)
            TransactionsEvent.DesfazerExclusao -> desfazerExclusao()
            TransactionsEvent.DispensarMensagem -> controle.update { it.copy(mensagem = null) }
            is TransactionsEvent.AbrirTrocaCategoria -> abrirTrocaDeCategoria(evento.publicId)
            TransactionsEvent.FecharTrocaCategoria -> controle.update { it.copy(troca = null) }
            is TransactionsEvent.TrocarCategoria -> trocarCategoria(evento.publicId, evento.categoriaPublicId)
        }
    }

    private fun selecionarFiltro(evento: TransactionsEvent) {
        when (evento) {
            is TransactionsEvent.SelecionarMes -> {
                mesEscolhidoPeloUsuario = true
                atualizarFiltros { filtros -> filtros.copy(mes = filtros.mes.alternar(evento.mes)) }
            }
            is TransactionsEvent.SelecionarTipo -> atualizarFiltros { filtros -> filtros.copy(tipo = filtros.tipo.alternar(evento.tipo)) }
            is TransactionsEvent.SelecionarVinculo -> atualizarFiltros { filtros ->
                filtros.copy(vinculo = filtros.vinculo.alternar(evento.vinculo))
            }
            is TransactionsEvent.SelecionarCategoria -> atualizarFiltros { filtros ->
                filtros.copy(categoriaPublicId = filtros.categoriaPublicId.alternar(evento.categoriaPublicId))
            }
            is TransactionsEvent.SelecionarStatus -> atualizarFiltros { filtros ->
                filtros.copy(status = filtros.status.alternar(evento.status))
            }
            else -> Unit
        }
    }

    private fun aplicarBase(base: BaseDeDados) {
        dados.value = base

        if (!mesEscolhidoPeloUsuario) {
            val mes = mesInicial(base.lancamentos)
            controle.update { it.copy(filtros = it.filtros.copy(mes = mes)) }
        }

        recompor()
    }

    private fun recompor() {
        val base = dados.value
        val atual = controle.value
        val contasPorId = base.contas.associateBy { it.publicId }
        val instrumentosPorId = base.instrumentos.associateBy { it.publicId }
        val categoriasPorId = base.categorias.associateBy { it.publicId }
        val filtrados = ListaDeLancamentos.filtrar(base.lancamentos, atual.busca, atual.filtros, instrumentosPorId, zonaCalendario)

        mutableUiState.value = TransactionsUiState(
            carregando = !base.carregado,
            busca = atual.busca,
            filtros = atual.filtros,
            filtrosVisiveis = atual.filtrosVisiveis,
            mesesDisponiveis = mesesDisponiveis(base.lancamentos),
            opcoesDeVinculo = opcoesDeVinculo(base.contas, base.instrumentos),
            opcoesDeCategoria = base.categorias.filter { it.podeSerEscolhida }.map { OpcaoDeCategoria(it.publicId, it.nome) },
            dias = ListaDeLancamentos.agrupar(
                lancamentos = filtrados,
                categoriasPorId = categoriasPorId,
                instrumentosPorId = instrumentosPorId,
                contasPorId = contasPorId,
                hoje = clock.instant().atZone(zonaCalendario).toLocalDate(),
                zona = zonaCalendario,
            ),
            quantidadeDeLancamentos = filtrados.size,
            filtrosAtivos = atual.filtros.ativos,
            estadoVazio = estadoVazio(base, filtrados),
            mensagemDeExclusao = atual.mensagem,
            trocaDeCategoria = atual.troca,
        )
    }

    private fun estadoVazio(base: BaseDeDados, filtrados: List<Lancamento>): EstadoVazioDosLancamentos? = when {
        !base.carregado -> null
        base.lancamentos.isEmpty() -> EstadoVazioDosLancamentos.SEM_LANCAMENTOS
        filtrados.isEmpty() -> EstadoVazioDosLancamentos.NADA_COM_OS_FILTROS
        else -> null
    }

    private fun mesesDisponiveis(lancamentos: List<Lancamento>): List<YearMonth> {
        val doBase = lancamentos.map { YearMonth.from(it.dataHora.atZone(zonaCalendario)) }
        val atual = YearMonth.from(clock.instant().atZone(zonaCalendario))
        return (doBase + atual).distinct().sortedDescending()
    }

    private fun opcoesDeVinculo(contas: List<Conta>, instrumentos: List<Instrumento>): List<OpcaoDeVinculo> {
        val instrumentosPorConta = instrumentos.filterNot { it.arquivado }.groupBy { it.contaPublicId }

        return contas.filterNot { it.arquivada }.flatMap { conta ->
            val daConta = instrumentosPorConta[conta.publicId].orEmpty().map { instrumento ->
                OpcaoDeVinculo(
                    filtro = VinculoFiltro.Instrumento(instrumento.publicId),
                    rotulo = rotuloDoVinculo(conta, instrumento),
                    chave = "instrumento-${instrumento.publicId}",
                )
            }
            listOf(OpcaoDeVinculo(VinculoFiltro.Conta(conta.publicId), conta.apelido, "conta-${conta.publicId}")) + daConta
        }
    }

    private fun rotuloDoVinculo(conta: Conta, instrumento: Instrumento): String {
        val tipo = when (instrumento.tipo) {
            TipoInstrumento.CREDITO -> "crédito"
            TipoInstrumento.DEBITO -> "débito"
            TipoInstrumento.VOUCHER -> instrumento.beneficio?.name ?: "voucher"
        }
        val digitos = instrumento.ultimosDigitos?.let { " $it" }.orEmpty()

        return "${conta.apelido} $tipo$digitos"
    }

    private fun abrirTrocaDeCategoria(publicId: String) {
        val base = dados.value
        val lancamento = base.lancamentos.firstOrNull { it.publicId == publicId } ?: return
        val opcoes = base.categorias
            .filter { it.podeSerEscolhida && compativel(it, lancamento.tipo) }
            .map { OpcaoDeCategoria(it.publicId, it.nome) }

        controle.update {
            it.copy(
                filtrosVisiveis = false,
                troca = TrocaDeCategoria(
                    lancamentoPublicId = publicId,
                    estabelecimento = lancamento.estabelecimento ?: "Lançamento",
                    categoriaAtualPublicId = lancamento.categoriaPublicId,
                    opcoes = opcoes,
                ),
            )
        }
    }

    private fun compativel(categoria: Categoria, tipo: TipoLancamento): Boolean = when (tipo) {
        TipoLancamento.DESPESA -> categoria.tipo == TipoCategoria.DESPESA
        TipoLancamento.RECEITA -> categoria.tipo == TipoCategoria.RECEITA
        TipoLancamento.TRANSFERENCIA, TipoLancamento.AJUSTE -> true
    }

    private fun excluir(publicId: String) {
        viewModelScope.launch {
            runCatching { lancamentoRepository.excluir(publicId, clock.instant()) }
                .onSuccess { controle.update { estado -> estado.copy(mensagem = MensagemDeExclusao(publicId)) } }
        }
    }

    private fun desfazerExclusao() {
        val mensagem = controle.value.mensagem ?: return
        controle.update { it.copy(mensagem = null) }

        viewModelScope.launch {
            val lancamento = lancamentoRepository.buscar(mensagem.publicId) ?: return@launch
            lancamentoRepository.salvar(lancamento.copy(excluidoEm = null, atualizadoEm = clock.instant()))
        }
    }

    private fun trocarCategoria(publicId: String, categoriaPublicId: String) {
        controle.update { it.copy(troca = null) }

        viewModelScope.launch {
            val lancamento = lancamentoRepository.buscar(publicId) ?: return@launch
            lancamentoRepository.salvar(lancamento.copy(categoriaPublicId = categoriaPublicId, atualizadoEm = clock.instant()))
        }
    }

    private fun atualizarFiltros(transformacao: (FiltrosDeLancamentos) -> FiltrosDeLancamentos) {
        controle.update { it.copy(filtros = transformacao(it.filtros)) }
    }

    private fun mesInicial(lancamentos: List<Lancamento>): YearMonth {
        val atual = YearMonth.from(clock.instant().atZone(zonaCalendario))

        if (lancamentos.any { YearMonth.from(it.dataHora.atZone(zonaCalendario)) == atual }) {
            return atual
        }

        return lancamentos.maxOfOrNull { YearMonth.from(it.dataHora.atZone(zonaCalendario)) } ?: atual
    }

    private data class ControleDaTela(
        val busca: String = "",
        val filtros: FiltrosDeLancamentos = FiltrosDeLancamentos(),
        val filtrosVisiveis: Boolean = false,
        val mensagem: MensagemDeExclusao? = null,
        val troca: TrocaDeCategoria? = null,
    )

    private data class BaseDeDados(
        val carregado: Boolean = false,
        val lancamentos: List<Lancamento> = emptyList(),
        val contas: List<Conta> = emptyList(),
        val instrumentos: List<Instrumento> = emptyList(),
        val categorias: List<Categoria> = emptyList(),
    )
}

private fun <T> T?.alternar(novo: T?): T? = if (novo != null && this == novo) null else novo

private fun filtrosDeDrillDown(savedStateHandle: SavedStateHandle): FiltrosDeLancamentos? {
    val categoriaId = savedStateHandle.get<String>(TransactionsRoute.ARG_CATEGORIA_ID)?.takeIf { it.isNotBlank() }
    val mes = savedStateHandle.get<String>(TransactionsRoute.ARG_MES)
        ?.let { texto -> runCatching { YearMonth.parse(texto) }.getOrNull() }
    if (categoriaId == null || mes == null) return null
    return FiltrosDeLancamentos(
        mes = mes,
        tipo = TipoLancamento.DESPESA,
        categoriaPublicId = categoriaId,
        status = StatusFiltro.CONFIRMADO,
    )
}
