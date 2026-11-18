package com.pingo.feature.accounts.impl

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.data.impl.ContaRepositoryImpl
import com.pingo.core.data.impl.InstrumentoRepositoryImpl
import com.pingo.core.data.impl.RoomTransacaoLocal
import com.pingo.core.database.PingoDatabase
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.TipoInstrumento
import com.pingo.feature.accounts.api.AccountsRoute
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class AccountsFluxoRoomTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val agora = Instant.parse("2026-11-10T12:00:00Z")
    private val clock: Clock = Clock.fixed(agora, ZoneOffset.UTC)
    private val dbNome = "accounts-fluxo-room-test.db"

    private lateinit var database: PingoDatabase
    private lateinit var contas: ContaRepositoryImpl
    private lateinit var instrumentos: InstrumentoRepositoryImpl

    @Before
    fun preparaBanco() {
        contexto.getDatabasePath(dbNome).delete()
        abrirRepositorios()
    }

    @After
    fun limpaBanco() {
        database.close()
        contexto.getDatabasePath(dbNome).delete()
    }

    @Test
    fun criarValidarEditarEReabrirBancoOfflinePreservaIdsEValores() = runBlocking {
        val fluxo = ControladorDeTela()
        exibirFluxo(fluxo)

        aguardarTexto("Adicionar conta")
        composeRule.onNodeWithText("Adicionar conta").performClick()

        validarLimitesDeDiaEValor()
        preencherDoisCreditosEVoucher()
        composeRule.onNodeWithText("Salvar conta").performScrollTo().performClick()

        val contaCriada = aguardarContaUnica()
        val idsOriginais = instrumentos.observePorConta(contaCriada.publicId).first().map { it.publicId }.toSet()
        assertEquals(3, idsOriginais.size)

        aguardarTexto("Nubank")
        composeRule.onNodeWithTag("conta-${contaCriada.publicId}").performScrollTo().performClick()
        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodesWithContentDescription("Apelido da conta").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithContentDescription("Apelido da conta").performScrollTo().performTextReplacement("Nubank Principal")
        composeRule.onNodeWithText("+ Débito").performScrollTo().performClick()
        composeRule.onNodeWithText("Salvar conta").performScrollTo().performClick()
        aguardarTexto("Nubank Principal")

        database.close()
        abrirRepositorios()

        val contaReaberta = contas.observeContas().first().single()
        val instReabertos = instrumentos.observePorConta(contaCriada.publicId).first()
        assertEquals(contaCriada.publicId, contaReaberta.publicId)
        assertEquals("Nubank Principal", contaReaberta.apelido)
        assertEquals(4, instReabertos.size)
        assertEquals(idsOriginais, idsOriginais.intersect(instReabertos.map { it.publicId }.toSet()))
        assertEquals(100_000L, instReabertos.single { it.tipo == TipoInstrumento.VOUCHER }.saldoCentavos)
        assertEquals(1, instReabertos.count { it.tipo == TipoInstrumento.DEBITO })
    }

    @Test
    fun criarOutraComDebitoPadraoPersisteNoBancoOffline() = runBlocking {
        val fluxo = ControladorDeTela()
        exibirFluxo(fluxo)

        aguardarTexto("Adicionar conta")
        composeRule.onNodeWithText("Adicionar conta").performClick()
        composeRule.onNodeWithTag("instituicao-OUTRA").performScrollTo().performClick()
        composeRule.onNodeWithText("Salvar conta").performScrollTo().performClick()
        val contaCriada = aguardarContaUnica()

        database.close()
        abrirRepositorios()

        val reaberta = contas.observeContas().first().single()
        val instrumentosSalvos = instrumentos.observePorConta(reaberta.publicId).first()
        assertEquals(contaCriada.publicId, reaberta.publicId)
        assertEquals(InstituicaoFinanceira.OUTRA, reaberta.instituicao)
        assertEquals(1, instrumentosSalvos.size)
        assertEquals(TipoInstrumento.DEBITO, instrumentosSalvos.single().tipo)
    }

    private fun validarLimitesDeDiaEValor() {
        composeRule.onNodeWithContentDescription("Fechamento").performScrollTo().performTextReplacement("0")
        composeRule.onNodeWithContentDescription("Vencimento").performScrollTo().performTextReplacement("32")
        composeRule.onAllNodesWithText("Informe um dia entre 1 e 31").assertCountEquals(2)
        composeRule.onNodeWithText("Salvar conta").performScrollTo().assertIsNotEnabled()
    }

    private fun preencherDoisCreditosEVoucher() {
        composeRule.onNodeWithContentDescription("Últimos 4 dígitos (opcional)").performScrollTo().performTextReplacement("1111")
        composeRule.onNodeWithContentDescription("Fechamento").performScrollTo().performTextReplacement("5")
        composeRule.onNodeWithContentDescription("Vencimento").performScrollTo().performTextReplacement("12")
        composeRule.onNodeWithText("+ Crédito").performScrollTo().performClick()
        composeRule.onAllNodesWithContentDescription("Apelido do crédito")[1].performScrollTo().performTextReplacement("Adicional")
        composeRule.onAllNodesWithContentDescription("Fechamento")[1].performScrollTo().performTextReplacement("20")
        composeRule.onAllNodesWithContentDescription("Vencimento")[1].performScrollTo().performTextReplacement("28")
        composeRule.onNodeWithText("+ Voucher").performScrollTo().performClick()
        composeRule.onNodeWithText("VR").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Saldo inicial (R$)").performScrollTo().performTextReplacement("1,234")
        composeRule.onNodeWithText("Informe um valor em reais.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Salvar conta").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Saldo inicial (R$)").performScrollTo().performTextReplacement("1.000,00")
    }

    private fun aguardarTexto(texto: String) {
        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun aguardarContaUnica(): Conta {
        composeRule.waitUntil(ESPERA_MS) { runBlocking { contas.observeContas().first() }.isNotEmpty() }
        return runBlocking { contas.observeContas().first().single() }
    }

    private fun abrirRepositorios() {
        database = Room.databaseBuilder(contexto, PingoDatabase::class.java, dbNome)
            .allowMainThreadQueries()
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        db.setForeignKeyConstraintsEnabled(true)
                    }
                },
            )
            .build()
        contas = ContaRepositoryImpl(database.contaDao(), database.instrumentoDao(), RoomTransacaoLocal(database))
        instrumentos = InstrumentoRepositoryImpl(database.instrumentoDao())
    }

    private fun exibirFluxo(fluxo: ControladorDeTela) {
        composeRule.setContent {
            PingoTheme {
                when (val destino = fluxo.telaAtual) {
                    Tela.Lista -> {
                        val listaVm = remember(database) { AccountsViewModel(contas, instrumentos) }
                        AccountsScreen(
                            onSelecionarAba = {},
                            onNovaConta = { fluxo.abrirFormulario(null) },
                            onAbrirConta = { id -> fluxo.abrirFormulario(id) },
                            viewModel = listaVm,
                        )
                    }
                    is Tela.Formulario -> {
                        val formVm = remember(destino) {
                            val handle = if (destino.contaId == null) {
                                SavedStateHandle()
                            } else {
                                SavedStateHandle(mapOf(AccountsRoute.ARG_CONTA_ID to destino.contaId))
                            }
                            ContaFormViewModel(contas, instrumentos, clock, handle)
                        }
                        ContaFormScreen(onVoltar = { fluxo.voltarParaLista() }, viewModel = formVm)
                    }
                }
            }
        }
    }

    private sealed interface Tela {
        data object Lista : Tela

        data class Formulario(val contaId: String?) : Tela
    }

    private class ControladorDeTela {
        var telaAtual by mutableStateOf<Tela>(Tela.Lista)

        fun abrirFormulario(contaId: String?) {
            telaAtual = Tela.Formulario(contaId)
        }

        fun voltarParaLista() {
            telaAtual = Tela.Lista
        }
    }

    private companion object {
        const val ESPERA_MS = 5000L
    }
}
