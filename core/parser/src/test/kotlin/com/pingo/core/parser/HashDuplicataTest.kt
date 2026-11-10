package com.pingo.core.parser

import com.pingo.core.model.InstituicaoFinanceira
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class HashDuplicataTest {
    private val postTime = Instant.parse("2026-11-10T13:54:48Z")

    @Test
    fun `mesma instituicao valor estabelecimento e minuto geram o mesmo hash`() {
        val primeiro = HashDuplicata.de(InstituicaoFinanceira.ITAU, 43065, "davi", postTime)
        val repostado = HashDuplicata.de(InstituicaoFinanceira.ITAU, 43065, "davi", postTime.plusSeconds(11))

        assertEquals(primeiro, repostado)
    }

    @Test
    fun `minuto seguinte muda o hash`() {
        assertNotEquals(
            HashDuplicata.de(InstituicaoFinanceira.ITAU, 43065, "davi", postTime),
            HashDuplicata.de(InstituicaoFinanceira.ITAU, 43065, "davi", postTime.plusSeconds(60)),
        )
    }

    @Test
    fun `valor estabelecimento e instituicao participam do hash`() {
        val base = HashDuplicata.de(InstituicaoFinanceira.ITAU, 43065, "davi", postTime)

        assertNotEquals(base, HashDuplicata.de(InstituicaoFinanceira.ITAU, 43066, "davi", postTime))
        assertNotEquals(base, HashDuplicata.de(InstituicaoFinanceira.ITAU, 43065, "fulano", postTime))
        assertNotEquals(base, HashDuplicata.de(InstituicaoFinanceira.NUBANK, 43065, "davi", postTime))
    }

    @Test
    fun `estabelecimento ausente equivale a vazio`() {
        assertEquals(
            HashDuplicata.de(InstituicaoFinanceira.NUBANK, 4500, null, postTime),
            HashDuplicata.de(InstituicaoFinanceira.NUBANK, 4500, "", postTime),
        )
    }

    @Test
    fun `hash tem 64 caracteres hexadecimais`() {
        val hash = HashDuplicata.de(InstituicaoFinanceira.ITAU, 43065, "davi", postTime)

        assertEquals(64, hash.length)
        assertTrue(hash.all { caractere -> caractere in '0'..'9' || caractere in 'a'..'f' })
    }
}
