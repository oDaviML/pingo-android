package com.pingo.feature.home.impl

import org.junit.Assert.assertEquals
import org.junit.Test

class CentavosAnimadosTest {
    @Test
    fun `interpolarCentavos preserva centavos inteiros acima da precisao de float`() {
        val origem = 25_000_000L
        val destino = 25_000_051L

        assertEquals(origem, interpolarCentavos(origem = origem, destino = destino, progresso = 0f))
        assertEquals(destino, interpolarCentavos(origem = origem, destino = destino, progresso = 1f))
        assertEquals(25_000_026L, interpolarCentavos(origem = origem, destino = destino, progresso = 0.5f))
    }

    @Test
    fun `interpolarCentavos funciona para saldos negativos altos`() {
        val origem = 0L
        val destino = -30_000_077L

        assertEquals(destino, interpolarCentavos(origem = origem, destino = destino, progresso = 1f))
    }
}
