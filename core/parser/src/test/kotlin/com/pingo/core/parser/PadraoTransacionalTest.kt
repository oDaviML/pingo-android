package com.pingo.core.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PadraoTransacionalTest {
    private val engine = DefaultParserEngine()

    @Test
    fun `padrao com extracao de moeda devolve a moeda declarada e o valor na moeda original`() {
        val dados = transacaoDe(PADRAO_MOEDA_ESTRANGEIRA, "Compra aprovada de USD 20.00 em LOJA ALFA no dia 03/11")

        assertEquals("USD", dados.moeda)
        assertEquals(2000L, dados.valorCentavos)
    }

    @Test
    fun `padrao sem extracao de moeda assume BRL`() {
        val dados = transacaoDe(PADRAO_SALDO_BENEFICIO, TEXTO_SALDO_BENEFICIO)

        assertEquals("BRL", dados.moeda)
    }

    @Test
    fun `extrai saldo e beneficio quando o padrao declara os campos`() {
        val dados = transacaoDe(PADRAO_SALDO_BENEFICIO, TEXTO_SALDO_BENEFICIO)

        assertEquals("LOJA ALFA", dados.estabelecimento)
        assertEquals(2002L, dados.saldoCentavos)
        assertEquals("SALDO LIVRE", dados.beneficio)
    }

    @Test
    fun `remove o fragmento de CPF que vem antes do nome do remetente`() {
        val dados = transacaoDe(PADRAO_REMETENTE, "Você recebeu uma transferência de R$ 45,00 de 123.456.789 MARIA SOUZA.")

        assertEquals("MARIA SOUZA", dados.estabelecimento)
    }

    @Test
    fun `estabelecimento formado so por fragmento de CPF fica nulo`() {
        val dados = transacaoDe(PADRAO_REMETENTE, "Você recebeu uma transferência de R$ 45,00 de 123.456.789.")

        assertNull(dados.estabelecimento)
    }

    @Test
    fun `remove pontuacao e traco finais do estabelecimento`() {
        val dados = transacaoDe(PADRAO_REMETENTE, "Você recebeu uma transferência de R$ 800,00 de INSTITUTO LEGAL -.")

        assertEquals("INSTITUTO LEGAL", dados.estabelecimento)
    }

    private fun transacaoDe(padroes: String, texto: String): TransacaoExtraida {
        val resultado = engine.classificar(NotificacaoBruta(texto = texto), versaoDe(padroes))
        assertTrue("esperada transação, obtido $resultado", resultado is ResultadoParse.Transacao)
        return (resultado as ResultadoParse.Transacao).dados
    }
}

private const val TEXTO_SALDO_BENEFICIO = "Compra de R$ 31,00 em LOJA ALFA APROVADA no CRÉDITO, seu saldo em SALDO LIVRE é R$ 20,02."

private const val PADRAO_MOEDA_ESTRANGEIRA = """
[{"id":"usd","tipoLancamento":"COMPRA","direcao":"DESPESA","ancoras":["Compra aprovada de USD"],
"extracoes":{"moeda":"Compra aprovada de {moeda}","valor":"Compra aprovada de USD {valor} em",
"estabelecimento":"Compra aprovada de USD {valor} em {estabelecimento} no dia"}}]
"""

private const val PADRAO_SALDO_BENEFICIO = """
[{"id":"caju","tipoLancamento":"COMPRA","direcao":"DESPESA","ancoras":["Compra de R$","APROVADA no"],
"extracoes":{"valor":"Compra de R$ {valor} em","estabelecimento":"Compra de R$ {valor} em {estabelecimento} APROVADA",
"saldo":"seu saldo em {beneficio} é R$ {saldo}","beneficio":"seu saldo em {beneficio} é R$ {saldo}"}}]
"""

private const val PADRAO_REMETENTE = """
[{"id":"transferencia","tipoLancamento":"TRANSFERENCIA_RECEBIDA","direcao":"RECEITA",
"ancoras":["Você recebeu uma transferência de R$"],
"extracoes":{"valor":"transferência de R$ {valor} de","estabelecimento":"transferência de R$ {valor} de {estabelecimento}"}}]
"""
