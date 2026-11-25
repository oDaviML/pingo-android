package com.pingo.core.parser

import kotlinx.serialization.Serializable

@Serializable
enum class TipoInstrumentoPadrao {
    CREDITO,
    DEBITO,
    VOUCHER,
}
