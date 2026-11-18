package com.pingo.contas

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.MainActivity
import com.pingo.core.auth.SessionState
import com.pingo.core.data.di.CapturaEntryPoint
import com.pingo.core.database.PingoDatabase
import com.pingo.core.database.entity.CategoriaEntity
import com.pingo.core.database.entity.ConfiguracaoCartaoEntity
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.database.entity.FaturaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.datastore.CapturaDataSourceImpl
import com.pingo.core.datastore.UserPreferencesDataSourceImpl
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusFatura
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.fatura.CalendarioDeFatura
import com.pingo.core.model.fatura.ResultadoCalculoCiclo
import com.pingo.navigation.DestinoInicial
import com.pingo.navigation.PingoNavHost
import com.pingo.support.capturarTelaSeSolicitado
import dagger.hilt.android.EntryPointAccessors
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
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class FaturasEIndicadoresFluxoTest {
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
    fun fluxoDeContasCompletarDatasFaturasAtalhoInicioEEdicaoDeFechamento() {
        val cenario = runBlocking { semearCenario() }
        abrirNavHost()

        aguardarTag("secao-faturas-credito")
        composeRule.onNodeWithTag("fatura-inicio-${cenario.cartaoCompletoId}").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("aba-CONTAS").performClick()

        aguardarTag("instrumento-credito-${cenario.cartaoIncompletoId}")
        composeRule.onNodeWithTag("instrumento-debito-${cenario.debitoId}").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("R$ 120,00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Datas não configuradas").performScrollTo().assertIsDisplayed()
        capturarTelaSeSolicitado(contexto, composeRule, "contas")

        composeRule.onNodeWithText("Completar datas").performScrollTo().performClick()
        aguardarTag("modal-completar-datas")
        composeRule.onNodeWithContentDescription("Dia de fechamento").performTextReplacement("10")
        composeRule.onNodeWithContentDescription("Dia de vencimento").performTextReplacement("18")
        composeRule.onNodeWithTag("salvar-datas-cartao").performClick()

        aguardarTexto("R$ 150,00")
        composeRule.waitUntil(ESPERA_MS) {
            runBlocking { database.lancamentoDao().buscar(cenario.compraIncompletoId) }?.faturaPublicId != null
        }

        composeRule.onNodeWithTag("instrumento-credito-${cenario.cartaoCompletoId}").performScrollTo().performClick()
        aguardarTag("faturas-resumo-ciclo")
        composeRule.onNodeWithText("R$ 242,80").assertIsDisplayed()
        capturarTelaSeSolicitado(contexto, composeRule, "faturas")
        composeRule.onNodeWithText("2/6").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("1/1").assertCountEquals(0)
        composeRule.onNodeWithText("+ R$ 150,00").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("Pagamento Fatura").assertCountEquals(0)

        composeRule.onNodeWithText("Próximas").performClick()
        aguardarTexto("Compra Futura")
        composeRule.onNodeWithText("R$ 80,00").assertIsDisplayed()

        composeRule.onNodeWithText("Fechadas").performClick()
        aguardarTexto("Compra Fechada")
        composeRule.onNodeWithText("R$ 95,00").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Voltar").performClick()
        aguardarTag("conta-${cenario.contaId}")

        composeRule.onNodeWithTag("conta-${cenario.contaId}").performClick()
        aguardarTexto("Editar conta")
        composeRule.onAllNodesWithContentDescription("Fechamento")[0].performScrollTo().performTextReplacement("20")
        composeRule.onAllNodesWithContentDescription("Vencimento")[0].performScrollTo().performTextReplacement("27")
        composeRule.onNodeWithText("Salvar conta").performScrollTo().performClick()
        aguardarTag("conta-${cenario.contaId}")

        val faturaFechadaDepois = runBlocking { database.faturaDao().buscar(cenario.faturaFechadaId) }
        assertEquals(cenario.dataFechamentoHistorica, faturaFechadaDepois?.dataFechamento)
        assertEquals(cenario.dataVencimentoHistorica, faturaFechadaDepois?.dataVencimento)
        assertEquals(9500L, faturaFechadaDepois?.totalCentavos)
        assertTrue(runBlocking { database.configuracaoCartaoDao().listarPorInstrumento(cenario.cartaoCompletoId) }.size >= 2)

        composeRule.onNodeWithTag("aba-INICIO").performClick()
        aguardarTag("fatura-inicio-${cenario.cartaoCompletoId}")
        composeRule.onNodeWithTag("fatura-inicio-${cenario.cartaoCompletoId}").performScrollTo().performClick()
        aguardarTag("faturas-top-bar")
        composeRule.onNodeWithTag("faturas-resumo-ciclo").assertIsDisplayed()
    }

    private suspend fun semearCenario(): CenarioFaturas {
        val agora = Instant.now()
        val hoje = LocalDate.now(ZONA)
        val fechamentoCompleto = 15
        val vencimentoCompleto = 22
        val cicloAberto = (
            CalendarioDeFatura.cicloParaInstante(
                agora,
                fechamentoCompleto,
                vencimentoCompleto,
            ) as ResultadoCalculoCiclo.CicloCalculado
            ).ciclo
        val instanteAberto = cicloAberto.inicioInclusivo.plusSeconds(3600)
        val competenciaProxima = cicloAberto.competencia.plusMonths(1)
        val cicloProximo = (
            CalendarioDeFatura.cicloDaCompetencia(
                competenciaProxima,
                fechamentoCompleto,
                vencimentoCompleto,
            ) as ResultadoCalculoCiclo.CicloCalculado
            ).ciclo
        val instanteProximo = cicloProximo.inicioInclusivo.plusSeconds(3600)
        val competenciaFechada = cicloAberto.competencia.minusMonths(1)
        val cicloFechado = (
            CalendarioDeFatura.cicloDaCompetencia(
                competenciaFechada,
                fechamentoCompleto,
                vencimentoCompleto,
            ) as ResultadoCalculoCiclo.CicloCalculado
            ).ciclo
        val instanteFechado = cicloFechado.inicioInclusivo.plusSeconds(3600)
        val inicioMesAtual = ZonedDateTime.of(hoje.withDayOfMonth(1), LocalTime.of(12, 0), ZONA).toInstant()

        val contaId = "fat-conta-nu"
        val cartaoCompletoId = "fat-inst-completo"
        val cartaoIncompletoId = "fat-inst-incompleto"
        val debitoId = "fat-inst-debito"
        val faturaAbertaId = "fat-aberta"
        val faturaProximaId = "fat-proxima"
        val faturaFechadaId = "fat-fechada"
        val compraIncompletoId = "fat-lanc-incompleto"

        database.categoriaDao().upsertAll(
            listOf(
                CategoriaEntity("cat-alim", "Alimentação", TipoCategoria.DESPESA, true, false, agora, agora, null),
            ),
        )
        database.contaDao().upsert(
            ContaEntity(contaId, InstituicaoFinanceira.NUBANK, "Nubank", TipoConta.CORRENTE, false, agora, agora, null),
        )
        database.instrumentoDao().upsert(
            InstrumentoEntity(
                publicId = cartaoCompletoId,
                contaPublicId = contaId,
                tipo = TipoInstrumento.CREDITO,
                apelido = "Cartão A Principal",
                ultimosDigitos = "1111",
                arquivado = false,
                fechamentoDia = fechamentoCompleto,
                vencimentoDia = vencimentoCompleto,
                beneficio = null,
                saldoCentavos = null,
                diaRecarga = null,
                valorRecargaCentavos = null,
                limiteCreditoCentavos = null,
                criadoEm = agora.minusSeconds(86400 * 90),
                atualizadoEm = agora,
                excluidoEm = null,
            ),
        )
        database.instrumentoDao().upsert(
            InstrumentoEntity(
                publicId = cartaoIncompletoId,
                contaPublicId = contaId,
                tipo = TipoInstrumento.CREDITO,
                apelido = "Cartão Z Legado",
                ultimosDigitos = "9999",
                arquivado = false,
                fechamentoDia = null,
                vencimentoDia = null,
                beneficio = null,
                saldoCentavos = null,
                diaRecarga = null,
                valorRecargaCentavos = null,
                limiteCreditoCentavos = null,
                criadoEm = agora.minusSeconds(86400 * 30),
                atualizadoEm = agora,
                excluidoEm = null,
            ),
        )
        database.instrumentoDao().upsert(
            InstrumentoEntity(
                publicId = debitoId,
                contaPublicId = contaId,
                tipo = TipoInstrumento.DEBITO,
                apelido = "Conta Débito",
                ultimosDigitos = null,
                arquivado = false,
                fechamentoDia = null,
                vencimentoDia = null,
                beneficio = null,
                saldoCentavos = null,
                diaRecarga = null,
                valorRecargaCentavos = null,
                limiteCreditoCentavos = null,
                criadoEm = agora.minusSeconds(86400 * 90),
                atualizadoEm = agora,
                excluidoEm = null,
            ),
        )
        database.configuracaoCartaoDao().upsert(
            ConfiguracaoCartaoEntity(
                publicId = "fat-cfg-inicial",
                instrumentoPublicId = cartaoCompletoId,
                vigenciaInicio = agora.minusSeconds(86400 * 90),
                fechamentoDia = fechamentoCompleto,
                vencimentoDia = vencimentoCompleto,
                criadoEm = agora.minusSeconds(86400 * 90),
                atualizadoEm = agora,
                excluidoEm = null,
            ),
        )
        database.faturaDao().upsertAll(
            listOf(
                FaturaEntity(
                    publicId = faturaFechadaId,
                    instrumentoPublicId = cartaoCompletoId,
                    competencia = cicloFechado.competencia,
                    dataFechamento = cicloFechado.dataFechamento,
                    dataVencimento = cicloFechado.dataVencimento,
                    totalCentavos = 9500L,
                    totalPagoCentavos = 0L,
                    status = StatusFatura.FECHADA,
                    dataPagamento = null,
                    pagamentoLancamentoPublicId = null,
                    criadoEm = agora,
                    atualizadoEm = agora,
                    excluidoEm = null,
                ),
                FaturaEntity(
                    publicId = faturaAbertaId,
                    instrumentoPublicId = cartaoCompletoId,
                    competencia = cicloAberto.competencia,
                    dataFechamento = cicloAberto.dataFechamento,
                    dataVencimento = cicloAberto.dataVencimento,
                    totalCentavos = 24280L,
                    totalPagoCentavos = 0L,
                    status = StatusFatura.ABERTA,
                    dataPagamento = null,
                    pagamentoLancamentoPublicId = null,
                    criadoEm = agora,
                    atualizadoEm = agora,
                    excluidoEm = null,
                ),
                FaturaEntity(
                    publicId = faturaProximaId,
                    instrumentoPublicId = cartaoCompletoId,
                    competencia = cicloProximo.competencia,
                    dataFechamento = cicloProximo.dataFechamento,
                    dataVencimento = cicloProximo.dataVencimento,
                    totalCentavos = 8000L,
                    totalPagoCentavos = 0L,
                    status = StatusFatura.ABERTA,
                    dataPagamento = null,
                    pagamentoLancamentoPublicId = null,
                    criadoEm = agora,
                    atualizadoEm = agora,
                    excluidoEm = null,
                ),
            ),
        )
        database.lancamentoDao().upsertAll(
            listOf(
                lancamento(
                    publicId = "fat-lanc-parcela",
                    instrumentoPublicId = cartaoCompletoId,
                    faturaPublicId = faturaAbertaId,
                    valorCentavos = 34990L,
                    dataHora = instanteAberto,
                    estabelecimento = "Magazine Luiza",
                    parcela = 2,
                    totalParcelas = 6,
                ),
                lancamento(
                    publicId = "fat-lanc-avista",
                    instrumentoPublicId = cartaoCompletoId,
                    faturaPublicId = faturaAbertaId,
                    valorCentavos = 4290L,
                    dataHora = instanteAberto.plusSeconds(60),
                    estabelecimento = "Padaria Central",
                ),
                lancamento(
                    publicId = "fat-lanc-estorno",
                    instrumentoPublicId = cartaoCompletoId,
                    faturaPublicId = faturaAbertaId,
                    valorCentavos = 15000L,
                    dataHora = instanteAberto.plusSeconds(120),
                    estabelecimento = "Estorno Loja",
                    estornoDe = "fat-lanc-parcela",
                ),
                lancamento(
                    publicId = "fat-lanc-pagamento",
                    instrumentoPublicId = cartaoCompletoId,
                    faturaPublicId = null,
                    valorCentavos = 20000L,
                    dataHora = instanteAberto.plusSeconds(180),
                    estabelecimento = "Pagamento Fatura",
                    tipo = TipoLancamento.TRANSFERENCIA,
                ),
                lancamento(
                    publicId = "fat-lanc-futuro",
                    instrumentoPublicId = cartaoCompletoId,
                    faturaPublicId = faturaProximaId,
                    valorCentavos = 8000L,
                    dataHora = instanteProximo,
                    estabelecimento = "Compra Futura",
                ),
                lancamento(
                    publicId = "fat-lanc-fechado",
                    instrumentoPublicId = cartaoCompletoId,
                    faturaPublicId = faturaFechadaId,
                    valorCentavos = 9500L,
                    dataHora = instanteFechado,
                    estabelecimento = "Compra Fechada",
                ),
                lancamento(
                    publicId = compraIncompletoId,
                    instrumentoPublicId = cartaoIncompletoId,
                    faturaPublicId = null,
                    valorCentavos = 15000L,
                    dataHora = agora.minusSeconds(3600),
                    estabelecimento = "Compra Legada",
                ),
                lancamento(
                    publicId = "fat-lanc-debito",
                    instrumentoPublicId = debitoId,
                    faturaPublicId = null,
                    valorCentavos = 12000L,
                    dataHora = inicioMesAtual,
                    estabelecimento = "Mercado Débito",
                ),
            ),
        )
        return CenarioFaturas(
            contaId = contaId,
            cartaoCompletoId = cartaoCompletoId,
            cartaoIncompletoId = cartaoIncompletoId,
            debitoId = debitoId,
            faturaFechadaId = faturaFechadaId,
            compraIncompletoId = compraIncompletoId,
            dataFechamentoHistorica = cicloFechado.dataFechamento,
            dataVencimentoHistorica = cicloFechado.dataVencimento,
        )
    }

    private fun lancamento(
        publicId: String,
        instrumentoPublicId: String,
        faturaPublicId: String?,
        valorCentavos: Long,
        dataHora: Instant,
        estabelecimento: String,
        tipo: TipoLancamento = TipoLancamento.DESPESA,
        parcela: Int = 1,
        totalParcelas: Int = 1,
        estornoDe: String? = null,
    ): LancamentoEntity = LancamentoEntity(
        publicId = publicId,
        instrumentoPublicId = instrumentoPublicId,
        categoriaPublicId = "cat-alim",
        faturaPublicId = faturaPublicId,
        assinaturaPublicId = null,
        compraPaiPublicId = null,
        estornoDePublicId = estornoDe,
        transferenciaId = null,
        valorCentavos = valorCentavos,
        dataHora = dataHora,
        tipo = tipo,
        tipoSemTransferencia = null,
        estabelecimento = estabelecimento,
        status = StatusLancamento.CONFIRMADO,
        origem = OrigemLancamento.MANUAL,
        instituicaoOrigem = InstituicaoFinanceira.NUBANK,
        confianca = 1.0,
        motivoPendencia = null,
        hashDuplicata = null,
        observacao = null,
        parcelaNumero = parcela,
        totalParcelas = totalParcelas,
        valorTotalCompraCentavos = null,
        criadoEm = dataHora,
        atualizadoEm = dataHora,
        excluidoEm = null,
        textoBruto = null,
        estabelecimentoNormalizado = estabelecimento.lowercase(),
        beneficio = null,
        saldoObservadoCentavos = null,
    )

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

    private data class CenarioFaturas(
        val contaId: String,
        val cartaoCompletoId: String,
        val cartaoIncompletoId: String,
        val debitoId: String,
        val faturaFechadaId: String,
        val compraIncompletoId: String,
        val dataFechamentoHistorica: LocalDate,
        val dataVencimentoHistorica: LocalDate,
    )

    private companion object {
        val ZONA = CalendarioDeFatura.ZONA_SAO_PAULO
        const val ESPERA_MS = 10_000L
    }
}
