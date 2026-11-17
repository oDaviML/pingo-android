package com.pingo.core.model.voucher

import androidx.compose.runtime.Immutable
import com.pingo.core.model.BeneficioVoucher
import java.time.Instant
import java.time.LocalDate

enum class StatusRecargaProgramada {
    PENDENTE,
    EFETIVADA,
    SUBSTITUIDA,
    IGNORADA,
}

enum class MotivoRevisaoRecarga {
    BENEFICIO_AMBIGUO,
    DIVERGENCIA_RECARGA,
}

@Immutable
data class RecargaProgramada(
    val publicId: String,
    val instrumentoPublicId: String,
    val beneficio: BeneficioVoucher,
    val competenciaMes: LocalDate,
    val dataPrevista: LocalDate,
    val valorPrevistoCentavos: Long,
    val status: StatusRecargaProgramada,
    val lancamentoPublicId: String? = null,
    val excluidoEm: Instant? = null,
)

@Immutable
data class OcorrenciaPrevista(
    val instrumentoPublicId: String,
    val beneficio: BeneficioVoucher,
    val competenciaMes: LocalDate,
    val dataPrevista: LocalDate,
    val valorPrevistoCentavos: Long,
)

@Immutable
data class DepositoVoucherCapturado(
    val instrumentoPublicId: String?,
    val beneficio: BeneficioVoucher?,
    val valorCentavos: Long,
    val dataHora: Instant,
)

sealed interface ResultadoReconciliacaoRecarga {
    @Immutable
    data class SubstituicaoUnica(val ocorrencia: RecargaProgramada) : ResultadoReconciliacaoRecarga

    @Immutable
    data class RequerRevisao(val motivo: MotivoRevisaoRecarga, val candidatos: List<RecargaProgramada>) : ResultadoReconciliacaoRecarga

    @Immutable
    data object SemPrevisaoCompativel : ResultadoReconciliacaoRecarga
}
