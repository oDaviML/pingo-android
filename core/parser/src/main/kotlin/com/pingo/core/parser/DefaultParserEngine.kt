package com.pingo.core.parser

class DefaultParserEngine : ParserEngine {
    override fun classificar(notificacao: NotificacaoBruta, regras: VersaoRegras?): ResultadoParse {
        val texto = textoParaClassificacao(notificacao) ?: return ResultadoParse.Descartada
        val padrao = regras?.padroes?.firstOrNull { it.casa(texto) } ?: return ResultadoParse.NaoEntendida
        return padrao.resultadoPara(texto)
    }

    private fun textoParaClassificacao(notificacao: NotificacaoBruta): String? {
        val tituloNormalizado = TextoNormalizer.normalizar(notificacao.titulo.orEmpty())
        val corpoNormalizado = listOf(notificacao.texto, notificacao.bigText)
            .map { TextoNormalizer.normalizar(it.orEmpty()) }
            .firstOrNull(String::isNotEmpty)
            .orEmpty()
        return listOf(tituloNormalizado, corpoNormalizado).filter(String::isNotEmpty).joinToString("\n").takeIf(String::isNotEmpty)
    }
}
