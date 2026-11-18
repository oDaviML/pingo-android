package com.pingo.relatorios

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.MainActivity
import com.pingo.core.auth.KeystoreTokenStore
import com.pingo.core.auth.SessionState
import com.pingo.core.auth.StoredTokens
import com.pingo.core.common.DefaultAppDispatchers
import com.pingo.core.data.di.CapturaEntryPoint
import com.pingo.core.database.PingoDatabase
import com.pingo.core.database.entity.CategoriaEntity
import com.pingo.core.database.entity.ContaEntity
import com.pingo.core.database.entity.InstrumentoEntity
import com.pingo.core.database.entity.LancamentoEntity
import com.pingo.core.datastore.CapturaDataSourceImpl
import com.pingo.core.datastore.UserPreferencesDataSourceImpl
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.fatura.CalendarioDeFatura
import com.pingo.core.ui.Valores
import com.pingo.navigation.DestinoInicial
import com.pingo.navigation.PingoNavHost
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.After
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
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class RelatoriosDesempenhoTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val captura: CapturaEntryPoint = EntryPointAccessors.fromApplication(contexto, CapturaEntryPoint::class.java)
    private val database: PingoDatabase by lazy { captura.database() }
    private val preferences by lazy { UserPreferencesDataSourceImpl(contexto) }
    private val tokens by lazy { KeystoreTokenStore(contexto, DefaultAppDispatchers) }
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun preparaAmbiente() {
        database.clearAllTables()
        runBlocking {
            preferences.clear()
            CapturaDataSourceImpl(contexto).clear()
            tokens.clear()
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
            tokens.clear()
        }
    }

    @Test
    fun aberturaFriaEAquecidaDeRelatoriosComSeiscentosLancamentosAteUmSegundoEMeio() {
        val totalMesAtualEsperado = runBlocking { semearSeiscentosLancamentos() }
        val totalFormatado = Valores.formatar(totalMesAtualEsperado)

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
        aguardarTag("saldo-do-mes")
        aguardarTag("aba-RELATORIOS")
        composeRule.waitForIdle()

        val coldMs = medirAberturaRelatorios(totalFormatado)

        val warmAmostras = mutableListOf<Long>()
        repeat(AMOSTRAS_AQUECIDAS) {
            composeRule.onNodeWithTag("aba-INICIO").performClick()
            aguardarTag("saldo-do-mes")
            composeRule.waitForIdle()
            warmAmostras += medirAberturaRelatorios(totalFormatado)
        }

        val todas = listOf(coldMs) + warmAmostras
        val piorMs = todas.max()
        val resumo = "cold_ms=$coldMs warm_ms=${warmAmostras.joinToString(",")} pior_ms=$piorMs total_mes=$totalFormatado"
        Log.i(TAG_LOG, resumo)
        InstrumentationRegistry.getInstrumentation().sendStatus(
            0,
            Bundle().apply {
                putString("val_rep_006", resumo)
                putLong("cold_ms", coldMs)
                putLong("pior_ms", piorMs)
            },
        )

        assertTrue("Cold excedeu 1500 ms: $coldMs ms", coldMs <= LIMITE_MS)
        warmAmostras.forEachIndexed { indice, tempo ->
            assertTrue("Warm #${indice + 1} excedeu 1500 ms: $tempo ms", tempo <= LIMITE_MS)
        }
    }

    private fun medirAberturaRelatorios(totalFormatado: String): Long {
        val inicioNano = SystemClock.elapsedRealtimeNanos()
        composeRule.onNodeWithTag("aba-RELATORIOS").performClick()
        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodesWithTag("relatorios-mes-selecionado").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithTag("relatorios-donut-card").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithTag("relatorios-evolucao-card").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithText(totalFormatado).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
        return (SystemClock.elapsedRealtimeNanos() - inicioNano) / NANOS_POR_MS
    }

    private suspend fun semearSeiscentosLancamentos(): Long {
        val agora = Instant.now()
        val hoje = LocalDate.now(ZONA)
        val mesAtual = YearMonth.from(hoje)
        preferences.setConsent("v1", agora)
        preferences.setContasConfiguradas(true)
        preferences.setActivatedDeviceId(preferences.deviceId())
        tokens.write(
            StoredTokens(
                accessToken = "fixture-access",
                refreshToken = "fixture-refresh",
                expiresAtEpochMillis = agora.toEpochMilli() + TimeUnit.DAYS.toMillis(30),
            ),
        )

        val categorias = listOf(
            CategoriaEntity("bench-cat-0", "Alimentação", TipoCategoria.DESPESA, true, false, agora, agora, null),
            CategoriaEntity("bench-cat-1", "Transporte", TipoCategoria.DESPESA, true, false, agora, agora, null),
            CategoriaEntity("bench-cat-2", "Moradia", TipoCategoria.DESPESA, true, false, agora, agora, null),
            CategoriaEntity("bench-cat-3", "Lazer", TipoCategoria.DESPESA, true, false, agora, agora, null),
            CategoriaEntity("bench-cat-4", "Saúde", TipoCategoria.DESPESA, true, false, agora, agora, null),
            CategoriaEntity("bench-cat-5", "Educação", TipoCategoria.DESPESA, true, false, agora, agora, null),
        )
        database.categoriaDao().upsertAll(categorias)
        database.contaDao().upsert(
            ContaEntity("bench-conta", InstituicaoFinanceira.NUBANK, "Nubank", TipoConta.CORRENTE, false, agora, agora, null),
        )
        database.instrumentoDao().upsert(
            InstrumentoEntity(
                publicId = "bench-inst-cred",
                contaPublicId = "bench-conta",
                tipo = TipoInstrumento.CREDITO,
                apelido = "Roxinho",
                ultimosDigitos = "1234",
                arquivado = false,
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
            ),
        )
        database.instrumentoDao().upsert(
            InstrumentoEntity(
                publicId = "bench-inst-vou",
                contaPublicId = "bench-conta",
                tipo = TipoInstrumento.VOUCHER,
                apelido = "Benefício",
                ultimosDigitos = null,
                arquivado = false,
                fechamentoDia = null,
                vencimentoDia = null,
                beneficio = BeneficioVoucher.VA,
                saldoCentavos = 100_000L,
                diaRecarga = null,
                valorRecargaCentavos = null,
                limiteCreditoCentavos = null,
                criadoEm = agora,
                atualizadoEm = agora,
                excluidoEm = null,
            ),
        )

        var totalMesAtual = 0L
        val lote = ArrayList<LancamentoEntity>(TOTAL_LANCAMENTOS)
        for (indice in 0 until TOTAL_LANCAMENTOS) {
            val deslocamentoMes = (indice % 6).toLong()
            val mes = mesAtual.minusMonths(deslocamentoMes)
            val maxDia = if (deslocamentoMes == 0L) hoje.dayOfMonth else 26
            val dia = (indice % maxDia) + 1
            val instante = ZonedDateTime.of(mes.atDay(dia), LocalTime.of(10, indice % 50), ZONA).toInstant()
            val categoria = categorias[indice % categorias.size].publicId
            val instrumento = if (indice % 2 == 0) "bench-inst-cred" else "bench-inst-vou"
            val valor = 1_500L + (indice % 25) * 350L
            if (deslocamentoMes == 0L) {
                totalMesAtual += valor
            }
            lote += LancamentoEntity(
                publicId = "bench-lanc-$indice",
                instrumentoPublicId = instrumento,
                categoriaPublicId = categoria,
                faturaPublicId = null,
                assinaturaPublicId = null,
                compraPaiPublicId = null,
                estornoDePublicId = null,
                transferenciaId = null,
                valorCentavos = valor,
                dataHora = instante,
                tipo = TipoLancamento.DESPESA,
                tipoSemTransferencia = null,
                estabelecimento = "Loja $indice",
                status = StatusLancamento.CONFIRMADO,
                origem = OrigemLancamento.MANUAL,
                instituicaoOrigem = InstituicaoFinanceira.NUBANK,
                confianca = 1.0,
                motivoPendencia = null,
                hashDuplicata = null,
                observacao = null,
                parcelaNumero = 1,
                totalParcelas = 1,
                valorTotalCompraCentavos = null,
                criadoEm = instante,
                atualizadoEm = instante,
                excluidoEm = null,
                textoBruto = null,
                estabelecimentoNormalizado = "loja $indice",
                beneficio = null,
                saldoObservadoCentavos = null,
            )
        }
        database.lancamentoDao().upsertAll(lote)
        return totalMesAtual
    }

    private fun aguardarTag(tag: String) {
        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        val ZONA = CalendarioDeFatura.ZONA_SAO_PAULO
        const val TAG_LOG = "VAL_REP_006"
        const val TOTAL_LANCAMENTOS = 600
        const val AMOSTRAS_AQUECIDAS = 3
        const val LIMITE_MS = 1_500L
        const val NANOS_POR_MS = 1_000_000L
        const val ESPERA_MS = 10_000L
    }
}
