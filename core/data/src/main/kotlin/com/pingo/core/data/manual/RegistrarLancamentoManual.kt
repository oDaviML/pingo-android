package com.pingo.core.data.manual

import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.ConfiguracaoCartaoRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.data.assinatura.ConfirmadorDeAssinatura
import com.pingo.core.model.Categoria
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.assinatura.NormalizadorDeAssinatura
import com.pingo.core.model.fatura.GeradorDeParcelas
import com.pingo.core.model.fatura.ModoParcelamento
import com.pingo.core.model.fatura.ParcelaPlanejada
import com.pingo.core.model.fatura.VigenciaDeCartao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class RegistrarLancamentoManual @Inject constructor(
    private val lancamentoRepository: LancamentoRepository,
    private val instrumentoRepository: InstrumentoRepository,
    private val configuracaoCartaoRepository: ConfiguracaoCartaoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val confirmadorDeAssinatura: ConfirmadorDeAssinatura,
    private val transacaoLocal: TransacaoLocal,
    private val clock: Clock,
) {
    constructor(
        lancamentoRepository: LancamentoRepository,
        instrumentoRepository: InstrumentoRepository,
        configuracaoCartaoRepository: ConfiguracaoCartaoRepository,
        transacaoLocal: TransacaoLocal,
        clock: Clock,
    ) : this(
        lancamentoRepository = lancamentoRepository,
        instrumentoRepository = instrumentoRepository,
        configuracaoCartaoRepository = configuracaoCartaoRepository,
        categoriaRepository = SemCategorias,
        confirmadorDeAssinatura = ConfirmadorDeAssinatura.NoOp,
        transacaoLocal = transacaoLocal,
        clock = clock,
    )

    constructor(
        lancamentoRepository: LancamentoRepository,
        instrumentoRepository: InstrumentoRepository,
        transacaoLocal: TransacaoLocal,
        clock: Clock,
    ) : this(
        lancamentoRepository = lancamentoRepository,
        instrumentoRepository = instrumentoRepository,
        configuracaoCartaoRepository = SemConfiguracaoCartao,
        categoriaRepository = SemCategorias,
        confirmadorDeAssinatura = ConfirmadorDeAssinatura.NoOp,
        transacaoLocal = transacaoLocal,
        clock = clock,
    )
    suspend fun ultimoInstrumentoUsado(): String? {
        val ativos = instrumentosAtivos()
        if (ativos.isEmpty()) {
            return null
        }

        return lancamentoRepository.ultimoInstrumentoUsado(ativos.map { it.publicId })
            ?: ativos.singleOrNull()?.publicId
    }

    suspend fun registrar(novo: NovoLancamentoManual): ResultadoLancamentoManual = transacaoLocal.executar {
        val invalidos = validar(novo)
        if (invalidos.isNotEmpty()) {
            return@executar ResultadoLancamentoManual.Invalido(invalidos)
        }

        when (novo.tipo) {
            TipoLancamento.TRANSFERENCIA -> registrarTransferencia(novo)
            TipoLancamento.DESPESA, TipoLancamento.RECEITA -> registrarLancamento(novo)
            TipoLancamento.AJUSTE -> ResultadoLancamentoManual.Invalido(setOf(CampoDeLancamentoManual.TIPO))
        }
    }

    private suspend fun instrumentosAtivos(): List<Instrumento> = instrumentoRepository.observeInstrumentosAtivos().first()

    private suspend fun validar(novo: NovoLancamentoManual): Set<CampoDeLancamentoManual> = buildSet {
        if (novo.valorCentavos <= 0) {
            add(CampoDeLancamentoManual.VALOR)
        }
        if (novo.totalParcelas !in GeradorDeParcelas.MIN_PARCELAS..GeradorDeParcelas.MAX_PARCELAS) {
            add(CampoDeLancamentoManual.PARCELAS)
        }
        val instrumento = validarTipoEInstrumento(novo)
        validarParcelamento(novo, instrumento)
    }

    private suspend fun MutableSet<CampoDeLancamentoManual>.validarTipoEInstrumento(novo: NovoLancamentoManual): Instrumento? =
        when (novo.tipo) {
            TipoLancamento.AJUSTE -> {
                add(CampoDeLancamentoManual.TIPO)
                null
            }
            TipoLancamento.TRANSFERENCIA -> {
                adicionarErrosDeTransferencia(novo)
                null
            }
            TipoLancamento.DESPESA, TipoLancamento.RECEITA -> {
                val encontrado = novo.instrumentoPublicId?.let { instrumentoRepository.buscarAtivo(it) }
                if (encontrado == null) {
                    add(CampoDeLancamentoManual.INSTRUMENTO)
                }
                encontrado
            }
        }

    private fun MutableSet<CampoDeLancamentoManual>.validarParcelamento(novo: NovoLancamentoManual, instrumento: Instrumento?) {
        if (novo.totalParcelas <= 1) return
        val permite = novo.tipo == TipoLancamento.DESPESA && instrumento?.tipo == TipoInstrumento.CREDITO
        if (!permite) {
            add(CampoDeLancamentoManual.PARCELAS)
        } else if (
            novo.modoParcelamento == ModoParcelamento.VALOR_TOTAL &&
            novo.valorCentavos in 1 until novo.totalParcelas
        ) {
            add(CampoDeLancamentoManual.VALOR)
        }
    }

    private suspend fun MutableSet<CampoDeLancamentoManual>.adicionarErrosDeTransferencia(novo: NovoLancamentoManual) {
        val origem = novo.instrumentoPublicId
        val destino = novo.destinoPublicId

        if (!instrumentoAtivo(origem)) {
            add(CampoDeLancamentoManual.ORIGEM)
        }
        if (destino == null || destino == origem || !instrumentoAtivo(destino)) {
            add(CampoDeLancamentoManual.DESTINO)
        }
    }

    private suspend fun instrumentoAtivo(publicId: String?): Boolean =
        publicId != null && instrumentoRepository.buscarAtivo(publicId) != null

    private suspend fun registrarLancamento(novo: NovoLancamentoManual): ResultadoLancamentoManual {
        val instrumentoId = requireNotNull(novo.instrumentoPublicId)
        if (novo.totalParcelas <= 1) {
            val duplicata = temCapturadoSemelhante(instrumentoId, novo.valorCentavos, novo.dataHora)
            val base = montar(novo, instrumentoId, duplicata = duplicata, transferenciaId = null)
            val categoria = novo.categoriaPublicId?.let { categoriaRepository.buscar(it) }
            val lancamento = confirmadorDeAssinatura.prepararAoSalvarManual(base, categoria)
            lancamentoRepository.salvar(lancamento)

            return ResultadoLancamentoManual.Salvo(listOf(lancamento))
        }

        return registrarParcelado(novo, instrumentoId)
    }

    private suspend fun registrarParcelado(novo: NovoLancamentoManual, instrumentoId: String): ResultadoLancamentoManual {
        val instrumento = requireNotNull(instrumentoRepository.buscarAtivo(instrumentoId))
        val vigencias = configuracaoCartaoRepository.listarPorInstrumento(instrumentoId)
        val parcelas = GeradorDeParcelas.planejar(
            valorBaseCentavos = novo.valorCentavos,
            totalParcelas = novo.totalParcelas,
            modo = novo.modoParcelamento,
            dataHoraInicial = novo.dataHora,
            instrumento = instrumento,
            vigencias = vigencias,
        )
        val primeiraDuplicata = temCapturadoSemelhante(
            instrumentoPublicId = instrumentoId,
            valorCentavos = parcelas.first().valorCentavos,
            dataHora = novo.dataHora,
        )
        val paiPublicId = UUID.randomUUID().toString()
        val lancamentos = parcelas.map { parcela ->
            montarParcela(
                novo = novo,
                instrumentoPublicId = instrumentoId,
                parcela = parcela,
                paiPublicId = paiPublicId,
                duplicata = parcela.numero == 1 && primeiraDuplicata,
            )
        }
        lancamentoRepository.salvarTodos(lancamentos)

        return ResultadoLancamentoManual.Salvo(lancamentos)
    }

    private suspend fun registrarTransferencia(novo: NovoLancamentoManual): ResultadoLancamentoManual {
        val origem = requireNotNull(novo.instrumentoPublicId)
        val destino = requireNotNull(novo.destinoPublicId)
        val transferenciaId = UUID.randomUUID().toString()
        val saida = montar(
            novo,
            origem,
            duplicata = temCapturadoSemelhante(origem, novo.valorCentavos, novo.dataHora),
            transferenciaId = transferenciaId,
        )
        val entrada = saida
            .copy(
                publicId = UUID.randomUUID().toString(),
                instrumentoPublicId = destino,
                tipoSemTransferencia = TipoLancamento.RECEITA,
            )
            .comDuplicata(temCapturadoSemelhante(destino, novo.valorCentavos, novo.dataHora))

        lancamentoRepository.salvar(saida)
        lancamentoRepository.salvar(entrada)

        return ResultadoLancamentoManual.Salvo(listOf(saida, entrada))
    }

    private suspend fun temCapturadoSemelhante(instrumentoPublicId: String, valorCentavos: Long, dataHora: Instant): Boolean =
        lancamentoRepository.capturadosSemelhantes(
            instrumentoPublicId = instrumentoPublicId,
            valorCentavos = valorCentavos,
            inicio = dataHora.minus(JANELA_DE_DUPLICATA),
            fim = dataHora.plus(JANELA_DE_DUPLICATA),
        ).isNotEmpty()

    private fun montarParcela(
        novo: NovoLancamentoManual,
        instrumentoPublicId: String,
        parcela: ParcelaPlanejada,
        paiPublicId: String,
        duplicata: Boolean,
    ): Lancamento = montar(
        novo = novo.copy(valorCentavos = parcela.valorCentavos, dataHora = parcela.dataHora),
        instrumentoPublicId = instrumentoPublicId,
        duplicata = duplicata,
        transferenciaId = null,
    ).copy(
        publicId = if (parcela.numero == 1) paiPublicId else UUID.randomUUID().toString(),
        compraPaiPublicId = if (parcela.numero == 1) null else paiPublicId,
        parcelaNumero = parcela.numero,
        totalParcelas = parcela.totalParcelas,
        valorTotalCompraCentavos = parcela.valorTotalCompraCentavos,
    )

    private fun montar(novo: NovoLancamentoManual, instrumentoPublicId: String, duplicata: Boolean, transferenciaId: String?): Lancamento {
        val agora = clock.instant()
        val transferencia = novo.tipo == TipoLancamento.TRANSFERENCIA
        val estabelecimento = novo.estabelecimento?.trim()?.takeIf { it.isNotEmpty() }

        return Lancamento(
            publicId = UUID.randomUUID().toString(),
            instrumentoPublicId = instrumentoPublicId,
            categoriaPublicId = novo.categoriaPublicId.takeUnless { transferencia },
            transferenciaId = transferenciaId,
            valorCentavos = novo.valorCentavos,
            dataHora = novo.dataHora,
            tipo = novo.tipo,
            tipoSemTransferencia = TipoLancamento.DESPESA.takeIf { transferencia },
            estabelecimento = estabelecimento,
            status = statusDaDuplicata(duplicata),
            origem = OrigemLancamento.MANUAL,
            confianca = 1.0,
            motivoPendencia = motivoDaDuplicata(duplicata),
            criadoEm = agora,
            atualizadoEm = agora,
            estabelecimentoNormalizado = NormalizadorDeAssinatura.normalizar(estabelecimento)?.take(LIMITE_NORMALIZADO),
        )
    }

    private companion object {
        val JANELA_DE_DUPLICATA: Duration = Duration.ofHours(24)
        const val LIMITE_NORMALIZADO = 200
    }

    private fun Lancamento.comDuplicata(duplicata: Boolean): Lancamento = copy(
        status = statusDaDuplicata(duplicata),
        motivoPendencia = motivoDaDuplicata(duplicata),
    )

    private fun statusDaDuplicata(duplicata: Boolean): StatusLancamento =
        if (duplicata) StatusLancamento.PENDENTE else StatusLancamento.CONFIRMADO

    private fun motivoDaDuplicata(duplicata: Boolean): String? = if (duplicata) MotivoPendencia.POSSIVEL_DUPLICATA.name else null
}

private object SemConfiguracaoCartao : ConfiguracaoCartaoRepository {
    override fun observePorInstrumento(instrumentoPublicId: String): Flow<List<VigenciaDeCartao>> = emptyFlow()

    override suspend fun listarPorInstrumento(instrumentoPublicId: String): List<VigenciaDeCartao> = emptyList()

    override suspend fun listarAtivas(): List<VigenciaDeCartao> = emptyList()
}

private object SemCategorias : CategoriaRepository {
    override fun observeCategorias(): Flow<List<Categoria>> = emptyFlow()

    override fun observeEscolhiveis(): Flow<List<Categoria>> = emptyFlow()

    override suspend fun buscar(publicId: String): Categoria? = null

    override suspend fun criar(nome: String, tipo: TipoCategoria, agora: Instant): ResultadoCategoria = ResultadoCategoria.NomeVazio

    override suspend fun renomear(publicId: String, novoNome: String, quando: Instant): ResultadoCategoria =
        ResultadoCategoria.NaoEncontrada

    override suspend fun arquivar(publicId: String, quando: Instant) = Unit

    override suspend fun salvar(categoria: Categoria) = Unit

    override suspend fun salvarTodas(categorias: List<Categoria>) = Unit

    override suspend fun excluir(publicId: String, quando: Instant) = Unit
}
