package com.pingo.feature.transactions.impl

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pingo.core.auth.StoredTokens
import com.pingo.core.auth.TokenStore
import com.pingo.core.auth.UsuarioAutenticado
import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.data.categoria.CategoriaAprendida
import com.pingo.core.data.detalhe.EditarLancamento
import com.pingo.core.data.detalhe.MarcarTransferencia
import com.pingo.core.data.impl.CategoriaRepositoryImpl
import com.pingo.core.data.impl.ContaRepositoryImpl
import com.pingo.core.data.impl.InstrumentoRepositoryImpl
import com.pingo.core.data.impl.LancamentoRepositoryImpl
import com.pingo.core.data.impl.RegraCategoriaRepositoryImpl
import com.pingo.core.data.impl.RoomTransacaoLocal
import com.pingo.core.database.PingoDatabase
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.Categoria
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.Lancamento
import com.pingo.core.model.MotivoPendencia
import com.pingo.core.model.NovaConta
import com.pingo.core.model.NovoInstrumento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.feature.transactions.api.TransactionsRoute
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.Base64

@RunWith(AndroidJUnit4::class)
class DetalheDoLancamentoRoomTest {
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
    fun origemCapturadaIndependeDoInstrumentoCorrigido() {
        val cenario = runBlocking {
            val contaInstrumentos = criarDuasContas()
            val categoria = criarCategoria("Alimentação", TipoCategoria.DESPESA)
            val capturado = lancamento(
                publicId = "capturado-padaria",
                instrumentoPublicId = contaInstrumentos[0].publicId,
                categoriaPublicId = categoria.publicId,
                instituicaoOrigem = InstituicaoFinanceira.MERCADO_PAGO,
                textoBruto = "Padaria do Zé R$ 42,90 aprovado no Mercado Pago",
            )
            lancamentos.salvar(capturado)

            Cenario(capturado, contaInstrumentos)
        }

        val viewModel = criarViewModel(cenario.lancamento.publicId)
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        composeRule.onNodeWithTag("origem-lancamento").performScrollTo()
        composeRule.onNodeWithText("Mercado Pago").assertIsDisplayed()
        composeRule.onNodeWithText("Padaria do Zé R$ 42,90 aprovado no Mercado Pago").assertIsDisplayed()

        composeRule.onNodeWithTag("campo-instrumento-detalhe").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.painel == PainelDoDetalhe.INSTRUMENTO }
        composeRule.onNodeWithTag("instrumento-detalhe-${cenario.instrumentos[1].publicId}").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.instrumento?.publicId == cenario.instrumentos[1].publicId }
        composeRule.onNodeWithTag("salvar-detalhe").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.salvo }

        val persistido = runBlocking { lancamentos.buscar(cenario.lancamento.publicId) }
        assertEquals(cenario.instrumentos[1].publicId, persistido?.instrumentoPublicId)
        assertEquals(InstituicaoFinanceira.MERCADO_PAGO, persistido?.instituicaoOrigem)
        assertEquals("Padaria do Zé R$ 42,90 aprovado no Mercado Pago", persistido?.textoBruto)
    }

    @Test
    fun restauracaoSemTextoBrutoMostraIndisponibilidade() {
        runBlocking {
            criarContaComInstrumento()
            lancamentos.salvar(
                lancamento(
                    publicId = "restaurado",
                    instrumentoPublicId = instrumentos.observeInstrumentos().first().single().publicId,
                    categoriaPublicId = null,
                    instituicaoOrigem = null,
                    textoBruto = null,
                ),
            )
        }

        val viewModel = criarViewModel("restaurado")
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        composeRule.onNodeWithTag("origem-lancamento").performScrollTo()
        composeRule.onNodeWithText("Origem não informada").assertIsDisplayed()
        composeRule.onNodeWithText("Texto indisponível neste aparelho").assertIsDisplayed()
        composeRule.onNodeWithText("Manual").assertDoesNotExist()
    }

    @Test
    fun capturaPendenteExigeEscolhaExplicitaDoInstrumento() {
        val cenario = runBlocking {
            criarContaComInstrumento()
            val instrumento = instrumentos.observeInstrumentos().first().single()
            val categoria = criarCategoria("Alimentação", TipoCategoria.DESPESA)
            val pendente = lancamento(
                publicId = "pendente",
                instrumentoPublicId = null,
                categoriaPublicId = categoria.publicId,
                instituicaoOrigem = InstituicaoFinanceira.INTER,
                textoBruto = null,
                status = StatusLancamento.PENDENTE,
                motivoPendencia = MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name,
            )
            lancamentos.salvar(pendente)

            Cenario(pendente, listOf(instrumento))
        }

        val viewModel = criarViewModel(cenario.lancamento.publicId)
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        assertNull(viewModel.uiState.value.instrumento)

        composeRule.onNodeWithTag("campo-instrumento-detalhe").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.painel == PainelDoDetalhe.INSTRUMENTO }
        composeRule.onNodeWithTag("instrumento-detalhe-${cenario.instrumentos.single().publicId}").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.instrumento != null }
        composeRule.onNodeWithTag("salvar-detalhe").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.salvo }

        val persistido = runBlocking { lancamentos.buscar(cenario.lancamento.publicId) }
        assertEquals(cenario.instrumentos.single().publicId, persistido?.instrumentoPublicId)
        assertEquals(StatusLancamento.CONFIRMADO, persistido?.status)
        assertNull(persistido?.motivoPendencia)
        assertEquals(agora, persistido?.atualizadoEm)
    }

    @Test
    fun editarValorRefleteNaListaDeLancamentos() {
        val cenario = runBlocking {
            criarContaComInstrumento()
            val instrumento = instrumentos.observeInstrumentos().first().single()
            val capturado = lancamento(
                publicId = "capturado",
                instrumentoPublicId = instrumento.publicId,
                categoriaPublicId = criarCategoria("Alimentação", TipoCategoria.DESPESA).publicId,
                instituicaoOrigem = InstituicaoFinanceira.NUBANK,
                textoBruto = "Padaria do Zé R$ 42,90",
            )
            lancamentos.salvar(capturado)
            capturado
        }

        val viewModel = criarViewModel(cenario.publicId)
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        composeRule.onNodeWithTag("bloco-valor-detalhe").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.tecladoVisivel }
        composeRule.onNodeWithTag("tecla-5").performClick()
        composeRule.onNodeWithTag("salvar-detalhe").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.salvo }

        val daLista = runBlocking { lancamentos.observeLancamentos().first() }.single { it.publicId == cenario.publicId }
        assertEquals(42905L, daLista.valorCentavos)
    }

    @Test
    fun excluirComConfirmacaoRemoveDoBancoEDaLista() {
        val publicId = runBlocking {
            criarContaComInstrumento()
            val instrumento = instrumentos.observeInstrumentos().first().single()
            val capturado = lancamento(
                publicId = "capturado",
                instrumentoPublicId = instrumento.publicId,
                categoriaPublicId = null,
                instituicaoOrigem = InstituicaoFinanceira.NUBANK,
                textoBruto = null,
            )
            lancamentos.salvar(capturado)
            capturado.publicId
        }

        val viewModel = criarViewModel(publicId)
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        composeRule.onNodeWithTag("excluir-lancamento").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.dialogoDeExclusao }
        composeRule.onNodeWithText("Excluir lançamento?").assertIsDisplayed()
        composeRule.onNodeWithText("Excluir").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.excluido }

        val excluido = runBlocking { lancamentos.buscar(publicId) }
        assertEquals(agora, excluido?.excluidoEm)
        assertTrue(runBlocking { lancamentos.observeLancamentos().first() }.none { it.publicId == publicId })
    }

    @Test
    fun marcarEDesfazerTransferenciaNaoDuplicaNemDeixaVinculo() {
        val cenario = runBlocking {
            val contaInstrumentos = criarDuasContas()
            val capturado = lancamento(
                publicId = "capturado",
                instrumentoPublicId = contaInstrumentos[0].publicId,
                categoriaPublicId = criarCategoria("Alimentação", TipoCategoria.DESPESA).publicId,
                instituicaoOrigem = InstituicaoFinanceira.NUBANK,
                textoBruto = "Padaria do Zé R$ 42,90",
            )
            lancamentos.salvar(capturado)

            Cenario(capturado, contaInstrumentos)
        }

        val viewModel = criarViewModel(cenario.lancamento.publicId)
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        composeRule.onNodeWithTag("marcar-transferencia").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.painel == PainelDoDetalhe.MARCAR_TRANSFERENCIA }
        composeRule.onNodeWithTag("destino-marcacao-${cenario.instrumentos[1].publicId}").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.par != null }

        assertTrue(viewModel.uiState.value.ehTransferencia)
        val marcados = runBlocking { lancamentos.observeLancamentos().first() }.filter { it.transferenciaId != null }
        assertEquals(2, marcados.size)
        assertEquals(1, marcados.mapNotNull { it.transferenciaId }.distinct().size)
        assertEquals(
            setOf(cenario.instrumentos[0].publicId, cenario.instrumentos[1].publicId),
            marcados.mapNotNull { it.instrumentoPublicId }.toSet(),
        )

        composeRule.onNodeWithTag("desfazer-transferencia").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { !viewModel.uiState.value.ehTransferencia }

        val desfeitos = runBlocking { lancamentos.observeLancamentos().first() }
        assertEquals(2, desfeitos.size)
        assertTrue(desfeitos.all { it.transferenciaId == null })
        assertNull(runBlocking { lancamentos.buscar(cenario.lancamento.publicId) }?.transferenciaId)
        assertFalse(viewModel.uiState.value.ehTransferencia)
    }

    @Test
    fun desfazerPeloDestinoSemCategoriasPreservaDirecoesESaldoEmMesesDistintos() {
        runBlocking {
            val instrumentos = criarDuasContas()
            lancamentos.salvar(
                lancamento(
                    publicId = "saida",
                    instrumentoPublicId = instrumentos[0].publicId,
                    categoriaPublicId = null,
                    instituicaoOrigem = InstituicaoFinanceira.NUBANK,
                    textoBruto = null,
                    dataHora = Instant.parse("2026-12-31T23:55:00Z"),
                    tipo = TipoLancamento.DESPESA,
                    valorCentavos = 5000,
                ),
            )
            lancamentos.salvar(
                lancamento(
                    publicId = "entrada",
                    instrumentoPublicId = instrumentos[1].publicId,
                    categoriaPublicId = null,
                    instituicaoOrigem = InstituicaoFinanceira.INTER,
                    textoBruto = null,
                    dataHora = Instant.parse("2027-01-01T00:05:00Z"),
                    tipo = TipoLancamento.RECEITA,
                    valorCentavos = 5000,
                ),
            )
            transferirPara("saida", instrumentos[1].publicId)
        }

        val viewModel = criarViewModel("entrada")
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        composeRule.onNodeWithTag("desfazer-transferencia").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { !viewModel.uiState.value.ehTransferencia }

        assertEquals(TipoLancamento.DESPESA, runBlocking { lancamentos.buscar("saida") }?.tipo)
        assertEquals(TipoLancamento.RECEITA, runBlocking { lancamentos.buscar("entrada") }?.tipo)
        assertNull(runBlocking { lancamentos.buscar("saida") }?.tipoSemTransferencia)
        assertNull(runBlocking { lancamentos.buscar("entrada") }?.tipoSemTransferencia)
        assertEquals(-5000L, saldoDoMes("2026-12"))
        assertEquals(5000L, saldoDoMes("2027-01"))
    }

    @Test
    fun desfazerParLegadoSemDirecaoRecusaComAvisoESemEscritaParcial() {
        runBlocking {
            val instrumentos = criarDuasContas()
            lancamentos.salvar(
                lancamento(
                    publicId = "saida",
                    instrumentoPublicId = instrumentos[0].publicId,
                    categoriaPublicId = null,
                    instituicaoOrigem = null,
                    textoBruto = null,
                    tipo = TipoLancamento.TRANSFERENCIA,
                    transferenciaId = "transferencia-legada",
                ),
            )
            lancamentos.salvar(
                lancamento(
                    publicId = "entrada",
                    instrumentoPublicId = instrumentos[1].publicId,
                    categoriaPublicId = null,
                    instituicaoOrigem = null,
                    textoBruto = null,
                    tipo = TipoLancamento.TRANSFERENCIA,
                    transferenciaId = "transferencia-legada",
                ),
            )
        }
        val atualizadoAntes = runBlocking { lancamentos.buscar("saida") }?.atualizadoEm

        val viewModel = criarViewModel("entrada")
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        composeRule.onNodeWithTag("desfazer-transferencia").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.mensagem != null }

        assertEquals(MensagemDoDetalhe.DIRECAO_DESCONHECIDA, viewModel.uiState.value.mensagem)
        composeRule.onNodeWithText(
            "Este par antigo não tem a direção registrada. Desfazer poderia inverter despesas e receitas.",
        ).assertIsDisplayed()
        assertTrue(viewModel.uiState.value.ehTransferencia)
        assertEquals(TipoLancamento.TRANSFERENCIA, runBlocking { lancamentos.buscar("saida") }?.tipo)
        assertEquals(TipoLancamento.TRANSFERENCIA, runBlocking { lancamentos.buscar("entrada") }?.tipo)
        assertEquals(atualizadoAntes, runBlocking { lancamentos.buscar("saida") }?.atualizadoEm)
        assertEquals("transferencia-legada", runBlocking { lancamentos.buscar("entrada") }?.transferenciaId)
    }

    @Test
    fun marcarSemInstrumentoResolvidoNaoCriaParEAvisa() {
        val publicId = runBlocking {
            criarDuasContas()
            val pendente = lancamento(
                publicId = "pendente",
                instrumentoPublicId = null,
                categoriaPublicId = null,
                instituicaoOrigem = InstituicaoFinanceira.INTER,
                textoBruto = null,
                status = StatusLancamento.PENDENTE,
                motivoPendencia = MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO.name,
            )
            lancamentos.salvar(pendente)
            pendente.publicId
        }

        val viewModel = criarViewModel(publicId)
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        composeRule.onNodeWithTag("marcar-transferencia").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.mensagem != null }

        assertEquals(MensagemDoDetalhe.INSTRUMENTO_PARA_TRANSFERENCIA, viewModel.uiState.value.mensagem)
        composeRule.onNodeWithText("Escolha o instrumento antes de marcar como transferência.").assertIsDisplayed()
        val todos = runBlocking { lancamentos.observeLancamentos().first() }
        assertEquals(1, todos.size)
        assertNull(todos.single().transferenciaId)
    }

    @Test
    fun aplicarCategoriaATodosGravaRegraParaAsProximasCapturas() {
        val cenario = runBlocking {
            criarContaComInstrumento()
            val instrumento = instrumentos.observeInstrumentos().first().single()
            val alimentacao = criarCategoria("Alimentação", TipoCategoria.DESPESA)
            val mercado = criarCategoria("Mercado", TipoCategoria.DESPESA)
            val capturado = lancamento(
                publicId = "capturado",
                instrumentoPublicId = instrumento.publicId,
                categoriaPublicId = alimentacao.publicId,
                instituicaoOrigem = InstituicaoFinanceira.NUBANK,
                textoBruto = "Padaria do Zé R$ 42,90",
            )
            lancamentos.salvar(capturado)

            mercado
        }

        val viewModel = criarViewModel("capturado")
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        composeRule.onNodeWithTag("aplicar-a-todos-detalhe").performScrollTo().performClick()
        composeRule.onNodeWithTag("campo-categoria-detalhe").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.painel == PainelDoDetalhe.CATEGORIA }
        composeRule.onNodeWithTag("categoria-detalhe-${cenario.publicId}").performClick()
        composeRule.onNodeWithTag("salvar-detalhe").performScrollTo().performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.salvo }

        val regra = runBlocking { regras.buscarPorEstabelecimento("padaria do zé") }
        assertNotNull(regra)
        assertEquals(cenario.publicId, regra?.categoriaPublicId)

        val proxima = runBlocking { aprendida().resolver("Padaria do Zé") }
        assertEquals(cenario.publicId, proxima?.publicId)
    }

    private fun criarViewModel(publicId: String): DetalheDoLancamentoViewModel {
        val transacao = RoomTransacaoLocal(database)
        val aprendida = aprendida()

        return DetalheDoLancamentoViewModel(
            savedStateHandle = SavedStateHandle(mapOf(TransactionsRoute.ARG_PUBLIC_ID to publicId)),
            lancamentoRepository = lancamentos,
            contaRepository = contas,
            instrumentoRepository = instrumentos,
            categoriaRepository = categorias,
            editarLancamento = EditarLancamento(
                lancamentoRepository = lancamentos,
                instrumentoRepository = instrumentos,
                categoriaRepository = categorias,
                categoriaAprendida = aprendida,
                transacaoLocal = transacao,
                clock = clock,
            ),
            marcarTransferencia = MarcarTransferencia(
                lancamentoRepository = lancamentos,
                instrumentoRepository = instrumentos,
                transacaoLocal = transacao,
                clock = clock,
            ),
            clock = clock,
        )
    }

    private fun aprendida(): CategoriaAprendida = CategoriaAprendida(
        categoriaRepository = categorias,
        regraRepository = regras,
        usuario = UsuarioAutenticado(FakeTokenStore(tokensDoUsuario("auth0|lucas"))),
    )

    private fun mostrar(viewModel: DetalheDoLancamentoViewModel) {
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            PingoTheme {
                DetalheDoLancamentoContent(
                    state = state,
                    onEvent = viewModel::onEvent,
                    onVoltar = {},
                    zona = ZoneOffset.UTC,
                    agora = agora,
                )
            }
        }
    }

    private fun aguardarCarregamento(viewModel: DetalheDoLancamentoViewModel) {
        composeRule.waitUntil(TEMPO_ESPERA) { !viewModel.uiState.value.carregando }
    }

    private suspend fun criarContaComInstrumento() {
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
    }

    private suspend fun criarDuasContas(): List<Instrumento> {
        contas.criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.NUBANK,
                    apelido = "Nubank",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(NovoInstrumento(tipo = TipoInstrumento.CREDITO, apelido = "Cartão de crédito")),
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

        return contas.observeContas().first().map { conta -> instrumentos.observePorConta(conta.publicId).first().single() }
    }

    private suspend fun criarCategoria(nome: String, tipo: TipoCategoria): Categoria =
        when (val resultado = categorias.criar(nome, tipo, agora)) {
            is ResultadoCategoria.Sucesso -> resultado.categoria
            else -> error("categoria não criada: $resultado")
        }

    @Suppress("LongParameterList")
    private fun lancamento(
        publicId: String,
        instrumentoPublicId: String?,
        categoriaPublicId: String?,
        instituicaoOrigem: InstituicaoFinanceira?,
        textoBruto: String?,
        status: StatusLancamento = StatusLancamento.CONFIRMADO,
        motivoPendencia: String? = null,
        dataHora: Instant = agora.minus(Duration.ofMinutes(30)),
        tipo: TipoLancamento = TipoLancamento.DESPESA,
        valorCentavos: Long = 4290,
        transferenciaId: String? = null,
        tipoSemTransferencia: TipoLancamento? = null,
    ): Lancamento = Lancamento(
        publicId = publicId,
        instrumentoPublicId = instrumentoPublicId,
        categoriaPublicId = categoriaPublicId,
        transferenciaId = transferenciaId,
        valorCentavos = valorCentavos,
        dataHora = dataHora,
        tipo = tipo,
        tipoSemTransferencia = tipoSemTransferencia,
        estabelecimento = "Padaria do Zé",
        status = status,
        origem = OrigemLancamento.NOTIFICACAO,
        instituicaoOrigem = instituicaoOrigem,
        confianca = 0.86,
        motivoPendencia = motivoPendencia,
        criadoEm = dataHora,
        atualizadoEm = dataHora,
        textoBruto = textoBruto,
    )

    private fun saldoDoMes(competencia: String): Long = runBlocking {
        val mes = YearMonth.parse(competencia)

        lancamentos.observeLancamentos().first()
            .filter { it.transferenciaId == null && YearMonth.from(it.dataHora.atZone(ZoneOffset.UTC)) == mes }
            .sumOf { lancamento ->
                when (lancamento.tipo) {
                    TipoLancamento.RECEITA -> lancamento.valorCentavos
                    TipoLancamento.DESPESA -> -lancamento.valorCentavos
                    TipoLancamento.TRANSFERENCIA, TipoLancamento.AJUSTE -> 0L
                }
            }
    }

    private fun transferirPara(publicId: String, destinoPublicId: String) = runBlocking {
        MarcarTransferencia(
            lancamentoRepository = lancamentos,
            instrumentoRepository = instrumentos,
            transacaoLocal = RoomTransacaoLocal(database),
            clock = clock,
        ).marcar(publicId, destinoPublicId)
    }

    private data class Cenario(val lancamento: Lancamento, val instrumentos: List<Instrumento>)

    private class FakeTokenStore(private var tokens: StoredTokens?) : TokenStore {
        override suspend fun read(): StoredTokens? = tokens

        override suspend fun write(tokens: StoredTokens) {
            this.tokens = tokens
        }

        override suspend fun clear() {
            tokens = null
        }
    }

    private fun tokensDoUsuario(sub: String): StoredTokens =
        StoredTokens(accessToken = jwtComPayload("""{"sub":"$sub"}"""), refreshToken = null, expiresAtEpochMillis = Long.MAX_VALUE)

    private fun jwtComPayload(payload: String): String {
        val codificador = Base64.getUrlEncoder().withoutPadding()
        val cabecalho = codificador.encodeToString("""{"alg":"RS256"}""".toByteArray())

        return listOf(cabecalho, codificador.encodeToString(payload.toByteArray()), "assinatura").joinToString(".")
    }

    private companion object {
        const val TEMPO_ESPERA = 7000L
    }
}
