package com.pingo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValorEmReaisTest {
    @Test
    fun `converte reais inteiros para centavos`() {
        assertEquals(50000L, reaisParaCentavos("500"))
        assertEquals(0L, reaisParaCentavos("0"))
    }

    @Test
    fun `converte valores com virgula ou ponto`() {
        assertEquals(4290L, reaisParaCentavos("42,90"))
        assertEquals(4290L, reaisParaCentavos("42.90"))
        assertEquals(123456L, reaisParaCentavos("1.234,56"))
        assertEquals(120L, reaisParaCentavos("1,20"))
    }

    @Test
    fun `aceita prefixo de real e espacos`() {
        assertEquals(4290L, reaisParaCentavos(" R$ 42,90 "))
    }

    @Test
    fun `recusa texto vazio negativo ou com mais de duas casas`() {
        assertNull(reaisParaCentavos(""))
        assertNull(reaisParaCentavos("   "))
        assertNull(reaisParaCentavos("-1,00"))
        assertNull(reaisParaCentavos("1,234"))
        assertNull(reaisParaCentavos("abc"))
    }
}
