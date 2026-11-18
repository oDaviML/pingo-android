package com.pingo.feature.accounts.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.Categoria
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusFatura
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.fatura.CicloDeFaturaDetalhado
import com.pingo.core.model.fatura.TipoCicloFatura
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class FaturasContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun estadoCarregandoErroENaoEncontradaExibemEstadosCorretos() {
        var tentativas = 0
        var voltou = false
        mostrar(FaturasUiState(carregando = true), onVoltar = { voltou = true })
        composeRule.onNodeWithTag("faturas-carregando").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Voltar").performClick()
        assertTrue(voltou)

        mostrar(
            FaturasUiState(carregando = false, falha = true),
            onEvent = { if (it == FaturasEvent.TentarDeNovo) tentativas++ },
        )
        composeRule.onNodeWithTag("faturas-erro").assertIsDisplayed()
        composeRule.onNodeWithText("Tentar de novo").performClick()
        assertEquals(1, tentativas)

        mostrar(FaturasUiState(carregando = false, naoEncontrada = true))
        composeRule.onNodeWithTag("faturas-nao-encontrada").assertIsDisplayed()
    }

    @Test
    fun configuracaoIncompletaExibeGuardaEAbreModalComValidacao() {
        val eventos = mutableListOf<FaturasEvent>()
        val modalInvalido = ModalCompletarDatasUiState(
            instrumentoPublicId = "inst-1",
            tituloCartao = "Nubank · crédito",
            fechamentoDia = "0",
            vencimentoDia = "32",
        )
        mostrar(
            FaturasUiState(
                carregando = false,
                tituloCartao = "Nubank · crédito",
                instituicao = InstituicaoFinanceira.NUBANK,
                configuracaoIncompleta = true,
                modalCompletarDatas = modalInvalido,
            ),
            onEvent = { eventos.add(it) },
        )

        composeRule.onNodeWithTag("faturas-incompleta").assertIsDisplayed()
        composeRule.onNodeWithTag("modal-completar-datas").assertIsDisplayed()
        composeRule.onNodeWithTag("salvar-datas-cartao").assertIsNotEnabled()
        composeRule.onNodeWithTag("campo-fechamento-cartao").performTextReplacement("5")
        assertTrue(eventos.contains(FaturasEvent.AlterarFechamentoModal("5")))
    }

    @Test
    fun cicloAbertoExibeTotalParcelasEstornoEAbreLancamento() {
        var lancamentoAberto: String? = null
        val categoria = Categoria("cat-1", "Alimentação", TipoCategoria.DESPESA, criadoEm = INSTANTE, atualizadoEm = INSTANTE)
        val compraParcelada = lancamento("l-1", "Magazine Luiza", 34990L, parcela = 2, totalParcelas = 6, catId = "cat-1")
        val compraAvista = lancamento("l-2", "Padaria do Zé", 4290L, parcela = 1, totalParcelas = 1, catId = "cat-1")
        val estorno = lancamento("l-3", "Estorno Loja", 15000L, estornoDe = "l-1", catId = "cat-1")
        val ciclo = cicloDetalhado(
            competencia = LocalDate.of(2026, 11, 1),
            tipo = TipoCicloFatura.ABERTA,
            status = StatusFatura.ABERTA,
            totalCentavos = 24280L,
            lancamentos = listOf(compraParcelada, compraAvista, estorno),
        )

        mostrar(
            FaturasUiState(
                carregando = false,
                tituloCartao = "Nubank · crédito",
                instituicao = InstituicaoFinanceira.NUBANK,
                ciclosAbertos = listOf(ciclo),
                categoriasPorId = mapOf("cat-1" to categoria),
                hoje = LocalDate.of(2026, 10, 22),
                zona = ZoneOffset.UTC,
            ),
            onAbrirLancamento = { lancamentoAberto = it },
        )

        composeRule.onNodeWithTag("faturas-resumo-ciclo").assertIsDisplayed()
        composeRule.onNodeWithText("R$ 242,80").assertIsDisplayed()
        composeRule.onNodeWithText("2/6").assertIsDisplayed()
        composeRule.onNodeWithText("1/1").assertDoesNotExist()
        composeRule.onNodeWithText("Estorno").assertIsDisplayed()
        composeRule.onNodeWithText("+ R$ 150,00").assertIsDisplayed()

        composeRule.onNodeWithTag("fatura-lancamento-l-1").performScrollTo().performClick()
        assertEquals("l-1", lancamentoAberto)
    }

    @Test
    fun faturaAbertaVaziaExibeZeroSemInventarLancamentosETrocaAbasNoTemaEscuro() {
        val eventos = mutableListOf<FaturasEvent>()
        val cicloAbertoVazio = cicloDetalhado(LocalDate.of(2026, 11, 1), TipoCicloFatura.ABERTA, StatusFatura.ABERTA, 0L, emptyList())

        mostrar(
            FaturasUiState(
                carregando = false,
                tituloCartao = "Nubank · crédito",
                ciclosAbertos = listOf(cicloAbertoVazio),
                abaSelecionada = AbaFaturas.ABERTA,
                hoje = LocalDate.of(2026, 10, 22),
                zona = ZoneOffset.UTC,
            ),
            escuro = true,
            onEvent = { eventos.add(it) },
        )

        composeRule.onNodeWithText("R$ 0,00").assertIsDisplayed()
        composeRule.onNodeWithTag("faturas-ciclo-vazio").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Próximas").performClick()
        composeRule.onNodeWithText("Fechadas").performClick()

        assertTrue(eventos.contains(FaturasEvent.SelecionarAba(AbaFaturas.PROXIMAS)))
        assertTrue(eventos.contains(FaturasEvent.SelecionarAba(AbaFaturas.FECHADAS)))
    }

    private fun mostrar(
        state: FaturasUiState,
        escuro: Boolean = false,
        onVoltar: () -> Unit = {},
        onAbrirLancamento: (String) -> Unit = {},
        onEvent: (FaturasEvent) -> Unit = {},
    ) {
        composeRule.setContent {
            PingoTheme(darkTheme = escuro) {
                FaturasContent(
                    state = state,
                    onVoltar = onVoltar,
                    onAbrirLancamento = onAbrirLancamento,
                    onEvent = onEvent,
                )
            }
        }
    }

    private fun cicloDetalhado(
        competencia: LocalDate,
        tipo: TipoCicloFatura,
        status: StatusFatura,
        totalCentavos: Long,
        lancamentos: List<Lancamento>,
    ): CicloDeFaturaDetalhado = CicloDeFaturaDetalhado(
        faturaPublicId = "fat-$competencia",
        instrumentoPublicId = "inst-1",
        competencia = competencia,
        dataFechamento = competencia.withDayOfMonth(3),
        dataVencimento = competencia.withDayOfMonth(10),
        totalCentavos = totalCentavos,
        status = status,
        tipoCiclo = tipo,
        diasParaVencimento = 19L,
        vencida = false,
        lancamentos = lancamentos,
    )

    private fun lancamento(
        publicId: String,
        estabelecimento: String,
        valorCentavos: Long,
        parcela: Int = 1,
        totalParcelas: Int = 1,
        estornoDe: String? = null,
        catId: String? = null,
    ): Lancamento = Lancamento(
        publicId = publicId,
        instrumentoPublicId = "inst-1",
        categoriaPublicId = catId,
        estornoDePublicId = estornoDe,
        valorCentavos = valorCentavos,
        dataHora = INSTANTE,
        tipo = TipoLancamento.DESPESA,
        estabelecimento = estabelecimento,
        status = StatusLancamento.CONFIRMADO,
        origem = OrigemLancamento.NOTIFICACAO,
        confianca = 1.0,
        parcelaNumero = parcela,
        totalParcelas = totalParcelas,
        criadoEm = INSTANTE,
        atualizadoEm = INSTANTE,
    )

    private companion object {
        val INSTANTE: Instant = Instant.parse("2026-10-22T14:32:00Z")
    }
}
