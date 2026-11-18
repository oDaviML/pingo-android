package com.pingo.revisao

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.MainActivity
import com.pingo.core.auth.SessionState
import com.pingo.core.common.DefaultAppDispatchers
import com.pingo.core.data.di.CapturaEntryPoint
import com.pingo.core.database.PingoDatabase
import com.pingo.core.database.entity.CategoriaEntity
import com.pingo.core.database.entity.RecargaProgramadaVoucherEntity
import com.pingo.core.datastore.CapturaDataSourceImpl
import com.pingo.core.datastore.UserPreferencesDataSourceImpl
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.NovaConta
import com.pingo.core.model.NovoInstrumento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.fatura.CalendarioDeFatura
import com.pingo.core.model.voucher.StatusRecargaProgramada
import com.pingo.core.model.voucher.TipoReferenciaSaldoVoucher
import com.pingo.core.notifications.AcessoNotificacoesDoSistema
import com.pingo.core.notifications.CapturaControllerImpl
import com.pingo.core.notifications.ConexaoDoListener
import com.pingo.core.notifications.EscopoDeCaptura
import com.pingo.core.notifications.RebindDeListenerDoSistema
import com.pingo.core.notifications.ServicoDeCapturaDoAndroid
import com.pingo.navigation.DestinoInicial
import com.pingo.navigation.PingoNavHost
import com.pingo.support.capturarTelaSeSolicitado
import com.pingo.support.prepararListenerDeCaptura
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class VoucherCajuEFluxoFinanceiroTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val captura: CapturaEntryPoint = EntryPointAccessors.fromApplication(contexto, CapturaEntryPoint::class.java)
    private val database: PingoDatabase by lazy { captura.database() }
    private val preferences by lazy { UserPreferencesDataSourceImpl(contexto) }
    private val capturaDataSource by lazy { CapturaDataSourceImpl(contexto) }
    private val acessoNotificacoes by lazy { AcessoNotificacoesDoSistema(contexto) }
    private val capturaController by lazy {
        CapturaControllerImpl(
            capturaDataSource = capturaDataSource,
            acessoNotificacoes = acessoNotificacoes,
            rebindDeListener = RebindDeListenerDoSistema(contexto),
            servicoDeCaptura = ServicoDeCapturaDoAndroid(contexto),
            conexaoDoListener = ConexaoDoListener(),
            escopoDeCaptura = EscopoDeCaptura(DefaultAppDispatchers),
            clock = Clock.systemDefaultZone(),
        )
    }
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun preparaAmbiente() {
        database.clearAllTables()
        runBlocking {
            preferences.clear()
            capturaDataSource.clear()
            preferences.setConsent("v1", Instant.now())
            preferences.setContasConfiguradas(true)
            garantirListenerConectado()
        }
    }

    @After
    fun limpaAmbiente() {
        scenario?.close()
        scenario = null
        database.clearAllTables()
        runBlocking {
            preferences.clear()
            capturaDataSource.clear()
        }
    }

    @Test
    fun cajuAdbCompraAjusteRevisaoVoucherEManualEditarExcluirDesfazer() {
        val cenario = runBlocking { semearCenario() }
        abrirNavHost()
        aguardarTag("secao-saldos-beneficios")

        postarCaju(
            titulo = "Pagamento aprovado",
            texto = "Compra de R$ 50,00 em PADARIA CENTRAL ${System.nanoTime()} APROVADA no CRÉDITO, " +
                "seu saldo em AUXÍLIO ALIMENTAÇÃO é R$ 450,00.",
        )
        aguardarNoBanco {
            runBlocking {
                database.lancamentoDao().observeAtivos().first().any {
                    it.origem == OrigemLancamento.NOTIFICACAO &&
                        it.estabelecimento?.startsWith("PADARIA CENTRAL") == true &&
                        it.status == StatusLancamento.CONFIRMADO
                }
            }
        }
        aguardarTextoNoCard("voucher-inicio-${cenario.vaId}", "R$ 450,00")

        composeRule.onNodeWithTag("aba-CONTAS").performClick()
        aguardarTag("instrumento-voucher-${cenario.vrId}")
        composeRule.onNode(
            hasText("Ajustar saldo") and hasAnyAncestor(hasTestTag("instrumento-voucher-${cenario.vrId}")),
        ).performScrollTo().performClick()
        aguardarTag("modal-ajustar-saldo")
        composeRule.onNodeWithContentDescription("Novo saldo (R$)").performTextReplacement("600,00")
        composeRule.onNodeWithTag("salvar-ajuste-voucher").performClick()
        aguardarTexto("R$ 600,00")

        val referenciasVr = runBlocking { database.referenciaSaldoVoucherDao().listarPorInstrumento(cenario.vrId) }
        assertTrue(referenciasVr.any { it.tipoReferencia == TipoReferenciaSaldoVoucher.AJUSTE && it.saldoCentavos == 60_000L })

        composeRule.onNodeWithTag("aba-RELATORIOS").performClick()
        aguardarTag("relatorios-donut-card")
        aguardarTexto("R$ 50,00")
        composeRule.onNodeWithTag("aba-INICIO").performClick()
        aguardarTextoNoCard("voucher-inicio-${cenario.vrId}", "R$ 600,00")

        postarCaju(
            titulo = "Recarga Caju",
            texto = "Oba! R$ 650,00 disponíveis no seu Caju",
        )
        val pendencia = aguardarPendenciaCaju()
        assertEquals(MotivoPendencia.BENEFICIO_AMBIGUO.name, pendencia.motivoPendencia)

        aguardarTagUnmerged("aviso-revisao")
        composeRule.onNodeWithTag("aviso-revisao", useUnmergedTree = true).performScrollTo().performClick()
        aguardarTag("pendencia-${pendencia.publicId}")
        composeRule.onNodeWithTag("confirmar-${pendencia.publicId}").assertIsNotEnabled()

        composeRule.onNodeWithTag("resolver-voucher-${pendencia.publicId}").performClick()
        aguardarTag("painel-voucher-revisao")
        capturarTelaSeSolicitado(contexto, composeRule, "revisao")
        composeRule.onNodeWithTag("voucher-opcao-${cenario.vaId}").performClick()
        composeRule.onNodeWithTag("decisao-substituir-previsao").performClick()
        composeRule.onNodeWithTag("confirmar-voucher-revisao").performClick()
        aguardarTag("revisao-vazia")

        val recargaSubstituida = runBlocking { database.recargaProgramadaVoucherDao().buscar(cenario.recargaProgramadaId) }
        assertEquals(StatusRecargaProgramada.SUBSTITUIDA, recargaSubstituida?.status)
        assertEquals(pendencia.publicId, recargaSubstituida?.lancamentoPublicId)

        composeRule.onNodeWithTag("voltar-revisao").performClick()
        aguardarTextoNoCard("voucher-inicio-${cenario.vaId}", "R$ 1.100,00")

        postarCaju(
            titulo = "Recarga Extra Caju",
            texto = "Oba! R$ 200,00 disponíveis no seu Caju",
        )
        val pendenciaExtra = aguardarPendenciaCaju()
        aguardarTagUnmerged("aviso-revisao")
        composeRule.onNodeWithTag("aviso-revisao", useUnmergedTree = true).performScrollTo().performClick()
        aguardarTag("resolver-voucher-${pendenciaExtra.publicId}")
        composeRule.onNodeWithTag("resolver-voucher-${pendenciaExtra.publicId}").performClick()
        aguardarTag("painel-voucher-revisao")
        composeRule.onNodeWithTag("voucher-opcao-${cenario.vrId}").performClick()
        composeRule.onNodeWithTag("confirmar-voucher-revisao").performClick()
        aguardarTag("revisao-vazia")
        composeRule.onNodeWithTag("voltar-revisao").performClick()
        aguardarTextoNoCard("voucher-inicio-${cenario.vrId}", "R$ 800,00")

        composeRule.onNodeWithTag("novo-lancamento").performClick()
        aguardarTag("tela-novo-lancamento")
        composeRule.onNodeWithTag("campo-instrumento").performScrollTo().performClick()
        aguardarTag("instrumento-opcao-${cenario.cartaoId}")
        composeRule.onNodeWithTag("instrumento-opcao-${cenario.cartaoId}").performScrollTo().performClick()
        listOf("tecla-1", "tecla-0", "tecla-0", "tecla-0", "tecla-0").forEach { tag ->
            composeRule.onNodeWithTag(tag).performClick()
        }
        composeRule.onNodeWithTag("campo-estabelecimento").performScrollTo().performTextReplacement("Livraria E2E")
        composeRule.onNodeWithTag("salvar-lancamento").performClick()
        aguardarTag("aba-INICIO")
        aguardarTextoNoCard("fatura-inicio-${cenario.cartaoId}", "R$ 100,00")
        capturarTelaSeSolicitado(contexto, composeRule, "inicio")

        composeRule.onNodeWithTag("aba-LANCAMENTOS").performClick()
        val manualId = runBlocking {
            database.lancamentoDao().observeAtivos().first().first { it.estabelecimento == "Livraria E2E" }.publicId
        }
        aguardarTag("lancamento-$manualId")
        composeRule.onNodeWithTag("lancamento-$manualId").performClick()
        aguardarTag("tela-detalhe-lancamento")
        composeRule.onNodeWithTag("bloco-valor-detalhe").performClick()
        aguardarTag("tecla-apagar")
        repeat(5) {
            composeRule.onNodeWithTag("tecla-apagar").performClick()
        }
        listOf("tecla-1", "tecla-5", "tecla-0", "tecla-0", "tecla-0").forEach { tag ->
            composeRule.onNodeWithTag(tag).performClick()
        }
        composeRule.onNodeWithTag("salvar-detalhe").performScrollTo().performClick()
        aguardarTag("lancamento-$manualId")
        composeRule.onNodeWithTag("aba-INICIO").performClick()
        aguardarTextoNoCard("fatura-inicio-${cenario.cartaoId}", "R$ 150,00")

        composeRule.onNodeWithTag("aba-LANCAMENTOS").performClick()
        aguardarTag("lancamento-$manualId")
        composeRule.onNodeWithTag("lancamento-$manualId").performTouchInput { swipeLeft() }
        aguardarTag("snackbar-exclusao")
        aguardarNoBanco {
            runBlocking { database.lancamentoDao().buscar(manualId)?.excluidoEm != null }
        }
        composeRule.onNodeWithText("Desfazer").performClick()
        composeRule.onAllNodesWithTag("snackbar-exclusao").assertCountEquals(0)
        aguardarNoBanco {
            runBlocking { database.lancamentoDao().buscar(manualId)?.excluidoEm == null }
        }
        composeRule.onNodeWithTag("aba-INICIO").performClick()
        aguardarTextoNoCard("fatura-inicio-${cenario.cartaoId}", "R$ 150,00")
    }

    private suspend fun semearCenario(): CenarioVoucher {
        val agora = Instant.now()
        val hoje = LocalDate.now(ZONA)
        database.categoriaDao().upsertAll(
            listOf(
                CategoriaEntity("cat-outros", "Outros", TipoCategoria.DESPESA, true, false, agora, agora, null),
                CategoriaEntity("cat-alim", "Alimentação", TipoCategoria.DESPESA, true, false, agora, agora, null),
                CategoriaEntity("cat-ben", "Benefício", TipoCategoria.RECEITA, true, false, agora, agora, null),
            ),
        )
        captura.contaRepository().criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.NUBANK,
                    apelido = "Nubank",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(
                        NovoInstrumento(TipoInstrumento.CREDITO, "Roxinho", "1234", fechamentoDia = 28, vencimentoDia = 5),
                    ),
                ),
                NovaConta(
                    instituicao = InstituicaoFinanceira.CAJU,
                    apelido = "Caju",
                    tipo = TipoConta.BENEFICIO,
                    instrumentos = listOf(
                        NovoInstrumento(
                            tipo = TipoInstrumento.VOUCHER,
                            apelido = "Caju VA",
                            beneficio = BeneficioVoucher.VA,
                            saldoCentavos = 50_000L,
                        ),
                        NovoInstrumento(
                            tipo = TipoInstrumento.VOUCHER,
                            apelido = "Caju VR",
                            beneficio = BeneficioVoucher.VR,
                            saldoCentavos = 30_000L,
                        ),
                    ),
                ),
            ),
            agora,
        )
        val instrumentos = captura.instrumentoRepository().observeInstrumentos().first()
        val cartao = instrumentos.first { it.tipo == TipoInstrumento.CREDITO }
        val va = instrumentos.first { it.beneficio == BeneficioVoucher.VA }
        val vr = instrumentos.first { it.beneficio == BeneficioVoucher.VR }
        val recargaId = "vou-rec-prog-1"
        database.recargaProgramadaVoucherDao().upsert(
            RecargaProgramadaVoucherEntity(
                publicId = recargaId,
                instrumentoPublicId = va.publicId,
                beneficio = BeneficioVoucher.VA,
                competenciaMes = hoje.withDayOfMonth(1),
                dataPrevista = hoje,
                valorPrevistoCentavos = 50_000L,
                status = StatusRecargaProgramada.PENDENTE,
                lancamentoPublicId = null,
                criadoEm = agora,
                atualizadoEm = agora,
                excluidoEm = null,
            ),
        )
        return CenarioVoucher(
            cartaoId = cartao.publicId,
            vaId = va.publicId,
            vrId = vr.publicId,
            recargaProgramadaId = recargaId,
        )
    }

    private suspend fun garantirListenerConectado() {
        shell("pm grant $PACOTE android.permission.POST_NOTIFICATIONS")
        prepararListenerDeCaptura(::shell)
        val limite = System.currentTimeMillis() + ESPERA_MS
        while (System.currentTimeMillis() < limite && !acessoNotificacoes.concedido()) {
            delay(100L)
        }
        capturaController.reconectar()
        aguardarNotificacaoProcessada()
    }

    private suspend fun aguardarNotificacaoProcessada() {
        val limite = System.currentTimeMillis() + ESPERA_MS
        while (System.currentTimeMillis() < limite) {
            val inicio = Instant.now()
            postarCaju("Sonda do Pingo", "Wellhub")
            val leituras = kotlinx.coroutines.withTimeoutOrNull(2_000L) {
                capturaDataSource.ultimaLeituraPorApp.first { mapa ->
                    mapa[InstituicaoFinanceira.CAJU]?.isAfter(inicio.minusSeconds(2)) == true
                }
            }
            if (leituras != null) {
                return
            }
        }
        throw AssertionError("listener não processou a notificação de sondagem CAJU")
    }

    private fun postarCaju(titulo: String, texto: String) {
        val script = File(contexto.getExternalFilesDir(null), "caju_e2e.sh")
        script.writeText("cmd notification post -t \"$titulo\" pingo-teste-CAJU \"$texto\"\n")
        shell("sh ${script.absolutePath}")
    }

    private fun aguardarPendenciaCaju() = runBlocking {
        var encontrada: com.pingo.core.database.entity.LancamentoEntity? = null
        composeRule.waitUntil(ESPERA_MS) {
            encontrada = runBlocking {
                database.lancamentoDao().observeAtivos().first().firstOrNull {
                    it.status == StatusLancamento.PENDENTE && it.instituicaoOrigem == InstituicaoFinanceira.CAJU
                }
            }
            encontrada != null
        }
        encontrada!!
    }

    private fun shell(comando: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(comando)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
            stream.readBytes().decodeToString()
        }
    }

    private fun abrirNavHost() {
        scenario?.close()
        scenario = ActivityScenario.launch(MainActivity::class.java).also { launched ->
            launched.onActivity { activity ->
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
    }

    private fun aguardarTag(tag: String) {
        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun aguardarTagUnmerged(tag: String) {
        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun aguardarTexto(texto: String) {
        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodesWithText(texto, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun aguardarTextoNoCard(tag: String, texto: String) {
        aguardarTag(tag)
        composeRule.onNodeWithTag(tag).performScrollTo()
        aguardarTexto(texto)
    }

    private fun aguardarNoBanco(condicao: () -> Boolean) {
        composeRule.waitUntil(ESPERA_MS) { condicao() }
    }

    private data class CenarioVoucher(val cartaoId: String, val vaId: String, val vrId: String, val recargaProgramadaId: String)

    private companion object {
        val ZONA = CalendarioDeFatura.ZONA_SAO_PAULO
        const val PACOTE = "com.pingo"
        const val ESPERA_MS = 15_000L
    }
}
