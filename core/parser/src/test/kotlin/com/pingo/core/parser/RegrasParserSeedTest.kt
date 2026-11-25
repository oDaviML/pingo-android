package com.pingo.core.parser

import com.pingo.core.model.InstituicaoFinanceira
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegrasParserSeedTest {
    private val regras = RegrasParserSeed.regras
    private val vigentes = RegrasVigentes.selecionar(regras)

    @Test
    fun `seed traz uma regra ativa na versao 2 para cada uma das cinco instituicoes`() {
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
        assertTrue(regras.all { it.ativa && it.versao == RegrasParserSeed.VERSAO })
        assertEquals(2, RegrasParserSeed.VERSAO)
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

    @Test
    fun `NuPay do Nubank 1x no credito extrai valor, estabelecimento e tipo CREDITO`() {
        val notificacao = NotificacaoBruta(
            titulo = "Pagamento com NuPay aprovado",
            texto = "R$ 10,89 em 1x no crédito sem juros com NuPay APROVADO em Uber.",
        )

        val dados = classificar(InstituicaoFinanceira.NUBANK, notificacao)

        assertEquals(1089L, dados.valorCentavos)
        assertEquals("Uber", dados.estabelecimento)
        assertEquals(1, dados.totalParcelas)
        assertEquals(TipoInstrumentoPadrao.CREDITO, dados.tipoInstrumento)
    }

    @Test
    fun `NuPay do Nubank parcelado extrai valor, parcelas, estabelecimento e tipo CREDITO`() {
        val notificacao = NotificacaoBruta(
            titulo = "Pagamento com NuPay aprovado",
            texto = "R$ 199,80 em 3x no crédito sem juros com NuPay APROVADO em Amazon.",
        )

        val dados = classificar(InstituicaoFinanceira.NUBANK, notificacao)

        assertEquals(19980L, dados.valorCentavos)
        assertEquals("Amazon", dados.estabelecimento)
        assertEquals(3, dados.totalParcelas)
        assertEquals(TipoInstrumentoPadrao.CREDITO, dados.tipoInstrumento)
    }

    @Test
    fun `NuPay no debito do Nubank extrai tipo DEBITO`() {
        val notificacao = NotificacaoBruta(
            titulo = "Pagamento com NuPay aprovado",
            texto = "R$ 25,00 no débito com NuPay APROVADO em Padaria Alfa.",
        )

        val dados = classificar(InstituicaoFinanceira.NUBANK, notificacao)

        assertEquals(2500L, dados.valorCentavos)
        assertEquals("Padaria Alfa", dados.estabelecimento)
        assertEquals(1, dados.totalParcelas)
        assertEquals(TipoInstrumentoPadrao.DEBITO, dados.tipoInstrumento)
    }

    @Test
    fun `compra parcelada do Itau extrai valor, parcelas, estabelecimento, data e tipo CREDITO`() {
        val notificacao = NotificacaoBruta(
            titulo = "Sua compra foi aprovada 💳",
            texto = "Olá! Compra parcelada aprovada de R$ 300,61 em 3x em HNA*OBOTICARIO " +
                "no dia 21/11 às 20:01 no cartão Itau. Boas compras.",
        )

        val dados = classificar(InstituicaoFinanceira.ITAU, notificacao)

        assertEquals(30061L, dados.valorCentavos)
        assertEquals(3, dados.totalParcelas)
        assertEquals("HNA*OBOTICARIO", dados.estabelecimento)
        assertEquals("21/11 às 20:01", dados.dataHoraTexto)
        assertEquals(TipoInstrumentoPadrao.CREDITO, dados.tipoInstrumento)
    }

    @Test
    fun `compra parcelada do Itau com final do cartao tambem extrai os 4 digitos`() {
        val notificacao = NotificacaoBruta(
            titulo = "Sua compra foi aprovada",
            texto = "Olá! Compra parcelada aprovada de R$ 240,00 em 4x em LOJA BETA " +
                "no dia 22/11 às 10:15 no cartão final 4640. Boas compras.",
        )

        val dados = classificar(InstituicaoFinanceira.ITAU, notificacao)

        assertEquals(24000L, dados.valorCentavos)
        assertEquals(4, dados.totalParcelas)
        assertEquals("LOJA BETA", dados.estabelecimento)
        assertEquals("4640", dados.digitos)
        assertEquals(TipoInstrumentoPadrao.CREDITO, dados.tipoInstrumento)
    }

    private fun classificar(instituicao: InstituicaoFinanceira, notificacao: NotificacaoBruta): TransacaoExtraida =
        (DefaultParserEngine().classificar(notificacao, vigentes.getValue(instituicao)) as ResultadoParse.Transacao).dados
}
