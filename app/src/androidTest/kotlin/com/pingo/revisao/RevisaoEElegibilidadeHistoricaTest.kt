package com.pingo.revisao

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.MainActivity
import com.pingo.core.auth.SessionState
import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.data.di.CapturaEntryPoint
import com.pingo.core.database.PingoDatabase
import com.pingo.core.datastore.CapturaDataSourceImpl
import com.pingo.core.datastore.UserPreferencesDataSourceImpl
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.NovaConta
import com.pingo.core.model.NovoInstrumento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.navigation.DestinoInicial
import com.pingo.navigation.PingoNavHost
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class RevisaoEElegibilidadeHistoricaTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val captura: CapturaEntryPoint = EntryPointAccessors.fromApplication(contexto, CapturaEntryPoint::class.java)
    private val database: PingoDatabase by lazy { captura.database() }
    private val contas get() = captura.contaRepository()
    private val instrumentos get() = captura.instrumentoRepository()
    private val lancamentos get() = captura.lancamentoRepository()
    private val preferences by lazy { UserPreferencesDataSourceImpl(contexto) }
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun preparaAmbiente() {
        database.clearAllTables()
        runBlocking {
            preferences.clear()
            CapturaDataSourceImpl(contexto).clear()
            preferences.setConsent("v1", Instant.now())
            preferences.setContasConfiguradas(true)
        }
    }

    @After
    fun limpaAmbiente() {
        scenario?.close()
        scenario = null
        database.clearAllTables()
        runBlocking {
            preferences.clear()
            CapturaDataSourceImpl(contexto).clear()
        }
    }

    @Test
    fun revisaoAntesEDepoisDaReativacaoEEdicaoHistoricaComInstrumentoArquivado() {
        val cenario = runBlocking { semearCenario() }
        abrirNavHost()

        aguardarTag("aviso-revisao")
        composeRule.onNodeWithTag("aviso-revisao").performScrollTo().performClick()
        aguardarTag("tela-revisao")

        verificarSeletoresNaRevisaoAntesDaReativacao(cenario)

        runBlocking {
            val agora = Instant.now()
            instrumentos.reativar(cenario.interArquivadoId, agora)
            contas.reativar(cenario.cajuContaId, agora)
        }

        verificarSeletoresNaRevisaoAposReativacaoEAssociarPendente(cenario)

        runBlocking {
            val agora = Instant.now()
            instrumentos.arquivar(cenario.interArquivadoId, agora)
            contas.arquivar(cenario.cajuContaId, agora)
        }

        verificarEdicaoHistoricaPreservandoVinculoArquivado(cenario)
    }

    private fun abrirNavHost() {
        scenario?.close()
        val novo = ActivityScenario.launch(MainActivity::class.java)
        scenario = novo
        novo.onActivity { activity ->
            activity.setContent {
                PingoTheme {
                    PingoNavHost(
                        session = SessionState.LoggedIn,
                        destinoInicial = DestinoInicial.HOME,
                        onConsentAccepted = {},
                    )
                }
            }
        }
    }

    private fun verificarSeletoresNaRevisaoAntesDaReativacao(cenario: Cenario) {
        rolarListaRevisaoPara("pendencia-${cenario.pendenteTransferenciaId}")
        composeRule.onNodeWithTag("marcar-transferencia-${cenario.pendenteTransferenciaId}").performClick()
        aguardarTag("painel-transferencia-revisao")
        composeRule.onNodeWithTag("destino-revisao-${cenario.interAtivoId}").assertIsDisplayed()
        composeRule.onNodeWithTag("destino-revisao-${cenario.interArquivadoId}").assertDoesNotExist()
        composeRule.onNodeWithTag("destino-revisao-${cenario.cajuVoucherId}").assertDoesNotExist()
        composeRule.onNodeWithTag("cancelar-transferencia-revisao").performClick()

        rolarListaRevisaoPara("pendencia-${cenario.pendenteSemInstrumentoId}")
        composeRule.onNodeWithTag("corrigir-${cenario.pendenteSemInstrumentoId}").performClick()
        aguardarTag("detalhe-${cenario.pendenteSemInstrumentoId}")
        composeRule.onNodeWithTag("campo-instrumento-detalhe").performScrollTo().performClick()
        aguardarTag("painel-detalhe")
        composeRule.onNodeWithTag("instrumento-detalhe-${cenario.nubankCreditoId}").assertIsDisplayed()
        composeRule.onNodeWithTag("instrumento-detalhe-${cenario.interAtivoId}").assertIsDisplayed()
        composeRule.onNodeWithTag("instrumento-detalhe-${cenario.interArquivadoId}").assertDoesNotExist()
        composeRule.onNodeWithTag("instrumento-detalhe-${cenario.cajuVoucherId}").assertDoesNotExist()
        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.onNodeWithTag("voltar-detalhe").performClick()
        aguardarTag("tela-revisao")
    }

    private fun verificarSeletoresNaRevisaoAposReativacaoEAssociarPendente(cenario: Cenario) {
        rolarListaRevisaoPara("pendencia-${cenario.pendenteTransferenciaId}")
        composeRule.onNodeWithTag("marcar-transferencia-${cenario.pendenteTransferenciaId}").performClick()
        aguardarTag("painel-transferencia-revisao")
        composeRule.onNodeWithTag("destino-revisao-${cenario.interArquivadoId}").assertIsDisplayed()
        composeRule.onNodeWithTag("destino-revisao-${cenario.cajuVoucherId}").assertIsDisplayed()
        composeRule.onNodeWithTag("cancelar-transferencia-revisao").performClick()

        rolarListaRevisaoPara("pendencia-${cenario.pendenteSemInstrumentoId}")
        composeRule.onNodeWithTag("corrigir-${cenario.pendenteSemInstrumentoId}").performClick()
        aguardarTag("detalhe-${cenario.pendenteSemInstrumentoId}")
        composeRule.onNodeWithTag("campo-instrumento-detalhe").performScrollTo().performClick()
        aguardarTag("painel-detalhe")
        composeRule.onNodeWithTag("instrumento-detalhe-${cenario.interArquivadoId}").assertIsDisplayed()
        composeRule.onNodeWithTag("instrumento-detalhe-${cenario.cajuVoucherId}").assertIsDisplayed()
        composeRule.onNodeWithTag("instrumento-detalhe-${cenario.interArquivadoId}").performClick()
        composeRule.onNodeWithTag("salvar-detalhe").performScrollTo().performClick()

        aguardarTag("tela-revisao")
        composeRule.waitUntil(ESPERA_MS) {
            runBlocking { lancamentos.buscar(cenario.pendenteSemInstrumentoId)?.status == StatusLancamento.CONFIRMADO }
        }
        val confirmado = runBlocking { lancamentos.buscar(cenario.pendenteSemInstrumentoId) }
        assertEquals(cenario.interArquivadoId, confirmado?.instrumentoPublicId)
    }

    private fun verificarEdicaoHistoricaPreservandoVinculoArquivado(cenario: Cenario) {
        composeRule.onNodeWithTag("voltar-revisao").performClick()
        aguardarTag("aba-LANCAMENTOS")
        composeRule.onNodeWithTag("aba-LANCAMENTOS").performClick()
        aguardarTag("lancamento-${cenario.historicoId}")
        composeRule.onNodeWithTag("lancamento-${cenario.historicoId}").performClick()

        aguardarTag("detalhe-${cenario.historicoId}")
        aguardarTexto("Inter · débito 8888")
        composeRule.onNodeWithTag("campo-instrumento-detalhe").performScrollTo().performClick()
        aguardarTag("painel-detalhe")
        composeRule.onNodeWithTag("instrumento-detalhe-${cenario.interAtivoId}").assertIsDisplayed()
        composeRule.onNodeWithTag("instrumento-detalhe-${cenario.cajuVoucherId}").assertDoesNotExist()
        composeRule.onNodeWithText("Cancelar").performClick()

        composeRule.onNodeWithTag("limpar-estabelecimento-detalhe").performScrollTo().performClick()
        composeRule.onNodeWithTag("campo-estabelecimento-detalhe").performScrollTo().performTextInput("Padaria Histórica Editada")
        composeRule.onNodeWithTag("salvar-detalhe").performScrollTo().performClick()

        aguardarTexto("Padaria Histórica Editada")
        val editado = runBlocking { lancamentos.buscar(cenario.historicoId) }
        assertEquals("Padaria Histórica Editada", editado?.estabelecimento)
        assertEquals(cenario.interArquivadoId, editado?.instrumentoPublicId)
        assertEquals(StatusLancamento.CONFIRMADO, editado?.status)
    }

    private suspend fun semearCenario(): Cenario {
        val agora = Instant.now()
        val ids = semearContas(agora)
        val pendenteTransferenciaId = "pend-transf-1"
        val pendenteSemInstrumentoId = "pend-sem-inst-1"
        val historicoId = "hist-arquivado-1"

        semearLancamentos(
            agora = agora,
            nubankCreditoId = ids.nubankCreditoId,
            interArquivadoId = ids.interArquivadoId,
            pendenteTransferenciaId = pendenteTransferenciaId,
            pendenteSemInstrumentoId = pendenteSemInstrumentoId,
            historicoId = historicoId,
        )
        instrumentos.arquivar(ids.interArquivadoId, agora)
        contas.arquivar(ids.cajuContaId, agora)

        return Cenario(
            cajuContaId = ids.cajuContaId,
            nubankCreditoId = ids.nubankCreditoId,
            interAtivoId = ids.interAtivoId,
            interArquivadoId = ids.interArquivadoId,
            cajuVoucherId = ids.cajuVoucherId,
            pendenteTransferenciaId = pendenteTransferenciaId,
            pendenteSemInstrumentoId = pendenteSemInstrumentoId,
            historicoId = historicoId,
        )
    }

    private suspend fun semearContas(agora: Instant): ContasSemedas {
        contas.criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.NUBANK,
                    apelido = "Nubank",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(
                        NovoInstrumento(
                            tipo = TipoInstrumento.CREDITO,
                            apelido = "Crédito Nubank",
                            ultimosDigitos = "1111",
                            fechamentoDia = 5,
                            vencimentoDia = 12,
                        ),
                    ),
                ),
                NovaConta(
                    instituicao = InstituicaoFinanceira.INTER,
                    apelido = "Inter",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(
                        NovoInstrumento(tipo = TipoInstrumento.DEBITO, apelido = "Débito ativo", ultimosDigitos = "7777"),
                        NovoInstrumento(tipo = TipoInstrumento.DEBITO, apelido = "Débito antigo", ultimosDigitos = "8888"),
                    ),
                ),
                NovaConta(
                    instituicao = InstituicaoFinanceira.CAJU,
                    apelido = "Caju",
                    tipo = TipoConta.BENEFICIO,
                    instrumentos = listOf(
                        NovoInstrumento(
                            tipo = TipoInstrumento.VOUCHER,
                            apelido = "Caju VR",
                            beneficio = BeneficioVoucher.VR,
                            saldoCentavos = 50_000L,
                        ),
                    ),
                ),
            ),
            agora,
        )
        val todasContas = contas.observeContas().first()
        val nubank = todasContas.single { it.instituicao == InstituicaoFinanceira.NUBANK }
        val inter = todasContas.single { it.instituicao == InstituicaoFinanceira.INTER }
        val caju = todasContas.single { it.instituicao == InstituicaoFinanceira.CAJU }
        val interInstrumentos = instrumentos.observePorConta(inter.publicId).first()
        return ContasSemedas(
            cajuContaId = caju.publicId,
            nubankCreditoId = instrumentos.observePorConta(nubank.publicId).first().single().publicId,
            interAtivoId = interInstrumentos.single { it.ultimosDigitos == "7777" }.publicId,
            interArquivadoId = interInstrumentos.single { it.ultimosDigitos == "8888" }.publicId,
            cajuVoucherId = instrumentos.observePorConta(caju.publicId).first().single().publicId,
        )
    }

    @Suppress("LongParameterList")
    private suspend fun semearLancamentos(
        agora: Instant,
        nubankCreditoId: String,
        interArquivadoId: String,
        pendenteTransferenciaId: String,
        pendenteSemInstrumentoId: String,
        historicoId: String,
    ) {
        val categoria = (
            captura.categoriaRepository().criar("Alimentação", TipoCategoria.DESPESA, agora) as ResultadoCategoria.Sucesso
            ).categoria

        lancamentos.salvar(
            Lancamento(
                publicId = pendenteTransferenciaId,
                instrumentoPublicId = nubankCreditoId,
                categoriaPublicId = categoria.publicId,
                valorCentavos = 15_000L,
                dataHora = agora.minusSeconds(300),
                tipo = TipoLancamento.DESPESA,
                estabelecimento = "Pix pendente",
                status = StatusLancamento.PENDENTE,
                origem = OrigemLancamento.NOTIFICACAO,
                instituicaoOrigem = InstituicaoFinanceira.NUBANK,
                confianca = 0.65,
                motivoPendencia = MotivoPendencia.TRANSFERENCIA_AMBIGUA.name,
                criadoEm = agora.minusSeconds(300),
                atualizadoEm = agora.minusSeconds(300),
            ),
        )
        lancamentos.salvar(
            Lancamento(
                publicId = pendenteSemInstrumentoId,
                instrumentoPublicId = null,
                categoriaPublicId = categoria.publicId,
                valorCentavos = 3_200L,
                dataHora = agora.minusSeconds(600),
                tipo = TipoLancamento.DESPESA,
                estabelecimento = "Cafeteria Pendente",
                status = StatusLancamento.PENDENTE,
                origem = OrigemLancamento.NOTIFICACAO,
                instituicaoOrigem = InstituicaoFinanceira.INTER,
                confianca = 0.80,
                motivoPendencia = MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name,
                criadoEm = agora.minusSeconds(600),
                atualizadoEm = agora.minusSeconds(600),
            ),
        )
        lancamentos.salvar(
            Lancamento(
                publicId = historicoId,
                instrumentoPublicId = interArquivadoId,
                categoriaPublicId = categoria.publicId,
                valorCentavos = 4_500L,
                dataHora = agora.minusSeconds(900),
                tipo = TipoLancamento.DESPESA,
                estabelecimento = "Padaria Histórica",
                status = StatusLancamento.CONFIRMADO,
                origem = OrigemLancamento.MANUAL,
                confianca = 1.0,
                criadoEm = agora.minusSeconds(900),
                atualizadoEm = agora.minusSeconds(900),
            ),
        )
    }

    private fun rolarListaRevisaoPara(tag: String) {
        composeRule.onNodeWithTag("lista-revisao").performScrollToNode(hasTestTag(tag))
    }

    private fun aguardarTag(tag: String) {
        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun aguardarTexto(texto: String) {
        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private data class ContasSemedas(
        val cajuContaId: String,
        val nubankCreditoId: String,
        val interAtivoId: String,
        val interArquivadoId: String,
        val cajuVoucherId: String,
    )

    private data class Cenario(
        val cajuContaId: String,
        val nubankCreditoId: String,
        val interAtivoId: String,
        val interArquivadoId: String,
        val cajuVoucherId: String,
        val pendenteTransferenciaId: String,
        val pendenteSemInstrumentoId: String,
        val historicoId: String,
    )

    private companion object {
        const val ESPERA_MS = 10_000L
    }
}
