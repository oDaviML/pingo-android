package com.pingo.feature.onboarding.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.TipoInstrumento
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingContasContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun mostraPassoSeteComOsBancosEAcaoDePular() {
        mostrar(OnboardingContasUiState())

        composeRule.onNodeWithText("Passo 7 de 8").assertIsDisplayed()
        composeRule.onNodeWithText("Quais bancos você usa?").assertIsDisplayed()
        listOf("Nubank", "Inter", "Itaú", "Mercado Pago", "Caju", "Outra").forEach { nome ->
            composeRule.onNodeWithText(nome).assertIsDisplayed()
        }
        composeRule.onNodeWithText("Pular").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").assertIsNotEnabled()
    }

    @Test
    fun escolherUmBancoMostraCamposDeContaSemFechamentoNemVencimento() {
        mostrar(OnboardingContasUiState(escolhas = listOf(escolhaPadrao(InstituicaoFinanceira.NUBANK))))

        composeRule.onNodeWithText("Apelido da conta").assertIsDisplayed()
        composeRule.onNodeWithText("Tipo de conta").assertIsDisplayed()
        composeRule.onNodeWithText("Corrente").assertIsDisplayed()
        composeRule.onNodeWithText("Benefício").assertIsDisplayed()
        composeRule.onNodeWithText("Crédito").assertIsDisplayed()
        composeRule.onNodeWithText("Débito").assertIsDisplayed()
        composeRule.onNodeWithText("Voucher").assertIsDisplayed()
        composeRule.onNodeWithText("Últimos 4 dígitos (opcional)").assertIsDisplayed()
        composeRule.onAllNodesWithText("Fechamento").assertCountEquals(0)
        composeRule.onAllNodesWithText("Vencimento").assertCountEquals(0)
    }

    @Test
    fun tocarNoBancoEmiteAlternarBanco() {
        val eventos = mutableListOf<OnboardingContasEvent>()
        mostrar(OnboardingContasUiState(), eventos)

        composeRule.onNodeWithText("Inter").performClick()

        assertEquals(listOf(OnboardingContasEvent.AlternarBanco(InstituicaoFinanceira.INTER)), eventos)
    }

    @Test
    fun continuarHabilitaComUmBancoEscolhidoEEmiteContinuar() {
        val eventos = mutableListOf<OnboardingContasEvent>()
        mostrar(OnboardingContasUiState(escolhas = listOf(escolhaPadrao(InstituicaoFinanceira.NUBANK))), eventos)

        composeRule.onNodeWithText("Continuar").assertIsEnabled().performClick()

        assertEquals(listOf(OnboardingContasEvent.Continuar), eventos)
    }

    @Test
    fun pularEmiteAPassagemDoPasso() {
        val eventos = mutableListOf<OnboardingContasEvent>()
        mostrar(OnboardingContasUiState(), eventos)

        composeRule.onNodeWithText("Pular").performClick()

        assertEquals(listOf(OnboardingContasEvent.Pular), eventos)
    }

    @Test
    fun marcarOutroInstrumentoEmiteAlternarInstrumento() {
        val eventos = mutableListOf<OnboardingContasEvent>()
        mostrar(OnboardingContasUiState(escolhas = listOf(escolhaPadrao(InstituicaoFinanceira.NUBANK))), eventos)

        composeRule.onNodeWithText("Débito").performClick()

        assertEquals(
            listOf(OnboardingContasEvent.AlternarInstrumento(InstituicaoFinanceira.NUBANK, TipoInstrumento.DEBITO)),
            eventos,
        )
    }

    @Test
    fun apelidoEmBrancoMostraErroEImpedeContinuar() {
        mostrar(
            OnboardingContasUiState(
                escolhas = listOf(escolhaPadrao(InstituicaoFinanceira.NUBANK).copy(apelido = "  ")),
            ),
        )

        composeRule.onNodeWithText("Use de 1 a 100 caracteres.").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").assertIsNotEnabled()
    }

    @Test
    fun ultimosDigitosIncompletosMostramErro() {
        val instrumentoIncompleto = InstrumentoEscolhido(TipoInstrumento.CREDITO, "Cartão de crédito", "12")
        mostrar(
            OnboardingContasUiState(
                escolhas = listOf(escolhaPadrao(InstituicaoFinanceira.NUBANK).copy(instrumentos = listOf(instrumentoIncompleto))),
            ),
        )

        composeRule.onNodeWithText("Use 4 números ou deixe em branco.").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").assertIsNotEnabled()
    }

    @Test
    fun falhaAoSalvarMostraMensagemEPermiteTentarDeNovo() {
        mostrar(
            OnboardingContasUiState(
                escolhas = listOf(escolhaPadrao(InstituicaoFinanceira.NUBANK)),
                salvamento = SalvamentoUiState.Error,
            ),
        )

        composeRule.onNodeWithText("Não foi possível salvar suas contas. Tente de novo.").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").assertIsEnabled()
    }

    private fun mostrar(state: OnboardingContasUiState, eventos: MutableList<OnboardingContasEvent> = mutableListOf()) {
        composeRule.setContent {
            PingoTheme {
                OnboardingContasContent(
                    state = state,
                    onEvent = { eventos.add(it) },
                )
            }
        }
    }

    private fun escolhaPadrao(instituicao: InstituicaoFinanceira): EscolhaBanco = BancoOferecido.de(instituicao).escolhaInicial()
}
