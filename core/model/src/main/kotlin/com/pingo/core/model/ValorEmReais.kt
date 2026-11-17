package com.pingo.core.model

import java.math.BigDecimal
import java.math.RoundingMode

fun reaisParaCentavos(texto: String): Long? {
    val limpo = texto.trim().removePrefix("R$").trim().replace(" ", "")
    if (limpo.isEmpty()) {
        return null
    }

    val normalizado = if (limpo.contains(',')) limpo.replace(".", "").replace(',', '.') else limpo

    return normalizado.toBigDecimalOrNull()
        ?.takeIf { it >= BigDecimal.ZERO }
        ?.let { runCatching { it.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact() }.getOrNull() }
}
