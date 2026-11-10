package com.pingo.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EstabelecimentoNormalizadoTest {
    @Test
    fun `minusculas e espacos repetidos viram um unico espaco`() {
        assertEquals("uber eats", EstabelecimentoNormalizado.de("  UBER    EATS  "))
    }

    @Test
    fun `espaco antes ou depois do asterisco nao muda o estabelecimento`() {
        assertEquals("dl*uberrides", EstabelecimentoNormalizado.de("DL *UberRides"))
        assertEquals("dl*uberrides", EstabelecimentoNormalizado.de("DL*UberRides"))
        assertEquals("contabo*hold only", EstabelecimentoNormalizado.de("CONTABO* HOLD ONLY"))
    }

    @Test
    fun `identificador numerico depois do asterisco sai do estabelecimento`() {
        assertEquals("claro", EstabelecimentoNormalizado.de("CLARO *31987905690"))
        assertEquals("claro", EstabelecimentoNormalizado.de("Claro *1234 "))
    }

    @Test
    fun `asterisco seguido de texto ou de outro bloco numerico permanece`() {
        assertEquals("dl*99 ride", EstabelecimentoNormalizado.de("DL*99 RIDE"))
        assertEquals("dl*99 99", EstabelecimentoNormalizado.de("DL*99 99"))
        assertEquals("paygo*empada", EstabelecimentoNormalizado.de("Paygo*empada"))
    }

    @Test
    fun `espaco nao quebravel conta como espaco comum`() {
        assertEquals("factory ai", EstabelecimentoNormalizado.de("FACTORY\u00A0AI"))
    }

    @Test
    fun `texto vazio ou so de identificador nao tem estabelecimento`() {
        assertNull(EstabelecimentoNormalizado.de(null))
        assertNull(EstabelecimentoNormalizado.de("   "))
        assertNull(EstabelecimentoNormalizado.de("*1234"))
    }
}
