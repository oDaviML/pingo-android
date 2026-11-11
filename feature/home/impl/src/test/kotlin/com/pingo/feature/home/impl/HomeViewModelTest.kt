package com.pingo.feature.home.impl

import com.pingo.core.notifications.AcessoNotificacoes
import com.pingo.core.notifications.DiagnosticoDaCaptura
import com.pingo.core.notifications.SaudeDaCaptura
import com.pingo.core.notifications.SituacaoDaCaptura
import com.pingo.core.testing.TestDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.Instant

class HomeViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule(UnconfinedTestDispatcher())

    @Test
    fun `sem acesso a captura aparece parada`() {
        val viewModel = HomeViewModel(FakeAcessoNotificacoes(concedido = false), FakeSaudeDaCaptura())

        assertFalse(viewModel.uiState.value.capturaAutomatica)
        assertTrue(viewModel.uiState.value.capturaParada)
    }

    @Test
    fun `atualizar reflete a concessao do acesso`() {
        val acesso = FakeAcessoNotificacoes(concedido = false)
        val viewModel = HomeViewModel(acesso, FakeSaudeDaCaptura())

        acesso.concedido = true
        viewModel.atualizarCaptura()

        assertTrue(viewModel.uiState.value.capturaAutomatica)
        assertFalse(viewModel.uiState.value.capturaParada)
    }

    @Test
    fun `sem leitura ha quarenta e oito horas aparece captura parada`() {
        val saude = FakeSaudeDaCaptura(
            diagnostico = DiagnosticoDaCaptura(
                situacao = SituacaoDaCaptura.SEM_LEITURA,
                semLeituraDesde = Instant.now().minus(Duration.ofHours(49)),
            ),
        )

        val viewModel = HomeViewModel(FakeAcessoNotificacoes(concedido = true), saude)

        assertTrue(viewModel.uiState.value.capturaParada)
        assertTrue(viewModel.uiState.value.semLeituraHa48h)
    }

    @Test
    fun `leitura recente volta a esconder o aviso`() {
        val saude = FakeSaudeDaCaptura(
            diagnostico = DiagnosticoDaCaptura(
                situacao = SituacaoDaCaptura.SEM_LEITURA,
                semLeituraDesde = Instant.now().minus(Duration.ofHours(49)),
            ),
        )
        val viewModel = HomeViewModel(FakeAcessoNotificacoes(concedido = true), saude)

        saude.diagnostico.value = DiagnosticoDaCaptura(situacao = SituacaoDaCaptura.ATIVA)
        viewModel.atualizarCaptura()

        assertFalse(viewModel.uiState.value.semLeituraHa48h)
        assertFalse(viewModel.uiState.value.capturaParada)
    }

    @Test
    fun `estado parado com silencio de quarenta e oito horas aparece na inicio`() {
        val saude = FakeSaudeDaCaptura(
            diagnostico = DiagnosticoDaCaptura(
                situacao = SituacaoDaCaptura.PARADA,
                semLeituraDesde = Instant.now().minus(Duration.ofHours(49)),
            ),
        )

        val viewModel = HomeViewModel(FakeAcessoNotificacoes(concedido = true), saude)

        assertTrue(viewModel.uiState.value.semLeituraHa48h)
        assertTrue(viewModel.uiState.value.capturaParada)
    }

    private class FakeAcessoNotificacoes(var concedido: Boolean) : AcessoNotificacoes {
        override fun concedido(): Boolean = concedido
    }

    private class FakeSaudeDaCaptura(diagnostico: DiagnosticoDaCaptura = DiagnosticoDaCaptura(situacao = SituacaoDaCaptura.ATIVA)) :
        SaudeDaCaptura {
        val diagnostico = MutableStateFlow(diagnostico)

        override fun observar(): Flow<DiagnosticoDaCaptura> = diagnostico

        override suspend fun atual(): DiagnosticoDaCaptura = diagnostico.value
    }
}
