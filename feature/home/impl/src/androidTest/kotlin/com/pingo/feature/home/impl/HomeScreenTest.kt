package com.pingo.feature.home.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.ui.Valores
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun semAcessoMostraAvisoFixoDeCapturaParada() {
        var correcoes = 0

        mostrar(HomeUiState(capturaAutomatica = false, mes = MES), onOpenCaptura = { correcoes++ })

        composeRule.onNodeWithText("Captura parada").assertIsDisplayed()
        composeRule.onNodeWithText("Sem acesso às notificações, o Pingo está no modo manual.").assertIsDisplayed()

        composeRule.onNodeWithText("Corrigir").performClick()

        assertEquals(1, correcoes)
    }

    @Test
    fun botaoNovoLancamentoDaInicioAbreOTerceiroPasso() {
        var aberturas = 0

        mostrar(estadoPadrao(), onOpenNovo = { aberturas++ })

        composeRule.onNodeWithTag("novo-lancamento").performClick()

        assertEquals(1, aberturas)
    }

    @Test
    fun comAcessoAtivoNaoMostraOAviso() {
        mostrar(estadoPadrao())

        composeRule.onAllNodesWithText("Captura parada").assertCountEquals(0)
    }

    @Test
    fun semLeituraHaDoisDiasMostraOAvisoDeCapturaParada() {
        var correcoes = 0

        mostrar(estadoPadrao().copy(semLeituraHa48h = true), onOpenCaptura = { correcoes++ })

        composeRule.onNodeWithText("Captura parada").assertIsDisplayed()
        composeRule.onNodeWithText("Nenhuma notificação lida há 2 dias. O Pingo pode não estar vendo seus bancos.").assertIsDisplayed()

        composeRule.onNodeWithText("Corrigir").performClick()

        assertEquals(1, correcoes)
    }

    @Test
    fun avisoDeRevisaoMostraContagemENavegaParaARevisao() {
        var aberturas = 0

        mostrar(estadoPadrao().copy(pendentes = 3), onOpenRevisao = { aberturas++ })

        composeRule.onNodeWithText("3 lançamentos para revisar").assertIsDisplayed()
        composeRule.onNodeWithTag("aviso-revisao").performClick()

        assertEquals(1, aberturas)
    }

    @Test
    fun avisoDeRevisaoUsaSingularQuandoHaUmPendente() {
        mostrar(estadoPadrao().copy(pendentes = 1))

        composeRule.onNodeWithText("1 lançamento para revisar").assertIsDisplayed()
    }

    @Test
    fun semPendentesNaoMostraAvisoDeRevisao() {
        mostrar(estadoPadrao())

        composeRule.onAllNodesWithText("3 lançamentos para revisar").assertCountEquals(0)
        composeRule.onAllNodesWithText("1 lançamento para revisar").assertCountEquals(0)
    }

    @Test
    fun saldoDoMesUsaRn08EExibeReceitasEDespesas() {
        mostrar(
            estadoPadrao().copy(
                saldoCentavos = 248190,
                receitasCentavos = 620000,
                despesasCentavos = 371810,
            ),
        )

        composeRule.onNodeWithTag("saldo-do-mes").assertIsDisplayed()
        composeRule.onNodeWithText(Valores.formatar(248190)).assertIsDisplayed()
        composeRule.onNodeWithText("+ ${Valores.formatar(620000)}").assertIsDisplayed()
        composeRule.onNodeWithText("- ${Valores.formatar(371810)}").assertIsDisplayed()
    }

    @Test
    fun estadoVazioMostraNenhumPingoEAtalhoManual() {
        var manuais = 0

        mostrar(estadoPadrao().copy(vazia = true), onOpenNovo = { manuais++ })

        composeRule.onNodeWithTag("inicio-vazio").assertIsDisplayed()
        composeRule.onNodeWithText("Nenhum pingo ainda").assertIsDisplayed()

        composeRule.onNodeWithTag("lancar-manualmente").performClick()

        assertEquals(1, manuais)
    }

    @Test
    fun seletorDeMesNavegaPeloViewModel() {
        var anteriores = 0
        var proximos = 0

        mostrar(
            estadoPadrao().copy(podeVoltarMes = true, podeAvancarMes = true),
            onMesAnterior = { anteriores++ },
            onMesProximo = { proximos++ },
        )

        composeRule.onNodeWithText("Novembro 2026").assertIsDisplayed()
        composeRule.onNodeWithTag("mes-anterior").performClick()
        composeRule.onNodeWithTag("mes-proximo").performClick()

        assertEquals(1, anteriores)
        assertEquals(1, proximos)
    }

    private fun mostrar(
        state: HomeUiState,
        onOpenCaptura: () -> Unit = {},
        onOpenRevisao: () -> Unit = {},
        onOpenNovo: () -> Unit = {},
        onMesAnterior: () -> Unit = {},
        onMesProximo: () -> Unit = {},
    ) {
        composeRule.setContent {
            PingoTheme {
                HomeContent(
                    state = state,
                    onOpenSettings = {},
                    onOpenCaptura = onOpenCaptura,
                    onOpenRevisao = onOpenRevisao,
                    onOpenNovo = onOpenNovo,
                    onMesAnterior = onMesAnterior,
                    onMesProximo = onMesProximo,
                )
            }
        }
    }

    private fun estadoPadrao(): HomeUiState = HomeUiState(
        carregando = false,
        capturaAutomatica = true,
        mes = MES,
        saldoCentavos = 0,
        receitasCentavos = 0,
        despesasCentavos = 0,
        vazia = false,
    )

    private companion object {
        val MES: YearMonth = YearMonth.of(2026, 11)
    }
}
