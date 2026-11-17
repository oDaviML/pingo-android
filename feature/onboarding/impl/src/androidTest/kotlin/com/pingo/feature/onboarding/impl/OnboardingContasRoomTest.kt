package com.pingo.feature.onboarding.impl

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.data.impl.ContaRepositoryImpl
import com.pingo.core.data.impl.InstrumentoRepositoryImpl
import com.pingo.core.data.impl.RoomTransacaoLocal
import com.pingo.core.database.PingoDatabase
import com.pingo.core.datastore.UserPreferencesDataSource
import com.pingo.core.datastore.UserPreferencesDataSourceImpl
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.BeneficioVoucher
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.TipoInstrumento
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class OnboardingContasRoomTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val agora = Instant.parse("2026-11-10T12:00:00Z")
    private val clock: Clock = Clock.fixed(agora, ZoneOffset.UTC)
    private val dbNome = "onboarding-contas-room-test.db"

    private lateinit var database: PingoDatabase
    private lateinit var preferences: UserPreferencesDataSourceImpl

    @Before
    fun preparaAmbiente() = runBlocking {
        contexto.getDatabasePath(dbNome).delete()
        preferences = UserPreferencesDataSourceImpl(contexto)
        preferences.clear()
        database = abrirBanco()
    }

    @After
    fun limpaAmbiente() = runBlocking {
        database.close()
        preferences.clear()
        contexto.getDatabasePath(dbNome).delete()
        Unit
    }

    @Test
    fun passo7ComDoisCreditosEDoisVouchersPersisteEReaberturaOfflineMantemDados() = runBlocking {
        val viewModel = criarViewModel(preferences)
        var concluiu = false
        exibirTela(viewModel) { concluiu = true }
        composeRule.waitUntil(ESPERA_MS) { !viewModel.uiState.value.recuperando }

        preencherNubankDoisCreditos()
        preencherCajuDoisVouchers()

        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.waitUntil(ESPERA_MS) { concluiu }
        assertTrue(preferences.contasConfiguradas())

        database.close()
        database = abrirBanco()

        val contasSalvas = repoContas().observeContas().first()
        val instSalvos = InstrumentoRepositoryImpl(database.instrumentoDao()).observeInstrumentos().first()
        assertEquals(
            setOf(InstituicaoFinanceira.NUBANK, InstituicaoFinanceira.CAJU),
            contasSalvas.map { it.instituicao }.toSet(),
        )
        assertEquals(4, instSalvos.size)
        assertEquals(2, instSalvos.count { it.tipo == TipoInstrumento.CREDITO })
        val vouchers = instSalvos.filter { it.tipo == TipoInstrumento.VOUCHER }
        assertEquals(
            mapOf(BeneficioVoucher.VR to 100_000L, BeneficioVoucher.VA to 25_050L),
            vouchers.associate { it.beneficio to it.saldoCentavos },
        )
    }

    @Test
    fun falhaNoDataStoreAposCommitPermiteEditarERetentarSemDuplicarContas() = runBlocking {
        val fonteComFalha = PreferenciasComFalhaDeConclusao(preferences, falharNaProximaConclusao = true)
        val viewModel = criarViewModel(fonteComFalha)
        var concluiu = false
        exibirTela(viewModel) { concluiu = true }
        composeRule.waitUntil(ESPERA_MS) { !viewModel.uiState.value.recuperando }

        composeRule.onNodeWithText("Nubank").performClick()
        composeRule.onNodeWithContentDescription("Fechamento").performScrollTo().performTextReplacement("5")
        composeRule.onNodeWithContentDescription("Vencimento").performScrollTo().performTextReplacement("12")
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.waitUntil(ESPERA_MS) { viewModel.uiState.value.salvamento == SalvamentoUiState.Error }

        composeRule.onNodeWithText("Não foi possível salvar suas contas. Tente de novo.").assertIsDisplayed()
        val contaAntes = repoContas().observeContas().first().single()
        val instAntes = InstrumentoRepositoryImpl(database.instrumentoDao()).observeInstrumentos().first().single()

        composeRule.onNodeWithContentDescription("Apelido da conta").performScrollTo().performTextReplacement("Nubank editado")
        composeRule.onNodeWithContentDescription("Fechamento").performScrollTo().performTextReplacement("8")
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.waitUntil(ESPERA_MS) { concluiu }

        database.close()
        database = abrirBanco()
        val contasDepois = repoContas().observeContas().first()
        val instDepois = InstrumentoRepositoryImpl(database.instrumentoDao()).observeInstrumentos().first()
        assertEquals(1, contasDepois.size)
        assertEquals(contaAntes.publicId, contasDepois.single().publicId)
        assertEquals("Nubank editado", contasDepois.single().apelido)
        assertEquals(1, instDepois.size)
        assertEquals(instAntes.publicId, instDepois.single().publicId)
        assertEquals(8, instDepois.single().fechamentoDia)
    }

    private fun preencherNubankDoisCreditos() {
        composeRule.onNodeWithText("Nubank").performClick()
        composeRule.onNodeWithContentDescription("Últimos 4 dígitos (opcional)").performScrollTo().performTextReplacement("1111")
        composeRule.onNodeWithContentDescription("Fechamento").performScrollTo().performTextReplacement("5")
        composeRule.onNodeWithContentDescription("Vencimento").performScrollTo().performTextReplacement("12")
        composeRule.onNodeWithText("+ Crédito").performScrollTo().performClick()
        composeRule.onAllNodesWithContentDescription("Apelido do crédito")[1].performScrollTo().performTextReplacement("Adicional")
        composeRule.onAllNodesWithContentDescription("Últimos 4 dígitos (opcional)")[1].performScrollTo().performTextReplacement("2222")
        composeRule.onAllNodesWithContentDescription("Fechamento")[1].performScrollTo().performTextReplacement("20")
        composeRule.onAllNodesWithContentDescription("Vencimento")[1].performScrollTo().performTextReplacement("28")
    }

    private fun preencherCajuDoisVouchers() {
        composeRule.onNodeWithText("Caju").performScrollTo().performClick()
        composeRule.onNodeWithText("VR").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Saldo inicial (R$)").performScrollTo().performTextReplacement("1.000")
        composeRule.onAllNodesWithText("+ Voucher").onLast().performScrollTo().performClick()
        composeRule.onAllNodesWithText("VA").onLast().performScrollTo().performClick()
        composeRule.onAllNodesWithContentDescription("Saldo inicial (R$)")[1].performScrollTo().performTextReplacement("250,50")
    }

    private fun abrirBanco(): PingoDatabase = Room.databaseBuilder(contexto, PingoDatabase::class.java, dbNome)
        .allowMainThreadQueries()
        .addCallback(
            object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(true)
                }
            },
        )
        .build()

    private fun repoContas(): ContaRepositoryImpl = ContaRepositoryImpl(
        database.contaDao(),
        database.instrumentoDao(),
        RoomTransacaoLocal(database),
    )

    private fun criarViewModel(fonte: UserPreferencesDataSource): OnboardingContasViewModel = OnboardingContasViewModel(
        contaRepository = repoContas(),
        instrumentoRepository = InstrumentoRepositoryImpl(database.instrumentoDao()),
        preferences = fonte,
        clock = clock,
    )

    private fun exibirTela(viewModel: OnboardingContasViewModel, onConcluir: () -> Unit) {
        composeRule.setContent {
            PingoTheme {
                OnboardingContasScreen(onConcluir = onConcluir, viewModel = viewModel)
            }
        }
    }

    private class PreferenciasComFalhaDeConclusao(private val delegate: UserPreferencesDataSource, var falharNaProximaConclusao: Boolean) :
        UserPreferencesDataSource by delegate {
        override suspend fun setContasConfiguradas(configuradas: Boolean) {
            if (configuradas && falharNaProximaConclusao) {
                falharNaProximaConclusao = false
                throw IOException("falha datastore")
            }
            delegate.setContasConfiguradas(configuradas)
        }
    }

    private companion object {
        const val ESPERA_MS = 5000L
    }
}
