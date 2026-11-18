package com.pingo.feature.reports.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.designsystem.component.AbaInferior
import com.pingo.core.designsystem.component.chart.PingoComparisonRowUi
import com.pingo.core.designsystem.component.chart.PingoComparisonTrend
import com.pingo.core.designsystem.component.chart.PingoDonutSlice
import com.pingo.core.designsystem.component.chart.PingoIncomeExpensePoint
import com.pingo.core.designsystem.component.chart.PingoSubscriptionRowUi
import com.pingo.core.designsystem.component.chart.PingoSubscriptionSuggestionUi
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.assinatura.ChaveAssinatura
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class ReportsContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val outubro = YearMonth.of(2026, 10)

    @Test
    fun estadoCarregandoMostraSkeleton() {
        mostrar(ReportsUiState(carregando = true, mes = outubro, rotuloMes = "Outubro 2026"))

        composeRule.onNodeWithTag("relatorios-carregando").assertIsDisplayed()
        composeRule.onNodeWithTag("aba-RELATORIOS").assertIsSelected()
    }

    @Test
    fun estadoFalhaMostraErroEPermiteTentarDeNovo() {
        val eventos = mutableListOf<ReportsEvent>()
        mostrar(ReportsUiState(carregando = false, falha = true, mes = outubro, rotuloMes = "Outubro 2026"), eventos = eventos)

        composeRule.onNodeWithTag("relatorios-erro").assertIsDisplayed()
        composeRule.onNodeWithText("Tentar de novo").performClick()

        assertEquals(listOf(ReportsEvent.TentarDeNovo), eventos)
    }

    @Test
    fun mesVazioMostraEstadoSemDadosELancarManualmente() {
        var abriuNovo = false
        mostrar(
            state = ReportsUiState(
                carregando = false,
                mes = YearMonth.of(2026, 11),
                rotuloMes = "Novembro 2026",
                mesVazio = true,
            ),
            onOpenNovo = { abriuNovo = true },
        )

        composeRule.onNodeWithTag("relatorios-vazio").assertIsDisplayed()
        composeRule.onNodeWithText("Sem dados neste mês").assertIsDisplayed()
        composeRule.onNodeWithTag("relatorios-assinaturas-vazio").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Lançar manualmente").performScrollTo().performClick()

        assertTrue(abriuNovo)
    }

    @Test
    fun sugestoesSemConfirmadasMantemEstadoVazioDeAssinaturasConfirmadasVisivel() {
        mostrar(
            state = estadoPreenchido().copy(
                sugestoesAssinatura = listOf(sugestaoSpotify()),
                assinaturasConfirmadas = emptyList(),
            ),
        )

        composeRule.onNodeWithText("Spotify").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("relatorios-assinaturas-vazio").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun relatorioPreenchidoMostraDonutComparativoSemBaseEvolucaoEVazioDeAssinaturas() {
        var categoriaAberta: Pair<String, YearMonth>? = null
        mostrar(
            state = estadoPreenchido(),
            onAbrirCategoriaNoMes = { cat, mes -> categoriaAberta = cat to mes },
        )

        composeRule.onAllNodesWithText("Relatórios").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Outubro 2026").assertIsDisplayed()
        composeRule.onNodeWithText("Despesas por categoria").assertIsDisplayed()
        composeRule.onNodeWithText("R$ 3.718,00").assertIsDisplayed()
        composeRule.onNodeWithText("32%").assertIsDisplayed()
        composeRule.onNodeWithText("Comparado a setembro").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("sem base").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Receitas × despesas").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("relatorios-assinaturas-vazio").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("relatorios-categoria-cat-alim").performScrollTo().performClick()

        assertEquals("cat-alim" to outubro, categoriaAberta)
    }

    @Test
    fun temaEscuroRenderizaRelatorioEAssinaturas() {
        val state = estadoPreenchido().copy(
            sugestoesAssinatura = listOf(sugestaoSpotify()),
            assinaturasConfirmadas = listOf(confirmadaNetflix()),
            totalMensalAssinaturasCentavos = 4490L,
            totalMensalAssinaturasFormatado = "R$ 44,90",
        )
        mostrar(state = state, darkTheme = true)

        composeRule.onNodeWithText("Despesas por categoria").assertIsDisplayed()
        composeRule.onNodeWithText("Assinaturas").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Total mensal estimado").performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasText("R$ 44,90") and hasAnyAncestor(hasTestTag("relatorios-assinaturas-total")))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNode(hasTestTag("relatorios-assinatura-ass-1") and hasText("Netflix") and hasText("R$ 44,90"))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun sugestoesAssinaturasConfirmacaoRecusaECancelamentoEmitemEventos() {
        val eventos = mutableListOf<ReportsEvent>()
        val sugestao = sugestaoSpotify()
        val confirmada = confirmadaNetflix()
        mostrar(
            state = estadoPreenchido().copy(
                sugestoesAssinatura = listOf(sugestao),
                assinaturasConfirmadas = listOf(confirmada),
                totalMensalAssinaturasCentavos = 4490L,
                totalMensalAssinaturasFormatado = "R$ 44,90",
            ),
            eventos = eventos,
        )

        composeRule.onNodeWithText("Confirmar").performScrollTo().performClick()
        composeRule.onNodeWithText("Recusar").performScrollTo().performClick()
        composeRule.onNodeWithTag("relatorios-assinatura-ass-1").performScrollTo().performClick()

        assertEquals(
            listOf(
                ReportsEvent.ConfirmarSugestao(sugestao.chave),
                ReportsEvent.RecusarSugestao(sugestao.chave),
                ReportsEvent.SolicitarCancelamentoAssinatura("ass-1"),
            ),
            eventos,
        )
    }

    @Test
    fun dialogoDeCancelamentoDeAssinaturaConfirmaRemocao() {
        val eventos = mutableListOf<ReportsEvent>()
        val confirmada = confirmadaNetflix()
        mostrar(
            state = estadoPreenchido().copy(
                assinaturasConfirmadas = listOf(confirmada),
                confirmacaoCancelamento = confirmada,
            ),
            eventos = eventos,
        )

        composeRule.onNodeWithText("Cancelar assinatura?").assertIsDisplayed()
        composeRule.onNodeWithText("Remover assinatura").performClick()

        assertEquals(listOf(ReportsEvent.ConfirmarCancelamentoAssinatura), eventos)
    }

    private fun mostrar(
        state: ReportsUiState,
        eventos: MutableList<ReportsEvent> = mutableListOf(),
        onOpenNovo: (() -> Unit)? = null,
        onAbrirCategoriaNoMes: (String, YearMonth) -> Unit = { _, _ -> },
        darkTheme: Boolean = false,
    ) {
        composeRule.setContent {
            PingoTheme(darkTheme = darkTheme) {
                ReportsContent(
                    state = state,
                    onEvent = { eventos += it },
                    onSelecionarAba = { _: AbaInferior -> },
                    onOpenNovo = onOpenNovo,
                    onAbrirCategoriaNoMes = onAbrirCategoriaNoMes,
                )
            }
        }
    }

    private fun estadoPreenchido(): ReportsUiState = ReportsUiState(
        carregando = false,
        mes = outubro,
        rotuloMes = "Outubro 2026",
        podeAvancarMes = true,
        rotuloMesAnterior = "setembro",
        totalDespesasCentavos = 371_800L,
        totalDespesasFormatado = "R$ 3.718,00",
        totalReceitasCentavos = 500_000L,
        saldoMensalCentavos = 128_200L,
        fatiasDonut = listOf(
            PingoDonutSlice(id = "cat-alim", label = "Alimentação", amount = 119_000.0, percentage = 32),
            PingoDonutSlice(id = "cat-transp", label = "Transporte", amount = 63_200.0, percentage = 17),
        ),
        categorias = listOf(
            CategoriaRelatorioUi(
                categoriaPublicId = "cat-alim",
                nome = "Alimentação",
                valorCentavos = 119_000L,
                valorFormatado = "R$ 1.190,00",
                percentualInteiro = 32,
                subtitulo = "32% · ↑ 12% vs set",
                acessibilidade = "Alimentação, R$ 1.190,00, 32% das despesas",
            ),
            CategoriaRelatorioUi(
                categoriaPublicId = "cat-transp",
                nome = "Transporte",
                valorCentavos = 63_200L,
                valorFormatado = "R$ 632,00",
                percentualInteiro = 17,
                subtitulo = "17% · sem base",
                acessibilidade = "Transporte, R$ 632,00, 17% das despesas",
            ),
        ),
        comparativoGeral = ComparativoCategoriaUi(
            categoriaPublicId = null,
            row = PingoComparisonRowUi(
                icon = PingoIcons.PieChart,
                categoryName = "Despesas totais",
                subtitle = "R$ 3.718,00 · +R$ 200,00 vs set",
                trend = PingoComparisonTrend.Up,
                badgeText = "6%",
            ),
        ),
        comparativos = listOf(
            ComparativoCategoriaUi(
                categoriaPublicId = "cat-alim",
                row = PingoComparisonRowUi(
                    icon = PingoIcons.Utensils,
                    categoryName = "Alimentação",
                    subtitle = "R$ 1.190,00 · +R$ 127,00 vs set",
                    trend = PingoComparisonTrend.Up,
                    badgeText = "12%",
                ),
            ),
            ComparativoCategoriaUi(
                categoriaPublicId = "cat-transp",
                row = PingoComparisonRowUi(
                    icon = PingoIcons.CarFront,
                    categoryName = "Transporte",
                    subtitle = "R$ 632,00 · +R$ 632,00 vs set",
                    trend = PingoComparisonTrend.NoBase,
                    badgeText = "sem base",
                ),
            ),
        ),
        serieSeisMeses = (5L downTo 0L).map { d ->
            val mesPonto = outubro.minusMonths(d)
            PontoSerieUi(
                mes = mesPonto,
                point = PingoIncomeExpensePoint(
                    monthLabel = mesAbreviado(mesPonto),
                    income = 500_000.0,
                    expense = 371_800.0,
                    incomeFormatted = "R$ 5.000,00",
                    expenseFormatted = "R$ 3.718,00",
                    isHighlighted = mesPonto == outubro,
                ),
            )
        },
    )

    private fun sugestaoSpotify(): SugestaoAssinaturaUi = SugestaoAssinaturaUi(
        chave = ChaveAssinatura("inst-nu", "spotify"),
        chaveTexto = "inst-nu|spotify",
        card = PingoSubscriptionSuggestionUi(
            merchant = "Spotify",
            instrumentName = "Nubank · Roxinho 1234 · Próx. ≈ 12/11",
            amountFormatted = "R$ 21,90",
            tagLabel = "Parece assinatura · Mensal",
        ),
    )

    private fun confirmadaNetflix(): AssinaturaConfirmadaUi = AssinaturaConfirmadaUi(
        publicId = "ass-1",
        estabelecimento = "Netflix",
        row = PingoSubscriptionRowUi(
            title = "Netflix",
            subtitle = "Próxima cobrança ≈ 18/11 · Inter · Crédito 5678",
            amountFormatted = "R$ 44,90",
        ),
    )
}
