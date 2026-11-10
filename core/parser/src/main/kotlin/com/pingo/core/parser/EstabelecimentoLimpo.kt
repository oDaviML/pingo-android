package com.pingo.core.parser

internal object EstabelecimentoLimpo {
    private val fragmentoDeCpf = Regex("""^\d{1,3}(?:\.\d{3})+\.?\s*""")
    private val pontuacaoFinal = Regex("""[\s.,\-]+$""")

    fun de(bruto: String): String? = bruto
        .replace(fragmentoDeCpf, "")
        .replace(pontuacaoFinal, "")
        .trim()
        .takeIf(String::isNotEmpty)
}
