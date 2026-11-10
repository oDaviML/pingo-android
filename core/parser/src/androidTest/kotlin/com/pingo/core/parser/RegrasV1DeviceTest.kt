package com.pingo.core.parser

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.model.InstituicaoFinanceira
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegrasV1DeviceTest {
    private val compiladas = RegrasVigentes.selecionar(RegrasParserSeed.regras)

    @Test
    fun compilaAsRegrasV1DasCincoInstituicoesNoDispositivo() {
        assertEquals(
            setOf(
                InstituicaoFinanceira.NUBANK,
                InstituicaoFinanceira.INTER,
                InstituicaoFinanceira.ITAU,
                InstituicaoFinanceira.MERCADO_PAGO,
                InstituicaoFinanceira.CAJU,
            ),
            compiladas.keys,
        )
    }

    @Test
    fun extraiCompraDoCorpusComOsTemplatesCompiladosNoDispositivo() {
        val resultado = DefaultParserEngine().classificar(
            NotificacaoBruta(
                titulo = "Sua compra foi aprovada",
                texto = "Olá! Compra aprovada de R$ 37,90 em CLARO *31987905690 no dia 10/11 às 00:55 no seu cartão Itau. Boas compras.",
            ),
            compiladas.getValue(InstituicaoFinanceira.ITAU),
        )

        val transacao = (resultado as ResultadoParse.Transacao).dados
        assertEquals(TipoLancamentoPadrao.COMPRA, transacao.tipo)
        assertEquals(3790L, transacao.valorCentavos)
        assertEquals("CLARO *31987905690", transacao.estabelecimento)
        assertTrue(transacao.dataHoraTexto.orEmpty().startsWith("10/11"))
    }

    @Test
    fun classificaRuidoEForaDePadraoNoDispositivo() {
        val engine = DefaultParserEngine()
        val itau = compiladas.getValue(InstituicaoFinanceira.ITAU)

        assertEquals(ResultadoParse.Ignorada, engine.classificar(NotificacaoBruta(texto = "Mais controle para gerenciar seu limite"), itau))
        assertEquals(ResultadoParse.NaoEntendida, engine.classificar(NotificacaoBruta(texto = "Seu extrato está disponível"), itau))
        assertEquals(ResultadoParse.Descartada, engine.classificar(NotificacaoBruta(), itau))
    }
}
