package com.pingo.feature.onboarding.impl

import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento

internal fun TipoConta.rotulo(): String = when (this) {
    TipoConta.CORRENTE -> "Corrente"
    TipoConta.BENEFICIO -> "Benefício"
}

internal fun TipoInstrumento.rotulo(): String = when (this) {
    TipoInstrumento.CREDITO -> "Crédito"
    TipoInstrumento.DEBITO -> "Débito"
    TipoInstrumento.VOUCHER -> "Voucher"
}

internal fun TipoInstrumento.apelidoPadrao(): String = when (this) {
    TipoInstrumento.CREDITO -> "Cartão de crédito"
    TipoInstrumento.DEBITO -> "Cartão de débito"
    TipoInstrumento.VOUCHER -> "Voucher"
}
