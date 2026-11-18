package com.pingo.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PingoLogosAndCurrencyMaskTest {
    @Test
    fun `todos os bancos suportados possuem logo vetorial e outra usa fallback`() {
        listOf(
            PingoInstitution.Nubank,
            PingoInstitution.Inter,
            PingoInstitution.Itau,
            PingoInstitution.MercadoPago,
            PingoInstitution.Caju,
        ).forEach { instituicao ->
            val logo = PingoInstitutionLogos.para(instituicao)
            assertNotNull("Logo ausente para $instituicao", logo)
            assertTrue("Logo sem paths para $instituicao", logo!!.root.size > 0)
        }
        assertNull(PingoInstitutionLogos.para(PingoInstitution.Outra))
    }

    @Test
    fun `mascara monetaria formata centavos da direita para esquerda e limpa ao apagar`() {
        var valor = ""
        valor = MascaraMonetaria.aplicarAlteracao(valor, "2")
        assertEquals("0,02", valor)

        valor = MascaraMonetaria.aplicarAlteracao(valor, "25")
        assertEquals("0,25", valor)

        valor = MascaraMonetaria.aplicarAlteracao(valor, "25000")
        assertEquals("250,00", valor)

        valor = MascaraMonetaria.aplicarAlteracao(valor, "123456")
        assertEquals("1.234,56", valor)

        valor = MascaraMonetaria.aplicarAlteracao(valor, "")
        assertEquals("", valor)
    }

    @Test
    fun `mascara monetaria aceita colagem com separadores e alternancia de sinal`() {
        assertEquals("600,00", MascaraMonetaria.aplicarAlteracao("", "600,00"))
        assertEquals("0,00", MascaraMonetaria.aplicarAlteracao("", "0"))
        assertEquals("-25,00", MascaraMonetaria.aplicarAlteracao("", "-25,00", permitirNegativo = true))
        assertEquals("25,00", MascaraMonetaria.alternarSinal("-25,00"))
        assertEquals("-25,00", MascaraMonetaria.alternarSinal("25,00"))
    }
}
