package com.pingo.core.parser

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PadroesRegraCodecTest {
    @Test
    fun `decodifica tipo direcao ancoras exclusoes extracoes e ruido`() {
        val padroes = PadroesRegraCodec.decodificar(PADROES_ITAU)

        val compra = padroes.first()
        assertEquals("itau-compra-aprovada", compra.id)
        assertEquals(TipoLancamentoPadrao.COMPRA, compra.tipoLancamento)
        assertEquals(DirecaoPadrao.DESPESA, compra.direcao)
        assertEquals(listOf("Compra aprovada de"), compra.ancoras)
        assertEquals(listOf("negada"), compra.exclusoes)
        assertEquals(
            mapOf("valor" to "Compra aprovada de R$ {valor}", "estabelecimento" to "em {estabelecimento} no dia"),
            compra.extracoes,
        )
        assertFalse(compra.ruido)
    }

    @Test
    fun `padrao de ruido nao exige tipo nem direcao`() {
        val promocao = PadroesRegraCodec.decodificar(PADROES_ITAU).last()

        assertTrue(promocao.ruido)
        assertNull(promocao.tipoLancamento)
        assertNull(promocao.direcao)
    }

    @Test
    fun `codificar e decodificar preservam os padroes`() {
        val padroes = PadroesRegraCodec.decodificar(PADROES_ITAU)

        assertEquals(padroes, PadroesRegraCodec.decodificar(PadroesRegraCodec.codificar(padroes)))
    }

    @Test
    fun `ignora campos desconhecidos para aceitar versoes futuras do schema`() {
        val padroes = PadroesRegraCodec.decodificar("""[{"id":"futuro","ancoras":["oi"],"campoNovo":{"x":1}}]""")

        assertEquals("futuro", padroes.single().id)
    }

    @Test
    fun `rejeita tipo de lancamento fora do schema`() {
        assertThrows(SerializationException::class.java) {
            PadroesRegraCodec.decodificar("""[{"id":"x","tipoLancamento":"SAQUE","ancoras":["oi"]}]""")
        }
    }
}

internal const val PADROES_ITAU = """
[
  {
    "id": "itau-compra-aprovada",
    "tipoLancamento": "COMPRA",
    "direcao": "DESPESA",
    "ancoras": ["Compra aprovada de"],
    "exclusoes": ["negada"],
    "extracoes": {
      "valor": "Compra aprovada de R$ {valor}",
      "estabelecimento": "em {estabelecimento} no dia"
    }
  },
  {
    "id": "itau-promocao",
    "ancoras": ["Aproveite"],
    "ruido": true
  }
]
"""
