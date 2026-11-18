package com.pingo.core.data.impl

import com.pingo.core.analytics.ResultadoTelemetriaCaptura
import com.pingo.core.analytics.TelemetriaDeCaptura
import com.pingo.core.common.AppDispatchers
import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.data.captura.CapturaParaProcessar
import com.pingo.core.data.captura.ConfiancaDaLeitura
import com.pingo.core.data.captura.ProcessadorDeCaptura
import com.pingo.core.data.captura.ResolucaoDeInstrumento
import com.pingo.core.data.captura.ResultadoProcessamento
import com.pingo.core.data.categoria.CategoriaAprendida
import com.pingo.core.data.reconciliacao.ReconciliadorVoucherCaptura
import com.pingo.core.data.reconciliacao.ResolucaoCapturaCaju
import com.pingo.core.model.Categoria
import com.pingo.core.model.EstabelecimentoNormalizado
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.assinatura.NormalizadorDeAssinatura
import com.pingo.core.model.direcaoDaTransferencia
import com.pingo.core.model.voucher.BeneficioCajuMapper
import com.pingo.core.parser.DataHoraDoLancamento
import com.pingo.core.parser.DirecaoPadrao
import com.pingo.core.parser.HashDuplicata
import com.pingo.core.parser.TipoLancamentoPadrao
import com.pingo.core.parser.TransacaoExtraida
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessadorDeCapturaImpl @Inject constructor(
    private val lancamentoRepository: LancamentoRepository,
    private val instrumentoRepository: InstrumentoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val categoriaAprendida: CategoriaAprendida,
    private val contexto: ContextoDeCaptura,
) : ProcessadorDeCaptura {
    constructor(
        lancamentoRepository: LancamentoRepository,
        instrumentoRepository: InstrumentoRepository,
        categoriaRepository: CategoriaRepository,
        categoriaAprendida: CategoriaAprendida,
        transacaoLocal: TransacaoLocal,
        clock: Clock,
        dispatchers: AppDispatchers,
    ) : this(
        lancamentoRepository = lancamentoRepository,
        instrumentoRepository = instrumentoRepository,
        categoriaRepository = categoriaRepository,
        categoriaAprendida = categoriaAprendida,
        contexto = ContextoDeCaptura(transacaoLocal, clock, dispatchers, ReconciliadorVoucherCaptura.NoOp),
    )

    private val transacaoLocal = contexto.transacaoLocal
    private val clock = contexto.clock
    private val dispatchers = contexto.dispatchers
    private val reconciliadorVoucher = contexto.reconciliadorVoucher
    private val mutex = Mutex()

    override suspend fun processar(captura: CapturaParaProcessar): ResultadoProcessamento = withContext(dispatchers.io) {
        mutex.withLock { processarEmSerie(captura) }
    }

    private suspend fun processarEmSerie(captura: CapturaParaProcessar): ResultadoProcessamento {
        val estabelecimentoNormalizado = EstabelecimentoNormalizado.de(captura.transacao.estabelecimento)
        val hash = HashDuplicata.de(
            captura.instituicao,
            captura.transacao.valorCentavos,
            estabelecimentoNormalizado,
            captura.postTime,
        )

        if (lancamentoRepository.buscarPorHash(hash) != null) {
            return ResultadoProcessamento.Duplicada
        }

        val dataHora = DataHoraDoLancamento.resolver(captura.transacao.dataHoraTexto, captura.postTime, clock.zone)

        return transacaoLocal.executar {
            gravar(captura, hash, dataHora)
        }
    }

    private suspend fun instrumentosElegiveis(instituicao: InstituicaoFinanceira): List<Instrumento> =
        if (instituicao == InstituicaoFinanceira.OUTRA) {
            emptyList()
        } else {
            instrumentoRepository.ativosDaInstituicao(instituicao)
        }

    private suspend fun gravar(captura: CapturaParaProcessar, hash: String, dataHora: Instant): ResultadoProcessamento {
        if (lancamentoRepository.buscarPorHash(hash) != null) {
            return ResultadoProcessamento.Duplicada
        }
        val transacao = captura.transacao
        val depositoCaju = captura.instituicao == InstituicaoFinanceira.CAJU && transacao.tipo == TipoLancamentoPadrao.DEPOSITO
        val resolucaoCaju = resolverSeCaju(captura, depositoCaju, dataHora)
        val resolucaoPadrao = if (resolucaoCaju == null) {
            ResolucaoDeInstrumento.de(transacao.digitos, instrumentosElegiveis(captura.instituicao))
        } else {
            null
        }
        val instrumentoResolvido = resolucaoCaju?.instrumento ?: resolucaoPadrao?.instrumento
        val motivoInstrumento = if (resolucaoCaju != null) resolucaoCaju.motivo else resolucaoPadrao?.motivo
        val agora = clock.instant()
        val candidatos = instrumentoResolvido?.let { candidatosDeTransferencia(captura, it, dataHora) }.orEmpty()
        val lancamento = criarLancamento(
            captura,
            resolucaoCaju,
            depositoCaju,
            instrumentoResolvido,
            motivoInstrumento,
            candidatos,
            dataHora,
            hash,
            agora,
        )
        salvar(lancamento, candidatos.singleOrNull())
        if (resolucaoCaju != null) {
            reconciliadorVoucher.confirmarEfeitoAposGravar(lancamento, resolucaoCaju, agora)
        }
        registrarTelemetria(captura.instituicao, lancamento.status)
        return ResultadoProcessamento.Gravado(lancamento)
    }

    @Suppress("LongParameterList")
    private suspend fun criarLancamento(
        captura: CapturaParaProcessar,
        resolucaoCaju: ResolucaoCapturaCaju?,
        depositoCaju: Boolean,
        instrumentoResolvido: Instrumento?,
        motivoInstrumento: MotivoPendencia?,
        candidatos: List<Lancamento>,
        dataHora: Instant,
        hash: String,
        agora: Instant,
    ): Lancamento {
        val categoria = resolverCategoria(captura.transacao, depositoCaju)
        val motivo = motivoDe(captura.transacao, motivoInstrumento, categoria, depositoCaju)
            ?: if (candidatos.size > 1) MotivoPendencia.TRANSFERENCIA_AMBIGUA else null
        return lancamentoDe(
            DadosDoLancamento(
                captura,
                instrumentoResolvido,
                resolucaoCaju,
                depositoCaju,
                categoria,
                dataHora,
                motivo,
                hash,
                candidatos.singleOrNull(),
                agora,
            ),
        )
    }

    private fun registrarTelemetria(instituicao: InstituicaoFinanceira, status: StatusLancamento) {
        val resultado = if (status == StatusLancamento.CONFIRMADO) {
            ResultadoTelemetriaCaptura.RECONHECIDA
        } else {
            ResultadoTelemetriaCaptura.REVISAO
        }
        TelemetriaDeCaptura.registrarCaptura(contexto.analytics, instituicao.name, resultado)
    }

    private suspend fun resolverSeCaju(captura: CapturaParaProcessar, depositoCaju: Boolean, dataHora: Instant): ResolucaoCapturaCaju? {
        if (captura.instituicao != InstituicaoFinanceira.CAJU) {
            return null
        }
        return reconciliadorVoucher.resolverCaju(
            instrumentosAtivos = instrumentosElegiveis(captura.instituicao),
            beneficioBruto = captura.transacao.beneficio,
            digitos = captura.transacao.digitos,
            ehDeposito = depositoCaju,
            valorCentavos = captura.transacao.valorCentavos,
            dataHora = dataHora,
        )
    }

    private fun lancamentoDe(dados: DadosDoLancamento): Lancamento {
        val estabelecimentoExibicao = dados.captura.transacao.estabelecimento
            ?: if (dados.depositoCaju) ESTABELECIMENTO_DEPOSITO_CAJU else null
        return Lancamento(
            publicId = UUID.randomUUID().toString(),
            instrumentoPublicId = dados.instrumento?.publicId,
            categoriaPublicId = dados.categoria?.publicId,
            transferenciaId = dados.par?.let { UUID.randomUUID().toString() },
            valorCentavos = dados.captura.transacao.valorCentavos,
            dataHora = dados.dataHora,
            tipo = if (dados.par == null) tipoDe(dados.captura.transacao.direcao) else TipoLancamento.TRANSFERENCIA,
            tipoSemTransferencia = tipoDe(dados.captura.transacao.direcao).takeIf { dados.par != null },
            estabelecimento = estabelecimentoExibicao,
            status = if (dados.motivo == null) StatusLancamento.CONFIRMADO else StatusLancamento.PENDENTE,
            origem = OrigemLancamento.NOTIFICACAO,
            instituicaoOrigem = dados.captura.instituicao,
            confianca = ConfiancaDaLeitura.calcular(
                dados.captura.transacao.valorCentavos,
                estabelecimentoExibicao,
                dados.categoria,
            ),
            motivoPendencia = dados.motivo?.name,
            hashDuplicata = dados.hash,
            criadoEm = dados.agora,
            atualizadoEm = dados.agora,
            textoBruto = dados.captura.textoBruto,
            estabelecimentoNormalizado = NormalizadorDeAssinatura.normalizar(estabelecimentoExibicao)?.take(LIMITE_NORMALIZADO),
            beneficio = dados.resolucaoCaju?.beneficio ?: BeneficioCajuMapper.mapear(dados.captura.transacao.beneficio),
            saldoObservadoCentavos = dados.captura.transacao.saldoCentavos,
        )
    }

    private data class DadosDoLancamento(
        val captura: CapturaParaProcessar,
        val instrumento: Instrumento?,
        val resolucaoCaju: ResolucaoCapturaCaju?,
        val depositoCaju: Boolean,
        val categoria: Categoria?,
        val dataHora: Instant,
        val motivo: MotivoPendencia?,
        val hash: String,
        val par: Lancamento?,
        val agora: Instant,
    )

    private suspend fun salvar(lancamento: Lancamento, par: Lancamento?) {
        lancamentoRepository.salvar(lancamento)
        if (par != null) {
            lancamentoRepository.salvar(
                par.copy(
                    tipo = TipoLancamento.TRANSFERENCIA,
                    tipoSemTransferencia = par.direcaoDaCaptura(),
                    transferenciaId = lancamento.transferenciaId,
                    atualizadoEm = lancamento.atualizadoEm,
                ),
            )
        }
    }

    private suspend fun resolverCategoria(transacao: TransacaoExtraida, depositoCaju: Boolean): Categoria? {
        if (depositoCaju) {
            val receitas = categoriaRepository.observeEscolhiveis().first().filter { it.tipo == TipoCategoria.RECEITA }
            return receitas.firstOrNull { it.nome.equals(CATEGORIA_BENEFICIO, ignoreCase = true) }
                ?: receitas.firstOrNull { it.nome.equals(CATEGORIA_PADRAO, ignoreCase = true) }
        }
        return categoriaAprendida.resolver(transacao.estabelecimento) ?: categoriaPadrao(transacao.direcao)
    }

    private suspend fun categoriaPadrao(direcao: DirecaoPadrao): Categoria? {
        val tipo = tipoDaCategoria(direcao)

        return categoriaRepository.observeEscolhiveis().first().firstOrNull { categoria ->
            categoria.tipo == tipo && categoria.nome.equals(CATEGORIA_PADRAO, ignoreCase = true)
        }
    }

    private fun motivoDe(
        transacao: TransacaoExtraida,
        motivoInstrumento: MotivoPendencia?,
        categoria: Categoria?,
        depositoCaju: Boolean,
    ): MotivoPendencia? = when {
        motivoInstrumento != null -> motivoInstrumento
        transacao.moeda != MOEDA_NACIONAL -> MotivoPendencia.MOEDA_ESTRANGEIRA
        transacao.valorCentavos <= 0 || (!depositoCaju && transacao.estabelecimento.isNullOrBlank()) ->
            MotivoPendencia.CAMPOS_INCOMPLETOS
        categoria == null -> MotivoPendencia.CATEGORIA_DESCONHECIDA
        else -> null
    }

    private suspend fun candidatosDeTransferencia(
        captura: CapturaParaProcessar,
        instrumento: Instrumento,
        dataHora: Instant,
    ): List<Lancamento> {
        if (captura.transacao.moeda != MOEDA_NACIONAL) {
            return emptyList()
        }

        return lancamentoRepository.candidatosDeTransferencia(
            tipo = tipoOposto(tipoDe(captura.transacao.direcao)),
            valorCentavos = captura.transacao.valorCentavos,
            inicio = dataHora.minus(JANELA_DE_TRANSFERENCIA),
            fim = dataHora.plus(JANELA_DE_TRANSFERENCIA),
            contaPublicId = instrumento.contaPublicId,
            origem = OrigemLancamento.NOTIFICACAO,
        ).filter { candidato ->
            candidato.instrumentoPublicId?.let { id -> instrumentoRepository.buscarAtivo(id) } != null
        }
    }

    private fun tipoDe(direcao: DirecaoPadrao): TipoLancamento = when (direcao) {
        DirecaoPadrao.DESPESA -> TipoLancamento.DESPESA
        DirecaoPadrao.RECEITA -> TipoLancamento.RECEITA
    }

    private fun tipoDaCategoria(direcao: DirecaoPadrao): TipoCategoria = when (direcao) {
        DirecaoPadrao.DESPESA -> TipoCategoria.DESPESA
        DirecaoPadrao.RECEITA -> TipoCategoria.RECEITA
    }

    private fun tipoOposto(tipo: TipoLancamento): TipoLancamento = when (tipo) {
        TipoLancamento.RECEITA -> TipoLancamento.DESPESA
        else -> TipoLancamento.RECEITA
    }

    private fun Lancamento.direcaoDaCaptura(): TipoLancamento? = tipoSemTransferencia ?: tipo.direcaoDaTransferencia

    private companion object {
        const val MOEDA_NACIONAL = "BRL"
        const val CATEGORIA_PADRAO = "Outros"
        const val CATEGORIA_BENEFICIO = "Benefício"
        const val ESTABELECIMENTO_DEPOSITO_CAJU = "Recarga Caju"
        const val LIMITE_NORMALIZADO = 200
        val JANELA_DE_TRANSFERENCIA: Duration = Duration.ofMinutes(15)
    }
}
