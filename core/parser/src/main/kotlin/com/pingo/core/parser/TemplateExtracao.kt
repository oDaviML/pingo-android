package com.pingo.core.parser

internal object TemplateExtracao {
    private val marcador = Regex("""\{([^{}]*)}""")

    fun regexDe(campo: CampoExtracao, template: String): Regex {
        val marcadores = marcador.findAll(template).toList()
        require(marcadores.size == 1 && marcadores.single().groupValues[1] == campo.chave) {
            "template de ${campo.chave} precisa ter somente o placeholder {${campo.chave}}: $template"
        }
        val unico = marcadores.single()
        val antes = template.substring(0, unico.range.first)
        val depois = template.substring(unico.range.last + 1)
        val captura = campo.regexDeCaptura(seguidoDeTexto = depois.isNotBlank())
        return Regex(literal(antes) + "($captura)" + literal(depois), RegexOption.IGNORE_CASE)
    }

    private fun literal(trecho: String): String = trecho.split(' ').joinToString("\\s*") { Regex.escape(it) }
}
