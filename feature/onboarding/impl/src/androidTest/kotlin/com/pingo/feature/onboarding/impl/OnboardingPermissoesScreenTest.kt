package com.pingo.feature.onboarding.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.datastore.PermissaoDoPingo
import com.pingo.core.datastore.PermissoesDataSource
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.notifications.AcessoNotificacoes
import com.pingo.core.notifications.NotificacoesDoSistema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingPermissoesScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val acesso = FakeAcessoNotificacoes(concedido = false)
    private val permissoes = FakePermissoesDataSource()

    @Test
    fun aoVoltarDosAjustesComAcessoConcedidoMostraQueEstaTudoCerto() {
        val viewModel = viewModel()

        composeRule.setContent {
            PingoTheme {
                OnboardingPermissoesScreen(
                    onConcluir = {},
                    viewModel = viewModel,
                    permissaoDeNotificacao = FakePermissaoDeNotificacao(concedida = false),
                )
            }
        }

        composeRule.onNodeWithText("Abrir ajustes").assertIsDisplayed()

        acesso.concedido = true
        recriarResumo()

        composeRule.onNodeWithText("Tudo certo, o Pingo já pode ler as notificações dos bancos.").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").assertIsDisplayed()
    }

    @Test
    fun aoVoltarDosAjustesSemConcederMostraOModoManual() {
        val viewModel = viewModel()
        viewModel.onEvent(OnboardingPermissoesEvent.AbrirAjustesDeNotificacoes)

        composeRule.setContent {
            PingoTheme {
                OnboardingPermissoesScreen(
                    onConcluir = {},
                    viewModel = viewModel,
                    permissaoDeNotificacao = FakePermissaoDeNotificacao(concedida = false),
                )
            }
        }

        recriarResumo()

        composeRule.onNodeWithText("Tudo bem, você decide").assertIsDisplayed()
        composeRule.onNodeWithText("Sem acesso às notificações").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar só no manual").assertIsDisplayed()
    }

    private fun recriarResumo() {
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
    }

    private fun viewModel() = OnboardingPermissoesViewModel(
        acessoNotificacoes = acesso,
        notificacoesDoSistema = FakeNotificacoesDoSistema(habilitadas = false),
        permissoes = permissoes,
        fabricanteDoAparelho = FakeFabricanteDoAparelho(FabricanteDoSistema.SAMSUNG),
        bateriaDoAparelho = FakeBateriaDoAparelho(semRestricao = false),
    )

    private class FakeAcessoNotificacoes(var concedido: Boolean) : AcessoNotificacoes {
        override fun concedido(): Boolean = concedido
    }

    private class FakeNotificacoesDoSistema(private val habilitadas: Boolean) : NotificacoesDoSistema {
        override fun habilitadas(): Boolean = habilitadas
    }

    private class FakePermissoesDataSource : PermissoesDataSource {
        private val acesso = MutableStateFlow(PermissaoDoPingo.NAO_PERGUNTADA)
        private val notificacoes = MutableStateFlow(PermissaoDoPingo.NAO_PERGUNTADA)

        override val acessoAsNotificacoes: Flow<PermissaoDoPingo> = acesso
        override val notificacoesDoPingo: Flow<PermissaoDoPingo> = notificacoes

        override suspend fun registrarAcessoAsNotificacoes(permissao: PermissaoDoPingo) {
            acesso.value = permissao
        }

        override suspend fun registrarNotificacoesDoPingo(permissao: PermissaoDoPingo) {
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

    private class FakePermissaoDeNotificacao(private val concedida: Boolean) : PermissaoDeNotificacao {
        override fun concedida(): Boolean = concedida

        override fun solicitar(aoResultado: (Boolean) -> Unit) = aoResultado(concedida)
    }
}
