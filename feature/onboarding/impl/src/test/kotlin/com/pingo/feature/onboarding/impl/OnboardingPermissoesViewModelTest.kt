package com.pingo.feature.onboarding.impl

import com.pingo.core.datastore.PermissaoDoPingo
import com.pingo.core.datastore.PermissoesDataSource
import com.pingo.core.notifications.AcessoNotificacoes
import com.pingo.core.notifications.NotificacoesDoSistema
import com.pingo.core.notifications.RevisaoPorNotificacaoConfigurada
import com.pingo.core.testing.TestDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingPermissoesViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule(UnconfinedTestDispatcher())

    private val acesso = FakeAcessoNotificacoes(concedido = false)
    private val notificacoesDoSistema = FakeNotificacoesDoSistema(habilitadas = true)
    private val permissoes = FakePermissoesDataSource()
    private val fabricante = FakeFabricanteDoAparelho(FabricanteDoSistema.XIAOMI)
    private val bateria = FakeBateriaDoAparelho(semRestricao = false)

    private fun viewModel() = OnboardingPermissoesViewModel(
        acessoNotificacoes = acesso,
        notificacoesDoSistema = notificacoesDoSistema,
        permissoes = permissoes,
        fabricanteDoAparelho = fabricante,
        bateriaDoAparelho = bateria,
    )

    @Test
    fun `inicia no passo 4 com o estado real do sistema e do fabricante`() = runTest {
        val estado = viewModel().uiState.value

        assertEquals(PassoDePermissoes.ACESSO_AS_NOTIFICACOES, estado.passo)
        assertEquals(8, estado.totalPassos)
        assertFalse(estado.acessoConcedido)
        assertTrue(estado.notificacoesHabilitadas)
        assertEquals(FabricanteDoSistema.XIAOMI, estado.fabricanteDetectado)
        assertEquals(FabricanteDoSistema.XIAOMI, estado.fabricanteSelecionado)
        assertEquals("Xiaomi · MIUI e HyperOS", estado.instrucoes.titulo)
        assertFalse(estado.semRestricaoDeBateria)
        assertFalse(estado.concluido)
    }

    @Test
    fun `negar acesso mostra o modo manual sem bloquear e registra a negacao`() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(OnboardingPermissoesEvent.NegarAcesso)

        assertTrue(viewModel.uiState.value.modoManual)
        assertEquals(PassoDePermissoes.ACESSO_AS_NOTIFICACOES, viewModel.uiState.value.passo)
        assertFalse(viewModel.uiState.value.concluido)

        viewModel.onEvent(OnboardingPermissoesEvent.ContinuarSemAcesso)

        assertEquals(PermissaoDoPingo.NEGADA, permissoes.acesso.value)
        assertEquals(PassoDePermissoes.NOTIFICACOES_DO_PINGO, viewModel.uiState.value.passo)
        assertTrue(viewModel.uiState.value.modoManual)
        assertFalse(viewModel.uiState.value.concluido)
    }

    @Test
    fun `voltar dos ajustes sem acesso mantem o modo manual`() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(OnboardingPermissoesEvent.AbrirAjustesDeNotificacoes)
        viewModel.atualizarEstadosDoSistema()

        assertTrue(viewModel.uiState.value.solicitouAjustes)
        assertTrue(viewModel.uiState.value.modoManual)
        assertFalse(viewModel.uiState.value.acessoConcedido)
        assertEquals(PermissaoDoPingo.NAO_PERGUNTADA, permissoes.acesso.value)
    }

    @Test
    fun `voltar dos ajustes com acesso concedido registra a permissao e sai do modo manual`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(OnboardingPermissoesEvent.NegarAcesso)

        acesso.concedido = true
        viewModel.onEvent(OnboardingPermissoesEvent.AbrirAjustesDeNotificacoes)
        viewModel.atualizarEstadosDoSistema()

        assertTrue(viewModel.uiState.value.acessoConcedido)
        assertFalse(viewModel.uiState.value.modoManual)
        assertEquals(PermissaoDoPingo.PERMITIDA, permissoes.acesso.value)
    }

    @Test
    fun `pular o acesso registra negacao e segue para o passo 5`() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(OnboardingPermissoesEvent.Pular)

        assertEquals(PermissaoDoPingo.NEGADA, permissoes.acesso.value)
        assertEquals(PassoDePermissoes.NOTIFICACOES_DO_PINGO, viewModel.uiState.value.passo)
    }

    @Test
    fun `negacao de notificacoes e registrada e desabilita a revisao sem quebrar o fluxo`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(OnboardingPermissoesEvent.ContinuarSemAcesso)

        viewModel.registrarNotificacoesDoSistema(habilitadas = false)

        assertEquals(PermissaoDoPingo.NEGADA, permissoes.notificacoes.value)
        assertEquals(PassoDePermissoes.BATERIA, viewModel.uiState.value.passo)
        assertFalse(viewModel.uiState.value.concluido)
        assertFalse(viewModel.uiState.value.notificacoesHabilitadas)
        assertFalse(RevisaoPorNotificacaoConfigurada(permissoes, notificacoesDoSistema).habilitada())
    }

    @Test
    fun `concessao de notificacoes registra a permissao e habilita a revisao`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(OnboardingPermissoesEvent.ContinuarSemAcesso)

        viewModel.registrarNotificacoesDoSistema(habilitadas = true)

        assertEquals(PermissaoDoPingo.PERMITIDA, permissoes.notificacoes.value)
        assertEquals(PassoDePermissoes.BATERIA, viewModel.uiState.value.passo)
        assertTrue(RevisaoPorNotificacaoConfigurada(permissoes, notificacoesDoSistema).habilitada())
    }

    @Test
    fun `pular as notificacoes registra que a permissao nao foi perguntada`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(OnboardingPermissoesEvent.Pular)

        viewModel.onEvent(OnboardingPermissoesEvent.PularNotificacoes)

        assertEquals(PermissaoDoPingo.NAO_PERGUNTADA, permissoes.notificacoes.value)
        assertEquals(PassoDePermissoes.BATERIA, viewModel.uiState.value.passo)
    }

    @Test
    fun `escolher fabricante troca as instrucoes exibidas`() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(OnboardingPermissoesEvent.SelecionarFabricante(FabricanteDoSistema.SAMSUNG))

        assertEquals(FabricanteDoSistema.SAMSUNG, viewModel.uiState.value.fabricanteSelecionado)
        assertEquals("Samsung · One UI", viewModel.uiState.value.instrucoes.titulo)
    }

    @Test
    fun `concluir o passo 6 finaliza as permissoes`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(OnboardingPermissoesEvent.Pular)
        viewModel.onEvent(OnboardingPermissoesEvent.PularNotificacoes)

        viewModel.onEvent(OnboardingPermissoesEvent.Pular)

        assertTrue(viewModel.uiState.value.concluido)
    }

    @Test
    fun `falha ao persistir nao impede a continuacao do onboarding`() = runTest {
        permissoes.falhar = true
        val viewModel = viewModel()

        viewModel.onEvent(OnboardingPermissoesEvent.ContinuarSemAcesso)

        assertEquals(PassoDePermissoes.NOTIFICACOES_DO_PINGO, viewModel.uiState.value.passo)
    }

    private class FakeAcessoNotificacoes(var concedido: Boolean) : AcessoNotificacoes {
        override fun concedido(): Boolean = concedido
    }

    private class FakeNotificacoesDoSistema(var habilitadas: Boolean) : NotificacoesDoSistema {
        override fun habilitadas(): Boolean = habilitadas
    }

    private class FakePermissoesDataSource : PermissoesDataSource {
        val acesso = MutableStateFlow(PermissaoDoPingo.NAO_PERGUNTADA)
        val notificacoes = MutableStateFlow(PermissaoDoPingo.NAO_PERGUNTADA)
        var falhar = false

        override val acessoAsNotificacoes: Flow<PermissaoDoPingo> = acesso
        override val notificacoesDoPingo: Flow<PermissaoDoPingo> = notificacoes

        override suspend fun registrarAcessoAsNotificacoes(permissao: PermissaoDoPingo) {
            if (falhar) {
                error("falha de persistencia")
            }
            acesso.value = permissao
        }

        override suspend fun registrarNotificacoesDoPingo(permissao: PermissaoDoPingo) {
            if (falhar) {
                error("falha de persistencia")
            }
            notificacoes.value = permissao
        }

        override suspend fun clear() = Unit
    }

    private class FakeFabricanteDoAparelho(private val fabricante: FabricanteDoSistema) : FabricanteDoAparelho {
        override fun fabricante(): FabricanteDoSistema = fabricante
    }

    private class FakeBateriaDoAparelho(private val semRestricao: Boolean) : BateriaDoAparelho {
        override fun semRestricaoDeBateria(): Boolean = semRestricao
    }
}
