package com.pingo.feature.transactions.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.data.detalhe.CampoDoDetalhe
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.InstituicaoFinanceira
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class DetalheDoLancamentoContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val agora = Instant.parse("2026-11-10T15:30:00Z")
    private val zona = ZoneId.of("UTC")
    private val hoje = LocalDate.of(2026, 11, 10)
    private val nubank = OpcaoDeInstrumento("inst-nubank", "Nubank · crédito")
    private val inter = OpcaoDeInstrumento("inst-inter", "Inter · débito")
    private val alimentacao = OpcaoDeCategoria("cat-alimentacao", "Alimentação")
    private val mercado = OpcaoDeCategoria("cat-mercado", "Mercado")

    @Test
    fun mostraOsCamposEditaveisDoLancamento() {
        mostrar(estado())

        composeRule.onNodeWithText("Detalhe do lançamento").assertIsDisplayed()
        composeRule.onNodeWithText("R$ 42,90").assertIsDisplayed()
        composeRule.onNodeWithTag("campo-instrumento-detalhe").assertIsDisplayed()
        composeRule.onNodeWithTag("campo-categoria-detalhe").assertIsDisplayed()
        composeRule.onNodeWithTag("campo-data-detalhe").assertIsDisplayed()
        composeRule.onNodeWithTag("campo-estabelecimento-detalhe").assertIsDisplayed()
        composeRule.onNodeWithTag("origem-lancamento").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun origemCapturadaMostraInstituicaoTextoBrutoEConfianca() {
        mostrar(
            estado(
                origem = OrigemDoLancamentoUi.Capturado(
                    instituicao = InstituicaoFinanceira.NUBANK,
                    lidoEm = Instant.parse("2026-11-10T13:30:00Z"),
                    textoBruto = "Padaria do Zé R$ 42,90 aprovado no Nubank",
                    confianca = 0.86,
                ),
            ),
        )

        composeRule.onNodeWithTag("origem-lancamento").performScrollTo()
        composeRule.onNodeWithTag("origem-instituicao").assertIsDisplayed()
        composeRule.onNodeWithText("Nubank").assertIsDisplayed()
        composeRule.onNodeWithText("Lido hoje às 13:30").assertIsDisplayed()
        composeRule.onNodeWithText("Padaria do Zé R$ 42,90 aprovado no Nubank").assertIsDisplayed()
        composeRule.onNodeWithText("Confiança 86%").assertIsDisplayed()
    }

    @Test
    fun trocarInstrumentoNaoMudaAOrigemCapturada() {
        val capturada = OrigemDoLancamentoUi.Capturado(
            instituicao = InstituicaoFinanceira.NUBANK,
            lidoEm = agora,
            textoBruto = "Compra no Nubank",
            confianca = 0.9,
        )
        mostrar(estado(instrumento = inter, origem = capturada))

        composeRule.onNodeWithTag("origem-instituicao").performScrollTo()
        composeRule.onNodeWithText("Nubank").assertIsDisplayed()
        composeRule.onNodeWithTag("origem-texto").performScrollTo()
        composeRule.onNodeWithText("Compra no Nubank").assertIsDisplayed()
    }

    @Test
    fun restauracaoSemTextoBrutoMostraIndisponibilidade() {
        mostrar(
            estado(
                origem = OrigemDoLancamentoUi.Capturado(
                    instituicao = InstituicaoFinanceira.INTER,
                    lidoEm = agora,
                    textoBruto = null,
                    confianca = 0.5,
                ),
            ),
        )

        composeRule.onNodeWithTag("origem-texto").performScrollTo()
        composeRule.onNodeWithText("Texto indisponível neste aparelho").assertIsDisplayed()
    }

    @Test
    fun origemLegadaSemInstituicaoMostraAusencia() {
        mostrar(
            estado(
                origem = OrigemDoLancamentoUi.Capturado(
                    instituicao = null,
                    lidoEm = agora,
                    textoBruto = "Compra aprovada",
                    confianca = 0.5,
                ),
            ),
        )

        composeRule.onNodeWithTag("origem-instituicao").performScrollTo()
        composeRule.onNodeWithText("Origem não informada").assertIsDisplayed()
    }

    @Test
    fun lancamentoManualMostraOrigemManual() {
        mostrar(estado(origem = OrigemDoLancamentoUi.Manual(registradoEm = agora)))

        composeRule.onNodeWithTag("origem-lancamento").performScrollTo()
        composeRule.onNodeWithText("Manual").assertIsDisplayed()
        composeRule.onNodeWithText("Descrição").assertIsDisplayed()
    }

    @Test
    fun transferenciaMostraODestinoEOPar() {
        mostrar(
            estado(
                ehTransferencia = true,
                tipo = TipoDoLancamentoManual.TRANSFERENCIA,
                destino = inter,
                par = ParDaTransferenciaUi(
                    publicId = "entrada",
                    rotuloDoVinculo = "Inter · débito",
                    entrada = true,
                    valorCentavos = 5000,
                    dataHora = agora,
                ),
            ),
        )

        composeRule.onNodeWithTag("campo-destino-detalhe").assertIsDisplayed()
        composeRule.onNodeWithText("Valor transferido").assertIsDisplayed()
        composeRule.onNodeWithTag("par-transferencia").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Entrada em Inter").assertIsDisplayed()
        composeRule.onNodeWithText("Desfazer transferência").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun transferenciaSemParEncontradoNaoQuebra() {
        mostrar(estado(ehTransferencia = true, tipo = TipoDoLancamentoManual.TRANSFERENCIA, destino = inter, par = null))

        composeRule.onNodeWithTag("par-transferencia-ausente").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun capturaPendenteSemInstrumentoNaoEscolheCartaoSozinha() {
        val eventos = mutableListOf<DetalheDoLancamentoEvent>()
        mostrar(estado(instrumento = null, categoria = null, origem = null), eventos)

        composeRule.onNodeWithText("Escolha um instrumento").assertIsDisplayed()
        composeRule.onNodeWithText("Escolher categoria").assertIsDisplayed()
        composeRule.onNodeWithTag("salvar-detalhe").performScrollTo().performClick()

        assertEquals(listOf<DetalheDoLancamentoEvent>(DetalheDoLancamentoEvent.Salvar), eventos)
    }

    @Test
    fun semInstrumentosMostraMensagemSemCriarOpcao() {
        mostrar(estado(instrumento = null, instrumentos = emptyList(), erros = setOf(CampoDoDetalhe.INSTRUMENTO)))

        composeRule.onNodeWithText("Nenhum instrumento cadastrado. Cadastre um banco para editar.").assertIsDisplayed()
    }

    @Test
    fun valorZeradoMostraMensagemNoBlocoDeValor() {
        mostrar(estado(valorCentavos = 0, erros = setOf(CampoDoDetalhe.VALOR)))

        composeRule.onNodeWithText("R$ 0,00").assertIsDisplayed()
        composeRule.onNodeWithText("O valor precisa ser maior que zero").assertIsDisplayed()
    }

    @Test
    fun excluirAbreDialogoEConfirmaEmissao() {
        val eventos = mutableListOf<DetalheDoLancamentoEvent>()
        mostrar(estado(dialogoDeExclusao = true), eventos)

        composeRule.onNodeWithText("Excluir lançamento?").assertIsDisplayed()
        composeRule.onNodeWithText("Excluir").performClick()

        assertEquals(listOf<DetalheDoLancamentoEvent>(DetalheDoLancamentoEvent.ConfirmarExclusao), eventos)
    }

    @Test
    fun botaoExcluirPedeConfirmacao() {
        val eventos = mutableListOf<DetalheDoLancamentoEvent>()
        mostrar(estado(), eventos)

        composeRule.onNodeWithTag("excluir-lancamento").performScrollTo().performClick()

        assertEquals(listOf<DetalheDoLancamentoEvent>(DetalheDoLancamentoEvent.SolicitarExclusao), eventos)
    }

    @Test
    fun marcarComoTransferenciaEmiteEvento() {
        val eventos = mutableListOf<DetalheDoLancamentoEvent>()
        mostrar(estado(), eventos)

        composeRule.onNodeWithTag("marcar-transferencia").performScrollTo().performClick()

        assertEquals(listOf<DetalheDoLancamentoEvent>(DetalheDoLancamentoEvent.SolicitarTransferencia), eventos)
    }

    @Test
    fun painelDeMarcacaoEmiteODestinoEscolhido() {
        val eventos = mutableListOf<DetalheDoLancamentoEvent>()
        mostrar(estado(painel = PainelDoDetalhe.MARCAR_TRANSFERENCIA, destinosDaTransferencia = listOf(inter)), eventos)

        composeRule.onNodeWithTag("destino-marcacao-inst-inter").performClick()

        assertEquals(listOf<DetalheDoLancamentoEvent>(DetalheDoLancamentoEvent.SelecionarDestino("inst-inter")), eventos)
    }

    @Test
    fun mensagemDeRecusaApareceEDispensa() {
        val eventos = mutableListOf<DetalheDoLancamentoEvent>()
        mostrar(estado(mensagem = MensagemDoDetalhe.PAR_AMBIGUO), eventos)

        composeRule.onNodeWithText("Há mais de um lançamento candidato nessa conta.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("OK").performClick()

        assertEquals(listOf<DetalheDoLancamentoEvent>(DetalheDoLancamentoEvent.DispensarMensagem), eventos)
    }

    @Test
    fun aplicarATodosEmiteEstadoDoCheckbox() {
        val eventos = mutableListOf<DetalheDoLancamentoEvent>()
        mostrar(estado(categoria = alimentacao), eventos)

        composeRule.onNodeWithTag("aplicar-a-todos-detalhe").performScrollTo().performClick()

        assertEquals(listOf<DetalheDoLancamentoEvent>(DetalheDoLancamentoEvent.AplicarATodosAlterado(true)), eventos)
    }

    @Test
    fun painelDeInstrumentoEmiteSelecao() {
        val eventos = mutableListOf<DetalheDoLancamentoEvent>()
        mostrar(estado(painel = PainelDoDetalhe.INSTRUMENTO), eventos)

        composeRule.onNodeWithTag("instrumento-detalhe-inst-inter").performClick()

        assertEquals(listOf<DetalheDoLancamentoEvent>(DetalheDoLancamentoEvent.SelecionarInstrumento("inst-inter")), eventos)
    }

    @Test
    fun painelDeCategoriaEmiteSelecao() {
        val eventos = mutableListOf<DetalheDoLancamentoEvent>()
        mostrar(estado(painel = PainelDoDetalhe.CATEGORIA), eventos)

        composeRule.onNodeWithTag("categoria-detalhe-cat-mercado").performClick()

        assertEquals(listOf<DetalheDoLancamentoEvent>(DetalheDoLancamentoEvent.SelecionarCategoria("cat-mercado")), eventos)
    }

    @Test
    fun tecladoDoValorEmiteDigitos() {
        val eventos = mutableListOf<DetalheDoLancamentoEvent>()
        mostrar(estado(tecladoVisivel = true), eventos)

        composeRule.onNodeWithTag("tecla-7").performClick()

        assertEquals(listOf<DetalheDoLancamentoEvent>(DetalheDoLancamentoEvent.DigitoPressionado(7)), eventos)
    }

    @Test
    fun estabelecimentoDigitadoEmiteTexto() {
        val eventos = mutableListOf<DetalheDoLancamentoEvent>()
        mostrar(estado(estabelecimento = ""), eventos)

        composeRule.onNodeWithTag("campo-estabelecimento-detalhe").performScrollTo().performTextInput("Padaria")

        assertEquals(listOf<DetalheDoLancamentoEvent>(DetalheDoLancamentoEvent.EstabelecimentoAlterado("Padaria")), eventos)
    }

    @Test
    fun voltarEmiteCallback() {
        var voltar = 0
        composeRule.setContent {
            PingoTheme {
                DetalheDoLancamentoContent(
                    state = estado(),
                    onEvent = {},
                    onVoltar = { voltar++ },
                    zona = zona,
                    agora = agora,
                )
            }
        }

        composeRule.onNodeWithTag("voltar-detalhe").performClick()

        assertEquals(1, voltar)
    }

    @Test
    fun naoEncontradoMostraAviso() {
        mostrar(estado(naoEncontrado = true, carregando = false))

        composeRule.onNodeWithText("Lançamento não encontrado").assertIsDisplayed()
    }

    private fun mostrar(estado: DetalheDoLancamentoUiState, eventos: MutableList<DetalheDoLancamentoEvent> = mutableListOf()) {
        composeRule.setContent {
            PingoTheme {
                DetalheDoLancamentoContent(
                    state = estado,
                    onEvent = { eventos += it },
                    onVoltar = {},
                    zona = zona,
                    agora = agora,
                )
            }
        }
    }

    @Suppress("LongParameterList")
    private fun estado(
        carregando: Boolean = false,
        naoEncontrado: Boolean = false,
        ehTransferencia: Boolean = false,
        tipo: TipoDoLancamentoManual = TipoDoLancamentoManual.DESPESA,
        valorCentavos: Long = 4290,
        instrumento: OpcaoDeInstrumento? = nubank,
        destino: OpcaoDeInstrumento? = null,
        categoria: OpcaoDeCategoria? = alimentacao,
        estabelecimento: String = "Padaria do Zé",
        aplicarCategoriaATodos: Boolean = false,
        instrumentos: List<OpcaoDeInstrumento> = listOf(nubank, inter),
        destinosDaTransferencia: List<OpcaoDeInstrumento> = listOf(inter),
        categorias: List<OpcaoDeCategoria> = listOf(alimentacao, mercado),
        origem: OrigemDoLancamentoUi? = OrigemDoLancamentoUi.Capturado(
            instituicao = InstituicaoFinanceira.NUBANK,
            lidoEm = Instant.parse("2026-11-10T13:30:00Z"),
            textoBruto = "Padaria do Zé R$ 42,90 aprovado no Nubank",
            confianca = 0.86,
        ),
        par: ParDaTransferenciaUi? = null,
        painel: PainelDoDetalhe? = null,
        calendarioVisivel: Boolean = false,
        tecladoVisivel: Boolean = false,
        erros: Set<CampoDoDetalhe> = emptySet(),
        dialogoDeExclusao: Boolean = false,
        mensagem: MensagemDoDetalhe? = null,
    ): DetalheDoLancamentoUiState = DetalheDoLancamentoUiState(
        carregando = carregando,
        naoEncontrado = naoEncontrado,
        ehTransferencia = ehTransferencia,
        tipo = tipo,
        valorCentavos = valorCentavos,
        instrumento = instrumento,
        destino = destino,
        categoria = if (tipo == TipoDoLancamentoManual.TRANSFERENCIA) null else categoria,
        dataHora = agora,
        hoje = hoje,
        estabelecimento = estabelecimento,
        aplicarCategoriaATodos = aplicarCategoriaATodos,
        instrumentos = instrumentos,
        destinosDaTransferencia = destinosDaTransferencia,
        categorias = if (tipo == TipoDoLancamentoManual.TRANSFERENCIA) emptyList() else categorias,
        origem = origem,
        par = par,
        painel = painel,
        calendarioVisivel = calendarioVisivel,
        tecladoVisivel = tecladoVisivel,
        erros = erros,
        dialogoDeExclusao = dialogoDeExclusao,
        mensagem = mensagem,
    )
}
