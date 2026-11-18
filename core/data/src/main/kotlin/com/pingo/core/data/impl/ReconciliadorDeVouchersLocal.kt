package com.pingo.core.data.impl

import com.pingo.core.database.dao.InstrumentoDao
import com.pingo.core.database.dao.RecargaProgramadaVoucherDao
import com.pingo.core.database.dao.ReferenciaSaldoVoucherDao
import com.pingo.core.database.entity.ReferenciaSaldoVoucherEntity
import com.pingo.core.model.Lancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoInstrumento
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

        if (depois == null || depois.excluidoEm != null) {
            reverterRecargasVinculadas(publicId)
        }
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
            val alterada = existente.instrumentoPublicId != instrumentoPublicId ||
                existente.saldoCentavos != saldoCentavos ||
                existente.instanteReferencia != lancamento.dataHora ||
                existente.excluidoEm != null
            if (alterada) {
                referenciaDao.upsert(
                    existente.copy(
                        instrumentoPublicId = instrumentoPublicId,
                        tipoReferencia = TipoReferenciaSaldoVoucher.OBSERVADO_APOS_TRANSACAO,
                        saldoCentavos = saldoCentavos,
                        deltaCentavos = null,
                        instanteReferencia = lancamento.dataHora,
                        atualizadoEm = agora,
                        excluidoEm = null,
                    ),
                )
            }
            return
        }
        referenciaDao.upsert(
            ReferenciaSaldoVoucherEntity(
                publicId = UUID.randomUUID().toString(),
                instrumentoPublicId = instrumentoPublicId,
                tipoReferencia = TipoReferenciaSaldoVoucher.OBSERVADO_APOS_TRANSACAO,
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

    private suspend fun desativarReferenciaDoLancamento(lancamentoPublicId: String) {
        val existente = referenciaDao.buscarPorLancamento(lancamentoPublicId) ?: return
        referenciaDao.marcarExcluida(existente.publicId, clock.instant())
    }

    private suspend fun reverterRecargasVinculadas(lancamentoPublicId: String) {
        val vinculadas = recargaDao.buscarPorLancamento(lancamentoPublicId)
        if (vinculadas.isEmpty()) {
            return
        }
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
