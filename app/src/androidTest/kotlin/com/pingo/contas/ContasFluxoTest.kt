package com.pingo.contas

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.data.di.CapturaEntryPoint
import com.pingo.core.data.impl.LancamentoRepositoryImpl
import com.pingo.core.data.impl.RegraCategoriaRepositoryImpl
import com.pingo.core.data.impl.RoomTransacaoLocal
import com.pingo.core.data.manual.RegistrarLancamentoManual
import com.pingo.core.database.PingoDatabase
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.TipoInstrumento
import com.pingo.feature.accounts.impl.AccountsScreen
import com.pingo.feature.accounts.impl.AccountsViewModel
import com.pingo.feature.accounts.impl.ContaFormScreen
import com.pingo.feature.accounts.impl.ContaFormViewModel
import com.pingo.feature.transactions.impl.NovoLancamentoScreen
import com.pingo.feature.transactions.impl.NovoLancamentoViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class ContasFluxoTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val agora: Instant = Instant.parse("2026-11-10T12:00:00Z")
    private val clock: Clock = Clock.fixed(agora, ZoneOffset.UTC)
    private val captura: CapturaEntryPoint =
        EntryPointAccessors.fromApplication(ApplicationProvider.getApplicationContext(), CapturaEntryPoint::class.java)

    private val database: PingoDatabase by lazy { captura.database() }
    private lateinit var accountsViewModel: AccountsViewModel
    private lateinit var formViewModel: ContaFormViewModel
    private var manualViewModel: NovoLancamentoViewModel? = null
    private var tela by mutableStateOf(Tela.CONTAS)
    private var contaAberta: String? = null

    @Before
    fun limpaBanco() {
        database.clearAllTables()
    }

    @After
    fun fechaBanco() {
        database.clearAllTables()
    }

    @Test
    fun contaOutraCriadaNaAbaContasFicaDisponivelNoLancamentoManualVinculado() {
        accountsViewModel = AccountsViewModel(captura.contaRepository(), captura.instrumentoRepository())
        formViewModel = ContaFormViewModel(
            captura.contaRepository(),
            captura.instrumentoRepository(),
            clock,
            SavedStateHandle(),
        )
        mostrar()

        composeRule.onNodeWithTag("contas-vazio").assertIsDisplayed()
        composeRule.onNodeWithText("Adicionar conta").performClick()
        composeRule.onNodeWithText("Qual é a instituição?").assertIsDisplayed()

        composeRule.onNodeWithTag("instituicao-OUTRA").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Apelido da conta").performTextClearance()
        composeRule.onNodeWithContentDescription("Apelido da conta").performTextInput("Conta manual")
        composeRule.onNodeWithText("Salvar conta").performScrollTo().performClick()

        composeRule.waitUntil(TEMPO_DE_ESPERA) { formViewModel.uiState.value.concluido }
        composeRule.waitUntil(TEMPO_DE_ESPERA) {
            composeRule.onAllNodesWithText("Conta manual").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Débito · Cartão de débito").assertIsDisplayed()

        val conta = runBlocking { captura.contaRepository().observeContas().first().single() }
        val instrumento = runBlocking { captura.instrumentoRepository().observeInstrumentos().first().single() }
        assertEquals(InstituicaoFinanceira.OUTRA, conta.instituicao)
        assertEquals(TipoInstrumento.DEBITO, instrumento.tipo)
        assertFalse(instrumento.arquivado)

        composeRule.onNodeWithTag("conta-${conta.publicId}").performClick()
        assertEquals(conta.publicId, contaAberta)

        val reaberto = AccountsViewModel(captura.contaRepository(), captura.instrumentoRepository())
        composeRule.waitUntil(TEMPO_DE_ESPERA) {
            reaberto.uiState.value.grupos.any { grupo ->
                grupo.contas.any { item -> item.conta.publicId == conta.publicId }
            }
        }

        manualViewModel = criarLancamentoManualViewModel()
        tela = Tela.MANUAL
        composeRule.waitUntil(TEMPO_DE_ESPERA) { manualViewModel?.uiState?.value?.carregando == false }
        assertEquals(instrumento.publicId, manualViewModel?.uiState?.value?.instrumentos?.single()?.publicId)

        listOf("tecla-4", "tecla-2", "tecla-9", "tecla-0").forEach { tecla ->
            composeRule.onNodeWithTag(tecla).performClick()
        }
        composeRule.onNodeWithTag("salvar-lancamento").performClick()
        composeRule.waitUntil(TEMPO_DE_ESPERA) { manualViewModel?.uiState?.value?.salvo == true }

        val lancamento = runBlocking { captura.lancamentoRepository().observeLancamentos().first().single() }
        assertEquals(OrigemLancamento.MANUAL, lancamento.origem)
        assertEquals(instrumento.publicId, lancamento.instrumentoPublicId)
        assertEquals(4290L, lancamento.valorCentavos)
    }

    private fun mostrar() {
        composeRule.setContent {
            PingoTheme {
                when (tela) {
                    Tela.CONTAS -> AccountsScreen(
                        onSelecionarAba = {},
                        onNovaConta = { tela = Tela.FORMULARIO },
                        onAbrirConta = { contaAberta = it },
                        viewModel = accountsViewModel,
                    )

                    Tela.FORMULARIO -> ContaFormScreen(
                        onVoltar = { tela = Tela.CONTAS },
                        viewModel = formViewModel,
                    )

                    Tela.MANUAL -> manualViewModel?.let { viewModel ->
                        NovoLancamentoScreen(onVoltar = {}, viewModel = viewModel)
                    }
                }
            }
        }
    }

    private fun criarLancamentoManualViewModel(): NovoLancamentoViewModel {
        val lancamentos = LancamentoRepositoryImpl(database.lancamentoDao())
        val regras = RegraCategoriaRepositoryImpl(database.regraCategoriaDao())

        return NovoLancamentoViewModel(
            contaRepository = captura.contaRepository(),
            instrumentoRepository = captura.instrumentoRepository(),
            categoriaRepository = captura.categoriaRepository(),
            regraCategoriaRepository = regras,
            registrarLancamentoManual = RegistrarLancamentoManual(
                lancamentoRepository = lancamentos,
                instrumentoRepository = captura.instrumentoRepository(),
                transacaoLocal = RoomTransacaoLocal(database),
                clock = clock,
            ),
            clock = clock,
        )
    }

    private enum class Tela {
        CONTAS,
        FORMULARIO,
        MANUAL,
    }

    private companion object {
        const val TEMPO_DE_ESPERA = 5_000L
    }
}
