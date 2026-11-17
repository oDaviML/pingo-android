package com.pingo.feature.home.impl

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import com.pingo.core.notifications.AcessoNotificacoes
import com.pingo.core.notifications.DiagnosticoDaCaptura
import com.pingo.core.notifications.SaudeDaCaptura
import com.pingo.core.notifications.SituacaoDaCaptura
import com.pingo.core.ui.Valores
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class HomeRoomTest {
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
    private lateinit var viewModel: HomeViewModel

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
    }

    @After
    fun fechaBanco() {
        database.close()
    }

    @Test
    fun inicioDoRoomMostraSaldoDoMesTopCategoriasEUltimosCinco() {
        val cenario = runBlocking { semearLancamentos() }
        criarViewModel()
        mostrar()
        aguardarCarregamento()

        composeRule.onNodeWithTag("saldo-do-mes").assertIsDisplayed()
        composeRule.onNodeWithText(Valores.formatar(cenario.saldo)).assertIsDisplayed()
        composeRule.onNodeWithText("- ${Valores.formatar(cenario.despesas)}").assertIsDisplayed()

        composeRule.onNodeWithTag("top-categorias").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Alimentação").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("35% das despesas").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Mercado").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Transporte").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("ultimos-lancamentos").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sem Cartão").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Instrumento não identificado", substring = true).performScrollTo().assertIsDisplayed()
        assertEquals(5, viewModel.uiState.value.ultimos.size)
        assertEquals("Sem Cartão", viewModel.uiState.value.ultimos.first().lancamento.estabelecimento)
    }

    @Test
    fun seletorDeMesRecalculaOPeriodoNoRoom() {
        runBlocking { semearLancamentos() }
        criarViewModel()
        mostrar()
        aguardarCarregamento()

        composeRule.onNodeWithText("Novembro 2026").assertIsDisplayed()

        composeRule.onNodeWithTag("mes-anterior").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.mes?.monthValue == 10 }
        composeRule.onNodeWithTag("mes-anterior").performClick()

        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.mes?.monthValue == 9 }
        composeRule.onNodeWithText("Setembro 2026").assertIsDisplayed()
        assertEquals(1500, viewModel.uiState.value.despesasCentavos)
        assertEquals(0, viewModel.uiState.value.receitasCentavos)
        assertEquals(-1500, viewModel.uiState.value.saldoCentavos)
        assertEquals("Mercado", viewModel.uiState.value.topCategorias.single().nome)
        assertEquals(5, viewModel.uiState.value.ultimos.size)
    }

    @Test
    fun avisosNavegamParaRevisaoECapturaParada() {
        runBlocking { semearLancamentos() }
        var revisoes = 0
        var capturas = 0
        criarViewModel(acesso = AcessoFixo(concedido = false))
        mostrar(onOpenRevisao = { revisoes++ }, onOpenCaptura = { capturas++ })
        aguardarCarregamento()

        composeRule.onNodeWithTag("aviso-revisao").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1 lançamento para revisar").assertIsDisplayed()
        composeRule.onNodeWithTag("aviso-revisao").performClick()
        composeRule.onNodeWithTag("aviso-captura-parada").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Corrigir").performScrollTo().performClick()

        assertEquals(1, revisoes)
        assertEquals(1, capturas)
    }

    @Test
    fun semPendentesNemCapturaParadaAvisosNaoAparecem() {
        criarViewModel()
        mostrar()
        aguardarCarregamento()

        composeRule.onNodeWithTag("inicio-vazio").assertIsDisplayed()
        composeRule.onNodeWithTag("aviso-revisao").assertDoesNotExist()
        composeRule.onNodeWithTag("aviso-captura-parada").assertDoesNotExist()
    }

    @Test
    fun semLancamentosMostraNenhumPingoEAtalhoManual() {
        var manuais = 0
        criarViewModel()
        mostrar(onOpenNovo = { manuais++ })
        aguardarCarregamento()

        composeRule.onNodeWithTag("inicio-vazio").assertIsDisplayed()
        composeRule.onNodeWithText("Nenhum pingo ainda").assertIsDisplayed()

        composeRule.onNodeWithTag("lancar-manualmente").performScrollTo().performClick()

        assertEquals(1, manuais)
    }

    @Test
    fun pendenteSemInstrumentoApareceNosUltimosSemCartaoArbitrario() {
        runBlocking { semearLancamentos() }
        criarViewModel()
        mostrar()
        aguardarCarregamento()

        val pendente = runBlocking { lancamentos.buscar(PENDENTE) }
        assertEquals(null, pendente?.instrumentoPublicId)
        assertEquals(InstituicaoFinanceira.NUBANK, pendente?.instituicaoOrigem)

        composeRule.onNodeWithTag("ultimos-lancamentos").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sem Cartão").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Pendente").assertIsDisplayed()
        composeRule.onNodeWithText("Instrumento não identificado", substring = true).performScrollTo().assertIsDisplayed()
    }

    private fun criarViewModel(acesso: AcessoNotificacoes = AcessoFixo(concedido = true)) {
        viewModel = HomeViewModel(
            acessoNotificacoes = acesso,
            saudeDaCaptura = SaudeFixa(DiagnosticoDaCaptura(situacao = SituacaoDaCaptura.ATIVA)),
            lancamentoRepository = lancamentos,
            contaRepository = contas,
            instrumentoRepository = instrumentos,
            categoriaRepository = categorias,
            clock = clock,
        )
    }

    private fun mostrar(onOpenCaptura: () -> Unit = {}, onOpenRevisao: () -> Unit = {}, onOpenNovo: () -> Unit = {}) {
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            PingoTheme {
                HomeContent(
                    state = state,
                    onOpenSettings = {},
                    onOpenCaptura = onOpenCaptura,
                    onOpenRevisao = onOpenRevisao,
                    onOpenNovo = onOpenNovo,
                    onMesAnterior = viewModel::mesAnterior,
                    onMesProximo = viewModel::mesProximo,
                    zona = ZoneOffset.UTC,
                )
            }
        }
    }

    private fun aguardarCarregamento() {
        composeRule.waitUntil(TEMPO_ESPERA) { !viewModel.uiState.value.carregando }
    }

    private suspend fun semearLancamentos(): Cenario {
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
        val instrumento: Instrumento = instrumentos.observeInstrumentos().first().single()
        val alimentacao = categoriaCriada("Alimentação")
        val mercado = categoriaCriada("Mercado")
        val transporte = categoriaCriada("Transporte")
        val lazer = categoriaCriada("Lazer")
        val salario = categoriaCriada("Salário", TipoCategoria.RECEITA)

        salvar("receita", 620000, instant("2026-11-01T12:00:00Z"), TipoLancamento.RECEITA, salario.publicId, instrumento.publicId)
        salvar(
            "padaria",
            4290,
            instant("2026-11-02T12:00:00Z"),
            categoriaPublicId = alimentacao.publicId,
            instrumentoPublicId = instrumento.publicId,
            estabelecimento = "Padaria do Zé",
        )
        salvar(
            "mercado",
            3000,
            instant("2026-11-03T12:00:00Z"),
            categoriaPublicId = mercado.publicId,
            instrumentoPublicId = instrumento.publicId,
            estabelecimento = "Mercado Bom Preço",
        )
        salvar(
            "uber",
            2000,
            instant("2026-11-04T12:00:00Z"),
            categoriaPublicId = transporte.publicId,
            instrumentoPublicId = instrumento.publicId,
            estabelecimento = "Uber *Trip",
        )
        salvar(
            "lazer",
            1000,
            instant("2026-11-05T12:00:00Z"),
            categoriaPublicId = lazer.publicId,
            instrumentoPublicId = instrumento.publicId,
            estabelecimento = "Cinema",
        )
        salvar(
            "transferencia",
            50000,
            instant("2026-11-06T12:00:00Z"),
            TipoLancamento.TRANSFERENCIA,
            null,
            instrumento.publicId,
            "Transferência",
        )
        salvar(
            PENDENTE,
            1890,
            instant("2026-11-07T12:00:00Z"),
            status = StatusLancamento.PENDENTE,
            categoriaPublicId = null,
            instrumentoPublicId = null,
            estabelecimento = "Sem Cartão",
        )
        salvar(
            "setembro",
            1500,
            instant("2026-09-10T12:00:00Z"),
            categoriaPublicId = mercado.publicId,
            instrumentoPublicId = instrumento.publicId,
            estabelecimento = "Mercado Antigo",
        )

        return Cenario(saldo = 620000 - 12180, despesas = 12180)
    }

    private suspend fun salvar(
        publicId: String,
        valorCentavos: Long,
        dataHora: Instant,
        tipo: TipoLancamento = TipoLancamento.DESPESA,
        categoriaPublicId: String? = null,
        instrumentoPublicId: String? = null,
        estabelecimento: String? = null,
        status: StatusLancamento = StatusLancamento.CONFIRMADO,
    ) {
        lancamentos.salvar(
            Lancamento(
                publicId = publicId,
                instrumentoPublicId = instrumentoPublicId,
                categoriaPublicId = categoriaPublicId,
                valorCentavos = valorCentavos,
                dataHora = dataHora,
                tipo = tipo,
                estabelecimento = estabelecimento,
                status = status,
                origem = if (instrumentoPublicId == null) OrigemLancamento.NOTIFICACAO else OrigemLancamento.MANUAL,
                instituicaoOrigem = InstituicaoFinanceira.NUBANK,
                confianca = 0.9,
                criadoEm = dataHora,
                atualizadoEm = dataHora,
            ),
        )
    }

    private suspend fun categoriaCriada(nome: String, tipo: TipoCategoria = TipoCategoria.DESPESA): Categoria =
        when (val resultado = categorias.criar(nome, tipo, agora)) {
            is ResultadoCategoria.Sucesso -> resultado.categoria
            else -> throw AssertionError("não foi possível criar a categoria $nome: $resultado")
        }

    private fun instant(valor: String): Instant = Instant.parse(valor)

    private data class Cenario(val saldo: Long, val despesas: Long)

    private class AcessoFixo(private val concedido: Boolean) : AcessoNotificacoes {
        override fun concedido(): Boolean = concedido
    }

    private class SaudeFixa(private val diagnostico: DiagnosticoDaCaptura) : SaudeDaCaptura {
        private val fluxo = MutableStateFlow(diagnostico)

        override fun observar(): Flow<DiagnosticoDaCaptura> = fluxo

        override suspend fun atual(): DiagnosticoDaCaptura = fluxo.value
    }

    private companion object {
        const val PENDENTE = "pendente-sem-cartao"
        const val TEMPO_ESPERA = 10_000L
    }
}
