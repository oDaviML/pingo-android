package com.pingo.feature.home.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.designsystem.component.AbaInferior
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.StatusFatura
import com.pingo.core.model.organizacao.EstadoBlocoFinanceiro
import com.pingo.core.model.organizacao.FaturaAbertaInicio
import com.pingo.core.model.organizacao.SaldoVoucherInicio
import com.pingo.core.model.voucher.UltimaRecargaVoucher
import com.pingo.core.notifications.SituacaoDaCaptura
import com.pingo.core.ui.Valores
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
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
    fun desconexaoComAcessoELeituraRecenteMostraAvisoHonesto() {
        var correcoes = 0

        mostrar(
            estadoPadrao().copy(situacao = SituacaoDaCaptura.PARADA, motivo = MotivoParada.DESCONECTADO),
            onOpenCaptura = { correcoes++ },
        )

        composeRule.onNodeWithText("Captura parada").assertIsDisplayed()
        composeRule.onNodeWithText("O Android encerrou o serviço de captura. Toque em Corrigir para reconectar.")
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("Nenhuma notificação lida há 2 dias", substring = true).assertCountEquals(0)

        composeRule.onNodeWithText("Corrigir").performClick()

        assertEquals(1, correcoes)
    }

    @Test
    fun permissaoRevogadaNoDiagnosticoMostraTextoDeSemAcesso() {
        mostrar(
            estadoPadrao().copy(
                capturaAutomatica = true,
                situacao = SituacaoDaCaptura.PARADA,
                motivo = MotivoParada.PERMISSAO_REVOGADA,
            ),
        )

        composeRule.onNodeWithText("Captura parada").assertIsDisplayed()
        composeRule.onNodeWithText("Sem acesso às notificações, o Pingo está no modo manual.").assertIsDisplayed()
        composeRule.onAllNodesWithText("Nenhuma notificação lida há 2 dias", substring = true).assertCountEquals(0)
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

    @Test
    fun barraInferiorOcultaNaoRenderizaAtalhosDuplicados() {
        mostrar(estadoPadrao(), mostrarBarraInferior = false)

        composeRule.onAllNodesWithTag("aba-inicio").assertCountEquals(0)
        composeRule.onAllNodesWithTag("novo-lancamento").assertCountEquals(0)
    }

    @Test
    fun blocosDeFaturasEVouchersPopuladosExibemCardsENavegam() {
        val abasSelecionadas = mutableListOf<AbaInferior>()
        val faturasAbertas = mutableListOf<String>()
        val fatura = faturaDeTeste()
        val voucher = voucherDeTeste()

        mostrar(
            estadoPadrao().copy(
                faturasAbertas = EstadoBlocoFinanceiro.Sucesso(listOf(fatura)),
                saldosVoucher = EstadoBlocoFinanceiro.Sucesso(listOf(voucher)),
            ),
            onSelecionarAba = { abasSelecionadas.add(it) },
            onAbrirFaturas = { faturasAbertas.add(it) },
        )

        composeRule.onNodeWithText("Faturas de crédito").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Nubank · crédito").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(Valores.formatar(128450)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Vence em 19 dias").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Saldos de benefícios").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("VR · Refeição").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(Valores.formatar(48620)).performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("fatura-inicio-${fatura.instrumentoPublicId}").performScrollTo().performClick()
        assertEquals(listOf(fatura.instrumentoPublicId), faturasAbertas)

        composeRule.onNodeWithTag("voucher-inicio-${voucher.instrumentoPublicId}").performScrollTo().performClick()
        assertEquals(listOf(AbaInferior.CONTAS), abasSelecionadas)

        composeRule.onAllNodesWithText("Ver contas")[0].performScrollTo().performClick()
        assertEquals(listOf(AbaInferior.CONTAS, AbaInferior.CONTAS), abasSelecionadas)
    }

    @Test
    fun blocosVaziosSaoOmitidosCompletamente() {
        mostrar(
            estadoPadrao().copy(
                faturasAbertas = EstadoBlocoFinanceiro.Sucesso(emptyList()),
                saldosVoucher = EstadoBlocoFinanceiro.Sucesso(emptyList()),
            ),
        )

        composeRule.onAllNodesWithText("Faturas de crédito").assertCountEquals(0)
        composeRule.onAllNodesWithText("Saldos de benefícios").assertCountEquals(0)
        composeRule.onAllNodesWithText("Ver contas").assertCountEquals(0)
    }

    @Test
    fun erroEmFaturasNaoAfetaVouchersENaoExibeZero() {
        val voucher = voucherDeTeste()

        mostrar(
            estadoPadrao().copy(
                faturasAbertas = EstadoBlocoFinanceiro.Erro,
                saldosVoucher = EstadoBlocoFinanceiro.Sucesso(listOf(voucher)),
            ),
        )

        composeRule.onNodeWithTag("erro-faturas-inicio").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Não foi possível carregar as faturas").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("voucher-inicio-${voucher.instrumentoPublicId}").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(Valores.formatar(48620)).performScrollTo().assertIsDisplayed()
        composeRule.onAllNodes(hasText("R$ 0,00") and hasAnyAncestor(hasTestTag("secao-faturas-credito"))).assertCountEquals(0)
    }

    @Test
    fun erroEmVouchersNaoAfetaFaturasENaoExibeZero() {
        val fatura = faturaDeTeste()

        mostrar(
            estadoPadrao().copy(
                faturasAbertas = EstadoBlocoFinanceiro.Sucesso(listOf(fatura)),
                saldosVoucher = EstadoBlocoFinanceiro.Erro,
            ),
        )

        composeRule.onNodeWithTag("erro-vouchers-inicio").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Não foi possível carregar os saldos de benefícios").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("fatura-inicio-${fatura.instrumentoPublicId}").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(Valores.formatar(128450)).performScrollTo().assertIsDisplayed()
        composeRule.onAllNodes(hasText("R$ 0,00") and hasAnyAncestor(hasTestTag("secao-saldos-beneficios"))).assertCountEquals(0)
    }

    private fun mostrar(
        state: HomeUiState,
        onOpenCaptura: () -> Unit = {},
        onOpenRevisao: () -> Unit = {},
        onOpenNovo: () -> Unit = {},
        onMesAnterior: () -> Unit = {},
        onMesProximo: () -> Unit = {},
        onSelecionarAba: (AbaInferior) -> Unit = {},
        onAbrirFaturas: (String) -> Unit = { onSelecionarAba(AbaInferior.CONTAS) },
        mostrarBarraInferior: Boolean = true,
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
                    onSelecionarAba = onSelecionarAba,
                    mostrarBarraInferior = mostrarBarraInferior,
                    onAbrirFaturas = onAbrirFaturas,
                )
            }
        }
    }

    private fun faturaDeTeste(): FaturaAbertaInicio = FaturaAbertaInicio(
        instrumentoPublicId = "cartao-nu",
        contaPublicId = "conta-nu",
        instituicao = InstituicaoFinanceira.NUBANK,
        apelidoCartao = "Nubank · crédito",
        ultimosDigitos = "4321",
        faturaPublicId = "fatura-1",
        competencia = LocalDate.of(2026, 11, 1),
        dataFechamento = LocalDate.of(2026, 11, 3),
        dataVencimento = LocalDate.of(2026, 11, 10),
        valorCentavos = 128450,
        diasParaVencimento = 19,
        status = StatusFatura.ABERTA,
    )

    private fun voucherDeTeste(): SaldoVoucherInicio = SaldoVoucherInicio(
        instrumentoPublicId = "voucher-vr",
        contaPublicId = "conta-caju",
        instituicao = InstituicaoFinanceira.CAJU,
        apelido = "Caju · benefício",
        beneficio = BeneficioVoucher.VR,
        saldoCentavos = 48620,
        ultimaRecarga = UltimaRecargaVoucher(LocalDate.of(2026, 10, 1), 80000, false),
    )

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
