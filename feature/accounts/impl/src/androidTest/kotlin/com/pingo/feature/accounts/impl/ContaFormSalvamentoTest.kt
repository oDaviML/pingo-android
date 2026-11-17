package com.pingo.feature.accounts.impl

import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.data.CadastroSalvo
import com.pingo.core.data.ContaRepository
import com.pingo.core.data.impl.ContaRepositoryImpl
import com.pingo.core.data.impl.InstrumentoRepositoryImpl
import com.pingo.core.data.impl.RoomTransacaoLocal
import com.pingo.core.database.PingoDatabase
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.NovaConta
import com.pingo.core.ui.contas.EventoDoFormularioDeConta
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class ContaFormSalvamentoTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val agora = Instant.parse("2026-11-10T12:00:00Z")
    private val clock: Clock = Clock.fixed(agora, ZoneOffset.UTC)

    private lateinit var database: PingoDatabase
    private lateinit var contas: ContaRepositoryImpl
    private lateinit var instrumentos: InstrumentoRepositoryImpl
    private lateinit var controlado: RepositorioDeContaSobControle

    @Before
    fun preparaBanco() {
        database = Room.inMemoryDatabaseBuilder(contexto, PingoDatabase::class.java)
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
        controlado = RepositorioDeContaSobControle(contas)
    }

    @After
    fun fechaBanco() {
        database.close()
    }

    @Test
    fun salvamentoPendenteDesabilitaEnvioEToquesRepetidosCriamUmUnicoConjunto() {
        val viewModel = abrirFormulario()

        controlado.portao = CompletableDeferred()
        composeRule.onNodeWithText("Salvar conta").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_DE_ESPERA) { viewModel.uiState.value.salvando }

        composeRule.onNodeWithText("Salvar conta").assertIsNotEnabled()
        salvarCaptura("conta-salvando")
        viewModel.onEvent(ContaFormEvent.Salvar)
        viewModel.onEvent(ContaFormEvent.Salvar)

        controlado.portao?.complete(Unit)
        composeRule.waitUntil(TEMPO_DE_ESPERA) { viewModel.uiState.value.concluido }

        assertEquals(1, controlado.criacoes)
        assertEquals(1, runBlocking { contas.observeContas().first() }.size)
        assertEquals(1, runBlocking { instrumentos.observeInstrumentos().first() }.size)
    }

    @Test
    fun falhaDePersistenciaNaoConcluiNemDeixaCadastroParcialERetryCriaUmaVez() {
        val viewModel = abrirFormulario()

        controlado.falharProximaCriacao = true
        composeRule.onNodeWithText("Salvar conta").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_DE_ESPERA) { viewModel.uiState.value.falhouAoSalvar }

        composeRule.onNodeWithText(MENSAGEM_ERRO_AO_SALVAR_CONTA).assertIsDisplayed()
        salvarCaptura("conta-erro-salvar")
        assertFalse(viewModel.uiState.value.concluido)
        assertEquals(0, runBlocking { contas.observeContas().first() }.size)
        assertEquals(0, runBlocking { instrumentos.observeInstrumentos().first() }.size)

        composeRule.onNodeWithText("Salvar conta").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_DE_ESPERA) { viewModel.uiState.value.concluido }

        assertEquals(2, controlado.criacoes)
        assertEquals(1, runBlocking { contas.observeContas().first() }.size)
        assertEquals(1, runBlocking { instrumentos.observeInstrumentos().first() }.size)
    }

    @Test
    fun edicaoDuranteSalvamentoPendenteNaoAlteraSnapshotEGravaValoresOriginais() {
        val viewModel = abrirFormulario()

        controlado.portao = CompletableDeferred()
        composeRule.onNodeWithText("Salvar conta").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_DE_ESPERA) { viewModel.uiState.value.salvando }

        composeRule.onNodeWithContentDescription("Apelido da conta").assertIsNotEnabled()
        composeRule.onNodeWithText("+ Crédito").performScrollTo().assertIsNotEnabled()

        viewModel.onEvent(
            ContaFormEvent.Formulario(EventoDoFormularioDeConta.AlterarApelidoConta("Alterado durante o salvamento")),
        )
        assertEquals("Outra", viewModel.uiState.value.formulario.apelido)

        controlado.portao?.complete(Unit)
        composeRule.waitUntil(TEMPO_DE_ESPERA) { viewModel.uiState.value.concluido }

        assertEquals("Outra", runBlocking { contas.observeContas().first() }.single().apelido)
    }

    @Test
    fun falhaAoSalvarLiberaEdicaoERetryPersisteValorCorrigido() {
        val viewModel = abrirFormulario()

        controlado.falharProximaCriacao = true
        composeRule.onNodeWithText("Salvar conta").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_DE_ESPERA) { viewModel.uiState.value.falhouAoSalvar }

        composeRule.onNodeWithContentDescription("Apelido da conta").assertIsEnabled().performTextReplacement("Outra corrigida")
        composeRule.waitUntil(TEMPO_DE_ESPERA) { viewModel.uiState.value.formulario.apelido == "Outra corrigida" }

        composeRule.onNodeWithText("Salvar conta").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_DE_ESPERA) { viewModel.uiState.value.concluido }

        assertEquals("Outra corrigida", runBlocking { contas.observeContas().first() }.single().apelido)
        assertEquals(2, controlado.criacoes)
    }

    private fun abrirFormulario(): ContaFormViewModel {
        val viewModel = ContaFormViewModel(controlado, instrumentos, clock, SavedStateHandle())
        composeRule.setContent {
            PingoTheme {
                ContaFormScreen(onVoltar = {}, viewModel = viewModel)
            }
        }
        composeRule.waitUntil(TEMPO_DE_ESPERA) { !viewModel.uiState.value.carregando }
        composeRule.onNodeWithTag("instituicao-OUTRA").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_DE_ESPERA) {
            viewModel.uiState.value.formulario.instituicao == InstituicaoFinanceira.OUTRA && viewModel.uiState.value.podeSalvar
        }

        return viewModel
    }

    private fun salvarCaptura(nome: String) {
        val arquivo = "/sdcard/Download/$nome-${Build.VERSION.SDK_INT}.png"
        shell("screencap -p $arquivo")
        assertTrue("screenshot não gerado: $arquivo", shell("ls $arquivo").contains(arquivo))
    }

    private fun shell(comando: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(comando)

        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream -> stream.readBytes().decodeToString() }
    }

    private class RepositorioDeContaSobControle(private val delegado: ContaRepository) : ContaRepository by delegado {
        var portao: CompletableDeferred<Unit>? = null
        var falharProximaCriacao = false
        var criacoes = 0

        override suspend fun criarComInstrumentos(contas: List<NovaConta>, quando: Instant, publicIds: List<String>): List<CadastroSalvo> {
            criacoes++
            portao?.await()
            if (falharProximaCriacao) {
                falharProximaCriacao = false
                throw IOException("falha controlada")
            }

            return delegado.criarComInstrumentos(contas, quando, publicIds)
        }
    }

    private companion object {
        const val TEMPO_DE_ESPERA = 5_000L
    }
}
