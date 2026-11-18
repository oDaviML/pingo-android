package com.pingo.core.data.reconciliacao

import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import java.time.Instant

data class ResolucaoCapturaCaju(
    val beneficio: BeneficioVoucher?,
    val instrumento: Instrumento?,
    val motivo: MotivoPendencia?,
    val ocorrenciaParaSubstituirPublicId: String? = null,
    val ocorrenciaParaCriarPendente: com.pingo.core.model.voucher.OcorrenciaPrevista? = null,
    val lancamentoPrevisaoParaExcluirPublicId: String? = null,
)

interface ReconciliadorVoucherCaptura {
    suspend fun resolverCaju(
        instrumentosAtivos: List<Instrumento>,
        beneficioBruto: String?,
        digitos: String?,
        ehDeposito: Boolean,
        valorCentavos: Long,
        dataHora: Instant,
    ): ResolucaoCapturaCaju?

    suspend fun confirmarEfeitoAposGravar(lancamento: Lancamento, resolucao: ResolucaoCapturaCaju, agora: Instant)

    object NoOp : ReconciliadorVoucherCaptura {
        override suspend fun resolverCaju(
            instrumentosAtivos: List<Instrumento>,
            beneficioBruto: String?,
            digitos: String?,
            ehDeposito: Boolean,
            valorCentavos: Long,
            dataHora: Instant,
        ): ResolucaoCapturaCaju? = null

        override suspend fun confirmarEfeitoAposGravar(lancamento: Lancamento, resolucao: ResolucaoCapturaCaju, agora: Instant) = Unit
    }
}
