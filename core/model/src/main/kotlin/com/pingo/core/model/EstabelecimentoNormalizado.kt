package com.pingo.core.model

object EstabelecimentoNormalizado {
    private val espacos = Regex("[\\s\\p{Zs}]+")
    private val espacosAoRedorDoAsterisco = Regex("\\s*\\*\\s*")
    private val sufixoDeIdentificador = Regex("\\*[0-9]*$")

    fun de(bruto: String?): String? = bruto
        ?.lowercase()
        ?.replace(espacos, " ")
        ?.replace(espacosAoRedorDoAsterisco, "*")
        ?.trim()
        ?.replace(sufixoDeIdentificador, "")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}
