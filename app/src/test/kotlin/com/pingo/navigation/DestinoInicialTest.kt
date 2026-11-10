package com.pingo.navigation

import com.pingo.core.auth.SessionState
import org.junit.Assert.assertEquals
import org.junit.Test

class DestinoInicialTest {
    @Test
    fun `sem sessao comeca no onboarding`() {
        val destino = destinoInicialPara(SessionState.LoggedOut, consentimentoRegistrado = false, contasConfiguradas = false)

        assertEquals(DestinoInicial.ONBOARDING, destino)
    }

    @Test
    fun `com sessao mas sem consentimento comeca no onboarding`() {
        val destino = destinoInicialPara(SessionState.LoggedIn, consentimentoRegistrado = false, contasConfiguradas = false)

        assertEquals(DestinoInicial.ONBOARDING, destino)
    }

    @Test
    fun `consentido e sem contas configuradas volta ao passo de contas`() {
        val destino = destinoInicialPara(SessionState.LoggedIn, consentimentoRegistrado = true, contasConfiguradas = false)

        assertEquals(DestinoInicial.CONTAS, destino)
    }

    @Test
    fun `consentido e com contas configuradas abre a home`() {
        val destino = destinoInicialPara(SessionState.LoggedIn, consentimentoRegistrado = true, contasConfiguradas = true)

        assertEquals(DestinoInicial.HOME, destino)
    }

    @Test
    fun `sessao encerrada volta ao onboarding mesmo com contas configuradas`() {
        val destino = destinoInicialPara(SessionState.LoggedOut, consentimentoRegistrado = true, contasConfiguradas = true)

        assertEquals(DestinoInicial.ONBOARDING, destino)
    }
}
