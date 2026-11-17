package com.pingo.core.data.detalhe

import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.direcaoDaTransferencia
import com.pingo.core.model.direcaoOposta
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

enum class MotivoDaRecusa {
    INSTRUMENTO_NAO_RESOLVIDO,
    DESTINO_NAO_ENCONTRADO,
    MESMA_CONTA,
    PAR_AMBIGUO,
    PAR_NAO_RESOLVIDO,
    JA_E_TRANSFERENCIA,
    DIRECAO_DESCONHECIDA,
    PAR_INCONSISTENTE,
}

sealed interface ResultadoDaTransferencia {
    data class Marcada(val lancamento: Lancamento, val par: Lancamento?) : ResultadoDaTransferencia

    data class Desfeita(val lancamentos: List<Lancamento>) : ResultadoDaTransferencia

    data class Recusada(val motivo: MotivoDaRecusa) : ResultadoDaTransferencia

    data object NaoEncontrado : ResultadoDaTransferencia
}

class MarcarTransferencia @Inject constructor(
    private val lancamentoRepository: LancamentoRepository,
    private val instrumentoRepository: InstrumentoRepository,
    private val transacaoLocal: TransacaoLocal,
    private val clock: Clock,
) {
    suspend fun marcar(publicId: String, destinoPublicId: String): ResultadoDaTransferencia = transacaoLocal.executar {
        marcarNaTransacao(publicId, destinoPublicId)
    }

    private suspend fun marcarNaTransacao(publicId: String, destinoPublicId: String): ResultadoDaTransferencia {
        val original = lancamentoRepository.buscar(publicId)?.takeIf { it.excluidoEm == null }
            ?: return ResultadoDaTransferencia.NaoEncontrado
        val todos = lancamentoRepository.observeLancamentos().first()
        val destino = instrumentoAtivo(destinoPublicId)
        val recusa = motivoDaRecusa(original, destino, todos)
        if (recusa != null || destino == null) {
            return ResultadoDaTransferencia.Recusada(recusa ?: MotivoDaRecusa.DESTINO_NAO_ENCONTRADO)
        }

        val agora = clock.instant()
        val transferenciaId = UUID.randomUUID().toString()
        val lancamento = original.comoTransferencia(transferenciaId, agora)
        val candidato = candidatosReais(original, destino, todos).singleOrNull()?.lancamento
        val par = candidato?.comoTransferencia(transferenciaId, agora)
            ?: novoPar(original, destino, transferenciaId, agora)

        lancamentoRepository.salvar(lancamento)
        lancamentoRepository.salvar(par)

        return ResultadoDaTransferencia.Marcada(lancamento = lancamento, par = par)
    }

    private suspend fun motivoDaRecusa(original: Lancamento, destino: Instrumento?, todos: List<Lancamento>): MotivoDaRecusa? = when {
        original.transferenciaId != null || original.tipo == TipoLancamento.TRANSFERENCIA -> MotivoDaRecusa.JA_E_TRANSFERENCIA
        else -> motivoDoPar(original, destino, todos)
    }

    private suspend fun motivoDoPar(original: Lancamento, destino: Instrumento?, todos: List<Lancamento>): MotivoDaRecusa? {
        val origem = instrumentoAtivo(original.instrumentoPublicId) ?: return MotivoDaRecusa.INSTRUMENTO_NAO_RESOLVIDO
        if (destino == null) {
            return MotivoDaRecusa.DESTINO_NAO_ENCONTRADO
        }

        val reais = candidatosReais(original, destino, todos)

        return when {
            destino.contaPublicId == origem.contaPublicId -> MotivoDaRecusa.MESMA_CONTA
            parNaoResolvido(original, todos) -> MotivoDaRecusa.PAR_NAO_RESOLVIDO
            reais.any { !it.ativo } -> MotivoDaRecusa.INSTRUMENTO_NAO_RESOLVIDO
            reais.size > 1 -> MotivoDaRecusa.PAR_AMBIGUO
            else -> null
        }
    }

    suspend fun desfazer(publicId: String): ResultadoDaTransferencia {
        val original = lancamentoRepository.buscar(publicId)?.takeIf { it.excluidoEm == null }

        return when {
            original == null -> ResultadoDaTransferencia.NaoEncontrado
            original.transferenciaId == null -> ResultadoDaTransferencia.Desfeita(listOf(original))
            else -> desfazerGrupo(original)
        }
    }

    private suspend fun desfazerGrupo(original: Lancamento): ResultadoDaTransferencia {
        val transferenciaId = original.transferenciaId ?: return ResultadoDaTransferencia.Desfeita(listOf(original))
        val grupo = lancamentoRepository.observeLancamentos().first()
            .filter { it.transferenciaId == transferenciaId }
            .sortedBy { it.publicId }
        val direcoes = grupo.map { it.direcaoParaRestaurar() }
        val recusa = when {
            direcoes.any { it == null } -> MotivoDaRecusa.DIRECAO_DESCONHECIDA
            !direcoesConsistentes(direcoes.filterNotNull()) -> MotivoDaRecusa.PAR_INCONSISTENTE
            else -> null
        }

        if (recusa != null) {
            return ResultadoDaTransferencia.Recusada(recusa)
        }

        val agora = clock.instant()
        val desfeitos = grupo.map { lancamento -> lancamento.desfazendo(lancamento.direcaoParaRestaurar()!!, agora) }

        transacaoLocal.executar {
            desfeitos.forEach { lancamentoRepository.salvar(it) }
        }

        return ResultadoDaTransferencia.Desfeita(desfeitos)
    }

    private fun direcoesConsistentes(direcoes: List<TipoLancamento>): Boolean = when (direcoes.size) {
        0, 1 -> true
        2 -> direcoes[0] != direcoes[1]
        else -> false
    }

    private suspend fun instrumentoAtivo(publicId: String?): Instrumento? = publicId?.let { instrumentoRepository.buscarAtivo(it) }

    private suspend fun parNaoResolvido(original: Lancamento, todos: List<Lancamento>): Boolean = todos.any { candidato ->
        val instrumentoPublicId = candidato.instrumentoPublicId
        candidato.publicId != original.publicId &&
            candidato.transferenciaId == null &&
            (instrumentoPublicId == null || instrumentoNaoExiste(instrumentoPublicId)) &&
            candidato.tipo == original.tipo.oposto() &&
            candidato.naJanelaDe(original)
    }

    private suspend fun candidatosReais(original: Lancamento, destino: Instrumento, todos: List<Lancamento>): List<CandidatoReal> =
        todos.mapNotNull { candidato ->
            val elegivel = candidato.publicId != original.publicId &&
                candidato.transferenciaId == null &&
                candidato.tipo == original.tipo.oposto() &&
                candidato.naJanelaDe(original)
            val instrumento = candidato.instrumentoPublicId?.let { instrumentoRepository.buscar(it) }
            if (elegivel && instrumento?.contaPublicId == destino.contaPublicId) {
                CandidatoReal(
                    lancamento = candidato,
                    ativo = instrumentoRepository.buscarAtivo(instrumento.publicId) != null,
                )
            } else {
                null
            }
        }

    private suspend fun instrumentoNaoExiste(publicId: String): Boolean = instrumentoRepository.buscar(publicId) == null

    private data class CandidatoReal(val lancamento: Lancamento, val ativo: Boolean)

    private fun novoPar(original: Lancamento, destino: Instrumento, transferenciaId: String, agora: Instant): Lancamento = Lancamento(
        publicId = UUID.randomUUID().toString(),
        instrumentoPublicId = destino.publicId,
        transferenciaId = transferenciaId,
        valorCentavos = original.valorCentavos,
        dataHora = original.dataHora,
        tipo = TipoLancamento.TRANSFERENCIA,
        tipoSemTransferencia = original.direcaoParaRestaurar()?.direcaoOposta,
        status = StatusLancamento.CONFIRMADO,
        origem = OrigemLancamento.MANUAL,
        confianca = 1.0,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private fun Lancamento.comoTransferencia(transferenciaId: String, agora: Instant): Lancamento {
        val confirmada = status == StatusLancamento.PENDENTE

        return copy(
            tipo = TipoLancamento.TRANSFERENCIA,
            tipoSemTransferencia = direcaoParaRestaurar(),
            transferenciaId = transferenciaId,
            status = if (confirmada) StatusLancamento.CONFIRMADO else status,
            motivoPendencia = if (confirmada) null else motivoPendencia,
            atualizadoEm = agora,
        )
    }

    private fun Lancamento.desfazendo(tipoRestaurado: TipoLancamento, agora: Instant): Lancamento {
        val confirma = status == StatusLancamento.PENDENTE && motivoPendencia == MotivoPendencia.TRANSFERENCIA_AMBIGUA.name

        return copy(
            tipo = tipoRestaurado,
            tipoSemTransferencia = null,
            transferenciaId = null,
            status = if (confirma) StatusLancamento.CONFIRMADO else status,
            motivoPendencia = if (confirma) null else motivoPendencia,
            atualizadoEm = agora,
        )
    }

    private fun Lancamento.direcaoParaRestaurar(): TipoLancamento? = tipoSemTransferencia ?: tipo.direcaoDaTransferencia

    private fun Lancamento.naJanelaDe(original: Lancamento): Boolean =
        valorCentavos == original.valorCentavos && !dataHora.isBefore(original.dataHora.minus(JANELA)) &&
            !dataHora.isAfter(original.dataHora.plus(JANELA))

    private fun TipoLancamento.oposto(): TipoLancamento = when (this) {
        TipoLancamento.DESPESA -> TipoLancamento.RECEITA
        else -> TipoLancamento.DESPESA
    }

    private companion object {
        val JANELA: Duration = Duration.ofMinutes(15)
    }
}
