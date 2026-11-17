package com.pingo.feature.transactions.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.RegraCategoriaRepository
import com.pingo.core.data.manual.CampoDeLancamentoManual
import com.pingo.core.data.manual.NovoLancamentoManual
import com.pingo.core.data.manual.RegistrarLancamentoManual
import com.pingo.core.data.manual.ResultadoLancamentoManual
import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.EstabelecimentoNormalizado
import com.pingo.core.model.Instrumento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

@HiltViewModel
class NovoLancamentoViewModel @Inject constructor(
    contaRepository: ContaRepository,
    instrumentoRepository: InstrumentoRepository,
    categoriaRepository: CategoriaRepository,
    regraCategoriaRepository: RegraCategoriaRepository,
    private val registrarLancamentoManual: RegistrarLancamentoManual,
    private val clock: Clock,
) : ViewModel() {
    private val consultas = ConsultasDeNovoLancamento(categoriaRepository, regraCategoriaRepository)
    private val controle = MutableStateFlow(ControleDoNovoLancamento())
    private val dados = MutableStateFlow(BaseDoNovoLancamento())
    private val mutableUiState = MutableStateFlow(NovoLancamentoUiState())

    val uiState: StateFlow<NovoLancamentoUiState> = mutableUiState.asStateFlow()

    private var inicializado = false
    private var sugestaoEmAndamento: Job? = null

    init {
        viewModelScope.launch {
            combine(
                contaRepository.observeContasAtivas(),
                instrumentoRepository.observeInstrumentosAtivos(),
                categoriaRepository.observeEscolhiveis(),
            ) { contas, instrumentos, categorias ->
                BaseDoNovoLancamento(carregado = true, contas = contas, instrumentos = instrumentos, categorias = categorias)
            }.collect(::aplicarBase)
        }
        viewModelScope.launch {
            controle.collect { recompor() }
        }
    }

    fun onEvent(evento: NovoLancamentoEvent) {
        when (evento) {
            is NovoLancamentoEvent.TipoSelecionado -> selecionarTipo(evento.tipo)
            is NovoLancamentoEvent.DigitoPressionado -> digitar(evento.digito)
            NovoLancamentoEvent.ApagarPressionado -> apagar()
            is NovoLancamentoEvent.EstabelecimentoAlterado -> alterarEstabelecimento(evento.texto)
            is NovoLancamentoEvent.AbrirPainel -> controle.update { it.copy(painel = evento.painel, erros = emptySet()) }
            NovoLancamentoEvent.FecharPainel -> controle.update { it.copy(painel = null) }
            is NovoLancamentoEvent.SelecionarInstrumento -> selecionarInstrumento(evento.publicId)
            is NovoLancamentoEvent.SelecionarCategoria -> selecionarCategoria(evento.publicId)
            NovoLancamentoEvent.AbrirCalendario -> controle.update { it.copy(calendarioVisivel = true) }
            NovoLancamentoEvent.FecharCalendario -> controle.update { it.copy(calendarioVisivel = false) }
            is NovoLancamentoEvent.SelecionarData -> selecionarData(evento.data)
            NovoLancamentoEvent.Salvar -> salvar()
        }
    }

    private suspend fun aplicarBase(base: BaseDoNovoLancamento) {
        dados.value = base

        if (!inicializado) {
            inicializado = true
            val instrumento = registrarLancamentoManual.ultimoInstrumentoUsado()
            controle.update { it.copy(data = LocalDate.now(clock), instrumentoPublicId = instrumento) }
        }

        recompor()
    }

    private fun recompor() {
        val base = dados.value
        val atual = controle.value
        val instrumentos = base.instrumentosAtivos().map { instrumento ->
            OpcaoDeInstrumento(instrumento.publicId, rotuloDoInstrumentoDoLancamento(base.contas, instrumento))
        }
        val categorias = opcoesDeCategoria(base.categorias, atual.tipo)
        val categoria = categorias.firstOrNull { it.publicId == atual.categoriaPublicId }

        mutableUiState.value = NovoLancamentoUiState(
            carregando = !base.carregado,
            tipo = atual.tipo,
            valorCentavos = atual.valorCentavos,
            instrumento = instrumentos.firstOrNull { it.publicId == atual.instrumentoPublicId },
            destino = instrumentos.firstOrNull { it.publicId == atual.destinoPublicId },
            categoria = categoria,
            categoriaSugerida = atual.categoriaSugerida && categoria != null,
            data = atual.data,
            hoje = LocalDate.now(clock),
            estabelecimento = atual.estabelecimento,
            instrumentos = instrumentos,
            categorias = categorias,
            painel = atual.painel,
            calendarioVisivel = atual.calendarioVisivel,
            erros = atual.erros,
            salvando = atual.salvando,
            salvo = atual.salvo,
        )
    }

    private fun selecionarTipo(tipo: TipoDoLancamentoManual) {
        if (controle.value.tipo == tipo) {
            return
        }

        controle.update {
            it.copy(
                tipo = tipo,
                categoriaPublicId = null,
                categoriaSugerida = false,
                categoriaEscolhidaManualmente = false,
                painel = null,
                erros = emptySet(),
            )
        }
        atualizarSugestao()
    }

    private fun digitar(digito: Int) {
        val atual = controle.value
        if (atual.salvando || atual.salvo || digito !in 0..9) {
            return
        }

        val valor = atual.valorCentavos * 10 + digito
        if (valor > VALOR_MAXIMO_CENTAVOS) {
            return
        }

        controle.update { it.copy(valorCentavos = valor, erros = emptySet()) }
    }

    private fun apagar() {
        if (controle.value.salvando || controle.value.salvo) {
            return
        }

        controle.update { it.copy(valorCentavos = it.valorCentavos / 10, erros = emptySet()) }
    }

    private fun alterarEstabelecimento(texto: String) {
        if (controle.value.salvando || controle.value.salvo) {
            return
        }

        controle.update { it.copy(estabelecimento = texto, erros = emptySet()) }
        atualizarSugestao()
    }

    private fun atualizarSugestao() {
        sugestaoEmAndamento?.cancel()
        val atual = controle.value
        val tipoCategoria = tipoDaCategoria(atual.tipo)

        if (tipoCategoria == null || atual.categoriaEscolhidaManualmente || atual.estabelecimento.isBlank()) {
            limparSugestao()
            return
        }

        val estabelecimento = atual.estabelecimento
        sugestaoEmAndamento = viewModelScope.launch {
            val categoria = consultas.sugerir(estabelecimento, tipoCategoria)
            val depois = controle.value
            if (depois.estabelecimento != estabelecimento || depois.tipo != atual.tipo || depois.categoriaEscolhidaManualmente) {
                return@launch
            }

            if (categoria != null) {
                controle.update { it.copy(categoriaPublicId = categoria.publicId, categoriaSugerida = true) }
            } else {
                limparSugestao()
            }
        }
    }

    private fun limparSugestao() {
        controle.update { estado ->
            if (estado.categoriaSugerida) {
                estado.copy(categoriaPublicId = null, categoriaSugerida = false)
            } else {
                estado.copy(categoriaSugerida = false)
            }
        }
    }

    private fun selecionarInstrumento(publicId: String) {
        if (controle.value.painel == PainelDoNovoLancamento.DESTINO) {
            controle.update { it.copy(destinoPublicId = publicId, painel = null, erros = emptySet()) }
        } else {
            controle.update { it.copy(instrumentoPublicId = publicId, painel = null, erros = emptySet()) }
        }
    }

    private fun selecionarCategoria(publicId: String) {
        sugestaoEmAndamento?.cancel()
        controle.update {
            it.copy(
                categoriaPublicId = publicId,
                categoriaSugerida = false,
                categoriaEscolhidaManualmente = true,
                painel = null,
                erros = emptySet(),
            )
        }
    }

    private fun selecionarData(data: LocalDate) {
        controle.update { it.copy(data = data, calendarioVisivel = false, erros = emptySet()) }
    }

    private fun salvar() {
        val atual = controle.value
        if (atual.salvando || atual.salvo || !dados.value.carregado) {
            return
        }

        controle.update { it.copy(salvando = true, erros = emptySet()) }
        viewModelScope.launch {
            when (val resultado = registrarLancamentoManual.registrar(entradaDe(atual))) {
                is ResultadoLancamentoManual.Invalido -> controle.update { it.copy(salvando = false, erros = resultado.campos) }
                is ResultadoLancamentoManual.Salvo -> controle.update { it.copy(salvando = false, salvo = true) }
            }
        }
    }

    private fun entradaDe(atual: ControleDoNovoLancamento): NovoLancamentoManual = NovoLancamentoManual(
        tipo = tipoLancamento(atual.tipo),
        valorCentavos = atual.valorCentavos,
        instrumentoPublicId = atual.instrumentoPublicId,
        destinoPublicId = atual.destinoPublicId,
        categoriaPublicId = atual.categoriaPublicId,
        dataHora = dataHoraDe(atual.data),
        estabelecimento = atual.estabelecimento,
    )

    private fun dataHoraDe(data: LocalDate): Instant {
        val agora = clock.instant().atZone(clock.zone)

        return data.atTime(agora.toLocalTime()).atZone(clock.zone).toInstant()
    }

    private fun opcoesDeCategoria(categorias: List<Categoria>, tipo: TipoDoLancamentoManual): List<OpcaoDeCategoria> {
        val desejado = tipoDaCategoria(tipo) ?: return emptyList()

        return categorias.filter { it.tipo == desejado }.map { OpcaoDeCategoria(it.publicId, it.nome) }
    }

    private fun tipoDaCategoria(tipo: TipoDoLancamentoManual): TipoCategoria? = when (tipo) {
        TipoDoLancamentoManual.DESPESA -> TipoCategoria.DESPESA
        TipoDoLancamentoManual.RECEITA -> TipoCategoria.RECEITA
        TipoDoLancamentoManual.TRANSFERENCIA -> null
    }

    private fun tipoLancamento(tipo: TipoDoLancamentoManual): TipoLancamento = when (tipo) {
        TipoDoLancamentoManual.DESPESA -> TipoLancamento.DESPESA
        TipoDoLancamentoManual.RECEITA -> TipoLancamento.RECEITA
        TipoDoLancamentoManual.TRANSFERENCIA -> TipoLancamento.TRANSFERENCIA
    }

    private data class ControleDoNovoLancamento(
        val tipo: TipoDoLancamentoManual = TipoDoLancamentoManual.DESPESA,
        val valorCentavos: Long = 0,
        val instrumentoPublicId: String? = null,
        val destinoPublicId: String? = null,
        val categoriaPublicId: String? = null,
        val categoriaSugerida: Boolean = false,
        val categoriaEscolhidaManualmente: Boolean = false,
        val data: LocalDate = LocalDate.MIN,
        val estabelecimento: String = "",
        val painel: PainelDoNovoLancamento? = null,
        val calendarioVisivel: Boolean = false,
        val erros: Set<CampoDeLancamentoManual> = emptySet(),
        val salvando: Boolean = false,
        val salvo: Boolean = false,
    )

    private data class BaseDoNovoLancamento(
        val carregado: Boolean = false,
        val contas: List<Conta> = emptyList(),
        val instrumentos: List<Instrumento> = emptyList(),
        val categorias: List<Categoria> = emptyList(),
    ) {
        fun instrumentosAtivos(): List<Instrumento> {
            val contasAtivas = contas.filter { conta -> conta.excluidoEm == null && !conta.arquivada }.map { it.publicId }.toSet()

            return instrumentos.filter { instrumento ->
                instrumento.excluidoEm == null && !instrumento.arquivado && instrumento.contaPublicId in contasAtivas
            }
        }
    }

    private companion object {
        const val VALOR_MAXIMO_CENTAVOS = 999_999_999_999L
    }
}

internal fun rotuloDoInstrumentoDoLancamento(contas: List<Conta>, instrumento: Instrumento): String {
    val conta = contas.firstOrNull { it.publicId == instrumento.contaPublicId }
    val tipo = when (instrumento.tipo) {
        TipoInstrumento.CREDITO -> "crédito"
        TipoInstrumento.DEBITO -> "débito"
        TipoInstrumento.VOUCHER -> instrumento.beneficio?.name ?: "voucher"
    }
    val digitos = instrumento.ultimosDigitos?.let { " $it" }.orEmpty()

    return "${conta?.apelido ?: instrumento.apelido} · $tipo$digitos"
}

internal class ConsultasDeNovoLancamento(
    private val categoriaRepository: CategoriaRepository,
    private val regraCategoriaRepository: RegraCategoriaRepository,
) {
    suspend fun sugerir(estabelecimentoBruto: String, tipo: TipoCategoria): Categoria? {
        val estabelecimento = EstabelecimentoNormalizado.de(estabelecimentoBruto) ?: return null
        val regra = regraCategoriaRepository.buscarPorEstabelecimento(estabelecimento) ?: return null

        return categoriaRepository.buscar(regra.categoriaPublicId)?.takeIf { it.podeSerEscolhida && it.tipo == tipo }
    }
}
