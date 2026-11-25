package com.pingo.core.parser

internal object EstabelecimentoLimpo {
    private val fragmentoDeCpf = Regex("""^\d{1,3}(?:\.\d{3})+\.?\s*""")
    private val prefixoParcelamento = Regex("""^\d{1,2}x\s+(?:(?:sem|com)\s+juros\s+)?(?:em|na|no)\s+""", RegexOption.IGNORE_CASE)
    private val pontuacaoFinal = Regex("""[\s.,\-]+$""")

    fun de(bruto: String): String? = bruto
        .replace(fragmentoDeCpf, "")
        .replace(prefixoParcelamento, "")
        .replace(pontuacaoFinal, "")
        .trim()
        .takeIf(String::isNotEmpty)
}
