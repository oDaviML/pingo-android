package com.pingo.core.parser

import com.pingo.core.model.InstituicaoFinanceira

internal enum class ClasseDesfecho { TRANSACAO, IGNORADA, NAO_ENTENDIDA, DESCARTADA, NAO_BANCARIA }

internal data class DesfechoReplay(val entrada: EntradaCorpus, val instituicao: InstituicaoFinanceira?, val resultado: ResultadoParse?) {
    val classe: ClasseDesfecho
        get() = when (resultado) {
            null -> ClasseDesfecho.NAO_BANCARIA
            ResultadoParse.Descartada -> ClasseDesfecho.DESCARTADA
            ResultadoParse.NaoEntendida -> ClasseDesfecho.NAO_ENTENDIDA
            ResultadoParse.Ignorada -> ClasseDesfecho.IGNORADA
            is ResultadoParse.Transacao -> ClasseDesfecho.TRANSACAO
        }
}

internal class ReplayCorpus(private val engine: ParserEngine, private val regras: Map<InstituicaoFinanceira, VersaoRegras>) {
    fun executar(entradas: List<EntradaCorpus>): List<DesfechoReplay> = entradas.map(::classificar)

    private fun classificar(entrada: EntradaCorpus): DesfechoReplay {
        val instituicao = PacotesBancarios.instituicaoDe(entrada.pacote)
        val resultado = instituicao?.let { engine.classificar(entrada.notificacao(), regras[it]) }
        return DesfechoReplay(entrada, instituicao, resultado)
    }
}

internal object PacotesBancarios {
    private val instituicoes = mapOf(
        "com.nu.production" to InstituicaoFinanceira.NUBANK,
        "br.com.intermedium" to InstituicaoFinanceira.INTER,
        "com.itau" to InstituicaoFinanceira.ITAU,
        "br.com.itau.internet" to InstituicaoFinanceira.ITAU,
        "com.mercadopago.wallet" to InstituicaoFinanceira.MERCADO_PAGO,
        "com.caju.employeeApp" to InstituicaoFinanceira.CAJU,
    )

    fun instituicaoDe(pacote: String): InstituicaoFinanceira? = instituicoes[pacote]
}
