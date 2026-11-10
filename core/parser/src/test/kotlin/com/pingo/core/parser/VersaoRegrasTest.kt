package com.pingo.core.parser

import com.pingo.core.model.InstituicaoFinanceira.NUBANK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VersaoRegrasTest {
    @Test
    fun `compila a versao com instituicao, numero e padroes`() {
        val versao = VersaoRegras.compilar(regraParser(NUBANK, versao = 4, padroes = PADRAO_VALIDO))

        assertEquals(NUBANK, versao.instituicao)
        assertEquals(4, versao.versao)
    }

    @Test
    fun `rejeita padrao transacional sem extracao de valor`() {
        val padroes = """[{"id":"compra","tipoLancamento":"COMPRA","direcao":"DESPESA","ancoras":["Compra"]}]"""

        assertThrows(IllegalArgumentException::class.java) { VersaoRegras.compilar(regraParser(NUBANK, 1, padroes)) }
    }

    @Test
    fun `rejeita padrao transacional sem tipo ou sem direcao`() {
        val semTipo = """[{"id":"c","direcao":"DESPESA","ancoras":["Compra"],"extracoes":{"valor":"R$ {valor}"}}]"""
        val semDirecao = """[{"id":"c","tipoLancamento":"COMPRA","ancoras":["Compra"],"extracoes":{"valor":"R$ {valor}"}}]"""

        assertThrows(IllegalArgumentException::class.java) { VersaoRegras.compilar(regraParser(NUBANK, 1, semTipo)) }
        assertThrows(IllegalArgumentException::class.java) { VersaoRegras.compilar(regraParser(NUBANK, 1, semDirecao)) }
    }

    @Test
    fun `rejeita padrao sem ancoras`() {
        val padroes = """[{"id":"c","tipoLancamento":"COMPRA","direcao":"DESPESA","extracoes":{"valor":"R$ {valor}"}}]"""

        assertThrows(IllegalArgumentException::class.java) { VersaoRegras.compilar(regraParser(NUBANK, 1, padroes)) }
    }

    @Test
    fun `rejeita template que nao contem o placeholder do proprio campo`() {
        val padroes = """[{"id":"c","tipoLancamento":"COMPRA","direcao":"DESPESA","ancoras":["Compra"],"extracoes":{"valor":"R$ 10"}}]"""

        assertThrows(IllegalArgumentException::class.java) { VersaoRegras.compilar(regraParser(NUBANK, 1, padroes)) }
    }

    @Test
    fun `rejeita placeholder repetido no mesmo template`() {
        val padroes = """
            [{"id":"c","tipoLancamento":"COMPRA","direcao":"DESPESA","ancoras":["Compra"],
            "extracoes":{"valor":"{valor} R$ {valor}"}}]
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) { VersaoRegras.compilar(regraParser(NUBANK, 1, padroes)) }
    }

    @Test
    fun `aceita padrao de ruido sem tipo nem direcao`() {
        val padroes = """[{"id":"promo","ancoras":["Aproveite"],"ruido":true}]"""

        assertEquals(NUBANK, VersaoRegras.compilar(regraParser(NUBANK, 1, padroes)).instituicao)
    }

    @Test
    fun `ignora extracoes de campos que o motor nao conhece`() {
        val padroes = """
            [{"id":"c","tipoLancamento":"COMPRA","direcao":"DESPESA","ancoras":["Compra"],
            "extracoes":{"valor":"R$ {valor}","moeda":"USD {moeda}"}}]
        """.trimIndent()

        assertEquals(1, VersaoRegras.compilar(regraParser(NUBANK, 1, padroes)).padroes.size)
    }
}
