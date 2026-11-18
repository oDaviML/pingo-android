package com.pingo.relatorios

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.MainActivity
import com.pingo.core.auth.SessionState
import com.pingo.core.data.di.CapturaEntryPoint
import com.pingo.core.database.PingoDatabase
import com.pingo.core.database.entity.CategoriaEntity
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.datastore.CapturaDataSourceImpl
import com.pingo.core.datastore.UserPreferencesDataSourceImpl
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusAssinatura
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.fatura.CalendarioDeFatura
import com.pingo.navigation.DestinoInicial
import com.pingo.navigation.PingoNavHost
import com.pingo.support.capturarTelaSeSolicitado
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class RelatoriosAssinaturasEToggleTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val captura: CapturaEntryPoint = EntryPointAccessors.fromApplication(contexto, CapturaEntryPoint::class.java)
    private val database: PingoDatabase by lazy { captura.database() }
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
        shell("pm grant $PACOTE android.permission.POST_NOTIFICATIONS")
        shell("appops set $PACOTE POST_NOTIFICATION allow")
        shell("svc wifi enable")
        shell("svc data enable")
    }

    @After
    fun limpaAmbiente() {
        scenario?.close()
        scenario = null
        shell("pm grant $PACOTE android.permission.POST_NOTIFICATIONS")
        shell("appops set $PACOTE POST_NOTIFICATION allow")
        shell("svc wifi enable")
        shell("svc data enable")
        database.clearAllTables()
        runBlocking {
            preferences.clear()
            CapturaDataSourceImpl(contexto).clear()
        }
    }

    @Test
    fun relatoriosAssinaturasDrillDownOfflineEToggleDeAvisos() {
        val cenario = runBlocking { semearCenario() }
        abrirNavHost()

        aguardarTag("aba-RELATORIOS")
        composeRule.onNodeWithTag("aba-RELATORIOS").performClick()

        aguardarTag("relatorios-donut-card")
        composeRule.onNodeWithText("R$ 400,00").assertIsDisplayed()
        composeRule.onNodeWithText("75%").assertIsDisplayed()
        composeRule.onNodeWithText("25%").assertIsDisplayed()
        composeRule.onNodeWithText("sem base").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("relatorios-evolucao-card").performScrollTo().assertIsDisplayed()

        val tagSugestaoSpotify = "relatorios-sugestao-${cenario.cartaoId}|spotify"
        val tagSugestaoPrime = "relatorios-sugestao-${cenario.cartaoId}|prime video"
        aguardarTag(tagSugestaoSpotify)
        composeRule.onNode(hasText("Confirmar") and hasAnyAncestor(hasTestTag(tagSugestaoSpotify))).performScrollTo().performClick()

        aguardarTag("relatorios-assinaturas-total")
        composeRule.onNode(
            hasText("R$ 21,90") and hasAnyAncestor(hasTestTag("relatorios-assinaturas-total")),
        ).performScrollTo().assertIsDisplayed()
        capturarTelaSeSolicitado(contexto, composeRule, "relatorios")

        composeRule.onNode(hasText("Recusar") and hasAnyAncestor(hasTestTag(tagSugestaoPrime))).performScrollTo().performClick()
        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodesWithTag(tagSugestaoPrime).fetchSemanticsNodes().isEmpty()
        }

        val confirmada = runBlocking {
            database.assinaturaDao().listarAtivas().first { it.estabelecimentoNormalizado == "spotify" }
        }
        assertEquals(StatusAssinatura.CONFIRMADA, confirmada.status)
        val cobrancasVinculadas = runBlocking {
            database.lancamentoDao().observeAtivos().first().filter { it.assinaturaPublicId == confirmada.publicId }
        }
        assertEquals(2, cobrancasVinculadas.size)
        assertTrue(cobrancasVinculadas.all { it.categoriaPublicId == "cat-assinaturas" })

        composeRule.onNodeWithTag("relatorios-categoria-cat-alim").performScrollTo().performClick()
        aguardarTextNaLista("Supermercado Arquivado")
        composeRule.onAllNodesWithText("Corrida App").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("Voltar").performClick()

        aguardarTag("relatorios-donut-card")
        composeRule.onNodeWithTag("aba-LANCAMENTOS").performClick()
        aguardarTextNaLista("Corrida App")
        composeRule.onNodeWithText("Supermercado Arquivado").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("aba-RELATORIOS").performClick()
        aguardarTag("relatorios-assinatura-${confirmada.publicId}")
        composeRule.onNodeWithTag("relatorios-assinatura-${confirmada.publicId}").performScrollTo().performClick()
        aguardarTexto("Cancelar assinatura?")
        composeRule.onNodeWithText("Remover assinatura").performClick()
        aguardarTag("relatorios-assinaturas-vazio")
        assertEquals(
            2,
            runBlocking {
                database.lancamentoDao().observeAtivos().first().count { it.estabelecimentoNormalizado == "spotify" }
            },
        )

        abrirNavHost()
        aguardarTag("aba-RELATORIOS")
        try {
            shell("svc wifi disable")
            shell("svc data disable")
            composeRule.onNodeWithTag("aba-RELATORIOS").performClick()
            aguardarTag("relatorios-donut-card")
            composeRule.onNodeWithText("R$ 400,00").assertIsDisplayed()
            composeRule.onAllNodesWithTag(tagSugestaoPrime).assertCountEquals(0)
        } finally {
            shell("svc wifi enable")
            shell("svc data enable")
        }

        composeRule.onNodeWithTag("aba-INICIO").performClick()
        aguardarTag("aba-INICIO")
        composeRule.onNodeWithContentDescription("Configurações").performClick()
        aguardarTexto("Avisos de assinaturas")
        aguardarSwitch("switch_avisos_assinaturas", ligado = false)
        composeRule.onNodeWithTag("switch_avisos_assinaturas").performScrollTo().assertIsOff()
        capturarTelaSeSolicitado(contexto, composeRule, "configuracoes")

        composeRule.onNodeWithTag("switch_avisos_assinaturas").performScrollTo().performClick()
        composeRule.waitUntil(ESPERA_MS) {
            runBlocking { preferences.avisoAssinaturasAtivo.first() }
        }
        aguardarSwitch("switch_avisos_assinaturas", ligado = true)
        composeRule.onNodeWithTag("switch_avisos_assinaturas").assertIsOn()

        abrirNavHost()
        aguardarTag("aba-INICIO")
        composeRule.onNodeWithContentDescription("Configurações").performClick()
        aguardarTexto("Avisos de assinaturas")
        aguardarSwitch("switch_avisos_assinaturas", ligado = true)
        composeRule.onNodeWithTag("switch_avisos_assinaturas").performScrollTo().assertIsOn()
        composeRule.onNodeWithTag("switch_avisos_assinaturas").performScrollTo().performClick()
        composeRule.waitUntil(ESPERA_MS) {
            runBlocking { !preferences.avisoAssinaturasAtivo.first() }
        }
        aguardarSwitch("switch_avisos_assinaturas", ligado = false)
        composeRule.onNodeWithTag("switch_avisos_assinaturas").assertIsOff()
    }

    private suspend fun semearCenario(): CenarioRelatorios {
        val agora = Instant.now()
        val mesAtual = YearMonth.now(ZONA)
        val mesAnterior = mesAtual.minusMonths(1)
        val mesDoisAtras = mesAtual.minusMonths(2)
        val mesQuatroAtras = mesAtual.minusMonths(4)

        val contaAtivaId = "rel-conta-ativa"
        val contaArquivadaId = "rel-conta-arq"
        val cartaoId = "rel-inst-card"
        val cartaoArquivadoId = "rel-inst-arq"

        database.categoriaDao().upsertAll(
            listOf(
                CategoriaEntity("cat-alim", "Alimentação", TipoCategoria.DESPESA, true, false, agora, agora, null),
                CategoriaEntity("cat-transp", "Transporte", TipoCategoria.DESPESA, true, false, agora, agora, null),
                CategoriaEntity("cat-assinaturas", "Assinaturas", TipoCategoria.DESPESA, true, false, agora, agora, null),
                CategoriaEntity("cat-sal", "Salário", TipoCategoria.RECEITA, true, false, agora, agora, null),
            ),
        )
        database.contaDao().upsert(
            ContaEntity(contaAtivaId, InstituicaoFinanceira.NUBANK, "Nubank", TipoConta.CORRENTE, false, agora, agora, null),
        )
        database.contaDao().upsert(
            ContaEntity(contaArquivadaId, InstituicaoFinanceira.ITAU, "Itaú Antigo", TipoConta.CORRENTE, true, agora, agora, null),
        )
        database.instrumentoDao().upsert(
            instrumento(cartaoId, contaAtivaId, arquivado = false, apelido = "Roxinho", agora = agora),
        )
        database.instrumentoDao().upsert(
            instrumento(cartaoArquivadoId, contaArquivadaId, arquivado = true, apelido = "Itaucard", agora = agora),
        )

        val dataRec2 = mesAnterior.atDay(15)
        val dataRec1 = dataRec2.minusDays(30)

        database.lancamentoDao().upsertAll(
            listOf(
                lancamento("rel-alim-arq", cartaoArquivadoId, "cat-alim", 30_000L, instanteEm(mesAtual.atDay(1)), "Supermercado Arquivado"),
                lancamento("rel-transp", cartaoId, "cat-transp", 10_000L, instanteEm(mesAtual.atDay(1), 13), "Corrida App"),
                lancamento(
                    "rel-pend",
                    cartaoId,
                    "cat-alim",
                    9_000L,
                    instanteEm(mesAtual.atDay(1), 14),
                    "Pendente Ignorado",
                    status = StatusLancamento.PENDENTE,
                ),
                lancamento(
                    "rel-transf",
                    cartaoId,
                    null,
                    25_000L,
                    instanteEm(mesAtual.atDay(1), 15),
                    "Transferência Ignorada",
                    tipo = TipoLancamento.TRANSFERENCIA,
                ),
                lancamento("rel-alim-ant", cartaoId, "cat-alim", 20_000L, instanteEm(mesAnterior.atDay(5)), "Mercado Anterior"),
                lancamento(
                    "rel-rec-mes",
                    cartaoId,
                    "cat-sal",
                    500_000L,
                    instanteEm(mesAtual.atDay(1), 10),
                    "Salário",
                    tipo = TipoLancamento.RECEITA,
                ),
                lancamento("rel-evo-4", cartaoId, "cat-alim", 8_000L, instanteEm(mesQuatroAtras.atDay(10)), "Lanche 4M"),
                lancamento("rel-spot-1", cartaoId, null, 2_190L, instanteEm(dataRec1), "Spotify"),
                lancamento("rel-spot-2", cartaoId, null, 2_190L, instanteEm(dataRec2), "SPOTIFY  "),
                lancamento("rel-prime-1", cartaoId, null, 1_990L, instanteEm(mesDoisAtras.atDay(12)), "Prime Video"),
                lancamento("rel-prime-2", cartaoId, null, 1_990L, instanteEm(mesDoisAtras.atDay(12).plusDays(30)), "Prime Video"),
            ),
        )

        return CenarioRelatorios(cartaoId = cartaoId)
    }

    private fun instrumento(
        publicId: String,
        contaPublicId: String,
        arquivado: Boolean,
        apelido: String,
        agora: Instant,
    ): InstrumentoEntity = InstrumentoEntity(
        publicId = publicId,
        contaPublicId = contaPublicId,
        tipo = TipoInstrumento.CREDITO,
        apelido = apelido,
        ultimosDigitos = "4321",
        arquivado = arquivado,
        fechamentoDia = 15,
        vencimentoDia = 22,
        beneficio = null,
        saldoCentavos = null,
        diaRecarga = null,
        valorRecargaCentavos = null,
        limiteCreditoCentavos = null,
        criadoEm = agora,
        atualizadoEm = agora,
        excluidoEm = null,
    )

    private fun lancamento(
        publicId: String,
        instrumentoPublicId: String,
        categoriaPublicId: String?,
        valorCentavos: Long,
        dataHora: Instant,
        estabelecimento: String,
        tipo: TipoLancamento = TipoLancamento.DESPESA,
        status: StatusLancamento = StatusLancamento.CONFIRMADO,
    ): LancamentoEntity = LancamentoEntity(
        publicId = publicId,
        instrumentoPublicId = instrumentoPublicId,
        categoriaPublicId = categoriaPublicId,
        faturaPublicId = null,
        assinaturaPublicId = null,
        compraPaiPublicId = null,
        estornoDePublicId = null,
        transferenciaId = null,
        valorCentavos = valorCentavos,
        dataHora = dataHora,
        tipo = tipo,
        tipoSemTransferencia = null,
        estabelecimento = estabelecimento,
        status = status,
        origem = OrigemLancamento.MANUAL,
        instituicaoOrigem = InstituicaoFinanceira.NUBANK,
        confianca = 1.0,
        motivoPendencia = null,
        hashDuplicata = null,
        observacao = null,
        parcelaNumero = 1,
        totalParcelas = 1,
        valorTotalCompraCentavos = null,
        criadoEm = dataHora,
        atualizadoEm = dataHora,
        excluidoEm = null,
        textoBruto = null,
        estabelecimentoNormalizado = estabelecimento.trim().lowercase(),
        beneficio = null,
        saldoObservadoCentavos = null,
    )

    private fun instanteEm(data: LocalDate, hora: Int = 12): Instant = ZonedDateTime.of(data, LocalTime.of(hora, 0), ZONA).toInstant()

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

    private fun aguardarTexto(texto: String) {
        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodesWithText(texto, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun aguardarSwitch(tag: String, ligado: Boolean) {
        val condicao = if (ligado) isOn() else isOff()
        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodes(hasTestTag(tag) and condicao).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun aguardarTextNaLista(texto: String) {
        aguardarTexto(texto)
        composeRule.onNodeWithText(texto).performScrollTo().assertIsDisplayed()
    }

    private data class CenarioRelatorios(val cartaoId: String)

    private companion object {
        val ZONA = CalendarioDeFatura.ZONA_SAO_PAULO
        const val PACOTE = "com.pingo"
        const val ESPERA_MS = 12_000L
    }
}
