package com.pingo.feature.transactions.impl

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.data.impl.CategoriaRepositoryImpl
import com.pingo.core.data.impl.ContaRepositoryImpl
import com.pingo.core.data.impl.InstrumentoRepositoryImpl
import com.pingo.core.data.impl.LancamentoRepositoryImpl
import com.pingo.core.data.impl.RoomTransacaoLocal
import com.pingo.core.database.PingoDatabase
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.Categoria
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.NovaConta
import com.pingo.core.model.NovoInstrumento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
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
import java.time.YearMonth
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class TransactionsRoomTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val agora = Instant.parse("2026-11-10T12:00:00Z")
    private val clock = Clock.fixed(agora, ZoneId.systemDefault())
    private val armazemDeViewModels = ViewModelStore()
    private val contexto: Context = ApplicationProvider.getApplicationContext()

    private lateinit var database: PingoDatabase
    private lateinit var lancamentos: LancamentoRepositoryImpl
    private lateinit var contas: ContaRepositoryImpl
    private lateinit var instrumentos: InstrumentoRepositoryImpl
    private lateinit var categorias: CategoriaRepositoryImpl
    private lateinit var viewModel: TransactionsViewModel
    private lateinit var instrumento: Instrumento
    private lateinit var alimentacao: Categoria

    @Before
    fun preparaDados() {
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

        runBlocking { semear() }

        viewModel = TransactionsViewModel(
            lancamentoRepository = lancamentos,
            contaRepository = contas,
            instrumentoRepository = instrumentos,
            categoriaRepository = categorias,
            clock = clock,
        )
        armazemDeViewModels.put("transactions", viewModel)
    }

    @After
    fun limpa() {
        armazemDeViewModels.clear()
        database.close()
    }

    @Test
    fun listaGeralDoRoomIncluiPendenteSemInstrumentoEVeTotalDoDia() {
        mostrar()

        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.dias.isNotEmpty() }

        composeRule.onNodeWithText("HOJE").assertIsDisplayed()
        composeRule.onNodeWithText("+ R$ 88,20").assertIsDisplayed()
        composeRule.onNodeWithText("Padaria do Zé").assertIsDisplayed()
        composeRule.onNodeWithText("Loja Sem Cartão").assertIsDisplayed()
        composeRule.onNodeWithText("Instrumento não identificado", substring = true).assertIsDisplayed()
        assertEquals(YearMonth.of(2026, 11), viewModel.uiState.value.filtros.mes)
    }

    @Test
    fun filtroDeInstrumentoCriadoNoPassoSeteNaoAtribuiPendenteNulo() {
        mostrar()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.dias.isNotEmpty() }

        composeRule.onNodeWithContentDescription("Filtros").performClick()
        composeRule.onNodeWithText("Nubank").assertIsDisplayed()
        composeRule.onNodeWithText("Nubank crédito").assertIsDisplayed()
        composeRule.onNodeWithTag("filtro-vinculo-instrumento-${instrumento.publicId}").performClick()

        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.quantidadeDeLancamentos == 2 }
        composeRule.onNodeWithText("Padaria do Zé").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("Loja Sem Cartão").fetchSemanticsNodes().size)

        composeRule.onNodeWithTag("filtro-vinculo-todas").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.quantidadeDeLancamentos == 3 }

        composeRule.onNodeWithText("Loja Sem Cartão").assertIsDisplayed()
    }

    @Test
    fun trocaDeCategoriaPersisteNoRoom() {
        mostrar()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.dias.isNotEmpty() }

        val mercado = runBlocking { categoriaCriada("Mercado") }
        composeRule.waitUntil(TEMPO_ESPERA) {
            viewModel.uiState.value.opcoesDeCategoria.any { it.publicId == mercado.publicId }
        }

        viewModel.onEvent(TransactionsEvent.AbrirTrocaCategoria(PADARIA))
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.trocaDeCategoria != null }

        composeRule.onNodeWithTag("categoria-opcao-${mercado.publicId}").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) {
            runBlocking { lancamentos.buscar(PADARIA)?.categoriaPublicId } == mercado.publicId
        }

        assertEquals(mercado.publicId, runBlocking { lancamentos.buscar(PADARIA)?.categoriaPublicId })
    }

    private fun mostrar() {
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            PingoTheme {
                TransactionsContent(
                    state = state,
                    onEvent = viewModel::onEvent,
                    onOpenDetalhe = {},
                    onOpenNovo = {},
                    onSelecionarAba = {},
                )
            }
        }
    }

    private suspend fun semear() {
        contas.criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.NUBANK,
                    apelido = "Nubank",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(NovoInstrumento(tipo = TipoInstrumento.CREDITO, apelido = "Cartão de crédito")),
                ),
            ),
            agora,
        )
        instrumento = instrumentos.observeInstrumentos().first().single()
        alimentacao = categoriaCriada("Alimentação", TipoCategoria.DESPESA)
        val salario = categoriaCriada("Salário", TipoCategoria.RECEITA)

        lancamentos.salvar(
            lancamento(
                publicId = PADARIA,
                valorCentavos = 4290,
                tipo = TipoLancamento.DESPESA,
                estabelecimento = "Padaria do Zé",
                instrumentoPublicId = instrumento.publicId,
                categoriaPublicId = alimentacao.publicId,
            ),
        )
        lancamentos.salvar(
            lancamento(
                publicId = SEM_CARTAO,
                valorCentavos = 1890,
                tipo = TipoLancamento.DESPESA,
                estabelecimento = "Loja Sem Cartão",
                instrumentoPublicId = null,
                categoriaPublicId = null,
                status = StatusLancamento.PENDENTE,
                instituicaoOrigem = InstituicaoFinanceira.NUBANK,
            ),
        )
        lancamentos.salvar(
            lancamento(
                publicId = SALARIO,
                valorCentavos = 15000,
                tipo = TipoLancamento.RECEITA,
                estabelecimento = "Salário",
                instrumentoPublicId = instrumento.publicId,
                categoriaPublicId = salario.publicId,
            ),
        )
    }

    private suspend fun categoriaCriada(nome: String, tipo: TipoCategoria = TipoCategoria.DESPESA): Categoria =
        when (val resultado = categorias.criar(nome, tipo, agora)) {
            is ResultadoCategoria.Sucesso -> resultado.categoria
            else -> error("categoria não criada: $resultado")
        }

    private fun lancamento(
        publicId: String,
        valorCentavos: Long,
        tipo: TipoLancamento,
        estabelecimento: String,
        instrumentoPublicId: String?,
        categoriaPublicId: String?,
        status: StatusLancamento = StatusLancamento.CONFIRMADO,
        instituicaoOrigem: InstituicaoFinanceira? = null,
    ): Lancamento = Lancamento(
        publicId = publicId,
        instrumentoPublicId = instrumentoPublicId,
        categoriaPublicId = categoriaPublicId,
        valorCentavos = valorCentavos,
        dataHora = agora,
        tipo = tipo,
        estabelecimento = estabelecimento,
        status = status,
        origem = OrigemLancamento.NOTIFICACAO,
        instituicaoOrigem = instituicaoOrigem,
        confianca = 1.0,
        motivoPendencia = if (status == StatusLancamento.PENDENTE) "INSTRUMENTO_NAO_ENCONTRADO" else null,
        criadoEm = agora,
        atualizadoEm = agora,
    )

    private companion object {
        const val TEMPO_ESPERA = 7000L
        const val PADARIA = "lanc-padaria"
        const val SEM_CARTAO = "lanc-sem-cartao"
        const val SALARIO = "lanc-salario"
    }
}
