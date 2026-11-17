package com.pingo.core.model.voucher

import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoLancamento
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

object CalculadoraDeSaldoVoucher {
    val ZONA_SAO_PAULO: ZoneId = ZoneId.of("America/Sao_Paulo")

    fun calcularSaldoCentavos(instrumento: Instrumento, referencias: List<ReferenciaSaldoVoucher>, lancamentos: List<Lancamento>): Long {
        val lancamentosPorId = lancamentos.associateBy { it.publicId }
        val referenciaBase = referencias
            .asSequence()
            .filter { it.instrumentoPublicId == instrumento.publicId && it.excluidoEm == null }
            .filter { referenciaTemLancamentoValido(it, lancamentosPorId) }
            .maxWithOrNull(
                compareBy<ReferenciaSaldoVoucher> { it.instanteReferencia }
                    .thenBy { prioridadeTipo(it.tipo) }
                    .thenBy { it.publicId },
            )

        val saldoBase = referenciaBase?.saldoCentavos ?: (instrumento.saldoCentavos ?: 0L)
        val instanteBase = referenciaBase?.instanteReferencia ?: instrumento.criadoEm
        val lancamentoBaseId = referenciaBase?.lancamentoPublicId

        val deltaMovimentos = lancamentos
            .asSequence()
            .filter { lanc ->
                lanc.instrumentoPublicId == instrumento.publicId &&
                    lanc.excluidoEm == null &&
                    lanc.status == StatusLancamento.CONFIRMADO &&
                    lanc.publicId != lancamentoBaseId &&
                    lanc.dataHora.isAfter(instanteBase)
            }
            .sumOf { efeitoLancamentoNoSaldo(it) }

        return saldoBase + deltaMovimentos
    }

    fun calcularSaldoAteCentavos(
        instrumento: Instrumento,
        referencias: List<ReferenciaSaldoVoucher>,
        lancamentos: List<Lancamento>,
        ateInclusive: Instant,
    ): Long = calcularSaldoCentavos(
        instrumento = instrumento,
        referencias = referencias.filter { !it.instanteReferencia.isAfter(ateInclusive) },
        lancamentos = lancamentos.filter { !it.dataHora.isAfter(ateInclusive) },
    )

    fun calcularAjuste(
        instrumento: Instrumento,
        referencias: List<ReferenciaSaldoVoucher>,
        lancamentos: List<Lancamento>,
        saldoAlvoCentavos: Long,
        instanteAjuste: Instant,
    ): AjusteSaldoCalculado {
        val saldoAnterior = calcularSaldoAteCentavos(
            instrumento = instrumento,
            referencias = referencias,
            lancamentos = lancamentos,
            ateInclusive = instanteAjuste,
        )
        val delta = saldoAlvoCentavos - saldoAnterior
        return AjusteSaldoCalculado(
            saldoAnteriorCentavos = saldoAnterior,
            saldoAlvoCentavos = saldoAlvoCentavos,
            deltaCentavos = delta,
            valorLancamentoCentavos = abs(delta),
        )
    }

    fun efeitoLancamentoNoSaldo(lancamento: Lancamento): Long = when (lancamento.tipo) {
        TipoLancamento.RECEITA -> lancamento.valorCentavos
        TipoLancamento.DESPESA -> -lancamento.valorCentavos
        TipoLancamento.TRANSFERENCIA, TipoLancamento.AJUSTE -> 0L
    }

    private fun referenciaTemLancamentoValido(referencia: ReferenciaSaldoVoucher, lancamentosPorId: Map<String, Lancamento>): Boolean {
        val lancamentoId = referencia.lancamentoPublicId ?: return true
        val lancamento = lancamentosPorId[lancamentoId] ?: return true
        return lancamento.excluidoEm == null && lancamento.status == StatusLancamento.CONFIRMADO
    }

    private fun prioridadeTipo(tipo: TipoReferenciaSaldoVoucher): Int = when (tipo) {
        TipoReferenciaSaldoVoucher.ABERTURA -> 0
        TipoReferenciaSaldoVoucher.OBSERVADO_APOS_TRANSACAO -> 1
        TipoReferenciaSaldoVoucher.AJUSTE -> 2
    }
}
