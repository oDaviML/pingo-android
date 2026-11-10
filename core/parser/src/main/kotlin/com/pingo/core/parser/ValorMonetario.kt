package com.pingo.core.parser

internal object ValorMonetario {
    private val formato = Regex("""(\d+(?:\.\d{3})*)(?:[,.](\d{1,2}))?""")
    private const val LIMITE_REAIS = 100_000_000_000_000L

    fun centavos(valor: String): Long? {
        val partes = formato.matchEntire(valor) ?: return null
        val reais = partes.groupValues[1].replace(".", "").toLongOrNull()?.takeIf { it < LIMITE_REAIS } ?: return null
        val centavos = partes.groupValues[2].padEnd(2, '0').toLong()
        return reais * 100 + centavos
    }
}
