package com.pingo.core.parser

internal object TextoNormalizer {
    private val invisiveisEEmojis = Regex("[\\p{So}\\p{Cf}\\x{FE0E}\\x{FE0F}\\x{20E3}\\x{1F3FB}-\\x{1F3FF}]")
    private val espacosHorizontais = Regex("[\\t\\x0B\\f \\p{Zs}\\p{Zl}\\p{Zp}]+")

    fun normalizar(texto: String): String = texto.replace(invisiveisEEmojis, "")
        .lines()
        .map { linha -> linha.replace(espacosHorizontais, " ").trim() }
        .filter(String::isNotEmpty)
        .joinToString("\n")
}
