package com.pingo.feature.accounts.api

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountsRouteTest {
    @Test
    fun `rotas e helpers produzem caminhos esperados`() {
        assertEquals("accounts", AccountsRoute.ROUTE)
        assertEquals("contaId", AccountsRoute.ARG_CONTA_ID)
        assertEquals("instrumentoId", AccountsRoute.ARG_INSTRUMENTO_ID)
        assertEquals("accounts/nova", AccountsRoute.NOVA_ROUTE)
        assertEquals("accounts/editar/{contaId}", AccountsRoute.EDITAR_ROUTE)
        assertEquals("accounts/faturas/{instrumentoId}", AccountsRoute.FATURAS_ROUTE)
        assertEquals("accounts/editar/conta-1", AccountsRoute.editar("conta-1"))
        assertEquals("accounts/faturas/inst-1", AccountsRoute.faturas("inst-1"))
    }
}
