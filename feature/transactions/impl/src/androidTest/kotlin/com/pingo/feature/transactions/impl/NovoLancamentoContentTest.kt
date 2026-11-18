package com.pingo.feature.transactions.impl

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.data.manual.CampoDeLancamentoManual
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.fatura.ModoParcelamento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class NovoLancamentoContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val hoje = LocalDate.of(2026, 11, 10)
    private val nubank = OpcaoDeInstrumento("inst-nubank", "Nubank · crédito", ehCredito = true)
    private val inter = OpcaoDeInstrumento("inst-inter", "Inter · débito", ehCredito = false)
    private val alimentacao = OpcaoDeCategoria("cat-alimentacao", "Alimentação")
    private val mercado = OpcaoDeCategoria("cat-mercado", "Mercado")

    @Test
    fun abreComTecladoNumericoValorZeradoEDefaults() {
        mostrar(estado())

        composeRule.onNodeWithTag("teclado-numerico").assertIsDisplayed()
        composeRule.onNodeWithText("R$ 0,00").assertIsDisplayed()
        composeRule.onNodeWithText("Digite no teclado abaixo").assertIsDisplayed()
        composeRule.onNodeWithText("Nubank · crédito").assertIsDisplayed()
        composeRule.onNodeWithText("Hoje, 10 de novembro").assertIsDisplayed()
    }

    @Test
    fun digitarValorESalvarEmUmUnicoToque() {
        val eventos = mutableListOf<NovoLancamentoEvent>()
        composeRule.setContent {
            var atual by remember { mutableStateOf(estado()) }
            PingoTheme {
                NovoLancamentoContent(
                    state = atual,
                    onEvent = { evento ->
                        eventos += evento
                        atual = aplicarDigitacao(atual, evento)
                    },
                    onFechar = {},
                )
            }
        }

        listOf("tecla-4", "tecla-2", "tecla-9", "tecla-0").forEach { tecla ->
            composeRule.onNodeWithTag(tecla).performClick()
        }

        composeRule.onNodeWithText("R$ 42,90").assertIsDisplayed()
        composeRule.onNodeWithText("Pronto para salvar").assertIsDisplayed()

        composeRule.onNodeWithTag("salvar-lancamento").performClick()

        assertEquals(4, eventos.count { it is NovoLancamentoEvent.DigitoPressionado })
        assertEquals(
            listOf<NovoLancamentoEvent>(NovoLancamentoEvent.Salvar),
            eventos.filterNot {
                it is NovoLancamentoEvent.DigitoPressionado
            },
        )
    }

    @Test
    fun valorZeradoMostraMensagemNoCampoDeValor() {
        mostrar(estado(erros = setOf(CampoDeLancamentoManual.VALOR)))

        composeRule.onNodeWithText("O valor precisa ser maior que zero").assertIsDisplayed()
    }

    @Test
    fun semInstrumentosMostraMensagemEBloqueiaSemCriarOpcao() {
        mostrar(estado(instrumentos = emptyList(), instrumento = null))

        composeRule.onNodeWithText("Nenhum instrumento cadastrado. Cadastre um banco para lançar.").assertIsDisplayed()
        composeRule.onNodeWithText("Escolha um instrumento").assertIsDisplayed()
    }

    @Test
    fun instrumentoSemEscolhaMostraMensagemDeErro() {
        mostrar(estado(instrumento = null, erros = setOf(CampoDeLancamentoManual.INSTRUMENTO)))

        composeRule.onNodeWithText("Informe de onde saiu o dinheiro").assertIsDisplayed()
    }

    @Test
    fun transferenciaComOrigemIgualAoDestinoMostraMensagemNoDestino() {
        mostrar(
            estado(
                tipo = TipoDoLancamentoManual.TRANSFERENCIA,
                valorCentavos = 50000,
                destino = nubank,
                erros = setOf(CampoDeLancamentoManual.DESTINO),
            ),
        )

        composeRule.onNodeWithTag("campo-origem").assertIsDisplayed()
        composeRule.onNodeWithTag("campo-destino").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Origem e destino precisam ser diferentes").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun painelDeInstrumentosEmiteSelecao() {
        val eventos = mutableListOf<NovoLancamentoEvent>()
        mostrar(estado(painel = PainelDoNovoLancamento.INSTRUMENTO), eventos = eventos)

        composeRule.onNodeWithTag("instrumento-opcao-inst-inter").performClick()

        assertEquals(listOf(NovoLancamentoEvent.SelecionarInstrumento("inst-inter")), eventos)
    }

    @Test
    fun painelDeCategoriaEmiteSelecao() {
        val eventos = mutableListOf<NovoLancamentoEvent>()
        mostrar(estado(painel = PainelDoNovoLancamento.CATEGORIA), eventos = eventos)

        composeRule.onNodeWithTag("categoria-opcao-cat-mercado").performClick()

        assertEquals(listOf(NovoLancamentoEvent.SelecionarCategoria("cat-mercado")), eventos)
    }

    @Test
    fun categoriaSugeridaPeloEstabelecimentoApareceComoAviso() {
        mostrar(estado(categoria = alimentacao, categoriaSugerida = true))

        composeRule.onNodeWithText("Alimentação").assertIsDisplayed()
        composeRule.onNodeWithText("Categoria sugerida pelo estabelecimento").assertIsDisplayed()
    }

    @Test
    fun calendarioConfirmaADataEscolhida() {
        val eventos = mutableListOf<NovoLancamentoEvent>()
        mostrar(estado(calendarioVisivel = true), eventos = eventos)

        composeRule.onNodeWithTag("dia-2026-11-08").performClick()
        composeRule.onNodeWithTag("confirmar-data").performClick()

        assertEquals(listOf(NovoLancamentoEvent.SelecionarData(LocalDate.of(2026, 11, 8))), eventos)
    }

    @Test
    fun estabelecimentoDigitadoEmiteTexto() {
        val eventos = mutableListOf<NovoLancamentoEvent>()
        mostrar(estado(), eventos = eventos)

        composeRule.onNodeWithTag("campo-estabelecimento").performTextInput("Padaria")

        assertEquals(listOf(NovoLancamentoEvent.EstabelecimentoAlterado("Padaria")), eventos)
    }

    @Test
    fun fecharEmiteCallback() {
        var fechamentos = 0
        composeRule.setContent {
            PingoTheme {
                NovoLancamentoContent(
                    state = estado(),
                    onEvent = {},
                    onFechar = { fechamentos++ },
                )
            }
        }

        composeRule.onNodeWithTag("fechar-novo-lancamento").performClick()

        assertEquals(1, fechamentos)
    }

    @Test
    fun parcelamentoApareceParaDespesaNoCreditoESomeEmDebitoEReceita() {
        val estadoAtual = mutableStateOf(estado(instrumento = nubank, tipo = TipoDoLancamentoManual.DESPESA))
        composeRule.setContent {
            PingoTheme {
                NovoLancamentoContent(state = estadoAtual.value, onEvent = {}, onFechar = {})
            }
        }
        composeRule.onNodeWithTag("campo-parcelas").assertIsDisplayed()

        composeRule.runOnUiThread {
            estadoAtual.value = estado(instrumento = inter, tipo = TipoDoLancamentoManual.DESPESA)
        }
        composeRule.onAllNodesWithText("Parcelamento").assertCountEquals(0)

        composeRule.runOnUiThread {
            estadoAtual.value = estado(instrumento = nubank, tipo = TipoDoLancamentoManual.RECEITA)
        }
        composeRule.onAllNodesWithText("Parcelamento").assertCountEquals(0)
    }

    @Test
    fun abrirPainelDeParcelasESelecionarModoEMensagemDeResumo() {
        val eventos = mutableListOf<NovoLancamentoEvent>()
        mostrar(
            estado(
                instrumento = nubank,
                valorCentavos = 10000L,
                totalParcelas = 4,
                modoParcelamento = ModoParcelamento.VALOR_PARCELA,
                painel = PainelDoNovoLancamento.PARCELAS,
            ),
            eventos = eventos,
        )

        composeRule.onNodeWithTag("painel-parcelas").assertIsDisplayed()
        composeRule.onNodeWithText("4x").assertIsDisplayed()
        composeRule.onAllNodesWithText("4 parcelas de R$ 25,00 · Total R$ 100,00").assertCountEquals(0)
        composeRule.onNodeWithText("4 parcelas de R$ 100,00 · Total R$ 400,00").assertIsDisplayed()

        composeRule.onNodeWithTag("parcelas-preset-6").performClick()
        composeRule.onNodeWithTag("parcelas-modo-total").performClick()
        composeRule.onNodeWithTag("parcelas-confirmar").performClick()

        assertEquals(
            listOf(
                NovoLancamentoEvent.SelecionarParcelas(6),
                NovoLancamentoEvent.SelecionarModoParcelamento(ModoParcelamento.VALOR_TOTAL),
                NovoLancamentoEvent.FecharPainel,
            ),
            eventos,
        )
    }

    private fun mostrar(estado: NovoLancamentoUiState, eventos: MutableList<NovoLancamentoEvent> = mutableListOf()) {
        composeRule.setContent {
            PingoTheme {
                NovoLancamentoContent(
                    state = estado,
                    onEvent = { eventos += it },
                    onFechar = {},
                )
            }
        }
    }

    private fun aplicarDigitacao(atual: NovoLancamentoUiState, evento: NovoLancamentoEvent): NovoLancamentoUiState = when (evento) {
        is NovoLancamentoEvent.DigitoPressionado -> atual.copy(valorCentavos = atual.valorCentavos * 10 + evento.digito)
        NovoLancamentoEvent.ApagarPressionado -> atual.copy(valorCentavos = atual.valorCentavos / 10)
        else -> atual
    }

    private fun estado(
        tipo: TipoDoLancamentoManual = TipoDoLancamentoManual.DESPESA,
        valorCentavos: Long = 0,
        instrumento: OpcaoDeInstrumento? = nubank,
        destino: OpcaoDeInstrumento? = null,
        categoria: OpcaoDeCategoria? = null,
        categoriaSugerida: Boolean = false,
        instrumentos: List<OpcaoDeInstrumento> = listOf(nubank, inter),
        categorias: List<OpcaoDeCategoria> = listOf(alimentacao, mercado),
        painel: PainelDoNovoLancamento? = null,
        calendarioVisivel: Boolean = false,
        totalParcelas: Int = 1,
        modoParcelamento: ModoParcelamento = ModoParcelamento.VALOR_TOTAL,
        erros: Set<CampoDeLancamentoManual> = emptySet(),
        salvando: Boolean = false,
    ): NovoLancamentoUiState = NovoLancamentoUiState(
        carregando = false,
        tipo = tipo,
        valorCentavos = valorCentavos,
        instrumento = instrumento,
        destino = destino,
        categoria = categoria,
        categoriaSugerida = categoriaSugerida,
        data = hoje,
        hoje = hoje,
        totalParcelas = totalParcelas,
        modoParcelamento = modoParcelamento,
        instrumentos = instrumentos,
        categorias = if (tipo == TipoDoLancamentoManual.TRANSFERENCIA) emptyList() else categorias,
        painel = painel,
        calendarioVisivel = calendarioVisivel,
        erros = erros,
        salvando = salvando,
    )
}
