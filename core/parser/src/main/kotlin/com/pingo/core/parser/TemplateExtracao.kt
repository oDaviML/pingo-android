package com.pingo.core.parser

import java.util.regex.Pattern

internal object TemplateExtracao {
    private val marcador = Regex("""\{([^{}]*)}""")
    private const val CORINGA = ".*?"
    private const val OPCOES = Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE

    fun regexDe(campo: CampoExtracao, template: String): Regex {
        val proprios = marcador.findAll(template).filter { it.groupValues[1] == campo.chave }.toList()
        require(proprios.size == 1) { "template de ${campo.chave} precisa ter exatamente um placeholder {${campo.chave}}: $template" }
        val proprio = proprios.single()
        val antes = template.substring(0, proprio.range.first)
        val depois = template.substring(proprio.range.last + 1)
        val captura = campo.regexDeCaptura(seguidoDeTexto = depois.isNotBlank())
        return Pattern.compile(regexDoTrecho(antes) + "($captura)" + regexDoTrecho(depois), OPCOES).toRegex()
    }

    private fun regexDoTrecho(trecho: String): String = marcador.split(trecho).joinToString(CORINGA) { literal(it) }

    private fun literal(trecho: String): String = trecho.split(' ').joinToString("\\s*") { Regex.escape(it) }
}
