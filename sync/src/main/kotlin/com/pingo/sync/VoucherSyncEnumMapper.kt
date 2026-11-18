package com.pingo.sync

import com.pingo.core.model.voucher.StatusRecargaProgramada
import com.pingo.core.model.voucher.TipoReferenciaSaldoVoucher

internal fun TipoReferenciaSaldoVoucher.toApiValue(): String = when (this) {
    TipoReferenciaSaldoVoucher.ABERTURA -> "Abertura"
    TipoReferenciaSaldoVoucher.OBSERVADO_APOS_TRANSACAO -> "Observado"
    TipoReferenciaSaldoVoucher.AJUSTE -> "Ajuste"
}

internal fun tipoReferenciaFromApi(value: String): TipoReferenciaSaldoVoucher = when (value) {
    "Abertura" -> TipoReferenciaSaldoVoucher.ABERTURA
    "Observado" -> TipoReferenciaSaldoVoucher.OBSERVADO_APOS_TRANSACAO
    else -> TipoReferenciaSaldoVoucher.AJUSTE
}

internal fun StatusRecargaProgramada.toApiValue(): String = when (this) {
    StatusRecargaProgramada.PENDENTE -> "Pendente"
    StatusRecargaProgramada.EFETIVADA -> "Efetivada"
    StatusRecargaProgramada.SUBSTITUIDA -> "Substituida"
    StatusRecargaProgramada.IGNORADA -> "Ignorada"
}

internal fun statusRecargaFromApi(value: String): StatusRecargaProgramada = when (value) {
    "Efetivada" -> StatusRecargaProgramada.EFETIVADA
    "Substituida" -> StatusRecargaProgramada.SUBSTITUIDA
    "Ignorada" -> StatusRecargaProgramada.IGNORADA
    else -> StatusRecargaProgramada.PENDENTE
}
