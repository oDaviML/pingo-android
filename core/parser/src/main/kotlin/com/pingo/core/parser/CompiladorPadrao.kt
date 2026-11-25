package com.pingo.core.parser

internal object CompiladorPadrao {
    fun compilar(padrao: PadraoRegra): PadraoCompilado {
        require(padrao.id.isNotBlank()) { "padrão sem id" }
        val ancoras = ancorasDe(padrao)
        val exclusoes = textosNormalizados(padrao.exclusoes, padrao.id)
        return if (padrao.ruido) {
            PadraoRuido(padrao.id, ancoras, exclusoes)
        } else {
            compilarTransacional(padrao, ancoras, exclusoes)
        }
    }

    private fun compilarTransacional(padrao: PadraoRegra, ancoras: List<String>, exclusoes: List<String>): PadraoTransacional {
        val tipo = requireNotNull(padrao.tipoLancamento) { "padrão transacional ${padrao.id} sem tipoLancamento" }
        val direcao = requireNotNull(padrao.direcao) { "padrão transacional ${padrao.id} sem direção" }
        require(padrao.extracoes.containsKey(CampoExtracao.VALOR.chave)) { "padrão transacional ${padrao.id} sem extração de valor" }
        return PadraoTransacional(padrao.id, ancoras, exclusoes, tipo, direcao, padrao.tipoInstrumento, extratoresDe(padrao))
    }

    private fun ancorasDe(padrao: PadraoRegra): List<String> {
        require(padrao.ancoras.isNotEmpty()) { "padrão ${padrao.id} sem âncoras" }
        return textosNormalizados(padrao.ancoras, padrao.id)
    }

    private fun extratoresDe(padrao: PadraoRegra): Map<CampoExtracao, Regex> = padrao.extracoes.mapNotNull { (chave, template) ->
        CampoExtracao.porChave(chave)?.let { campo ->
            campo to TemplateExtracao.regexDe(campo, TextoNormalizer.normalizar(template))
        }
    }.toMap()

    private fun textosNormalizados(textos: List<String>, padraoId: String): List<String> =
        textos.map(TextoNormalizer::normalizar).onEach { require(it.isNotEmpty()) { "padrão $padraoId com texto vazio" } }
}
