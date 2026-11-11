package com.pingo.core.data.detalhe

import com.pingo.core.data.CategoriaRepository
import com.pingo.core.data.InstrumentoRepository
import com.pingo.core.data.LancamentoRepository
import com.pingo.core.data.TransacaoLocal
import com.pingo.core.model.Categoria
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoLancamento
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
    private val categoriaRepository: CategoriaRepository,
    private val transacaoLocal: TransacaoLocal,
    private val clock: Clock,
) {
    suspend fun marcar(publicId: String, destinoPublicId: String): ResultadoDaTransferencia {
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
        val par = candidatos(original, destino, todos).singleOrNull()?.comoTransferencia(transferenciaId, agora)
            ?: novoPar(original, destino, transferenciaId, agora)

        transacaoLocal.executar {
            lancamentoRepository.salvar(lancamento)
            lancamentoRepository.salvar(par)
        }

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

        return when {
            destino.contaPublicId == origem.contaPublicId -> MotivoDaRecusa.MESMA_CONTA
            parNaoResolvido(original, todos) -> MotivoDaRecusa.PAR_NAO_RESOLVIDO
            candidatos(original, destino, todos).size > 1 -> MotivoDaRecusa.PAR_AMBIGUO
            else -> null
        }
    }

    suspend fun desfazer(publicId: String): ResultadoDaTransferencia {
        val original = lancamentoRepository.buscar(publicId)?.takeIf { it.excluidoEm == null }
            ?: return ResultadoDaTransferencia.NaoEncontrado
        val transferenciaId = original.transferenciaId
            ?: return ResultadoDaTransferencia.Desfeita(listOf(original))

        val agora = clock.instant()
        val irmaos = lancamentoRepository.observeLancamentos().first()
            .filter { it.transferenciaId == transferenciaId && it.publicId != publicId }
        val doOriginal = tipoDaCategoria(original.categoriaPublicId)
        val doIrmao = irmaos.firstOrNull()?.let { tipoDaCategoria(it.categoriaPublicId) }
        val lancamento = original.desfazendo(doOriginal ?: doIrmao?.oposto() ?: TipoLancamento.DESPESA, agora)
        val desfeitos = listOf(lancamento) + irmaos.map { irmao ->
            irmao.desfazendo(doIrmao ?: doOriginal?.oposto() ?: TipoLancamento.RECEITA, agora)
        }

        transacaoLocal.executar {
            desfeitos.forEach { lancamentoRepository.salvar(it) }
        }

        return ResultadoDaTransferencia.Desfeita(desfeitos)
    }

    private suspend fun instrumentoAtivo(publicId: String?): Instrumento? = publicId
        ?.let { instrumentoRepository.buscar(it) }
        ?.takeIf { it.excluidoEm == null && !it.arquivado }

    private fun parNaoResolvido(original: Lancamento, todos: List<Lancamento>): Boolean = todos.any { candidato ->
        candidato.publicId != original.publicId &&
            candidato.transferenciaId == null &&
            candidato.instrumentoPublicId == null &&
            candidato.tipo == original.tipo.oposto() &&
            candidato.naJanelaDe(original)
    }

    private suspend fun candidatos(original: Lancamento, destino: Instrumento, todos: List<Lancamento>): List<Lancamento> {
        val contasPorInstrumento = todos.mapNotNull { candidato ->
            candidato.instrumentoPublicId?.let { publicId -> publicId to instrumentoAtivo(publicId)?.contaPublicId }
        }.toMap()

        return todos.filter { candidato ->
            candidato.publicId != original.publicId &&
                candidato.transferenciaId == null &&
                candidato.tipo == original.tipo.oposto() &&
                candidato.naJanelaDe(original) &&
                candidato.instrumentoPublicId != null &&
                contasPorInstrumento[candidato.instrumentoPublicId] == destino.contaPublicId
        }
    }

    private fun novoPar(original: Lancamento, destino: Instrumento, transferenciaId: String, agora: Instant): Lancamento = Lancamento(
        publicId = UUID.randomUUID().toString(),
        instrumentoPublicId = destino.publicId,
        transferenciaId = transferenciaId,
        valorCentavos = original.valorCentavos,
        dataHora = original.dataHora,
        tipo = TipoLancamento.TRANSFERENCIA,
        status = StatusLancamento.CONFIRMADO,
        origem = com.pingo.core.model.OrigemLancamento.MANUAL,
        confianca = 1.0,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private fun Lancamento.comoTransferencia(transferenciaId: String, agora: Instant): Lancamento {
        val pendente = status == StatusLancamento.PENDENTE && motivoPendencia == MotivoPendencia.TRANSFERENCIA_AMBIGUA.name

        return copy(
            tipo = TipoLancamento.TRANSFERENCIA,
            transferenciaId = transferenciaId,
            status = if (pendente) StatusLancamento.CONFIRMADO else status,
            motivoPendencia = if (pendente) null else motivoPendencia,
            atualizadoEm = agora,
        )
    }

    private fun Lancamento.desfazendo(tipoRestaurado: TipoLancamento, agora: Instant): Lancamento {
        val confirma = status == StatusLancamento.PENDENTE && motivoPendencia == MotivoPendencia.TRANSFERENCIA_AMBIGUA.name

        return copy(
            tipo = tipoRestaurado,
            transferenciaId = null,
            status = if (confirma) StatusLancamento.CONFIRMADO else status,
            motivoPendencia = if (confirma) null else motivoPendencia,
            atualizadoEm = agora,
        )
    }

    private fun Lancamento.naJanelaDe(original: Lancamento): Boolean =
        valorCentavos == original.valorCentavos && !dataHora.isBefore(original.dataHora.minus(JANELA)) &&
            !dataHora.isAfter(original.dataHora.plus(JANELA))

    private suspend fun tipoDaCategoria(publicId: String?): TipoLancamento? = publicId
        ?.let { categoriaRepository.buscar(it) }
        ?.paraTipo()

    private fun Categoria.paraTipo(): TipoLancamento = when (tipo) {
        TipoCategoria.DESPESA -> TipoLancamento.DESPESA
        TipoCategoria.RECEITA -> TipoLancamento.RECEITA
    }

    private fun TipoLancamento.oposto(): TipoLancamento = when (this) {
        TipoLancamento.DESPESA -> TipoLancamento.RECEITA
        else -> TipoLancamento.DESPESA
    }

    private companion object {
        val JANELA: Duration = Duration.ofMinutes(15)
    }
}
