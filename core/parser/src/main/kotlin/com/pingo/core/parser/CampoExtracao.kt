package com.pingo.core.parser

internal enum class CampoExtracao(val chave: String, private val captura: String, private val capturaSeguidaDeTexto: String = captura) {
    VALOR("valor", """\d(?:[\d.,]*\d)?"""),
    ESTABELECIMENTO("estabelecimento", ".+", ".+?"),
    DIGITOS("digitos", """\d+"""),
    ;

    fun regexDeCaptura(seguidoDeTexto: Boolean): String = if (seguidoDeTexto) capturaSeguidaDeTexto else captura

    companion object {
        fun porChave(chave: String): CampoExtracao? = entries.firstOrNull { it.chave == chave }
    }
}
