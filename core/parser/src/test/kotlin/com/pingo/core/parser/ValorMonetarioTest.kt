package com.pingo.core.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValorMonetarioTest {
    @Test
    fun `converte formatos brasileiros e decimal com ponto para centavos`() {
        val casos = mapOf(
            "37,90" to 3790L,
            "1.251,74" to 125174L,
            "1669,97" to 166997L,
            "119.23" to 11923L,
            "450,00" to 45000L,
            "37,5" to 3750L,
            "1.251" to 125100L,
            "9.600" to 960000L,
            "20.00" to 2000L,
        )

        casos.forEach { (valor, centavos) ->
            assertEquals("valor $valor", centavos, ValorMonetario.centavos(valor))
        }
    }

    @Test
    fun `texto que nao e um valor devolve nulo`() {
        assertNull(ValorMonetario.centavos("abc"))
        assertNull(ValorMonetario.centavos(""))
        assertNull(ValorMonetario.centavos("37,"))
    }
}
