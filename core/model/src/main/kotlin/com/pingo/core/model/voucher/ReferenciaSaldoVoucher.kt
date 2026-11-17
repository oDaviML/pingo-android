package com.pingo.core.model.voucher

import androidx.compose.runtime.Immutable
import java.time.Instant

enum class TipoReferenciaSaldoVoucher {
    ABERTURA,
    OBSERVADO_APOS_TRANSACAO,
    AJUSTE,
}

@Immutable
data class ReferenciaSaldoVoucher(
    val publicId: String,
    val instrumentoPublicId: String,
    val tipo: TipoReferenciaSaldoVoucher,
    val saldoCentavos: Long,
    val deltaCentavos: Long = 0L,
    val instanteReferencia: Instant,
    val lancamentoPublicId: String? = null,
    val excluidoEm: Instant? = null,
)

@Immutable
data class AjusteSaldoCalculado(
    val saldoAnteriorCentavos: Long,
    val saldoAlvoCentavos: Long,
    val deltaCentavos: Long,
    val valorLancamentoCentavos: Long,
)
