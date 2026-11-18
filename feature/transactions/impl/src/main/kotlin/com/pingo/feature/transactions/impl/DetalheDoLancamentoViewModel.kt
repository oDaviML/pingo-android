package com.pingo.feature.transactions.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.detalhe.CampoDoDetalhe
import com.pingo.core.data.detalhe.EdicaoDoLancamento
import com.pingo.core.data.detalhe.EditarLancamento
import com.pingo.core.data.detalhe.MarcarTransferencia
import com.pingo.core.data.detalhe.ResultadoDaEdicao
import com.pingo.core.data.detalhe.ResultadoDaTransferencia
import com.pingo.core.model.Lancamento
import com.pingo.core.model.TipoLancamento
import com.pingo.feature.transactions.api.TransactionsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class DetalheDoLancamentoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    lancamentoRepository: LancamentoRepository,
    contaRepository: ContaRepository,
    instrumentoRepository: InstrumentoRepository,
    categoriaRepository: CategoriaRepository,
    private val editarLancamento: EditarLancamento,
    private val marcarTransferencia: MarcarTransferencia,
    private val clock: Clock,
) : ViewModel() {
    private val publicId: String = savedStateHandle.get<String>(TransactionsRoute.ARG_PUBLIC_ID).orEmpty()
    private val lancamentos = lancamentoRepository
    private val controle = MutableStateFlow(ControleDoDetalhe())
    private val dados = MutableStateFlow(BaseDoDetalhe())
    private val mutableUiState = MutableStateFlow(DetalheDoLancamentoUiState())

    val uiState: StateFlow<DetalheDoLancamentoUiState> = mutableUiState.asStateFlow()

    private var inicializado = false
    private var registroAtual: Lancamento? = null

    init {
        viewModelScope.launch {
            combine(
                lancamentoRepository.observeLancamentos(),
                contaRepository.observeContas(),
                instrumentoRepository.observeInstrumentos(),
                categoriaRepository.observeCategorias(),
            ) { lancamentosAtivos, contas, instrumentos, categorias ->
                BaseDoDetalhe(
                    carregado = true,
                    lancamentos = lancamentosAtivos,
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

    fun onEvent(evento: DetalheDoLancamentoEvent) {
        when {
            tratarCampo(evento) -> Unit
            tratarSelecao(evento) -> Unit
            else -> tratarAcao(evento)
        }
    }

    private fun tratarCampo(evento: DetalheDoLancamentoEvent): Boolean = when (evento) {
        DetalheDoLancamentoEvent.AlternarTeclado -> {
            alternarTeclado()
            true
        }

        is DetalheDoLancamentoEvent.DigitoPressionado -> {
            digitar(evento.digito)
            true
        }

        DetalheDoLancamentoEvent.ApagarPressionado -> {
            apagar()
            true
        }

        is DetalheDoLancamentoEvent.TipoSelecionado -> {
            selecionarTipo(evento.tipo)
            true
        }

        is DetalheDoLancamentoEvent.EstabelecimentoAlterado -> {
            controle.update { it.copy(estabelecimento = evento.texto, erros = emptySet()) }
            true
        }

        is DetalheDoLancamentoEvent.AplicarATodosAlterado -> {
            controle.update { it.copy(aplicarCategoriaATodos = evento.aplicar, erros = emptySet()) }
            true
        }

        else -> false
    }

    private fun tratarSelecao(evento: DetalheDoLancamentoEvent): Boolean = when (evento) {
        is DetalheDoLancamentoEvent.AbrirPainel -> {
            controle.update { it.copy(painel = evento.painel, mensagem = null) }
            true
        }

        DetalheDoLancamentoEvent.FecharPainel -> {
            controle.update { it.copy(painel = null) }
            true
        }

        is DetalheDoLancamentoEvent.SelecionarInstrumento -> {
            selecionarInstrumento(evento.publicId)
            true
        }

        is DetalheDoLancamentoEvent.SelecionarDestino -> {
            selecionarDestino(evento.publicId)
            true
        }

        is DetalheDoLancamentoEvent.SelecionarCategoria -> {
            selecionarCategoria(evento.publicId)
            true
        }

        DetalheDoLancamentoEvent.AbrirCalendario -> {
            controle.update { it.copy(calendarioVisivel = true) }
            true
        }

        DetalheDoLancamentoEvent.FecharCalendario -> {
            controle.update { it.copy(calendarioVisivel = false) }
            true
        }

        is DetalheDoLancamentoEvent.SelecionarData -> {
            selecionarData(evento.data)
            true
        }

        else -> false
    }

    private fun tratarAcao(evento: DetalheDoLancamentoEvent): Boolean = when (evento) {
        DetalheDoLancamentoEvent.Salvar -> {
            salvar()
            true
        }

        DetalheDoLancamentoEvent.SolicitarTransferencia -> {
            solicitarTransferencia()
            true
        }

        DetalheDoLancamentoEvent.DesfazerTransferencia -> {
            desfazerTransferencia()
            true
        }

        DetalheDoLancamentoEvent.DispensarMensagem -> {
            controle.update { it.copy(mensagem = null) }
            true
        }

        else -> tratarExclusao(evento)
    }

    private fun tratarExclusao(evento: DetalheDoLancamentoEvent): Boolean = when (evento) {
        DetalheDoLancamentoEvent.SolicitarExclusao -> {
            controle.update { it.copy(dialogoDeExclusao = true, painel = null, mensagem = null) }
            true
        }

        DetalheDoLancamentoEvent.CancelarExclusao -> {
            controle.update { it.copy(dialogoDeExclusao = false) }
            true
        }

        DetalheDoLancamentoEvent.ConfirmarExclusao -> {
            confirmarExclusao()
            true
        }

        DetalheDoLancamentoEvent.SolicitarExclusaoParcelas -> {
            controle.update { it.copy(dialogoDeExclusaoParcelas = true, painel = null, mensagem = null) }
            true
        }

        DetalheDoLancamentoEvent.CancelarExclusaoParcelas -> {
            controle.update { it.copy(dialogoDeExclusaoParcelas = false) }
            true
        }

        DetalheDoLancamentoEvent.ConfirmarExclusaoParcelas -> {
            confirmarExclusaoTodasParcelas()
            true
        }

        else -> false
    }

    private fun aplicarBase(base: BaseDoDetalhe) {
        dados.value = base
        val registro = base.lancamentos.firstOrNull { it.publicId == publicId }

        when {
            !inicializado && registro != null -> {
                inicializado = true
                registroAtual = registro
                controle.value = ControleDoDetalhe(
                    tipo = registro.tipo,
                    valorCentavos = registro.valorCentavos,
                    instrumentoPublicId = registro.instrumentoPublicId,
                    destinoPublicId = irmaoDe(registro, base.lancamentos)?.instrumentoPublicId,
                    categoriaPublicId = registro.categoriaPublicId,
                    dataHora = registro.dataHora,
                    estabelecimento = registro.estabelecimento.orEmpty(),
                )
            }

            !inicializado && base.carregado -> {
                inicializado = true
                controle.update { it.copy(naoEncontrado = true) }
            }

            registro != null -> registroAtual = registro

            !controle.value.excluido && !controle.value.excluindo && !controle.value.salvo ->
                controle.update { it.copy(naoEncontrado = true) }

            else -> Unit
        }

        recompor()
    }

    private fun recompor() {
        val base = dados.value
        val atual = controle.value
        val registro = registroAtual
        val opcoes = opcoesDoDetalhe(base, atual.instrumentoPublicId, atual.destinoPublicId, atual.categoriaPublicId, atual.tipo)

        mutableUiState.value = DetalheDoLancamentoUiState(
            carregando = !base.carregado,
            naoEncontrado = atual.naoEncontrado,
            ehTransferencia = registro?.transferenciaId != null || atual.tipo == TipoLancamento.TRANSFERENCIA,
            tipo = tipoDaTela(atual.tipo),
            valorCentavos = atual.valorCentavos,
            instrumento = opcoes.instrumento,
            destino = opcoes.destino,
            categoria = opcoes.categoria,
            dataHora = atual.dataHora,
            hoje = LocalDate.now(clock),
            estabelecimento = atual.estabelecimento,
            aplicarCategoriaATodos = atual.aplicarCategoriaATodos,
            parcelamento = parcelamentoDe(registro),
            instrumentos = opcoes.instrumentos,
            destinosDaTransferencia = opcoes.destinos,
            categorias = opcoes.categorias,
            origem = registro?.let(::origemDe),
            par = parDe(registro, base),
            painel = atual.painel,
            calendarioVisivel = atual.calendarioVisivel,
            tecladoVisivel = atual.tecladoVisivel,
            erros = atual.erros,
            salvando = atual.salvando,
            salvo = atual.salvo,
            dialogoDeExclusao = atual.dialogoDeExclusao,
            dialogoDeExclusaoParcelas = atual.dialogoDeExclusaoParcelas,
            excluindo = atual.excluindo,
            excluido = atual.excluido,
            ocupado = atual.ocupado,
            mensagem = atual.mensagem,
        )
    }

    private fun alternarTeclado() {
        if (!mutableUiState.value.podeAlternarValor) {
            return
        }

        controle.update { it.copy(tecladoVisivel = !it.tecladoVisivel) }
    }

    private fun digitar(digito: Int) {
        val atual = controle.value
        if (atual.salvando || atual.excluido || digito !in 0..9) {
            return
        }

        val valor = atual.valorCentavos * 10 + digito
        if (valor > VALOR_MAXIMO_CENTAVOS) {
            return
        }

        controle.update { it.copy(valorCentavos = valor, erros = emptySet()) }
    }

    private fun apagar() {
        if (controle.value.salvando || controle.value.excluido) {
            return
        }

        controle.update { it.copy(valorCentavos = it.valorCentavos / 10, erros = emptySet()) }
    }

    private fun selecionarTipo(tipo: TipoDoLancamentoManual) {
        val atual = controle.value
        if (atual.tipo == TipoLancamento.TRANSFERENCIA || tipo == TipoDoLancamentoManual.TRANSFERENCIA) {
            return
        }

        val novo = tipoDoLancamento(tipo)
        val categoria = atual.categoriaPublicId?.let { publicId -> dados.value.categorias.firstOrNull { it.publicId == publicId } }
        controle.update {
            it.copy(
                tipo = novo,
                categoriaPublicId = categoria?.takeIf { candidata -> compativel(candidata, novo) }?.publicId,
                aplicarCategoriaATodos = it.aplicarCategoriaATodos && categoria != null && compativel(categoria, novo),
                erros = emptySet(),
            )
        }
    }

    private fun selecionarInstrumento(publicId: String) {
        controle.update { it.copy(instrumentoPublicId = publicId, painel = null, erros = emptySet()) }
    }

    private fun selecionarDestino(publicId: String) {
        if (controle.value.painel == PainelDoDetalhe.MARCAR_TRANSFERENCIA) {
            marcar(publicId)
            return
        }

        controle.update { it.copy(destinoPublicId = publicId, painel = null, erros = emptySet()) }
    }

    private fun selecionarCategoria(publicId: String) {
        controle.update { it.copy(categoriaPublicId = publicId, painel = null, erros = emptySet()) }
    }

    private fun selecionarData(data: LocalDate) {
        val atual = controle.value
        val horario = atual.dataHora.atZone(clock.zone).toLocalTime()
        val dataHora = data.atTime(horario).atZone(clock.zone).toInstant()

        controle.update { it.copy(dataHora = dataHora, calendarioVisivel = false, erros = emptySet()) }
    }

    private fun solicitarTransferencia() {
        val state = mutableUiState.value
        when {
            state.instrumento == null -> controle.update { it.copy(mensagem = MensagemDoDetalhe.INSTRUMENTO_PARA_TRANSFERENCIA) }
            state.destinosDaTransferencia.isEmpty() -> controle.update { it.copy(mensagem = MensagemDoDetalhe.DESTINO_OBRIGATORIO) }
            else -> controle.update { it.copy(painel = PainelDoDetalhe.MARCAR_TRANSFERENCIA, mensagem = null) }
        }
    }

    private fun marcar(destinoPublicId: String) {
        if (controle.value.ocupado) {
            return
        }

        controle.update { it.copy(ocupado = true, painel = null, mensagem = null, erros = emptySet()) }
        viewModelScope.launch {
            when (val resultado = marcarTransferencia.marcar(publicId, destinoPublicId)) {
                is ResultadoDaTransferencia.Marcada -> ressemear(resultado.lancamento, resultado.par)
                is ResultadoDaTransferencia.Recusada -> controle.update { it.copy(mensagem = mensagemDaRecusa(resultado.motivo)) }
                ResultadoDaTransferencia.NaoEncontrado -> controle.update { it.copy(naoEncontrado = true) }
                is ResultadoDaTransferencia.Desfeita -> Unit
            }
            controle.update { it.copy(ocupado = false) }
        }
    }

    private fun desfazerTransferencia() {
        if (controle.value.ocupado || controle.value.excluido) {
            return
        }

        controle.update { it.copy(ocupado = true, mensagem = null, erros = emptySet()) }
        viewModelScope.launch {
            when (val resultado = marcarTransferencia.desfazer(publicId)) {
                is ResultadoDaTransferencia.Desfeita -> ressemearDosDesfeitos(resultado.lancamentos)
                is ResultadoDaTransferencia.Recusada -> controle.update { it.copy(mensagem = mensagemDaRecusa(resultado.motivo)) }
                ResultadoDaTransferencia.NaoEncontrado -> controle.update { it.copy(naoEncontrado = true) }
                is ResultadoDaTransferencia.Marcada -> Unit
            }
            controle.update { it.copy(ocupado = false) }
        }
    }

    private fun salvar() {
        val atual = controle.value
        val pronto = dados.value.carregado && !atual.salvando && !atual.salvo && !atual.excluido
        if (!pronto) {
            return
        }

        controle.update { it.copy(salvando = true, erros = emptySet(), mensagem = null) }
        viewModelScope.launch {
            when (val resultado = editarLancamento.editar(edicaoDe(atual))) {
                is ResultadoDaEdicao.Salvo -> {
                    registroAtual = resultado.lancamento
                    controle.update { it.copy(salvando = false, salvo = true, tecladoVisivel = false) }
                }
                is ResultadoDaEdicao.Invalido -> controle.update { it.copy(salvando = false, erros = resultado.campos) }
                ResultadoDaEdicao.NaoEncontrado -> controle.update { it.copy(salvando = false, naoEncontrado = true) }
            }
        }
    }

    private fun confirmarExclusao() {
        if (controle.value.excluindo) {
            return
        }

        controle.update { it.copy(excluindo = true, dialogoDeExclusao = false) }
        viewModelScope.launch {
            runCatching { lancamentos.excluir(publicId, clock.instant()) }
                .onSuccess { controle.update { estado -> estado.copy(excluindo = false, excluido = true) } }
                .onFailure { controle.update { estado -> estado.copy(excluindo = false, mensagem = MensagemDoDetalhe.FALHA_AO_SALVAR) } }
        }
    }

    private fun confirmarExclusaoTodasParcelas() {
        val registro = registroAtual ?: return
        if (controle.value.excluindo) {
            return
        }

        val raizPublicId = registro.compraPaiPublicId ?: registro.publicId
        controle.update { it.copy(excluindo = true, dialogoDeExclusaoParcelas = false) }
        viewModelScope.launch {
            runCatching {
                val parcelas = lancamentos.listarParcelasDaCompra(raizPublicId)
                val ids = (parcelas.map { it.publicId } + publicId).distinct()
                lancamentos.excluirTodos(ids, clock.instant())
            }
                .onSuccess { controle.update { estado -> estado.copy(excluindo = false, excluido = true) } }
                .onFailure { controle.update { estado -> estado.copy(excluindo = false, mensagem = MensagemDoDetalhe.FALHA_AO_SALVAR) } }
        }
    }

    private fun ressemear(registro: Lancamento, par: Lancamento?) {
        registroAtual = registro
        dados.value = dados.value.copy(lancamentos = atualizarLancamentosNaBase(dados.value.lancamentos, registro, par))
        controle.update {
            it.copy(
                tipo = registro.tipo,
                valorCentavos = registro.valorCentavos,
                instrumentoPublicId = registro.instrumentoPublicId,
                destinoPublicId = par?.instrumentoPublicId,
                categoriaPublicId = registro.categoriaPublicId,
                dataHora = registro.dataHora,
                estabelecimento = registro.estabelecimento.orEmpty(),
                aplicarCategoriaATodos = false,
                painel = null,
                erros = emptySet(),
            )
        }
    }

    private fun ressemearDosDesfeitos(lancamentosDesfeitos: List<Lancamento>) {
        val registro = lancamentosDesfeitos.firstOrNull { it.publicId == publicId } ?: return
        ressemear(registro, null)
    }

    private fun edicaoDe(atual: ControleDoDetalhe): EdicaoDoLancamento {
        val transferencia = atual.tipo == TipoLancamento.TRANSFERENCIA

        return EdicaoDoLancamento(
            publicId = publicId,
            tipo = atual.tipo,
            valorCentavos = atual.valorCentavos,
            instrumentoPublicId = atual.instrumentoPublicId,
            destinoPublicId = atual.destinoPublicId.takeIf { transferencia },
            categoriaPublicId = atual.categoriaPublicId.takeUnless { transferencia },
            dataHora = atual.dataHora,
            estabelecimento = atual.estabelecimento,
            aplicarCategoriaATodos = atual.aplicarCategoriaATodos && !transferencia,
        )
    }

    private data class ControleDoDetalhe(
        val tipo: TipoLancamento = TipoLancamento.DESPESA,
        val valorCentavos: Long = 0,
        val instrumentoPublicId: String? = null,
        val destinoPublicId: String? = null,
        val categoriaPublicId: String? = null,
        val dataHora: Instant = Instant.EPOCH,
        val estabelecimento: String = "",
        val aplicarCategoriaATodos: Boolean = false,
        val painel: PainelDoDetalhe? = null,
        val calendarioVisivel: Boolean = false,
        val tecladoVisivel: Boolean = false,
        val erros: Set<CampoDoDetalhe> = emptySet(),
        val salvando: Boolean = false,
        val salvo: Boolean = false,
        val dialogoDeExclusao: Boolean = false,
        val dialogoDeExclusaoParcelas: Boolean = false,
        val excluindo: Boolean = false,
        val excluido: Boolean = false,
        val ocupado: Boolean = false,
        val naoEncontrado: Boolean = false,
        val mensagem: MensagemDoDetalhe? = null,
    )

    private companion object {
        const val VALOR_MAXIMO_CENTAVOS = 999_999_999_999L
    }
}

private fun parcelamentoDe(registro: Lancamento?): ParcelamentoDetalheUi? = registro?.takeIf { it.totalParcelas > 1 }?.let { item ->
    ParcelamentoDetalheUi(
        parcelaNumero = item.parcelaNumero,
        totalParcelas = item.totalParcelas,
        valorTotalCompraCentavos = item.valorTotalCompraCentavos,
    )
}

private fun atualizarLancamentosNaBase(atuais: List<Lancamento>, registro: Lancamento, par: Lancamento?): List<Lancamento> {
    val novos = listOfNotNull(registro, par).associateBy { it.publicId }

    return atuais.map { novos[it.publicId] ?: it } + novos.filterKeys { chave -> atuais.none { it.publicId == chave } }.values
}
