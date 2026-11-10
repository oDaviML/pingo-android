package com.pingo.core.notifications

sealed interface ResultadoCaptura {
    data class Transacao(val capturada: TransacaoCapturada) : ResultadoCaptura

    data object Ignorada : ResultadoCaptura

    data object NaoEntendida : ResultadoCaptura

    data object Duplicada : ResultadoCaptura

    data object SemInstrumento : ResultadoCaptura

    data object Descartada : ResultadoCaptura

    data object PacoteDesconhecido : ResultadoCaptura
}
