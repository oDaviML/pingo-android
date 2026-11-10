package com.pingo.core.parser

private const val VALOR_MONETARIO = """\d(?:[\d.,]*\d)?"""

internal enum class CampoExtracao(val chave: String, private val captura: String, private val capturaSeguidaDeTexto: String = captura) {
    VALOR("valor", VALOR_MONETARIO),
    SALDO("saldo", VALOR_MONETARIO),
    ESTABELECIMENTO("estabelecimento", ".+", ".+?"),
    BENEFICIO("beneficio", ".+", ".+?"),
    DIGITOS("digitos", """\d+"""),
    MOEDA("moeda", """[A-Za-z]{3}"""),
    ;

    fun regexDeCaptura(seguidoDeTexto: Boolean): String = if (seguidoDeTexto) capturaSeguidaDeTexto else captura

    companion object {
        fun porChave(chave: String): CampoExtracao? = entries.firstOrNull { it.chave == chave }
    }
}
