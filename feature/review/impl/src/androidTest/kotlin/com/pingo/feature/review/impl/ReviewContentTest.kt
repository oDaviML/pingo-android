package com.pingo.feature.review.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.TipoLancamento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class ReviewContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val agora = Instant.parse("2026-11-10T15:30:00Z")
    private val eventos = mutableListOf<ReviewEvent>()
    private val corrigidos = mutableListOf<String>()

    @Test
    fun mostraCabecalhoMotivoTrechoVinculoEValor() {
        mostrar(
            estado(
                listOf(
                    pendencia(
                        publicId = "padaria",
                        motivo = MotivoDaRevisao.BAIXA_CONFIANCA,
                        confianca = 0.62,
                        estabelecimento = "Padaria do Zé",
                        texto = "Compra aprovada R$ 42,90 PADARIA DO ZE",
                        vinculo = "Nubank · crédito",
                    ),
                    pendencia(
                        publicId = "mercado",
                        motivo = MotivoDaRevisao.POSSIVEL_DUPLICATA,
                        estabelecimento = "Mercado do Bairro",
                        texto = "Pix enviado R$ 80,00 para Mercado do Bairro",
                        valorCentavos = 8000,
                    ),
                    pendencia(
                        publicId = "loja",
                        motivo = MotivoDaRevisao.ESTABELECIMENTO_NOVO,
                        estabelecimento = "ZPT*LOJA2231",
                        texto = "Compra no débito R$ 19,90 ZPT*LOJA2231",
                        valorCentavos = 1990,
                    ),
                ),
            ),
        )

        composeRule.onNodeWithText("Revisão de pendentes").assertIsDisplayed()
        composeRule.onNodeWithText("3 para revisar").assertIsDisplayed()
        composeRule.onNodeWithText("O Pingo não teve certeza destes.").assertIsDisplayed()
        composeRule.onNodeWithText("Baixa confiança · 62%").assertIsDisplayed()
        composeRule.onNodeWithText("Padaria do Zé").assertIsDisplayed()
        composeRule.onAllNodesWithText("Nubank · crédito · hoje, 13:30").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("- R$ 42,90").assertIsDisplayed()
        composeRule.onNodeWithText("“Compra aprovada R$ 42,90 PADARIA DO ZE”").assertIsDisplayed()
        composeRule.onNodeWithText("Possível duplicata").assertIsDisplayed()

        rolarAte("pendencia-loja")
        composeRule.onNodeWithText("Estabelecimento novo").assertIsDisplayed()
        composeRule.onNodeWithText("ZPT*LOJA2231").assertIsDisplayed()
    }

    @Test
    fun mostraTodosOsMotivosDePendencia() {
        mostrar(
            estado(
                listOf(
                    pendencia(publicId = "instrumento-ambiguo", motivo = MotivoDaRevisao.INSTRUMENTO_AMBIGUO),
                    pendencia(publicId = "instrumento-nao-encontrado", motivo = MotivoDaRevisao.INSTRUMENTO_NAO_ENCONTRADO),
                    pendencia(publicId = "moeda", motivo = MotivoDaRevisao.MOEDA_ESTRANGEIRA),
                    pendencia(publicId = "transferencia", motivo = MotivoDaRevisao.TRANSFERENCIA_AMBIGUA),
                ),
            ),
        )

        composeRule.onNodeWithText("Instrumento ambíguo").assertIsDisplayed()
        rolarAte("motivo-instrumento-nao-encontrado")
        composeRule.onNodeWithText("Instrumento não encontrado").assertIsDisplayed()
        rolarAte("motivo-moeda")
        composeRule.onNodeWithText("Moeda estrangeira").assertIsDisplayed()
        rolarAte("motivo-transferencia")
        composeRule.onNodeWithText("Transferência ambígua").assertIsDisplayed()
    }

    @Test
    fun pendenteSemTextoMostraIndisponibilidadeSemInventarTrecho() {
        mostrar(
            estado(
                listOf(
                    pendencia(
                        publicId = "restaurado",
                        motivo = MotivoDaRevisao.INSTRUMENTO_NAO_ENCONTRADO,
                        texto = null,
                        podeConfirmar = false,
                    ),
                ),
            ),
        )

        composeRule.onNodeWithTag("trecho-restaurado").assertIsDisplayed()
        composeRule.onNodeWithText("Texto indisponível neste aparelho").assertIsDisplayed()
    }

    @Test
    fun pendenteSemInstrumentoOrientaCorrigirEDesabilitaConfirmar() {
        mostrar(
            estado(
                listOf(
                    pendencia(
                        publicId = "nulo",
                        motivo = MotivoDaRevisao.INSTRUMENTO_NAO_ENCONTRADO,
                        podeConfirmar = false,
                        vinculo = "Instrumento não identificado",
                    ),
                ),
            ),
        )

        composeRule.onNodeWithText("Escolha um instrumento no detalhe para confirmar.").assertIsDisplayed()
        composeRule.onNodeWithTag("confirmar-nulo").assertIsNotEnabled()
    }

    @Test
    fun corrigirEntregaOPublicIdDoPendente() {
        mostrar(estado(listOf(pendencia(publicId = "pendente-x", motivo = MotivoDaRevisao.BAIXA_CONFIANCA))))

        composeRule.onNodeWithTag("corrigir-pendente-x").performClick()

        assertEquals(listOf("pendente-x"), corrigidos)
        assertTrue(eventos.isEmpty())
    }

    @Test
    fun confirmarDescartarEMarcarTransferenciaEmitemEventos() {
        mostrar(estado(listOf(pendencia(publicId = "pendente-1", motivo = MotivoDaRevisao.BAIXA_CONFIANCA))))

        composeRule.onNodeWithTag("confirmar-pendente-1").performClick()
        composeRule.onNodeWithTag("descartar-pendente-1").performClick()
        composeRule.onNodeWithTag("marcar-transferencia-pendente-1").performClick()

        assertEquals(
            listOf(
                ReviewEvent.Confirmar("pendente-1"),
                ReviewEvent.Descartar("pendente-1"),
                ReviewEvent.AbrirMarcarTransferencia("pendente-1"),
            ),
            eventos,
        )
    }

    @Test
    fun confirmarTodosEmiteEventoDeLote() {
        mostrar(estado(listOf(pendencia(publicId = "pendente-1", motivo = MotivoDaRevisao.BAIXA_CONFIANCA))))

        composeRule.onNodeWithTag("confirmar-todos").performClick()

        assertEquals(listOf<ReviewEvent>(ReviewEvent.ConfirmarTodos), eventos)
    }

    @Test
    fun trocaDeCategoriaSelecionaAplicaComSempreUsarECancela() {
        mostrar(
            estado(
                listOf(pendencia(publicId = "padaria", motivo = MotivoDaRevisao.ESTABELECIMENTO_NOVO)),
                painelDeCategoria = TrocaDeCategoriaDaRevisao(
                    lancamentoPublicId = "padaria",
                    estabelecimento = "Padaria do Zé",
                    valorCentavos = 4290,
                    opcoes = listOf(
                        OpcaoDaRevisao("cat-alimentacao", "Alimentação"),
                        OpcaoDaRevisao("cat-mercado", "Mercado"),
                    ),
                    selecionadaPublicId = "cat-alimentacao",
                ),
            ),
        )

        composeRule.onNodeWithTag("painel-categoria-revisao").assertIsDisplayed()
        composeRule.onNodeWithText("Padaria do Zé · R$ 42,90").assertIsDisplayed()
        composeRule.onNodeWithTag("categoria-revisao-cat-mercado").performClick()
        composeRule.onNodeWithTag("aplicar-a-todos-revisao").performClick()
        composeRule.onNodeWithText("Sempre usar para este estabelecimento").assertIsDisplayed()
        composeRule.onNodeWithTag("aplicar-categoria-revisao").performClick()

        assertEquals(
            listOf(
                ReviewEvent.SelecionarCategoria("cat-mercado"),
                ReviewEvent.AlternarAplicarATodos,
                ReviewEvent.AplicarCategoria,
            ),
            eventos,
        )
    }

    @Test
    fun painelDeTransferenciaListaDestinosEEmiteSelecao() {
        mostrar(
            estado(
                listOf(pendencia(publicId = "saida", motivo = MotivoDaRevisao.TRANSFERENCIA_AMBIGUA)),
                painelDeTransferencia = MarcarTransferenciaDaRevisao(
                    lancamentoPublicId = "saida",
                    estabelecimento = "Conta própria",
                    valorCentavos = 15000,
                    opcoes = listOf(OpcaoDaRevisao("inst-inter", "Inter · débito")),
                ),
            ),
        )

        composeRule.onNodeWithText("Marcar como transferência").assertIsDisplayed()
        composeRule.onNodeWithText("Escolha o instrumento de destino.").assertIsDisplayed()
        composeRule.onNodeWithTag("destino-revisao-inst-inter").performClick()

        assertEquals(listOf<ReviewEvent>(ReviewEvent.SelecionarDestino("inst-inter")), eventos)
    }

    @Test
    fun painelDeTransferenciaSemDestinoMostraOrientacao() {
        mostrar(
            estado(
                listOf(pendencia(publicId = "saida", motivo = MotivoDaRevisao.TRANSFERENCIA_AMBIGUA)),
                painelDeTransferencia = MarcarTransferenciaDaRevisao(
                    lancamentoPublicId = "saida",
                    estabelecimento = "Conta própria",
                    valorCentavos = 15000,
                    opcoes = emptyList(),
                ),
            ),
        )

        composeRule.onNodeWithText("Nenhum instrumento de outra conta disponível.").assertIsDisplayed()
    }

    @Test
    fun vazioMostraTudoEmDia() {
        mostrar(ReviewUiState(carregando = false))

        composeRule.onNodeWithTag("revisao-vazia").assertIsDisplayed()
        composeRule.onNodeWithText("Tudo em dia 💧").assertIsDisplayed()
        composeRule.onNodeWithText("Nenhum lançamento esperando revisão.").assertIsDisplayed()
        composeRule.onNodeWithTag("confirmar-todos").assertDoesNotExist()
    }

    @Test
    fun mensagemApareceEPodeSerDispensada() {
        mostrar(
            estado(
                listOf(pendencia(publicId = "pendente-1", motivo = MotivoDaRevisao.BAIXA_CONFIANCA)),
                mensagem = MensagemDaRevisao.InstrumentoNecessario,
            ),
        )

        composeRule.onNodeWithText("Escolha um instrumento no detalhe para confirmar.").assertIsDisplayed()
        composeRule.onNodeWithTag("dispensar-mensagem-revisao").performClick()

        assertEquals(listOf<ReviewEvent>(ReviewEvent.DispensarMensagem), eventos)
    }

    private fun mostrar(state: ReviewUiState) {
        composeRule.setContent {
            PingoTheme {
                ReviewContent(
                    state = state,
                    onEvent = { eventos += it },
                    onCorrigir = { corrigidos += it },
                    onVoltar = {},
                )
            }
        }
    }

    private fun rolarAte(tag: String) {
        composeRule.onNodeWithTag("lista-revisao").performScrollToNode(hasTestTag(tag))
    }

    private fun estado(
        pendentes: List<PendenciaDaRevisao>,
        painelDeCategoria: TrocaDeCategoriaDaRevisao? = null,
        painelDeTransferencia: MarcarTransferenciaDaRevisao? = null,
        mensagem: MensagemDaRevisao? = null,
    ): ReviewUiState = ReviewUiState(
        carregando = false,
        pendentes = pendentes,
        painelDeCategoria = painelDeCategoria,
        painelDeTransferencia = painelDeTransferencia,
        mensagem = mensagem,
    )

    private fun pendencia(
        publicId: String,
        motivo: MotivoDaRevisao,
        estabelecimento: String = "Padaria do Zé",
        valorCentavos: Long = 4290,
        texto: String? = "Compra aprovada R$ 42,90 PADARIA DO ZE",
        vinculo: String = "Nubank · crédito",
        podeConfirmar: Boolean = true,
        confianca: Double = 0.62,
    ): PendenciaDaRevisao = PendenciaDaRevisao(
        publicId = publicId,
        motivo = motivo,
        confianca = confianca,
        estabelecimento = estabelecimento,
        valorCentavos = valorCentavos,
        tipo = TipoLancamento.DESPESA,
        dataHora = agora.minusSeconds(7200),
        horario = "hoje, 13:30",
        vinculo = vinculo,
        categoriaNome = "Alimentação",
        textoInterpretado = texto,
        podeConfirmar = podeConfirmar,
    )
}
