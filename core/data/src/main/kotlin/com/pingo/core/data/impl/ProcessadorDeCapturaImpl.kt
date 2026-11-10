package com.pingo.core.data.impl

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
import com.pingo.core.model.Categoria
import com.pingo.core.model.EstabelecimentoNormalizado
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoLancamento
import com.pingo.core.parser.DataHoraDoLancamento
import com.pingo.core.parser.DirecaoPadrao
import com.pingo.core.parser.HashDuplicata
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
    private val transacaoLocal: TransacaoLocal,
    private val clock: Clock,
    private val dispatchers: AppDispatchers,
) : ProcessadorDeCaptura {
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

        val instrumentos = instrumentoRepository.ativosDaInstituicao(captura.instituicao)

        return gravar(captura, instrumentos, hash)
    }

    private suspend fun gravar(captura: CapturaParaProcessar, instrumentos: List<Instrumento>, hash: String): ResultadoProcessamento {
        val transacao = captura.transacao
        val resolucao = ResolucaoDeInstrumento.de(transacao.digitos, instrumentos)
        val categoria = resolverCategoria(transacao)
        val dataHora = DataHoraDoLancamento.resolver(transacao.dataHoraTexto, captura.postTime, clock.zone)
        val candidatos = resolucao.instrumento?.let { instrumento ->
            candidatosDeTransferencia(captura, instrumento, dataHora)
        }.orEmpty()
        val motivo = motivoDe(transacao, resolucao, categoria)
            ?: if (candidatos.size > 1) MotivoPendencia.TRANSFERENCIA_AMBIGUA else null
        val par = candidatos.singleOrNull()
        val agora = clock.instant()
        val lancamento = lancamentoDe(
            DadosDoLancamento(
                captura = captura,
                resolucao = resolucao,
                categoria = categoria,
                dataHora = dataHora,
                motivo = motivo,
                hash = hash,
                par = par,
                agora = agora,
            ),
        )

        salvar(lancamento, par)

        return ResultadoProcessamento.Gravado(lancamento)
    }

    private fun lancamentoDe(dados: DadosDoLancamento): Lancamento = Lancamento(
        publicId = UUID.randomUUID().toString(),
        instrumentoPublicId = dados.resolucao.instrumento?.publicId,
        categoriaPublicId = dados.categoria?.publicId,
        transferenciaId = dados.par?.let { UUID.randomUUID().toString() },
        valorCentavos = dados.captura.transacao.valorCentavos,
        dataHora = dados.dataHora,
        tipo = if (dados.par == null) tipoDe(dados.captura.transacao.direcao) else TipoLancamento.TRANSFERENCIA,
        estabelecimento = dados.captura.transacao.estabelecimento,
        status = if (dados.motivo == null) StatusLancamento.CONFIRMADO else StatusLancamento.PENDENTE,
        origem = OrigemLancamento.NOTIFICACAO,
        instituicaoOrigem = dados.captura.instituicao,
        confianca = ConfiancaDaLeitura.calcular(
            dados.captura.transacao.valorCentavos,
            dados.captura.transacao.estabelecimento,
            dados.categoria,
        ),
        motivoPendencia = dados.motivo?.name,
        hashDuplicata = dados.hash,
        criadoEm = dados.agora,
        atualizadoEm = dados.agora,
        textoBruto = dados.captura.textoBruto,
    )

    private data class DadosDoLancamento(
        val captura: CapturaParaProcessar,
        val resolucao: ResolucaoDeInstrumento,
        val categoria: Categoria?,
        val dataHora: Instant,
        val motivo: MotivoPendencia?,
        val hash: String,
        val par: Lancamento?,
        val agora: Instant,
    )

    private suspend fun salvar(lancamento: Lancamento, par: Lancamento?) {
        if (par == null) {
            lancamentoRepository.salvar(lancamento)
            return
        }

        transacaoLocal.executar {
            lancamentoRepository.salvar(lancamento)
            lancamentoRepository.salvar(
                par.copy(
                    tipo = TipoLancamento.TRANSFERENCIA,
                    transferenciaId = lancamento.transferenciaId,
                    atualizadoEm = lancamento.atualizadoEm,
                ),
            )
        }
    }

    private suspend fun resolverCategoria(transacao: TransacaoExtraida): Categoria? =
        categoriaAprendida.resolver(transacao.estabelecimento) ?: categoriaPadrao(transacao.direcao)

    private suspend fun categoriaPadrao(direcao: DirecaoPadrao): Categoria? {
        val tipo = tipoDaCategoria(direcao)

        return categoriaRepository.observeEscolhiveis().first().firstOrNull { categoria ->
            categoria.tipo == tipo && categoria.nome.equals(CATEGORIA_PADRAO, ignoreCase = true)
        }
    }

    private fun motivoDe(transacao: TransacaoExtraida, resolucao: ResolucaoDeInstrumento, categoria: Categoria?): MotivoPendencia? = when {
        resolucao.motivo != null -> resolucao.motivo
        transacao.moeda != MOEDA_NACIONAL -> MotivoPendencia.MOEDA_ESTRANGEIRA
        transacao.valorCentavos <= 0 || transacao.estabelecimento.isNullOrBlank() -> MotivoPendencia.CAMPOS_INCOMPLETOS
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
        )
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

    private companion object {
        const val MOEDA_NACIONAL = "BRL"
        const val CATEGORIA_PADRAO = "Outros"
        val JANELA_DE_TRANSFERENCIA: Duration = Duration.ofMinutes(15)
    }
}
