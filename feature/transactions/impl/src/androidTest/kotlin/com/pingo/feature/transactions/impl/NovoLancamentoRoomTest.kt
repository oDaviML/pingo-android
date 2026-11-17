package com.pingo.feature.transactions.impl

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.data.impl.CategoriaRepositoryImpl
import com.pingo.core.data.impl.ContaRepositoryImpl
import com.pingo.core.data.impl.InstrumentoRepositoryImpl
import com.pingo.core.data.impl.LancamentoRepositoryImpl
import com.pingo.core.data.impl.RegraCategoriaRepositoryImpl
import com.pingo.core.data.impl.RoomTransacaoLocal
import com.pingo.core.data.manual.CampoDeLancamentoManual
import com.pingo.core.data.manual.RegistrarLancamentoManual
import com.pingo.core.database.PingoDatabase
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.NovaConta
import com.pingo.core.model.NovoInstrumento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class NovoLancamentoRoomTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val agora = Instant.parse("2026-11-10T12:00:00Z")
    private val clock = Clock.fixed(agora, ZoneOffset.UTC)
    private val contexto: Context = ApplicationProvider.getApplicationContext()

    private lateinit var database: PingoDatabase
    private lateinit var lancamentos: LancamentoRepositoryImpl
    private lateinit var contas: ContaRepositoryImpl
    private lateinit var instrumentos: InstrumentoRepositoryImpl
    private lateinit var categorias: CategoriaRepositoryImpl
    private lateinit var regras: RegraCategoriaRepositoryImpl

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
        lancamentos = LancamentoRepositoryImpl(database.lancamentoDao())
        contas = ContaRepositoryImpl(database.contaDao(), database.instrumentoDao(), RoomTransacaoLocal(database))
        instrumentos = InstrumentoRepositoryImpl(database.instrumentoDao())
        categorias = CategoriaRepositoryImpl(database.categoriaDao())
        regras = RegraCategoriaRepositoryImpl(database.regraCategoriaDao())
    }

    @After
    fun fechaBanco() {
        database.close()
    }

    @Test
    fun salvarDespesaComDefaultsGravaLancamentoManualNoRoom() {
        val instrumento = runBlocking { criarContaComInstrumento() }
        val viewModel = criarViewModel()

        mostrar(viewModel)
        aguardarCarregamento(viewModel)
        digitar("tecla-4", "tecla-2", "tecla-9", "tecla-0")
        composeRule.onNodeWithTag("salvar-lancamento").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.salvo }

        val salvo = runBlocking { lancamentos.observeLancamentos().first() }.single { it.origem == OrigemLancamento.MANUAL }
        assertEquals(4290L, salvo.valorCentavos)
        assertEquals(TipoLancamento.DESPESA, salvo.tipo)
        assertEquals(StatusLancamento.CONFIRMADO, salvo.status)
        assertEquals(instrumento.publicId, salvo.instrumentoPublicId)
        assertEquals(LocalDate.of(2026, 11, 10), salvo.dataHora.atZone(ZoneOffset.UTC).toLocalDate())
    }

    @Test
    fun transferenciaGravaOParComOMesmoIdentificadorNoRoom() {
        runBlocking { criarDuasContas() }
        val viewModel = criarViewModel()

        mostrar(viewModel)
        aguardarCarregamento(viewModel)
        composeRule.onNodeWithTag("tipo-TRANSFERENCIA").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.tipo == TipoDoLancamentoManual.TRANSFERENCIA }

        val opcoes = viewModel.uiState.value.instrumentos
        val origem = opcoes.first()
        val destino = opcoes.first { it.publicId != origem.publicId }
        composeRule.onNodeWithTag("campo-origem").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.painel == PainelDoNovoLancamento.INSTRUMENTO }
        composeRule.onNodeWithTag("instrumento-opcao-${origem.publicId}").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.instrumento?.publicId == origem.publicId }
        composeRule.onNodeWithTag("campo-destino").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.painel == PainelDoNovoLancamento.DESTINO }
        composeRule.onNodeWithTag("instrumento-opcao-${destino.publicId}").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.destino?.publicId == destino.publicId }
        digitar("tecla-5", "tecla-0", "tecla-0", "tecla-0", "tecla-0")
        composeRule.onNodeWithTag("salvar-lancamento").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.salvo }

        val par = runBlocking { lancamentos.observeLancamentos().first() }.filter { it.origem == OrigemLancamento.MANUAL }
        assertEquals(2, par.size)
        assertEquals(1, par.map { it.transferenciaId }.distinct().size)
        assertNotEquals(null, par.first().transferenciaId)
        assertEquals(setOf(origem.publicId, destino.publicId), par.map { it.instrumentoPublicId }.toSet())
        par.forEach { lancamento ->
            assertEquals(TipoLancamento.TRANSFERENCIA, lancamento.tipo)
            assertEquals(50000L, lancamento.valorCentavos)
        }
    }

    @Test
    fun transferenciaComCapturadoNaOrigemMarcaSoASaidaPendenteNaFronteiraDeVinteEQuatroHoras() {
        val cenario = runBlocking {
            prepararCenarioDeTransferencia(
                publicIdNaOrigem = "capturado-na-origem",
                dataHoraNaOrigem = agora.minus(Duration.ofHours(24)),
                publicIdNoDestino = "capturado-fora-no-destino",
                dataHoraNoDestino = agora.minus(Duration.ofHours(24)).minusMillis(1),
                valorCentavos = 50000,
            )
        }

        salvarTransferencia(cenario, "tecla-5", "tecla-0", "tecla-0", "tecla-0", "tecla-0")

        val par = runBlocking { lancamentos.observeLancamentos().first() }.filter { it.origem == OrigemLancamento.MANUAL }
        assertEquals(2, par.size)
        assertEquals(1, par.map { it.transferenciaId }.distinct().size)
        assertNotEquals(null, par.first().transferenciaId)
        assertEquals(2, par.map { it.publicId }.distinct().size)
        val porInstrumento = par.associateBy { it.instrumentoPublicId }
        val saida = porInstrumento.getValue(cenario.origem.publicId)
        val entrada = porInstrumento.getValue(cenario.destino.publicId)
        assertEquals(TipoLancamento.TRANSFERENCIA, saida.tipo)
        assertEquals(TipoLancamento.TRANSFERENCIA, entrada.tipo)
        assertNull(saida.categoriaPublicId)
        assertNull(entrada.categoriaPublicId)
        assertEquals(StatusLancamento.PENDENTE, saida.status)
        assertEquals(MotivoPendencia.POSSIVEL_DUPLICATA.name, saida.motivoPendencia)
        assertEquals(StatusLancamento.CONFIRMADO, entrada.status)
        assertNull(entrada.motivoPendencia)
        assertEquals(cenario.capturadoNaOrigem, runBlocking { lancamentos.buscar(cenario.capturadoNaOrigem.publicId) })
        assertEquals(cenario.capturadoNoDestino, runBlocking { lancamentos.buscar(cenario.capturadoNoDestino.publicId) })
    }

    @Test
    fun transferenciaComCapturadoNoDestinoMarcaSoAEntradaPendenteNaFronteiraDeVinteEQuatroHoras() {
        val cenario = runBlocking {
            prepararCenarioDeTransferencia(
                publicIdNaOrigem = "capturado-fora-na-origem",
                dataHoraNaOrigem = agora.plus(Duration.ofHours(24)).plusMillis(1),
                publicIdNoDestino = "capturado-no-destino",
                dataHoraNoDestino = agora.plus(Duration.ofHours(24)),
                valorCentavos = 75000,
            )
        }

        salvarTransferencia(cenario, "tecla-7", "tecla-5", "tecla-0", "tecla-0", "tecla-0")

        val par = runBlocking { lancamentos.observeLancamentos().first() }.filter { it.origem == OrigemLancamento.MANUAL }
        val porInstrumento = par.associateBy { it.instrumentoPublicId }
        val saida = porInstrumento.getValue(cenario.origem.publicId)
        val entrada = porInstrumento.getValue(cenario.destino.publicId)
        assertEquals(StatusLancamento.CONFIRMADO, saida.status)
        assertNull(saida.motivoPendencia)
        assertEquals(StatusLancamento.PENDENTE, entrada.status)
        assertEquals(MotivoPendencia.POSSIVEL_DUPLICATA.name, entrada.motivoPendencia)
        assertEquals(cenario.capturadoNaOrigem, runBlocking { lancamentos.buscar(cenario.capturadoNaOrigem.publicId) })
        assertEquals(cenario.capturadoNoDestino, runBlocking { lancamentos.buscar(cenario.capturadoNoDestino.publicId) })
    }

    @Test
    fun duplicataDeCapturadoGravaPendenteEMantemOCapturado() {
        val instrumento = runBlocking {
            val criado = criarContaComInstrumento()
            lancamentos.salvar(
                Lancamento(
                    publicId = "capturado",
                    instrumentoPublicId = criado.publicId,
                    valorCentavos = 4290,
                    dataHora = agora.minus(Duration.ofHours(1)),
                    tipo = TipoLancamento.DESPESA,
                    estabelecimento = "Padaria do Zé",
                    status = StatusLancamento.CONFIRMADO,
                    origem = OrigemLancamento.NOTIFICACAO,
                    confianca = 1.0,
                    criadoEm = agora.minus(Duration.ofHours(1)),
                    atualizadoEm = agora.minus(Duration.ofHours(1)),
                ),
            )
            criado
        }
        val viewModel = criarViewModel()

        mostrar(viewModel)
        aguardarCarregamento(viewModel)
        digitar("tecla-4", "tecla-2", "tecla-9", "tecla-0")
        composeRule.onNodeWithTag("salvar-lancamento").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.salvo }

        val todos = runBlocking { lancamentos.observeLancamentos().first() }
        val manual = todos.single { it.origem == OrigemLancamento.MANUAL }
        assertEquals(StatusLancamento.PENDENTE, manual.status)
        assertEquals(MotivoPendencia.POSSIVEL_DUPLICATA.name, manual.motivoPendencia)
        assertEquals(instrumento.publicId, manual.instrumentoPublicId)
        val capturado = todos.single { it.publicId == "capturado" }
        assertEquals(StatusLancamento.CONFIRMADO, capturado.status)
        assertEquals(OrigemLancamento.NOTIFICACAO, capturado.origem)
    }

    @Test
    fun ultimoCapturadoComInstrumentoNuloNaoViraDefaultEBloqueiaSalvamento() {
        runBlocking {
            criarDuasContas()
            val momento = agora.minus(Duration.ofMinutes(5))
            lancamentos.salvar(
                Lancamento(
                    publicId = "capturado-sem-instrumento",
                    instrumentoPublicId = null,
                    valorCentavos = 1234,
                    dataHora = momento,
                    tipo = TipoLancamento.DESPESA,
                    estabelecimento = "Padaria do Zé",
                    status = StatusLancamento.PENDENTE,
                    origem = OrigemLancamento.NOTIFICACAO,
                    confianca = 0.4,
                    motivoPendencia = MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name,
                    criadoEm = momento,
                    atualizadoEm = momento,
                ),
            )
        }
        val viewModel = criarViewModel()

        mostrar(viewModel)
        aguardarCarregamento(viewModel)
        assertNull(viewModel.uiState.value.instrumento)
        composeRule.onNodeWithText("Escolha um instrumento").assertIsDisplayed()
        digitar("tecla-1", "tecla-0", "tecla-0", "tecla-0")
        composeRule.onNodeWithTag("salvar-lancamento").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.erros.isNotEmpty() }

        assertTrue(CampoDeLancamentoManual.INSTRUMENTO in viewModel.uiState.value.erros)
        composeRule.onNodeWithText("Informe de onde saiu o dinheiro").assertIsDisplayed()
        assertTrue(runBlocking { lancamentos.observeLancamentos().first() }.none { it.origem == OrigemLancamento.MANUAL })
    }

    @Test
    fun semInstrumentosBloqueiaSalvamentoComMensagem() {
        val viewModel = criarViewModel()

        mostrar(viewModel)
        aguardarCarregamento(viewModel)
        composeRule.onNodeWithText("Nenhum instrumento cadastrado. Cadastre um banco para lançar.").assertIsDisplayed()
        digitar("tecla-1", "tecla-0", "tecla-0", "tecla-0")
        composeRule.onNodeWithTag("salvar-lancamento").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.erros.isNotEmpty() }

        assertTrue(runBlocking { lancamentos.observeLancamentos().first() }.isEmpty())
        assertTrue(runBlocking { instrumentos.observeInstrumentos().first() }.isEmpty())
    }

    private fun criarViewModel(): NovoLancamentoViewModel = NovoLancamentoViewModel(
        contaRepository = contas,
        instrumentoRepository = instrumentos,
        categoriaRepository = categorias,
        regraCategoriaRepository = regras,
        registrarLancamentoManual = RegistrarLancamentoManual(
            lancamentoRepository = lancamentos,
            instrumentoRepository = instrumentos,
            transacaoLocal = RoomTransacaoLocal(database),
            clock = clock,
        ),
        clock = clock,
    )

    private fun mostrar(viewModel: NovoLancamentoViewModel) {
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            PingoTheme {
                NovoLancamentoContent(
                    state = state,
                    onEvent = viewModel::onEvent,
                    onFechar = {},
                )
            }
        }
    }

    private fun aguardarCarregamento(viewModel: NovoLancamentoViewModel) {
        composeRule.waitUntil(TEMPO_ESPERA) { !viewModel.uiState.value.carregando }
    }

    private fun digitar(vararg teclas: String) {
        teclas.forEach { tecla -> composeRule.onNodeWithTag(tecla).performClick() }
    }

    private suspend fun criarContaComInstrumento(): Instrumento {
        contas.criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.NUBANK,
                    apelido = "Nubank",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(
                        NovoInstrumento(
                            tipo = TipoInstrumento.CREDITO,
                            apelido = "Cartão de crédito",
                            fechamentoDia = 5,
                            vencimentoDia = 12,
                        ),
                    ),
                ),
            ),
            agora,
        )

        return instrumentos.observeInstrumentos().first().single()
    }

    private suspend fun criarDuasContas() {
        contas.criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.NUBANK,
                    apelido = "Nubank",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(
                        NovoInstrumento(
                            tipo = TipoInstrumento.CREDITO,
                            apelido = "Cartão de crédito",
                            fechamentoDia = 5,
                            vencimentoDia = 12,
                        ),
                    ),
                ),
                NovaConta(
                    instituicao = InstituicaoFinanceira.INTER,
                    apelido = "Inter",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(NovoInstrumento(tipo = TipoInstrumento.DEBITO, apelido = "Cartão de débito")),
                ),
            ),
            agora,
        )
    }

    private suspend fun prepararCenarioDeTransferencia(
        publicIdNaOrigem: String,
        dataHoraNaOrigem: Instant,
        publicIdNoDestino: String,
        dataHoraNoDestino: Instant,
        valorCentavos: Long,
    ): CenarioDeTransferencia {
        criarDuasContas()
        val ativos = instrumentos.observeInstrumentos().first()
        val capturadoNaOrigem = capturado(publicIdNaOrigem, ativos[0].publicId, valorCentavos, dataHoraNaOrigem)
        val capturadoNoDestino = capturado(publicIdNoDestino, ativos[1].publicId, valorCentavos, dataHoraNoDestino)
        lancamentos.salvar(capturadoNaOrigem)
        lancamentos.salvar(capturadoNoDestino)

        return CenarioDeTransferencia(ativos[0], ativos[1], capturadoNaOrigem, capturadoNoDestino)
    }

    private fun capturado(publicId: String, instrumentoPublicId: String, valorCentavos: Long, dataHora: Instant): Lancamento = Lancamento(
        publicId = publicId,
        instrumentoPublicId = instrumentoPublicId,
        valorCentavos = valorCentavos,
        dataHora = dataHora,
        tipo = TipoLancamento.DESPESA,
        estabelecimento = "Padaria do Zé",
        status = StatusLancamento.CONFIRMADO,
        origem = OrigemLancamento.NOTIFICACAO,
        confianca = 1.0,
        criadoEm = dataHora,
        atualizadoEm = dataHora,
    )

    private fun salvarTransferencia(cenario: CenarioDeTransferencia, vararg teclas: String) {
        val viewModel = criarViewModel()

        mostrar(viewModel)
        aguardarCarregamento(viewModel)
        composeRule.onNodeWithTag("tipo-TRANSFERENCIA").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.tipo == TipoDoLancamentoManual.TRANSFERENCIA }
        composeRule.onNodeWithTag("campo-origem").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.painel == PainelDoNovoLancamento.INSTRUMENTO }
        composeRule.onNodeWithTag("instrumento-opcao-${cenario.origem.publicId}").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.instrumento?.publicId == cenario.origem.publicId }
        composeRule.onNodeWithTag("campo-destino").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.painel == PainelDoNovoLancamento.DESTINO }
        composeRule.onNodeWithTag("instrumento-opcao-${cenario.destino.publicId}").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.destino?.publicId == cenario.destino.publicId }
        digitar(*teclas)
        composeRule.onNodeWithTag("salvar-lancamento").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.salvo }
    }

    private data class CenarioDeTransferencia(
        val origem: Instrumento,
        val destino: Instrumento,
        val capturadoNaOrigem: Lancamento,
        val capturadoNoDestino: Lancamento,
    )

    private companion object {
        const val TEMPO_ESPERA = 7000L
    }
}
