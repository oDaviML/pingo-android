package com.pingo.core.network.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparadorDeVersaoTest {
    @Test
    fun `compara versoes semanticas com e sem prefixo v`() {
        assertTrue(ComparadorDeVersao.ehMaisRecente("v0.3.0", "0.2.0"))
        assertTrue(ComparadorDeVersao.ehMaisRecente("0.2.1", "0.2.0"))
        assertTrue(ComparadorDeVersao.ehMaisRecente("1.0.0", "0.9.9"))
        assertFalse(ComparadorDeVersao.ehMaisRecente("v0.2.0", "0.2.0"))
        assertFalse(ComparadorDeVersao.ehMaisRecente("0.1.9", "0.2.0"))
    }

    @Test
    fun `considera pre release menor do que versao estavel`() {
        assertTrue(ComparadorDeVersao.ehMaisRecente("0.3.0", "0.3.0-beta.1"))
        assertFalse(ComparadorDeVersao.ehMaisRecente("0.3.0-beta.1", "0.3.0"))
        assertTrue(ComparadorDeVersao.ehMaisRecente("0.3.0-beta.2", "0.3.0-beta.1"))
    }

    @Test
    fun `rejeita versoes invalidas e normaliza prefixo`() {
        assertEquals("0.2.0", ComparadorDeVersao.normalizar("  v0.2.0 "))
        assertNull(ComparadorDeVersao.parse(""))
        assertNull(ComparadorDeVersao.parse("abc"))
        assertNull(ComparadorDeVersao.parse("1.2.3-"))
        assertFalse(ComparadorDeVersao.ehMaisRecente("invalida", "0.2.0"))
    }
}
