package com.pingo.core.model

import java.math.BigDecimal
import java.math.RoundingMode

private val SOMENTE_DIGITOS = Regex("\\d+")
private val GRUPOS_DE_MILHAR = Regex("\\d{1,3}(\\.\\d{3})+")
private val DECIMAL_COM_PONTO = Regex("\\d+\\.\\d{1,2}")

fun reaisParaCentavos(texto: String): Long? {
    val limpo = texto.replace('\u00A0', ' ').trim().removePrefix("R$").trim().replace(" ", "")
    if (limpo.isEmpty()) {
        return null
    }

    val normalizado = normalizarValorPtBr(limpo) ?: return null

    return normalizado.toBigDecimalOrNull()
        ?.takeIf { it >= BigDecimal.ZERO }
        ?.let { runCatching { it.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact() }.getOrNull() }
}

private fun normalizarValorPtBr(valor: String): String? {
    val virgulas = valor.count { it == ',' }

    return when {
        virgulas == 1 -> normalizarComVirgula(valor)
        virgulas > 1 -> null
        valor.count { it == '.' } == 1 && DECIMAL_COM_PONTO.matches(valor) -> valor
        valor.contains('.') && GRUPOS_DE_MILHAR.matches(valor) -> valor.replace(".", "")
        valor.contains('.') -> null
        SOMENTE_DIGITOS.matches(valor) -> valor
        else -> null
    }
}

private fun normalizarComVirgula(valor: String): String? {
    val inteiro = valor.substringBefore(',')
    val decimal = valor.substringAfter(',')

    if (decimal.length !in 1..2 || !inteiroValido(inteiro)) {
        return null
    }

    return "${inteiro.replace(".", "")}.$decimal"
}

private fun inteiroValido(inteiro: String): Boolean = SOMENTE_DIGITOS.matches(inteiro) || GRUPOS_DE_MILHAR.matches(inteiro)
