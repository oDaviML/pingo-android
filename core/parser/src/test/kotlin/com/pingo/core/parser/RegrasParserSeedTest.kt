package com.pingo.core.parser

import com.pingo.core.model.InstituicaoFinanceira
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegrasParserSeedTest {
    private val regras = RegrasParserSeed.regras
    private val vigentes = RegrasVigentes.selecionar(regras)

    @Test
    fun `seed traz uma regra ativa na versao 1 para cada uma das cinco instituicoes`() {
        assertEquals(
            setOf(
                InstituicaoFinanceira.NUBANK,
                InstituicaoFinanceira.INTER,
                InstituicaoFinanceira.ITAU,
                InstituicaoFinanceira.MERCADO_PAGO,
                InstituicaoFinanceira.CAJU,
            ),
            regras.map { it.instituicao }.toSet(),
        )
        assertEquals(5, regras.size)
        assertTrue(regras.all { it.ativa && it.versao == 1 })
    }

    @Test
    fun `todas as regras do seed compilam na selecao vigente`() {
        assertEquals(regras.map { it.instituicao }.toSet(), vigentes.keys)
    }

    @Test
    fun `negada de qualquer instituicao vira IGNORADA e nao NAO_ENTENDIDA`() {
        val negada = NotificacaoBruta(titulo = "Compra negada", texto = "Compra de R$ 10,00 negada em LOJA ALFA")

        vigentes.forEach { (instituicao, versao) ->
            assertEquals(instituicao.name, ResultadoParse.Ignorada, DefaultParserEngine().classificar(negada, versao))
        }
    }

    @Test
    fun `texto desconhecido de qualquer instituicao vira NAO_ENTENDIDA`() {
        val desconhecida = NotificacaoBruta(texto = "Olá, tudo bem?")

        vigentes.forEach { (instituicao, versao) ->
            assertEquals(instituicao.name, ResultadoParse.NaoEntendida, DefaultParserEngine().classificar(desconhecida, versao))
        }
    }

    @Test
    fun `cartao final do Itau extrai os ultimos 4 digitos pelo seed`() {
        val notificacao = NotificacaoBruta(
            titulo = "Sua compra foi aprovada 💳",
            texto = "Olá! Compra aprovada de R$ 10,00 em LOJA ALFA no dia 01/11 às 10:00 no cartão final 4640. Boas compras.",
        )

        val resultado = DefaultParserEngine().classificar(notificacao, vigentes.getValue(InstituicaoFinanceira.ITAU))

        assertEquals("4640", (resultado as ResultadoParse.Transacao).dados.digitos)
    }
}
