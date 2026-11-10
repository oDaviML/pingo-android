package com.pingo.core.parser

sealed interface ResultadoParse {
    data class Transacao(val dados: TransacaoExtraida) : ResultadoParse

    data object Descartada : ResultadoParse

    data object Ignorada : ResultadoParse

    data object NaoEntendida : ResultadoParse
}
