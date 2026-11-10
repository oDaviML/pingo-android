package com.pingo.core.parser

internal sealed class PadraoCompilado(val id: String, private val ancoras: List<String>, private val exclusoes: List<String>) {
    fun casa(texto: String): Boolean =
        ancoras.all { texto.contains(it, ignoreCase = true) } && exclusoes.none { texto.contains(it, ignoreCase = true) }

    abstract fun resultadoPara(texto: String): ResultadoParse
}

internal class PadraoRuido(id: String, ancoras: List<String>, exclusoes: List<String>) : PadraoCompilado(id, ancoras, exclusoes) {
    override fun resultadoPara(texto: String): ResultadoParse = ResultadoParse.Ignorada
}

internal class PadraoTransacional(
    id: String,
    ancoras: List<String>,
    exclusoes: List<String>,
    private val tipo: TipoLancamentoPadrao,
    private val direcao: DirecaoPadrao,
    private val extratores: Map<CampoExtracao, Regex>,
) : PadraoCompilado(id, ancoras, exclusoes) {
    override fun resultadoPara(texto: String): ResultadoParse {
        val centavos = extrair(texto, CampoExtracao.VALOR)?.let(ValorMonetario::centavos) ?: return ResultadoParse.NaoEntendida
        return ResultadoParse.Transacao(
            TransacaoExtraida(
                padraoId = id,
                tipo = tipo,
                direcao = direcao,
                valorCentavos = centavos,
                estabelecimento = extrair(texto, CampoExtracao.ESTABELECIMENTO)?.let(EstabelecimentoLimpo::de),
                digitos = extrair(texto, CampoExtracao.DIGITOS)?.takeLast(QUANTIDADE_DIGITOS_CARTAO),
                moeda = extrair(texto, CampoExtracao.MOEDA)?.uppercase() ?: MOEDA_PADRAO,
                saldoCentavos = extrair(texto, CampoExtracao.SALDO)?.let(ValorMonetario::centavos),
                beneficio = extrair(texto, CampoExtracao.BENEFICIO),
            ),
        )
    }

    private fun extrair(texto: String, campo: CampoExtracao): String? =
        extratores[campo]?.find(texto)?.groupValues?.get(1)?.trim()?.takeIf(String::isNotEmpty)

    private companion object {
        const val QUANTIDADE_DIGITOS_CARTAO = 4
    }
}
