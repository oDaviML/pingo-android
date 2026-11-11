package com.pingo.feature.onboarding.impl

import com.pingo.core.notifications.FabricanteDoSistema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FabricanteDoSistemaTest {
    @Test
    fun `detecta samsung independente da caixa`() {
        assertEquals(FabricanteDoSistema.SAMSUNG, FabricanteDoSistema.de("samsung"))
        assertEquals(FabricanteDoSistema.SAMSUNG, FabricanteDoSistema.de("SAMSUNG"))
    }

    @Test
    fun `detecta xiaomi`() {
        assertEquals(FabricanteDoSistema.XIAOMI, FabricanteDoSistema.de("Xiaomi"))
    }

    @Test
    fun `detecta motorola`() {
        assertEquals(FabricanteDoSistema.MOTOROLA, FabricanteDoSistema.de("motorola"))
    }

    @Test
    fun `fabricante desconhecido cai nas instrucoes genericas`() {
        assertEquals(FabricanteDoSistema.OUTRO, FabricanteDoSistema.de("Google"))
        assertEquals(FabricanteDoSistema.OUTRO, FabricanteDoSistema.de(null))
        assertEquals(FabricanteDoSistema.OUTRO, FabricanteDoSistema.de(""))
    }

    @Test
    fun `instrucoes da samsung citam apps nunca suspensos`() {
        val instrucoes = InstrucoesPorFabricante.de(FabricanteDoSistema.SAMSUNG)

        assertEquals("Samsung · One UI", instrucoes.titulo)
        assertTrue(instrucoes.passos.any { it.contains("Apps nunca suspensos") })
        assertTrue(instrucoes.passos.any { it.contains("Sem restrições") })
    }

    @Test
    fun `instrucoes da xiaomi ensinam o inicio automatico`() {
        val instrucoes = InstrucoesPorFabricante.de(FabricanteDoSistema.XIAOMI)

        assertEquals("Xiaomi · MIUI e HyperOS", instrucoes.titulo)
        assertTrue(instrucoes.passos.any { it.contains("Início automático") })
        assertTrue(instrucoes.passos.any { it.contains("Sem restrições") })
    }

    @Test
    fun `instrucoes genericas valem para qualquer aparelho`() {
        val instrucoes = InstrucoesPorFabricante.de(FabricanteDoSistema.OUTRO)

        assertEquals("Instruções gerais", instrucoes.titulo)
        assertTrue(instrucoes.passos.any { it.contains("bateria") })
        assertTrue(instrucoes.passos.any { it.contains("início automático") })
    }
}
