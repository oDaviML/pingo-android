package com.pingo.core.data.impl

import com.pingo.core.database.dao.InstrumentoDao
import com.pingo.core.database.dao.RecargaProgramadaVoucherDao
import com.pingo.core.database.dao.ReferenciaSaldoVoucherDao
import com.pingo.core.database.entity.ReferenciaSaldoVoucherEntity
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.voucher.StatusRecargaProgramada
import com.pingo.core.model.voucher.TipoReferenciaSaldoVoucher
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReconciliadorDeVouchersLocal @Inject constructor(
    private val instrumentoDao: InstrumentoDao,
    private val referenciaDao: ReferenciaSaldoVoucherDao,
    private val recargaDao: RecargaProgramadaVoucherDao,
    private val clock: Clock,
) {
    suspend fun reconciliarAlteracoes(pares: List<Pair<Lancamento?, Lancamento?>>) {
        for ((antes, depois) in pares) {
            reconciliarPar(antes, depois)
        }
    }

    private suspend fun reconciliarPar(antes: Lancamento?, depois: Lancamento?) {
        val publicId = depois?.publicId ?: antes?.publicId ?: return
        val saldoObservado = depois?.saldoObservadoCentavos
        val instrumentoId = depois?.let { instrumentoVoucherConfirmadoId(it) }

        if (instrumentoId != null && saldoObservado != null && depois != null) {
            garantirReferenciaObservada(depois, instrumentoId, saldoObservado)
        } else {
            desativarReferenciaDoLancamento(publicId)
        }

        atualizarRecargasVinculadas(depois, publicId)
    }

    private suspend fun instrumentoVoucherConfirmadoId(lancamento: Lancamento): String? {
        val ativoConfirmado = lancamento.excluidoEm == null && lancamento.status == StatusLancamento.CONFIRMADO
        val instrumento = if (ativoConfirmado) lancamento.instrumentoPublicId?.let { instrumentoDao.buscar(it) } else null
        return instrumento?.takeIf { it.excluidoEm == null && it.tipo == TipoInstrumento.VOUCHER }?.publicId
    }

    private suspend fun garantirReferenciaObservada(lancamento: Lancamento, instrumentoPublicId: String, saldoCentavos: Long) {
        val agora = clock.instant()
        val existente = referenciaDao.buscarPorLancamentoIncluindoExcluidas(lancamento.publicId)
        if (existente != null) {
            atualizarReferenciaExistente(existente, lancamento, instrumentoPublicId, saldoCentavos, agora)
            return
        }
        val tipoRef = if (lancamento.tipo == TipoLancamento.AJUSTE) {
            TipoReferenciaSaldoVoucher.AJUSTE
        } else {
            TipoReferenciaSaldoVoucher.OBSERVADO_APOS_TRANSACAO
        }
        referenciaDao.upsert(
            ReferenciaSaldoVoucherEntity(
                publicId = UUID.randomUUID().toString(),
                instrumentoPublicId = instrumentoPublicId,
                tipoReferencia = tipoRef,
                saldoCentavos = saldoCentavos,
                deltaCentavos = null,
                instanteReferencia = lancamento.dataHora,
                lancamentoPublicId = lancamento.publicId,
                criadoEm = agora,
                atualizadoEm = agora,
                excluidoEm = null,
            ),
        )
    }

    private suspend fun atualizarReferenciaExistente(
        existente: ReferenciaSaldoVoucherEntity,
        lancamento: Lancamento,
        instrumentoPublicId: String,
        saldoCentavos: Long,
        agora: java.time.Instant,
    ) {
        val ehAjuste = existente.tipoReferencia == TipoReferenciaSaldoVoucher.AJUSTE || lancamento.tipo == TipoLancamento.AJUSTE
        val tipoPreservado = if (ehAjuste) {
            TipoReferenciaSaldoVoucher.AJUSTE
        } else {
            TipoReferenciaSaldoVoucher.OBSERVADO_APOS_TRANSACAO
        }
        val deltaPreservado = if (ehAjuste) existente.deltaCentavos else null
        val alterada = existente.instrumentoPublicId != instrumentoPublicId ||
            existente.saldoCentavos != saldoCentavos ||
            existente.instanteReferencia != lancamento.dataHora ||
            existente.tipoReferencia != tipoPreservado ||
            existente.deltaCentavos != deltaPreservado ||
            existente.excluidoEm != null
        if (alterada) {
            referenciaDao.upsert(
                existente.copy(
                    instrumentoPublicId = instrumentoPublicId,
                    tipoReferencia = tipoPreservado,
                    saldoCentavos = saldoCentavos,
                    deltaCentavos = deltaPreservado,
                    instanteReferencia = lancamento.dataHora,
                    atualizadoEm = agora,
                    excluidoEm = null,
                ),
            )
        }
    }

    private suspend fun desativarReferenciaDoLancamento(lancamentoPublicId: String) {
        val existente = referenciaDao.buscarPorLancamento(lancamentoPublicId) ?: return
        referenciaDao.marcarExcluida(existente.publicId, clock.instant())
    }

    private suspend fun atualizarRecargasVinculadas(depois: Lancamento?, publicId: String) {
        when {
            depois == null -> reverterRecargasVinculadas(publicId)
            depois.excluidoEm != null -> ignorarRecargasVinculadas(publicId)
            depois.status == StatusLancamento.CONFIRMADO -> restaurarRecargasIgnoradas(publicId, depois.origem)
        }
    }

    private suspend fun ignorarRecargasVinculadas(lancamentoPublicId: String) {
        val vinculadas = recargaDao.buscarPorLancamento(lancamentoPublicId)
        val agora = clock.instant()
        for (recarga in vinculadas) {
            if (recarga.status == StatusRecargaProgramada.SUBSTITUIDA || recarga.status == StatusRecargaProgramada.EFETIVADA) {
                recargaDao.upsert(
                    recarga.copy(
                        status = StatusRecargaProgramada.IGNORADA,
                        atualizadoEm = agora,
                    ),
                )
            }
        }
    }

    private suspend fun restaurarRecargasIgnoradas(lancamentoPublicId: String, origem: OrigemLancamento) {
        val vinculadas = recargaDao.buscarPorLancamento(lancamentoPublicId)
        val agora = clock.instant()
        val destino = if (origem == OrigemLancamento.NOTIFICACAO) {
            StatusRecargaProgramada.SUBSTITUIDA
        } else {
            StatusRecargaProgramada.EFETIVADA
        }
        for (recarga in vinculadas) {
            if (recarga.status == StatusRecargaProgramada.IGNORADA) {
                recargaDao.upsert(recarga.copy(status = destino, atualizadoEm = agora))
            }
        }
    }

    private suspend fun reverterRecargasVinculadas(lancamentoPublicId: String) {
        val vinculadas = recargaDao.buscarPorLancamento(lancamentoPublicId)
        val agora = clock.instant()
        for (recarga in vinculadas) {
            if (recarga.status == StatusRecargaProgramada.SUBSTITUIDA || recarga.status == StatusRecargaProgramada.EFETIVADA) {
                recargaDao.upsert(
                    recarga.copy(
                        status = StatusRecargaProgramada.PENDENTE,
                        lancamentoPublicId = null,
                        atualizadoEm = agora,
                    ),
                )
            }
        }
    }
}
