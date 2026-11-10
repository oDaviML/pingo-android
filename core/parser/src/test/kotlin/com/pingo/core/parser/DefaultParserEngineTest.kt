package com.pingo.core.parser

import com.pingo.core.model.InstituicaoFinanceira
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultParserEngineTest {
    private val engine = DefaultParserEngine()
    private val regrasItau = versaoDe(PADROES_COMPRA_ITAU)
    private val regrasInter = versaoDe(PADROES_DEBITO_INTER, InstituicaoFinanceira.INTER)

    @Test
    fun `normaliza NBSP emoji e espacos repetidos antes de extrair`() {
        val notificacao = NotificacaoBruta(
            titulo = "Sua compra foi aprovada 💳",
            texto = "Compra\u00A0aprovada de\u00A0R$\u00A01.251,74 em   LOJA\u00A0 ALFA  no dia 10/11",
        )

        assertEquals(
            TransacaoExtraida("compra-aprovada", TipoLancamentoPadrao.COMPRA, DirecaoPadrao.DESPESA, 125174L, "LOJA ALFA", null),
            transacaoDe(engine.classificar(notificacao, regrasItau)),
        )
    }

    @Test
    fun `extrai valor com espaco comum ou NBSP e virgula decimal como 3790 centavos`() {
        assertValorExtraido("R$ 37,90", 3790L)
    }

    @Test
    fun `extrai valor com ponto de milhar e virgula decimal como 125174 centavos`() {
        assertValorExtraido("R$ 1.251,74", 125174L)
    }

    @Test
    fun `extrai valor sem separador de milhar como 166997 centavos`() {
        assertValorExtraido("R$1669,97", 166997L)
    }

    @Test
    fun `extrai valor com ponto como separador decimal como 11923 centavos`() {
        assertValorExtraido("R$119.23", 11923L)
    }

    @Test
    fun `ancoras ignoram caixa alta e baixa`() {
        val notificacao = NotificacaoBruta(texto = "COMPRA APROVADA DE R$ 5,00 EM LOJA NO DIA 01/01")

        assertEquals(500L, transacaoDe(engine.classificar(notificacao, regrasItau)).valorCentavos)
    }

    @Test
    fun `texto transacional sem padrao vira NaoEntendida sem excecao`() {
        val notificacao = NotificacaoBruta(titulo = "Pagamento", texto = "Você fez um estorno de R$ 10,00 na loja X")

        assertEquals(ResultadoParse.NaoEntendida, engine.classificar(notificacao, regrasItau))
    }

    @Test
    fun `valor ausente deixa a notificacao NaoEntendida`() {
        val notificacao = NotificacaoBruta(texto = "Compra aprovada em LOJA no dia 10/11")

        assertEquals(ResultadoParse.NaoEntendida, engine.classificar(notificacao, regrasItau))
    }

    @Test
    fun `notificacao sem conteudo e descartada antes da classificacao`() {
        assertEquals(ResultadoParse.Descartada, engine.classificar(NotificacaoBruta("", "", ""), regrasItau))
        assertEquals(ResultadoParse.Descartada, engine.classificar(NotificacaoBruta("\u00A0 ", "💳", null), regrasItau))
        assertEquals(ResultadoParse.Descartada, engine.classificar(NotificacaoBruta(), null))
    }

    @Test
    fun `titulo vazio e lido pelo texto da Inter`() {
        val textoInter = "Olá FULANO, você acaba de comprar no débito no DL*99 99 o valor de R$ 37,50."
        val notificacao = NotificacaoBruta(titulo = "", texto = textoInter, bigText = textoInter)

        assertEquals(
            TransacaoExtraida("inter-compra-debito", TipoLancamentoPadrao.COMPRA, DirecaoPadrao.DESPESA, 3750L, "DL*99 99", null),
            transacaoDe(engine.classificar(notificacao, regrasInter)),
        )
    }

    @Test
    fun `compra negada nao gera lancamento mesmo com a ancora presente`() {
        val notificacao = NotificacaoBruta(texto = "Compra aprovada de R$ 10,00 em LOJA no dia 10/11. Compra NEGADA.")

        assertEquals(ResultadoParse.NaoEntendida, engine.classificar(notificacao, regrasItau))
    }

    @Test
    fun `negada real do Itau nao gera lancamento`() {
        val notificacao = NotificacaoBruta(
            titulo = "Segurança Cartoes Itau",
            texto = "Olá, foi negada a compra no cartão final 4640 no valor de R$119.23 em FACTORY AI no dia 03/11.",
        )

        assertEquals(ResultadoParse.NaoEntendida, engine.classificar(notificacao, regrasItau))
    }

    @Test
    fun `padrao de ruido vira Ignorada e fica separado de NaoEntendida`() {
        val promocao = NotificacaoBruta(titulo = "Aproveite", texto = "Ganhe R$ 20 de cashback")
        val desconhecida = NotificacaoBruta(texto = "Olá, tudo bem?")

        assertEquals(ResultadoParse.Ignorada, engine.classificar(promocao, regrasItau))
        assertEquals(ResultadoParse.NaoEntendida, engine.classificar(desconhecida, regrasItau))
    }

    @Test
    fun `extrai os ultimos 4 digitos do cartao quando presentes`() {
        val notificacao = NotificacaoBruta(texto = "Compra aprovada de R$ 23,99 em LOJA para o cartão com final 9759.")

        assertEquals("9759", transacaoDe(engine.classificar(notificacao, regrasItau)).digitos)
    }

    @Test
    fun `sem digitos o campo fica nulo e o valor continua extraido`() {
        val notificacao = NotificacaoBruta(texto = "Compra aprovada de R$ 23,99 em LOJA no dia 10/11")

        val dados = transacaoDe(engine.classificar(notificacao, regrasItau))

        assertNull(dados.digitos)
        assertEquals(2399L, dados.valorCentavos)
    }

    @Test
    fun `usa o BigText quando o Texto vem vazio`() {
        val notificacao = NotificacaoBruta(titulo = null, texto = "", bigText = "Compra aprovada de R$ 5,00 em LOJA no dia 01/01")

        assertEquals(500L, transacaoDe(engine.classificar(notificacao, regrasItau)).valorCentavos)
    }

    @Test
    fun `o primeiro padrao aplicavel vence`() {
        val notificacao = NotificacaoBruta(texto = "Aproveite: compra aprovada de R$ 5,00 em LOJA no dia 01/01")

        assertEquals(500L, transacaoDe(engine.classificar(notificacao, regrasItau)).valorCentavos)
    }

    @Test
    fun `sem regras vigentes a notificacao vira NaoEntendida e nao quebra a captura`() {
        val notificacao = NotificacaoBruta(texto = "Compra aprovada de R$ 5,00 em LOJA no dia 01/01")

        assertEquals(ResultadoParse.NaoEntendida, engine.classificar(notificacao, null))
    }

    private fun assertValorExtraido(valorTexto: String, centavos: Long) {
        listOf(valorTexto, valorTexto.replace(" ", "\u00A0")).forEach { valor ->
            val notificacao = NotificacaoBruta(texto = "Compra aprovada de $valor em LOJA no dia 10/11")

            assertEquals("valor $valor", centavos, transacaoDe(engine.classificar(notificacao, regrasItau)).valorCentavos)
        }
    }

    private fun transacaoDe(resultado: ResultadoParse): TransacaoExtraida {
        assertTrue("esperada transação, obtido $resultado", resultado is ResultadoParse.Transacao)
        return (resultado as ResultadoParse.Transacao).dados
    }
}

private const val PADROES_COMPRA_ITAU = """
[
  {
    "id": "compra-aprovada",
    "tipoLancamento": "COMPRA",
    "direcao": "DESPESA",
    "ancoras": ["Compra aprovada"],
    "exclusoes": ["negada"],
    "extracoes": {
      "valor": "Compra aprovada de R$ {valor}",
      "estabelecimento": "em {estabelecimento} no dia",
      "digitos": "cartão com final {digitos}"
    }
  },
  {
    "id": "promocao",
    "ancoras": ["Aproveite"],
    "ruido": true
  }
]
"""

private const val PADROES_DEBITO_INTER = """
[
  {
    "id": "inter-compra-debito",
    "tipoLancamento": "COMPRA",
    "direcao": "DESPESA",
    "ancoras": ["você acaba de comprar no débito"],
    "exclusoes": ["negada"],
    "extracoes": {
      "valor": "o valor de R$ {valor}",
      "estabelecimento": "comprar no débito no {estabelecimento} o valor"
    }
  }
]
"""
