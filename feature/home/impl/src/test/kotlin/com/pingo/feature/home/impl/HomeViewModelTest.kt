package com.pingo.feature.home.impl

import com.pingo.core.notifications.AcessoNotificacoes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelTest {
    @Test
    fun `sem acesso a captura aparece parada`() {
        val viewModel = HomeViewModel(FakeAcessoNotificacoes(concedido = false))

        assertFalse(viewModel.uiState.value.capturaAutomatica)
    }

    @Test
    fun `atualizar reflete a concessao do acesso`() {
        val acesso = FakeAcessoNotificacoes(concedido = false)
        val viewModel = HomeViewModel(acesso)

        acesso.concedido = true
        viewModel.atualizarCaptura()

        assertTrue(viewModel.uiState.value.capturaAutomatica)
    }

    private class FakeAcessoNotificacoes(var concedido: Boolean) : AcessoNotificacoes {
        override fun concedido(): Boolean = concedido
    }
}
