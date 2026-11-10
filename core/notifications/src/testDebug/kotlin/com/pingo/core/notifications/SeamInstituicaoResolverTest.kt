package com.pingo.core.notifications

import com.pingo.core.model.InstituicaoFinanceira
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeamInstituicaoResolverTest {
    private val resolver = SeamInstituicaoResolver()

    @Test
    fun mantemOsPacotesDeProducao() {
        assertEquals(InstituicaoFinanceira.ITAU, resolver.resolver("com.itau", tag = null))
        assertEquals(InstituicaoFinanceira.NUBANK, resolver.resolver("com.nu.production", tag = null))
        assertEquals(InstituicaoFinanceira.CAJU, resolver.resolver("com.caju.employeeApp", tag = null))
    }

    @Test
    fun aceitaShellComTagDeTeste() {
        assertEquals(InstituicaoFinanceira.ITAU, resolver.resolver("com.android.shell", tag = "pingo-teste-ITAU"))
        assertEquals(InstituicaoFinanceira.NUBANK, resolver.resolver("com.android.shell", tag = "pingo-teste-NUBANK"))
        assertEquals(InstituicaoFinanceira.INTER, resolver.resolver("com.android.shell", tag = "pingo-teste-INTER"))
        assertEquals(
            InstituicaoFinanceira.MERCADO_PAGO,
            resolver.resolver("com.android.shell", tag = "pingo-teste-MERCADO_PAGO"),
        )
        assertEquals(
            InstituicaoFinanceira.MERCADO_PAGO,
            resolver.resolver("com.android.shell", tag = "pingo-teste-MERCADOPAGO"),
        )
        assertEquals(InstituicaoFinanceira.CAJU, resolver.resolver("com.android.shell", tag = "pingo-teste-CAJU-123"))
    }

    @Test
    fun aceitaPacoteDeTesteComTagDeTeste() {
        assertEquals(InstituicaoFinanceira.ITAU, resolver.resolver("com.pingo.test", tag = "pingo-teste-ITAU"))
        assertEquals(InstituicaoFinanceira.ITAU, resolver.resolver("com.pingo.core.notifications.test", tag = "pingo-teste-ITAU"))
    }

    @Test
    fun ignoraTagAusenteOuForaDaConvencao() {
        assertNull(resolver.resolver("com.android.shell", tag = null))
        assertNull(resolver.resolver("com.android.shell", tag = "pingo"))
        assertNull(resolver.resolver("com.android.shell", tag = "pingo-teste-"))
        assertNull(resolver.resolver("com.android.shell", tag = "pingo-teste-XPTO"))
        assertNull(resolver.resolver("com.android.shell", tag = "outra-tag-ITAU"))
    }

    @Test
    fun ignoraPacoteDesconhecidoMesmoComTagDeTeste() {
        assertNull(resolver.resolver("com.app.desconhecido", tag = "pingo-teste-ITAU"))
    }
}
