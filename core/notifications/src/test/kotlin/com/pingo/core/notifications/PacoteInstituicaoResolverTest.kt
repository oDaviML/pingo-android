package com.pingo.core.notifications

import com.pingo.core.model.InstituicaoFinanceira
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PacoteInstituicaoResolverTest {
    private val resolver = PacoteInstituicaoResolver()

    @Test
    fun resolveOsPacotesDasCincoInstituicoes() {
        val esperado = mapOf(
            "com.nu.production" to InstituicaoFinanceira.NUBANK,
            "br.com.intermedium" to InstituicaoFinanceira.INTER,
            "com.itau" to InstituicaoFinanceira.ITAU,
            "br.com.itau.internet" to InstituicaoFinanceira.ITAU,
            "com.mercadopago.wallet" to InstituicaoFinanceira.MERCADO_PAGO,
            "com.caju.employeeApp" to InstituicaoFinanceira.CAJU,
        )

        esperado.forEach { (pacote, instituicao) -> assertEquals(instituicao, resolver.resolver(pacote, tag = null)) }
        assertEquals(6, esperado.size)
    }

    @Test
    fun ignoraPacotesDesconhecidos() {
        assertNull(resolver.resolver("com.android.shell", tag = "pingo-teste-ITAU"))
        assertNull(resolver.resolver("com.pingo", tag = null))
        assertNull(resolver.resolver("com.app.desconhecido", tag = null))
    }
}
