package com.pingo.contas

import android.content.Context
import androidx.activity.compose.setContent
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
import com.pingo.core.datastore.CapturaDataSourceImpl
import com.pingo.core.datastore.UserPreferencesDataSourceImpl
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.TipoInstrumento
import com.pingo.navigation.DestinoInicial
import com.pingo.navigation.PingoNavHost
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

@RunWith(AndroidJUnit4::class)
class OnboardingContasEdicaoNavegacaoTest {
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
    fun onboardingComDoisCreditosNavegacaoParaEdicaoEReaberturaOfflinePreservaIdsEValores() {
        runBlocking {
            preferences.setConsent("v1", Instant.now())
            preferences.setContasConfiguradas(false)
        }

        abrirNavHost(DestinoInicial.CONTAS)
        preencherOnboardingNubankDoisCreditos()
        concluirOnboardingEAguardarInicio()

        val contaInicial = runBlocking { captura.contaRepository().observeContas().first().single() }
        val creditosIniciais = runBlocking { captura.instrumentoRepository().observePorConta(contaInicial.publicId).first() }
        assertEquals(2, creditosIniciais.size)
        assertTrue(creditosIniciais.all { it.tipo == TipoInstrumento.CREDITO })
        val primeiroInicial = creditosIniciais.single { it.apelido == "Cartão A" }
        val segundoInicial = creditosIniciais.single { it.apelido == "Cartão B" }

        editarContaPelaAbaContas(contaInicial.publicId)
        reabrirAppOfflineEVerificarPersistencia(
            contaId = contaInicial.publicId,
            contaCriadoEm = contaInicial.criadoEm,
            primeiroId = primeiroInicial.publicId,
            primeiroCriadoEm = primeiroInicial.criadoEm,
            segundoId = segundoInicial.publicId,
            segundoCriadoEm = segundoInicial.criadoEm,
        )
    }

    private fun abrirNavHost(destino: DestinoInicial) {
        scenario?.close()
        val novo = ActivityScenario.launch(MainActivity::class.java)
        scenario = novo
        novo.onActivity { activity ->
            activity.setContent {
                PingoTheme {
                    PingoNavHost(
                        session = SessionState.LoggedIn,
                        destinoInicial = destino,
                        onConsentAccepted = {},
                    )
                }
            }
        }
    }

    private fun preencherOnboardingNubankDoisCreditos() {
        aguardarTexto("Quais bancos você usa?")
        composeRule.onNodeWithText("Nubank").performClick()
        aguardarDescricao("Apelido do crédito")
        composeRule.onNodeWithContentDescription("Apelido do crédito").performScrollTo().performTextReplacement("Cartão A")
        composeRule.onNodeWithContentDescription("Últimos 4 dígitos (opcional)").performScrollTo().performTextReplacement("1111")
        composeRule.onNodeWithContentDescription("Fechamento").performScrollTo().performTextReplacement("5")
        composeRule.onNodeWithContentDescription("Vencimento").performScrollTo().performTextReplacement("12")

        composeRule.onNodeWithText("+ Crédito").performScrollTo().performClick()
        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodesWithContentDescription("Apelido do crédito").fetchSemanticsNodes().size == 2
        }
        composeRule.onAllNodesWithContentDescription("Apelido do crédito")[1].performScrollTo().performTextReplacement("Cartão B")
        composeRule.onAllNodesWithContentDescription("Últimos 4 dígitos (opcional)")[1].performScrollTo().performTextReplacement("2222")
        composeRule.onAllNodesWithContentDescription("Fechamento")[1].performScrollTo().performTextReplacement("20")
        composeRule.onAllNodesWithContentDescription("Vencimento")[1].performScrollTo().performTextReplacement("28")
    }

    private fun concluirOnboardingEAguardarInicio() {
        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodesWithTag("botao_agora_nao_biometria").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("botao_agora_nao_biometria").performClick()
        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodesWithTag("aba-CONTAS").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(runBlocking { preferences.contasConfiguradas() })
    }

    private fun editarContaPelaAbaContas(contaId: String) {
        composeRule.onNodeWithTag("aba-CONTAS").performClick()
        aguardarTag("conta-$contaId")
        composeRule.onNodeWithText("Crédito · Cartão A · final 1111").assertIsDisplayed()
        composeRule.onNodeWithText("Crédito · Cartão B · final 2222").assertIsDisplayed()

        composeRule.onNodeWithTag("conta-$contaId").performClick()
        aguardarTexto("Editar conta")

        composeRule.onNodeWithContentDescription("Apelido da conta").performScrollTo().performTextReplacement("Nubank Atualizada")
        composeRule.onAllNodesWithContentDescription("Últimos 4 dígitos (opcional)")[0].performScrollTo().performTextReplacement("3333")
        composeRule.onAllNodesWithContentDescription("Fechamento")[0].performScrollTo().performTextReplacement("7")
        composeRule.onAllNodesWithContentDescription("Vencimento")[0].performScrollTo().performTextReplacement("14")
        composeRule.onAllNodesWithContentDescription("Apelido do crédito")[1].performScrollTo().performTextReplacement("Cartão B Família")

        composeRule.onNodeWithText("Salvar conta").performScrollTo().performClick()
        aguardarTag("conta-$contaId")
        aguardarTexto("Nubank Atualizada")
        aguardarTexto("Crédito · Cartão A · final 3333")
        composeRule.onNodeWithText("Crédito · Cartão B Família · final 2222").assertIsDisplayed()
    }

    private fun reabrirAppOfflineEVerificarPersistencia(
        contaId: String,
        contaCriadoEm: Instant,
        primeiroId: String,
        primeiroCriadoEm: Instant,
        segundoId: String,
        segundoCriadoEm: Instant,
    ) {
        abrirNavHost(DestinoInicial.HOME)
        aguardarTag("aba-CONTAS")
        composeRule.onNodeWithTag("aba-CONTAS").performClick()
        aguardarTag("conta-$contaId")
        composeRule.onNodeWithText("Nubank Atualizada").assertIsDisplayed()

        composeRule.onNodeWithTag("conta-$contaId").performClick()
        aguardarTexto("Editar conta")
        composeRule.onNodeWithText("Nubank Atualizada").assertIsDisplayed()
        composeRule.onNodeWithText("Cartão B Família").performScrollTo().assertIsDisplayed()

        val contaReaberta = runBlocking { captura.contaRepository().observeContas().first().single() }
        val instrumentosReabertos = runBlocking { captura.instrumentoRepository().observePorConta(contaId).first() }
        assertEquals(contaId, contaReaberta.publicId)
        assertEquals(InstituicaoFinanceira.NUBANK, contaReaberta.instituicao)
        assertEquals("Nubank Atualizada", contaReaberta.apelido)
        assertEquals(contaCriadoEm, contaReaberta.criadoEm)

        val primeiroReaberto = instrumentosReabertos.single { it.publicId == primeiroId }
        val segundoReaberto = instrumentosReabertos.single { it.publicId == segundoId }
        assertEquals(primeiroCriadoEm, primeiroReaberto.criadoEm)
        assertEquals("Cartão A", primeiroReaberto.apelido)
        assertEquals("3333", primeiroReaberto.ultimosDigitos)
        assertEquals(7, primeiroReaberto.fechamentoDia)
        assertEquals(14, primeiroReaberto.vencimentoDia)

        assertEquals(segundoCriadoEm, segundoReaberto.criadoEm)
        assertEquals("Cartão B Família", segundoReaberto.apelido)
        assertEquals("2222", segundoReaberto.ultimosDigitos)
        assertEquals(20, segundoReaberto.fechamentoDia)
        assertEquals(28, segundoReaberto.vencimentoDia)
    }

    private fun aguardarTexto(texto: String) {
        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun aguardarDescricao(descricao: String) {
        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodesWithContentDescription(descricao).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun aguardarTag(tag: String) {
        composeRule.waitUntil(ESPERA_MS) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val ESPERA_MS = 10_000L
    }
}
