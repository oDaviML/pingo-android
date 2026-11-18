package com.pingo.feature.transactions.impl

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.designsystem.component.AbaInferior
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.Categoria
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class TransactionsContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val agora = Instant.parse("2026-11-10T14:32:00Z")
    private val categoriaAlimentacao = Categoria(
        publicId = "cat-alim",
        nome = "Alimentação",
        tipo = TipoCategoria.DESPESA,
        criadoEm = agora,
        atualizadoEm = agora,
    )
    private val contaNubank = Conta(
        publicId = "conta-nubank",
        instituicao = InstituicaoFinanceira.NUBANK,
        apelido = "Nubank",
        tipo = TipoConta.CORRENTE,
        criadoEm = agora,
        atualizadoEm = agora,
    )
    private val instrumentoNubank = Instrumento(
        publicId = "inst-nubank",
        contaPublicId = contaNubank.publicId,
        tipo = TipoInstrumento.CREDITO,
        apelido = "Cartão de crédito",
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private val despesa = item(
        id = "l1",
        estabelecimento = "Padaria do Zé",
        valorCentavos = 4290,
        categoria = categoriaAlimentacao,
        instrumento = instrumentoNubank,
        conta = contaNubank,
    )
    private val pendenteSemInstrumento = item(
        id = "l2",
        estabelecimento = "Loja Sem Cartão",
        valorCentavos = 1890,
        status = StatusLancamento.PENDENTE,
        origem = OrigemLancamento.MANUAL,
        categoria = null,
        instrumento = null,
        conta = null,
    )
    private val receita = item(
        id = "l3",
        estabelecimento = "Salário",
        valorCentavos = 15000,
        tipo = TipoLancamento.RECEITA,
        categoria = null,
        instrumento = null,
        conta = null,
    )

    @Test
    fun mostraGrupoDoDiaComTotalItensSelosECores() {
        mostrar(estadoPadrao())

        composeRule.onNodeWithText("HOJE").assertIsDisplayed()
        composeRule.onNodeWithText("+ R$ 88,20").assertIsDisplayed()
        composeRule.onNodeWithText("Padaria do Zé").assertIsDisplayed()
        composeRule.onNodeWithText("- R$ 42,90").assertIsDisplayed()
        composeRule.onNodeWithText("+ R$ 150,00").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Categoria Alimentação").assertIsDisplayed()
        composeRule.onNodeWithText("Pendente").assertIsDisplayed()
        composeRule.onNodeWithText("Manual").assertIsDisplayed()
        composeRule.onNodeWithTag("aba-LANCAMENTOS").assertIsSelected()
    }

    @Test
    fun pendenteSemInstrumentoMostraAusenciaSemCartaoArbitrario() {
        mostrar(estadoPadrao())

        composeRule.onNodeWithTag("lancamento-l2")
            .assert(hasText("Instrumento não identificado", substring = true))
        composeRule.onNodeWithTag("lancamento-l1")
            .assert(hasText("Nubank", substring = true))
    }

    @Test
    fun buscaEmiteTextoDigitado() {
        val eventos = mutableListOf<TransactionsEvent>()
        mostrar(estadoPadrao(), eventos = eventos)

        composeRule.onNodeWithContentDescription("Buscar lançamentos").performTextInput("ifood")

        assertTrue(eventos.contains(TransactionsEvent.BuscaAlterada("ifood")))
    }

    @Test
    fun painelDeFiltrosTrazOpcoesDoPassoSeteEEmiteSelecao() {
        val eventos = mutableListOf<TransactionsEvent>()
        mostrar(estadoPadrao().copy(filtrosVisiveis = true), eventos = eventos)

        composeRule.onNodeWithText("Nubank crédito").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Receitas").performClick()

        assertTrue(eventos.contains(TransactionsEvent.SelecionarVinculo(VinculoFiltro.Instrumento(instrumentoNubank.publicId))))
        assertTrue(eventos.contains(TransactionsEvent.SelecionarTipo(TipoLancamento.RECEITA)))
    }

    @Test
    fun filtroDeVinculoSelecionadoApareceNaBarraDeFiltros() {
        mostrar(
            estadoPadrao().copy(
                filtros = FiltrosDeLancamentos(mes = YearMonth.of(2026, 11), vinculo = VinculoFiltro.Conta(contaNubank.publicId)),
                filtrosAtivos = 1,
            ),
        )

        composeRule.onNodeWithText("Nubank").assertIsDisplayed()
    }

    @Test
    fun vazioComFiltrosMostraMensagemELimparFiltros() {
        val eventos = mutableListOf<TransactionsEvent>()
        mostrar(
            estadoPadrao().copy(
                dias = emptyList(),
                quantidadeDeLancamentos = 0,
                estadoVazio = EstadoVazioDosLancamentos.NADA_COM_OS_FILTROS,
                filtros = FiltrosDeLancamentos(mes = YearMonth.of(2026, 11), status = StatusFiltro.PENDENTE),
                filtrosAtivos = 1,
            ),
            eventos = eventos,
        )

        composeRule.onNodeWithText("Nada com esses filtros").assertIsDisplayed()
        composeRule.onNodeWithText("Tente outro mês ou limpe os filtros.").assertIsDisplayed()
        composeRule.onNodeWithText("Limpar filtros").performClick()

        assertEquals(listOf(TransactionsEvent.LimparFiltros), eventos)
    }

    @Test
    fun deslizarParaEsquerdaExclui() {
        val eventos = mutableListOf<TransactionsEvent>()
        mostrar(estadoPadrao(), eventos = eventos)

        composeRule.onNodeWithTag("lancamento-l1").performTouchInput { swipeLeft() }

        assertEquals(listOf(TransactionsEvent.Excluir("l1")), eventos)
    }

    @Test
    fun deslizarParaDireitaAbreTrocaDeCategoria() {
        val eventos = mutableListOf<TransactionsEvent>()
        mostrar(estadoPadrao(), eventos = eventos)

        composeRule.onNodeWithTag("lancamento-l1").performTouchInput { swipeRight() }

        assertEquals(listOf(TransactionsEvent.AbrirTrocaCategoria("l1")), eventos)
    }

    @Test
    fun toqueAbreDetalheDoLancamento() {
        var detalhe: String? = null
        mostrar(estadoPadrao(), aoAbrirDetalhe = { detalhe = it })

        composeRule.onNodeWithTag("lancamento-l1").performClick()

        assertEquals("l1", detalhe)
    }

    @Test
    fun snackbarDesfazerEmiteEvento() {
        val eventos = mutableListOf<TransactionsEvent>()
        mostrar(estadoPadrao().copy(mensagemDeExclusao = MensagemDeExclusao("l1")), eventos = eventos)

        composeRule.onNodeWithText("Lançamento excluído").assertIsDisplayed()
        composeRule.onNodeWithText("Desfazer").performClick()

        assertEquals(listOf(TransactionsEvent.DesfazerExclusao), eventos)
    }

    @Test
    fun painelDeCategoriaAplicaCategoriaEscolhida() {
        val eventos = mutableListOf<TransactionsEvent>()
        mostrar(
            estadoPadrao().copy(
                trocaDeCategoria = TrocaDeCategoria(
                    lancamentoPublicId = "l1",
                    estabelecimento = "Padaria do Zé",
                    categoriaAtualPublicId = categoriaAlimentacao.publicId,
                    opcoes = listOf(
                        OpcaoDeCategoria(categoriaAlimentacao.publicId, "Alimentação"),
                        OpcaoDeCategoria("cat-mercado", "Mercado"),
                    ),
                ),
            ),
            eventos = eventos,
        )

        composeRule.onNodeWithText("Trocar categoria").assertIsDisplayed()
        composeRule.onNodeWithTag("categoria-opcao-cat-mercado").performClick()

        assertEquals(listOf(TransactionsEvent.TrocarCategoria("l1", "cat-mercado")), eventos)
    }

    @Test
    fun barraInferiorEmiteAbaSelecionada() {
        val abas = mutableListOf<AbaInferior>()
        mostrar(estadoPadrao(), aoSelecionarAba = { abas += it })

        composeRule.onNodeWithTag("aba-CONTAS").performClick()
        composeRule.onNodeWithTag("aba-RELATORIOS").performClick()
        composeRule.onNodeWithTag("aba-INICIO").performClick()

        assertEquals(listOf(AbaInferior.CONTAS, AbaInferior.RELATORIOS, AbaInferior.INICIO), abas)
    }

    @Test
    fun botaoNovoLancamentoDaBarraInferiorAbreOTerceiroPasso() {
        var aberturas = 0
        mostrar(estadoPadrao(), aoAbrirNovo = { aberturas++ })

        composeRule.onNodeWithContentDescription("Novo lançamento").performClick()

        assertEquals(1, aberturas)
    }

    @Test
    fun comOnVoltarExibeBotaoVoltarEAcionaCallback() {
        var voltou = 0
        composeRule.setContent {
            PingoTheme {
                TransactionsContent(
                    state = estadoPadrao(),
                    onEvent = {},
                    onOpenDetalhe = {},
                    onOpenNovo = {},
                    onSelecionarAba = {},
                    mostrarBarraInferior = false,
                    onVoltar = { voltou++ },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Voltar").assertIsDisplayed().performClick()

        assertEquals(1, voltou)
    }

    @Test
    fun botaoExportarCsvEmiteEventoEMostraAvisoDeFiltroVazio() {
        val eventos = mutableListOf<TransactionsEvent>()
        mostrar(
            estadoPadrao().copy(avisoExportacaoCsv = ExportadorCsvDeLancamentos.MENSAGEM_VAZIO),
            eventos = eventos,
        )

        composeRule.onNodeWithTag("botao_exportar_csv").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("snackbar-exportacao-vazia")
            .assertIsDisplayed()
            .assert(hasText(ExportadorCsvDeLancamentos.MENSAGEM_VAZIO, substring = true))

        assertTrue(eventos.contains(TransactionsEvent.ExportarCsv))
    }

    @Test
    fun lancamentoComParcelasEOrigemManualPosicionaSelosAbaixoSemCortarTitulo() {
        val parceladoManual = LancamentoDaLista(
            lancamento = despesa.lancamento.copy(
                publicId = "l-parc",
                estabelecimento = "Mercado Livre Brasil",
                valorCentavos = 4991,
                parcelaNumero = 12,
                totalParcelas = 12,
                status = StatusLancamento.PENDENTE,
                origem = OrigemLancamento.MANUAL,
            ),
            categoria = categoriaAlimentacao,
            instrumento = instrumentoNubank,
            conta = contaNubank,
        )

        composeRule.setContent {
            PingoTheme {
                Box(modifier = Modifier.width(340.dp)) {
                    TransactionsContent(
                        state = estadoPadrao().copy(
                            dias = listOf(
                                DiaDeLancamentos(
                                    data = LocalDate.of(2026, 11, 10),
                                    rotulo = "HOJE",
                                    totalCentavos = 4991,
                                    itens = listOf(parceladoManual),
                                ),
                            ),
                            quantidadeDeLancamentos = 1,
                        ),
                        onEvent = {},
                        onOpenDetalhe = {},
                        onOpenNovo = {},
                        onSelecionarAba = {},
                    )
                }
            }
        }

        val tituloBounds = composeRule.onNodeWithText("Mercado Livre Brasil").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val parcelaBounds = composeRule.onNodeWithText("12/12").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        composeRule.onNodeWithText("Pendente").assertIsDisplayed()
        composeRule.onNodeWithText("Manual").assertIsDisplayed()

        assertTrue(parcelaBounds.top > tituloBounds.bottom)
    }

    private fun mostrar(
        state: TransactionsUiState,
        eventos: MutableList<TransactionsEvent> = mutableListOf(),
        aoAbrirDetalhe: (String) -> Unit = {},
        aoAbrirNovo: () -> Unit = {},
        aoSelecionarAba: (AbaInferior) -> Unit = {},
    ) {
        composeRule.setContent {
            PingoTheme {
                TransactionsContent(
                    state = state,
                    onEvent = { eventos += it },
                    onOpenDetalhe = aoAbrirDetalhe,
                    onOpenNovo = aoAbrirNovo,
                    onSelecionarAba = aoSelecionarAba,
                )
            }
        }
    }

    private fun estadoPadrao(): TransactionsUiState = TransactionsUiState(
        carregando = false,
        filtros = FiltrosDeLancamentos(mes = YearMonth.of(2026, 11)),
        mesesDisponiveis = listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 10)),
        opcoesDeVinculo = listOf(
            OpcaoDeVinculo(VinculoFiltro.Conta(contaNubank.publicId), "Nubank", "conta-${contaNubank.publicId}"),
            OpcaoDeVinculo(
                VinculoFiltro.Instrumento(instrumentoNubank.publicId),
                "Nubank crédito",
                "instrumento-${instrumentoNubank.publicId}",
            ),
        ),
        opcoesDeCategoria = listOf(
            OpcaoDeCategoria(categoriaAlimentacao.publicId, categoriaAlimentacao.nome),
            OpcaoDeCategoria("cat-mercado", "Mercado"),
        ),
        dias = listOf(
            DiaDeLancamentos(
                data = LocalDate.of(2026, 11, 10),
                rotulo = "HOJE",
                totalCentavos = 8820,
                itens = listOf(despesa, pendenteSemInstrumento, receita),
            ),
        ),
        quantidadeDeLancamentos = 3,
    )

    private fun item(
        id: String,
        estabelecimento: String,
        valorCentavos: Long,
        tipo: TipoLancamento = TipoLancamento.DESPESA,
        status: StatusLancamento = StatusLancamento.CONFIRMADO,
        origem: OrigemLancamento = OrigemLancamento.NOTIFICACAO,
        categoria: Categoria?,
        instrumento: Instrumento?,
        conta: Conta?,
    ): LancamentoDaLista = LancamentoDaLista(
        lancamento = Lancamento(
            publicId = id,
            instrumentoPublicId = instrumento?.publicId,
            categoriaPublicId = categoria?.publicId,
            valorCentavos = valorCentavos,
            dataHora = agora,
            tipo = tipo,
            estabelecimento = estabelecimento,
            status = status,
            origem = origem,
            confianca = 1.0,
            criadoEm = agora,
            atualizadoEm = agora,
        ),
        categoria = categoria,
        instrumento = instrumento,
        conta = conta,
    )
}
