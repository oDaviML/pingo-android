package com.pingo.core.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TemplateExtracaoTest {
    @Test
    fun `placeholder de outro campo vira coringa e so o proprio campo e capturado`() {
        val regex = TemplateExtracao.regexDe(CampoExtracao.ESTABELECIMENTO, "R$ {valor} em {estabelecimento}")

        assertEquals("LOJA ALFA", regex.find("Compra de R$ 10,00 em LOJA ALFA")?.groupValues?.get(1))
    }

    @Test
    fun `captura de valor para antes do literal seguinte`() {
        val regex = TemplateExtracao.regexDe(CampoExtracao.VALOR, "R$ {valor} em")

        assertEquals("1.251,74", regex.find("compra de R$ 1.251,74 em LOJA")?.groupValues?.get(1))
    }

    @Test
    fun `literais com espaco casam com quebra de linha e ignoram caixa`() {
        val regex = TemplateExtracao.regexDe(CampoExtracao.DIGITOS, "cartao final {digitos}")

        assertEquals("4640", regex.find("CARTAO\nFINAL 4640")?.groupValues?.get(1))
    }

    @Test
    fun `rejeita template sem o placeholder do proprio campo`() {
        assertThrows(IllegalArgumentException::class.java) { TemplateExtracao.regexDe(CampoExtracao.VALOR, "R$ {outro}") }
    }

    @Test
    fun `rejeita placeholder do proprio campo repetido`() {
        assertThrows(IllegalArgumentException::class.java) { TemplateExtracao.regexDe(CampoExtracao.VALOR, "{valor} R$ {valor}") }
    }
}
