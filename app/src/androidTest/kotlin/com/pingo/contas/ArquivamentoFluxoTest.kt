package com.pingo.contas

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.data.di.CapturaEntryPoint
import com.pingo.core.data.impl.RegraCategoriaRepositoryImpl
import com.pingo.core.data.impl.RoomTransacaoLocal
import com.pingo.core.data.manual.RegistrarLancamentoManual
import com.pingo.core.database.PingoDatabase
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Lancamento
import com.pingo.core.model.NovaConta
import com.pingo.core.model.NovoInstrumento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.feature.accounts.api.AccountsRoute
import com.pingo.feature.accounts.impl.AccountsScreen
import com.pingo.feature.accounts.impl.AccountsViewModel
import com.pingo.feature.accounts.impl.ContaFormScreen
import com.pingo.feature.accounts.impl.ContaFormViewModel
import com.pingo.feature.transactions.impl.NovoLancamentoScreen
import com.pingo.feature.transactions.impl.NovoLancamentoViewModel
import com.pingo.feature.transactions.impl.TransactionsScreen
import com.pingo.feature.transactions.impl.TransactionsViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ArquivamentoFluxoTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val agora: Instant = Instant.parse("2026-11-10T12:00:00Z")
    private val clock: Clock = Clock.fixed(agora, ZoneOffset.UTC)
    private val captura: CapturaEntryPoint =
        EntryPointAccessors.fromApplication(ApplicationProvider.getApplicationContext(), CapturaEntryPoint::class.java)

    private val database: PingoDatabase by lazy { captura.database() }
    private val contas get() = captura.contaRepository()
    private val instrumentos get() = captura.instrumentoRepository()
    private val lancamentos get() = captura.lancamentoRepository()

    private lateinit var accountsViewModel: AccountsViewModel
    private lateinit var formViewModel: ContaFormViewModel
    private var transactionsViewModel: TransactionsViewModel? = null
    private var novoLancamentoViewModel: NovoLancamentoViewModel? = null
    private var tela by mutableStateOf(Tela.CONTAS)
    private var versaoDoFormulario by mutableStateOf(0)

    @Before
    fun limpaBanco() {
        database.clearAllTables()
    }

    @After
    fun fechaBanco() {
        database.clearAllTables()
    }

    @Test
    fun arquivarEreativarInstrumentoIsoladoPreservaContaIrmaosEIdAposReabrir() {
        val cenario = runBlocking { semearContaComInstrumentos() }
        abrirLista()
        mostrar()
        abrirFormulario(cenario.contaId)
        aguardarFormulario(cenario.contaId)

        composeRule.onNodeWithTag("arquivar-instrumento-${cenario.debitoId}").performScrollTo().performClick()
        composeRule.onNodeWithTag("confirmar-arquivamento").performClick()
        composeRule.waitUntil(TEMPO_DE_ESPERA) { runBlocking { instrumentos.buscar(cenario.debitoId)?.arquivado == true } }

        val contaAposArquivar = runBlocking { contas.buscar(cenario.contaId) }
        val creditoAposArquivar = runBlocking { instrumentos.buscar(cenario.creditoId) }
        val debitoArquivado = runBlocking { instrumentos.buscar(cenario.debitoId) }
        assertFalse(contaAposArquivar?.arquivada == true)
        assertFalse(creditoAposArquivar?.arquivado == true)
        assertTrue(debitoArquivado?.arquivado == true)
        assertFalse(debitoArquivado?.arquivadoPorCascata == true)

        abrirLista()
        composeRule.waitUntil(TEMPO_DE_ESPERA) {
            accountsViewModel.uiState.value.grupos.any { grupo ->
                grupo.contas.any { item -> item.conta.publicId == cenario.contaId }
            }
        }
        abrirFormulario(cenario.contaId)
        aguardarFormulario(cenario.contaId)
        composeRule.onNodeWithText("Arquivado").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("reativar-instrumento-${cenario.debitoId}").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_DE_ESPERA) { runBlocking { instrumentos.buscar(cenario.debitoId)?.arquivado == false } }

        val reativado = runBlocking { instrumentos.buscar(cenario.debitoId) }
        val irmao = runBlocking { instrumentos.buscar(cenario.creditoId) }
        assertEquals(cenario.debitoId, reativado?.publicId)
        assertEquals("Débito", reativado?.apelido)
        assertFalse(reativado?.arquivado == true)
        assertEquals(cenario.creditoId, irmao?.publicId)
        assertFalse(irmao?.arquivado == true)
        assertEquals(cenario.contaId, runBlocking { contas.buscar(cenario.contaId) }?.publicId)
        assertEquals(1, runBlocking { contas.observeContas().first() }.size)
    }

    @Test
    fun cascataCanceladaNaoAlteraEConfirmadaReativaSomenteOQueArquivouAposReabrir() {
        val cenario = runBlocking { semearContaComInstrumentoPreviamenteArquivado() }
        abrirLista()
        mostrar()
        abrirFormulario(cenario.contaId)
        aguardarFormulario(cenario.contaId)

        composeRule.onNodeWithTag("excluir-conta").performScrollTo().performClick()
        composeRule.onNodeWithText("Arquivar esta conta?").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()

        assertFalse(runBlocking { contas.buscar(cenario.contaId) }?.arquivada == true)
        assertFalse(runBlocking { instrumentos.buscar(cenario.creditoId) }?.arquivado == true)
        assertFalse(runBlocking { instrumentos.buscar(cenario.debitoId) }?.arquivado == true)
        assertTrue(runBlocking { instrumentos.buscar(cenario.antigoId) }?.arquivado == true)

        composeRule.onNodeWithTag("excluir-conta").performScrollTo().performClick()
        composeRule.onNodeWithTag("confirmar-arquivamento").performClick()
        composeRule.waitUntil(TEMPO_DE_ESPERA) { runBlocking { contas.buscar(cenario.contaId)?.arquivada == true } }

        val creditoArquivado = runBlocking { instrumentos.buscar(cenario.creditoId) }
        val debitoArquivado = runBlocking { instrumentos.buscar(cenario.debitoId) }
        val antigoArquivado = runBlocking { instrumentos.buscar(cenario.antigoId) }
        assertTrue(creditoArquivado?.arquivado == true)
        assertTrue(creditoArquivado?.arquivadoPorCascata == true)
        assertTrue(debitoArquivado?.arquivado == true)
        assertTrue(debitoArquivado?.arquivadoPorCascata == true)
        assertTrue(antigoArquivado?.arquivado == true)
        assertFalse(antigoArquivado?.arquivadoPorCascata == true)

        abrirLista()
        composeRule.waitUntil(TEMPO_DE_ESPERA) {
            accountsViewModel.uiState.value.arquivadas.any { it.conta.publicId == cenario.contaId }
        }
        composeRule.onNodeWithTag("conta-arquivada-${cenario.contaId}").performScrollTo().assertIsDisplayed()

        abrirFormulario(cenario.contaId)
        aguardarFormulario(cenario.contaId)
        composeRule.waitUntil(TEMPO_DE_ESPERA) { formViewModel.uiState.value.contaArquivada }
        composeRule.onNodeWithTag("conta-arquivada").assertIsDisplayed()
        composeRule.onNodeWithTag("reativar-conta").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_DE_ESPERA) { runBlocking { contas.buscar(cenario.contaId)?.arquivada == false } }

        val creditoReativado = runBlocking { instrumentos.buscar(cenario.creditoId) }
        val debitoReativado = runBlocking { instrumentos.buscar(cenario.debitoId) }
        val antigoMantido = runBlocking { instrumentos.buscar(cenario.antigoId) }
        assertFalse(creditoReativado?.arquivado == true)
        assertFalse(creditoReativado?.arquivadoPorCascata == true)
        assertFalse(debitoReativado?.arquivado == true)
        assertFalse(debitoReativado?.arquivadoPorCascata == true)
        assertTrue(antigoMantido?.arquivado == true)
        assertFalse(antigoMantido?.arquivadoPorCascata == true)

        composeRule.waitUntil(TEMPO_DE_ESPERA) { !formViewModel.uiState.value.contaArquivada }
        composeRule.onNodeWithTag("excluir-conta").performScrollTo().performClick()
        composeRule.onNodeWithTag("confirmar-arquivamento").performClick()
        composeRule.waitUntil(TEMPO_DE_ESPERA) { runBlocking { contas.buscar(cenario.contaId)?.arquivada == true } }
        abrirFormulario(cenario.contaId)
        aguardarFormulario(cenario.contaId)
        composeRule.waitUntil(TEMPO_DE_ESPERA) { formViewModel.uiState.value.contaArquivada }
        composeRule.onNodeWithTag("reativar-conta").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_DE_ESPERA) { runBlocking { contas.buscar(cenario.contaId)?.arquivada == false } }

        assertEquals(1, runBlocking { contas.observeContas().first() }.size)
        assertEquals(3, runBlocking { instrumentos.observePorConta(cenario.contaId).first() }.size)
        assertFalse(runBlocking { instrumentos.buscar(cenario.creditoId) }?.arquivado == true)
        assertFalse(runBlocking { instrumentos.buscar(cenario.debitoId) }?.arquivado == true)
        assertTrue(runBlocking { instrumentos.buscar(cenario.antigoId) }?.arquivado == true)
    }

    @Test
    fun arquivarContaComHistoricoMantemLancamentosConsultaveisEmLancamentos() {
        val hoje = Instant.now()
        val cenario = runBlocking { semearContaComLancamento(hoje) }
        abrirLista()
        mostrar()
        abrirFormulario(cenario.contaId)
        aguardarFormulario(cenario.contaId)

        composeRule.onNodeWithTag("excluir-conta").performScrollTo().performClick()
        composeRule.onNodeWithTag("confirmar-arquivamento").performClick()
        composeRule.waitUntil(TEMPO_DE_ESPERA) { runBlocking { contas.buscar(cenario.contaId)?.arquivada == true } }

        val lancamento = runBlocking { lancamentos.buscar(cenario.lancamentoId) }
        assertEquals(4290L, lancamento?.valorCentavos)
        assertEquals(cenario.instrumentoId, lancamento?.instrumentoPublicId)
        assertNull(lancamento?.excluidoEm)
        assertEquals(StatusLancamento.CONFIRMADO, lancamento?.status)

        abrirLista()
        composeRule.waitUntil(TEMPO_DE_ESPERA) {
            accountsViewModel.uiState.value.arquivadas.any { it.conta.publicId == cenario.contaId }
        }
        composeRule.onNodeWithTag("conta-arquivada-${cenario.contaId}").performScrollTo().assertIsDisplayed()

        transactionsViewModel = TransactionsViewModel(
            lancamentoRepository = lancamentos,
            contaRepository = contas,
            instrumentoRepository = instrumentos,
            categoriaRepository = captura.categoriaRepository(),
            clock = Clock.systemDefaultZone(),
        )
        tela = Tela.LANCAMENTOS
        composeRule.waitUntil(TEMPO_DE_ESPERA) { transactionsViewModel?.uiState?.value?.carregando == false }
        composeRule.onNodeWithTag("lancamento-${cenario.lancamentoId}").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Padaria do Zé").assertIsDisplayed()
        composeRule.onAllNodesWithText("- R$ 42,90").onFirst().assertIsDisplayed()

        val historico = runBlocking { instrumentos.observeInstrumentos().first() }
        assertTrue(historico.any { it.publicId == cenario.instrumentoId && it.contaPublicId == cenario.contaId })
        assertTrue(runBlocking { lancamentos.observeLancamentos().first() }.any { it.publicId == cenario.lancamentoId })
    }

    @Test
    fun seletorDeNovoLancamentoSoOfereceParAtivoAntesEDepoisDeArquivarEReativar() {
        val cenario = runBlocking { semearContaComInstrumentos() }
        val regras = RegraCategoriaRepositoryImpl(database.regraCategoriaDao())
        novoLancamentoViewModel = NovoLancamentoViewModel(
            contaRepository = contas,
            instrumentoRepository = instrumentos,
            categoriaRepository = captura.categoriaRepository(),
            regraCategoriaRepository = regras,
            registrarLancamentoManual = RegistrarLancamentoManual(
                lancamentoRepository = lancamentos,
                instrumentoRepository = instrumentos,
                transacaoLocal = RoomTransacaoLocal(database),
                clock = clock,
            ),
            clock = clock,
        )
        abrirLista()
        mostrar()
        tela = Tela.NOVO
        composeRule.waitUntil(TEMPO_DE_ESPERA) { novoLancamentoViewModel?.uiState?.value?.carregando == false }

        composeRule.onNodeWithTag("campo-instrumento").performClick()
        composeRule.onNodeWithTag("instrumento-opcao-${cenario.creditoId}").assertIsDisplayed()
        composeRule.onNodeWithTag("instrumento-opcao-${cenario.debitoId}").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()

        runBlocking { instrumentos.arquivar(cenario.creditoId, agora) }
        composeRule.waitUntil(TEMPO_DE_ESPERA) {
            novoLancamentoViewModel?.uiState?.value?.instrumentos?.none { it.publicId == cenario.creditoId } == true
        }
        composeRule.onNodeWithTag("campo-instrumento").performClick()
        composeRule.onNodeWithTag("instrumento-opcao-${cenario.creditoId}").assertDoesNotExist()
        composeRule.onNodeWithTag("instrumento-opcao-${cenario.debitoId}").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()

        runBlocking { instrumentos.reativar(cenario.creditoId, agora) }
        composeRule.waitUntil(TEMPO_DE_ESPERA) {
            novoLancamentoViewModel?.uiState?.value?.instrumentos?.any { it.publicId == cenario.creditoId } == true
        }
        composeRule.onNodeWithTag("campo-instrumento").performClick()
        composeRule.onNodeWithTag("instrumento-opcao-${cenario.creditoId}").assertIsDisplayed()
    }

    private fun mostrar() {
        composeRule.setContent {
            PingoTheme {
                when (tela) {
                    Tela.CONTAS -> AccountsScreen(
                        onSelecionarAba = {},
                        onNovaConta = {},
                        onAbrirConta = { abrirFormulario(it) },
                        viewModel = accountsViewModel,
                    )

                    Tela.FORMULARIO -> key(versaoDoFormulario) {
                        ContaFormScreen(
                            onVoltar = { abrirLista() },
                            viewModel = formViewModel,
                        )
                    }

                    Tela.LANCAMENTOS -> transactionsViewModel?.let { viewModel ->
                        TransactionsScreen(
                            onOpenDetalhe = {},
                            onOpenNovo = {},
                            onSelecionarAba = {},
                            viewModel = viewModel,
                        )
                    }

                    Tela.NOVO -> novoLancamentoViewModel?.let { viewModel ->
                        NovoLancamentoScreen(onVoltar = { abrirLista() }, viewModel = viewModel)
                    }
                }
            }
        }
    }

    private fun abrirLista() {
        accountsViewModel = AccountsViewModel(contas, instrumentos)
        tela = Tela.CONTAS
    }

    private fun abrirFormulario(contaPublicId: String) {
        formViewModel = ContaFormViewModel(
            contas,
            instrumentos,
            clock,
            SavedStateHandle(mapOf(AccountsRoute.ARG_CONTA_ID to contaPublicId)),
        )
        versaoDoFormulario++
        tela = Tela.FORMULARIO
    }

    private fun aguardarFormulario(contaPublicId: String) {
        composeRule.waitUntil(TEMPO_DE_ESPERA) { formViewModel.uiState.value.formulario.publicId == contaPublicId }
    }

    private suspend fun semearContaComInstrumentos(): Cenario {
        contas.criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.NUBANK,
                    apelido = "Nubank",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(
                        NovoInstrumento(tipo = TipoInstrumento.CREDITO, apelido = "Crédito", fechamentoDia = 5, vencimentoDia = 12),
                        NovoInstrumento(tipo = TipoInstrumento.DEBITO, apelido = "Débito"),
                    ),
                ),
            ),
            agora,
        )

        val conta = contas.observeContas().first().single()
        val instrumentosDaConta = instrumentos.observePorConta(conta.publicId).first()

        return Cenario(
            contaId = conta.publicId,
            creditoId = instrumentosDaConta.first { it.tipo == TipoInstrumento.CREDITO }.publicId,
            debitoId = instrumentosDaConta.first { it.tipo == TipoInstrumento.DEBITO }.publicId,
            antigoId = "",
            instrumentoId = "",
            lancamentoId = "",
        )
    }

    private suspend fun semearContaComInstrumentoPreviamenteArquivado(): Cenario {
        contas.criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.INTER,
                    apelido = "Inter",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(
                        NovoInstrumento(tipo = TipoInstrumento.CREDITO, apelido = "Crédito", fechamentoDia = 5, vencimentoDia = 12),
                        NovoInstrumento(tipo = TipoInstrumento.DEBITO, apelido = "Débito"),
                        NovoInstrumento(tipo = TipoInstrumento.DEBITO, apelido = "Débito antigo"),
                    ),
                ),
            ),
            agora,
        )

        val conta = contas.observeContas().first().single()
        val instrumentosDaConta = instrumentos.observePorConta(conta.publicId).first()
        val antigo = instrumentosDaConta.first { it.apelido == "Débito antigo" }
        instrumentos.arquivar(antigo.publicId, agora.plusSeconds(60))

        return Cenario(
            contaId = conta.publicId,
            creditoId = instrumentosDaConta.first { it.apelido == "Crédito" }.publicId,
            debitoId = instrumentosDaConta.first { it.apelido == "Débito" }.publicId,
            antigoId = antigo.publicId,
            instrumentoId = "",
            lancamentoId = "",
        )
    }

    private suspend fun semearContaComLancamento(quando: Instant): Cenario {
        contas.criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.NUBANK,
                    apelido = "Nubank",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(NovoInstrumento(tipo = TipoInstrumento.DEBITO, apelido = "Débito")),
                ),
            ),
            quando,
        )

        val conta = contas.observeContas().first().single()
        val instrumento = instrumentos.observePorConta(conta.publicId).first().single()
        val lancamentoId = UUID.randomUUID().toString()

        lancamentos.salvar(
            Lancamento(
                publicId = lancamentoId,
                instrumentoPublicId = instrumento.publicId,
                valorCentavos = 4290,
                dataHora = quando,
                tipo = TipoLancamento.DESPESA,
                estabelecimento = "Padaria do Zé",
                status = StatusLancamento.CONFIRMADO,
                origem = OrigemLancamento.MANUAL,
                confianca = 1.0,
                criadoEm = quando,
                atualizadoEm = quando,
            ),
        )

        return Cenario(
            contaId = conta.publicId,
            creditoId = "",
            debitoId = instrumento.publicId,
            antigoId = "",
            instrumentoId = instrumento.publicId,
            lancamentoId = lancamentoId,
        )
    }

    private data class Cenario(
        val contaId: String,
        val creditoId: String,
        val debitoId: String,
        val antigoId: String,
        val instrumentoId: String,
        val lancamentoId: String,
    )

    private enum class Tela {
        CONTAS,
        FORMULARIO,
        LANCAMENTOS,
        NOVO,
    }

    private companion object {
        const val TEMPO_DE_ESPERA = 5_000L
    }
}
