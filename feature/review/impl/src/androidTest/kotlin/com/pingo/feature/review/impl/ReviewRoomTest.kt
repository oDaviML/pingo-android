package com.pingo.feature.review.impl

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
import com.pingo.core.data.reconciliacao.ReconciliadorFinanceiro
import com.pingo.core.data.revisao.ConfirmarPendencias
import com.pingo.core.data.voucher.ResolverRecargaVoucherUseCase
import com.pingo.core.database.PingoDatabase
import com.pingo.core.database.entity.RecargaProgramadaVoucherEntity
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.BeneficioVoucher
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
import com.pingo.core.model.voucher.StatusRecargaProgramada
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
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Base64

@RunWith(AndroidJUnit4::class)
class ReviewRoomTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val agora = Instant.parse("2026-11-10T12:00:00Z")
    private val clock = Clock.fixed(agora, ZoneOffset.UTC)
    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val corrigidos = mutableListOf<String>()

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
    fun listaMostraMotivoTrechoEVinculoInclusiveSemInstrumento() {
        runBlocking {
            val contasDeTeste = criarDuasContas()
            val alimentacao = criarCategoria("Alimentação", TipoCategoria.DESPESA)
            val padaria = lancamento(
                publicId = "padaria",
                instrumentoPublicId = contasDeTeste.nubank.publicId,
                categoriaPublicId = alimentacao.publicId,
                motivo = MotivoPendencia.BAIXA_CONFIANCA,
                textoBruto = "Compra aprovada R\$ 42,90 PADARIA DO ZE",
            )
            val restaurado = lancamento(
                publicId = "restaurado",
                instrumentoPublicId = null,
                categoriaPublicId = null,
                motivo = MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO,
                textoBruto = null,
                estabelecimento = "ZPT*LOJA2231",
                instituicaoOrigem = InstituicaoFinanceira.ITAU,
                valorCentavos = 1990,
                dataHora = agora.minus(Duration.ofMinutes(10)),
            )
            lancamentos.salvar(padaria)
            lancamentos.salvar(restaurado)
        }

        val viewModel = criarViewModel()
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        composeRule.onNodeWithText("2 para revisar").assertIsDisplayed()
        composeRule.onNodeWithText("Baixa confiança · 62%").assertIsDisplayed()
        composeRule.onNodeWithText("Padaria do Zé").assertIsDisplayed()
        composeRule.onNodeWithText("Nubank · crédito · hoje, 11:30").assertIsDisplayed()
        composeRule.onNodeWithText("- R\$ 42,90").assertIsDisplayed()
        composeRule.onNodeWithText("“Compra aprovada R\$ 42,90 PADARIA DO ZE”").assertIsDisplayed()

        rolarAte("pendencia-restaurado")
        composeRule.onNodeWithText("Instrumento não encontrado").assertIsDisplayed()
        composeRule.onNodeWithText("Instrumento não identificado · hoje, 11:50").assertIsDisplayed()
        composeRule.onNodeWithText("ZPT*LOJA2231").assertIsDisplayed()
        composeRule.onNodeWithText("Texto indisponível neste aparelho").assertIsDisplayed()
        assertEquals(listOf("restaurado", "padaria"), viewModel.uiState.value.pendentes.map { it.publicId })
    }

    @Test
    fun confirmarPendenteElegivelRemoveEDeixaIrresolvido() {
        runBlocking {
            val contasDeTeste = criarDuasContas()
            lancamentos.salvar(
                lancamento(
                    publicId = "padaria",
                    instrumentoPublicId = contasDeTeste.nubank.publicId,
                    motivo = MotivoPendencia.BAIXA_CONFIANCA,
                    textoBruto = "Compra aprovada",
                ),
            )
            lancamentos.salvar(
                lancamento(
                    publicId = "sem-instrumento",
                    instrumentoPublicId = null,
                    motivo = MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO,
                    textoBruto = null,
                    dataHora = agora.minus(Duration.ofMinutes(10)),
                ),
            )
        }

        val viewModel = criarViewModel()
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        composeRule.onNodeWithTag("confirmar-padaria").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.pendentes.none { it.publicId == "padaria" } }

        assertEquals(StatusLancamento.CONFIRMADO, runBlocking { lancamentos.buscar("padaria") }?.status)
        assertNull(runBlocking { lancamentos.buscar("padaria") }?.motivoPendencia)

        rolarAte("pendencia-sem-instrumento")
        composeRule.onNodeWithText("Escolha um instrumento no detalhe para confirmar.").assertIsDisplayed()
        composeRule.onNodeWithTag("confirmar-sem-instrumento").assertIsNotEnabled()
        assertEquals(StatusLancamento.PENDENTE, runBlocking { lancamentos.buscar("sem-instrumento") }?.status)
    }

    @Test
    fun trocarCategoriaComSempreUsarGravaRegra() {
        runBlocking {
            val contasDeTeste = criarDuasContas()
            criarCategoria("Alimentação", TipoCategoria.DESPESA)
            criarCategoria("Mercado", TipoCategoria.DESPESA)
            lancamentos.salvar(
                lancamento(
                    publicId = "padaria",
                    instrumentoPublicId = contasDeTeste.nubank.publicId,
                    categoriaPublicId = null,
                    motivo = MotivoPendencia.CATEGORIA_DESCONHECIDA,
                    textoBruto = "Compra aprovada R\$ 42,90 PADARIA DO ZE",
                ),
            )
        }

        val viewModel = criarViewModel()
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        composeRule.onNodeWithTag("trocar-categoria-padaria").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.painelDeCategoria != null }
        val mercado = runBlocking { categorias.observeEscolhiveis().first() }.first { it.nome == "Mercado" }

        composeRule.onNodeWithTag("categoria-revisao-${mercado.publicId}").performClick()
        composeRule.onNodeWithTag("aplicar-a-todos-revisao").performClick()
        composeRule.onNodeWithTag("aplicar-categoria-revisao").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.painelDeCategoria == null }

        val persistido = runBlocking { lancamentos.buscar("padaria") }
        assertEquals(mercado.publicId, persistido?.categoriaPublicId)
        assertEquals(StatusLancamento.CONFIRMADO, persistido?.status)

        val regra = runBlocking { regras.buscarPorEstabelecimento("padaria do zé") }
        assertNotNull(regra)
        assertEquals(mercado.publicId, regra?.categoriaPublicId)
        assertEquals(mercado.publicId, runBlocking { aprendida().resolver("Padaria do Zé") }?.publicId)
    }

    @Test
    fun seletorDeDestinoDaRevisaoIgnoraInstrumentoArquivado() {
        val (ativo, arquivado) = runBlocking {
            val contasDeTeste = criarDuasContas()
            val antigo = Instrumento(
                publicId = "inst-arquivado",
                contaPublicId = contasDeTeste.inter.contaPublicId,
                tipo = TipoInstrumento.DEBITO,
                apelido = "Débito antigo",
                criadoEm = agora,
                atualizadoEm = agora,
            )
            instrumentos.salvar(antigo)
            instrumentos.arquivar(antigo.publicId, agora)
            lancamentos.salvar(
                lancamento(
                    publicId = "saida",
                    instrumentoPublicId = contasDeTeste.nubank.publicId,
                    motivo = MotivoPendencia.TRANSFERENCIA_AMBIGUA,
                    textoBruto = "Pix enviado R\$ 150,00",
                    valorCentavos = 15000,
                    tipo = TipoLancamento.DESPESA,
                ),
            )

            contasDeTeste.inter to antigo
        }

        val viewModel = criarViewModel()
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        composeRule.onNodeWithTag("marcar-transferencia-saida").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.painelDeTransferencia != null }

        composeRule.onNodeWithTag("destino-revisao-${ativo.publicId}").assertIsDisplayed()
        composeRule.onNodeWithTag("destino-revisao-${arquivado.publicId}").assertDoesNotExist()
    }

    @Test
    fun marcarTransferenciaResolveOPendente() {
        val destino = runBlocking {
            val contasDeTeste = criarDuasContas()
            lancamentos.salvar(
                lancamento(
                    publicId = "saida",
                    instrumentoPublicId = contasDeTeste.nubank.publicId,
                    motivo = MotivoPendencia.TRANSFERENCIA_AMBIGUA,
                    textoBruto = "Pix enviado R\$ 150,00",
                    valorCentavos = 15000,
                    tipo = TipoLancamento.DESPESA,
                ),
            )

            contasDeTeste.inter
        }

        val viewModel = criarViewModel()
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        composeRule.onNodeWithTag("marcar-transferencia-saida").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.painelDeTransferencia != null }
        composeRule.onNodeWithTag("destino-revisao-${destino.publicId}").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.pendentes.isEmpty() }

        val saida = runBlocking { lancamentos.buscar("saida") }
        assertEquals(TipoLancamento.TRANSFERENCIA, saida?.tipo)
        assertEquals(StatusLancamento.CONFIRMADO, saida?.status)
        assertNotNull(saida?.transferenciaId)
        val todos = runBlocking { lancamentos.observeLancamentos().first() }
        assertEquals(2, todos.size)
        assertTrue(todos.all { it.transferenciaId == saida?.transferenciaId })
        composeRule.onNodeWithTag("revisao-vazia").assertIsDisplayed()
    }

    @Test
    fun marcarTransferenciaResolvePendenciaDeBaixaConfianca() {
        val destino = runBlocking {
            val contasDeTeste = criarDuasContas()
            lancamentos.salvar(
                lancamento(
                    publicId = "baixa-confianca",
                    instrumentoPublicId = contasDeTeste.nubank.publicId,
                    motivo = MotivoPendencia.BAIXA_CONFIANCA,
                    valorCentavos = 15000,
                    tipo = TipoLancamento.DESPESA,
                ),
            )

            contasDeTeste.inter
        }

        val viewModel = criarViewModel()
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        composeRule.onNodeWithTag("marcar-transferencia-baixa-confianca").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.painelDeTransferencia != null }
        composeRule.onNodeWithTag("destino-revisao-${destino.publicId}").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.pendentes.isEmpty() }

        val marcado = runBlocking { lancamentos.buscar("baixa-confianca") }
        assertEquals(StatusLancamento.CONFIRMADO, marcado?.status)
        assertNull(marcado?.motivoPendencia)
        assertEquals(0.62, marcado?.confianca ?: 0.0, 0.0001)
        composeRule.onNodeWithTag("revisao-vazia").assertIsDisplayed()
    }

    @Test
    fun descartarRemoveOPendenteNuloSemAfetarOsDemais() {
        runBlocking {
            val contasDeTeste = criarDuasContas()
            lancamentos.salvar(
                lancamento(
                    publicId = "padaria",
                    instrumentoPublicId = contasDeTeste.nubank.publicId,
                    motivo = MotivoPendencia.BAIXA_CONFIANCA,
                    textoBruto = "Compra aprovada",
                ),
            )
            lancamentos.salvar(
                lancamento(
                    publicId = "sem-instrumento",
                    instrumentoPublicId = null,
                    motivo = MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO,
                    textoBruto = null,
                    dataHora = agora.minus(Duration.ofMinutes(10)),
                ),
            )
        }

        val viewModel = criarViewModel()
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        rolarAte("pendencia-sem-instrumento")
        composeRule.onNodeWithTag("descartar-sem-instrumento").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.pendentes.none { it.publicId == "sem-instrumento" } }

        assertNotNull(runBlocking { lancamentos.buscar("sem-instrumento") }?.excluidoEm)
        assertNull(runBlocking { lancamentos.buscar("padaria") }?.excluidoEm)
        assertEquals(1, viewModel.uiState.value.quantidade)
    }

    @Test
    fun confirmarTodosEmLoteMistoMantemIrresolvidosComOrientacao() {
        runBlocking {
            val contasDeTeste = criarDuasContas()
            lancamentos.salvar(
                lancamento(
                    publicId = "padaria",
                    instrumentoPublicId = contasDeTeste.nubank.publicId,
                    motivo = MotivoPendencia.BAIXA_CONFIANCA,
                    textoBruto = "Compra aprovada",
                ),
            )
            lancamentos.salvar(
                lancamento(
                    publicId = "sem-instrumento",
                    instrumentoPublicId = null,
                    motivo = MotivoPendencia.INSTRUMENTO_NAO_ENCONTRADO,
                    textoBruto = null,
                    dataHora = agora.minus(Duration.ofMinutes(10)),
                ),
            )
        }

        val viewModel = criarViewModel()
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        composeRule.onNodeWithTag("confirmar-todos").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.pendentes.none { it.publicId == "padaria" } }

        assertEquals(StatusLancamento.CONFIRMADO, runBlocking { lancamentos.buscar("padaria") }?.status)
        assertEquals(StatusLancamento.PENDENTE, runBlocking { lancamentos.buscar("sem-instrumento") }?.status)
        composeRule.onNodeWithText("1 lançamento continua pendente. Corrija o instrumento no detalhe.").assertIsDisplayed()
        assertFalse(viewModel.uiState.value.vazia)
    }

    @Test
    fun confirmarTodosEsvaziaQuandoTodosSaoElegiveis() {
        runBlocking {
            val contasDeTeste = criarDuasContas()
            lancamentos.salvar(
                lancamento(
                    publicId = "padaria",
                    instrumentoPublicId = contasDeTeste.nubank.publicId,
                    motivo = MotivoPendencia.BAIXA_CONFIANCA,
                    textoBruto = "Compra aprovada",
                ),
            )
            lancamentos.salvar(
                lancamento(
                    publicId = "duplicata",
                    instrumentoPublicId = contasDeTeste.nubank.publicId,
                    motivo = MotivoPendencia.POSSIVEL_DUPLICATA,
                    textoBruto = "Pix enviado",
                    dataHora = agora.minus(Duration.ofMinutes(5)),
                ),
            )
        }

        val viewModel = criarViewModel()
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        composeRule.onNodeWithTag("confirmar-todos").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.vazia }

        composeRule.onNodeWithText("Tudo em dia 💧").assertIsDisplayed()
        composeRule.onNodeWithText("Nenhum lançamento esperando revisão.").assertIsDisplayed()
        assertEquals(StatusLancamento.CONFIRMADO, runBlocking { lancamentos.buscar("padaria") }?.status)
        assertEquals(StatusLancamento.CONFIRMADO, runBlocking { lancamentos.buscar("duplicata") }?.status)
    }

    @Test
    fun corrigirEntregaOPublicIdDoPendente() {
        runBlocking {
            val contasDeTeste = criarDuasContas()
            lancamentos.salvar(
                lancamento(
                    publicId = "padaria",
                    instrumentoPublicId = contasDeTeste.nubank.publicId,
                    motivo = MotivoPendencia.BAIXA_CONFIANCA,
                    textoBruto = "Compra aprovada",
                ),
            )
        }

        val viewModel = criarViewModel()
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        composeRule.onNodeWithTag("corrigir-padaria").performClick()

        assertEquals(listOf("padaria"), corrigidos)
    }

    @Test
    fun resolveBeneficioAmbiguoComRoomEAtualizaLancamento() {
        val vouchers = runBlocking { criarVouchersCaju() }
        runBlocking {
            lancamentos.salvar(
                Lancamento(
                    publicId = "rec-amb-room",
                    instrumentoPublicId = null,
                    categoriaPublicId = null,
                    valorCentavos = 70000,
                    dataHora = agora,
                    tipo = TipoLancamento.RECEITA,
                    estabelecimento = "Recarga Caju",
                    status = StatusLancamento.PENDENTE,
                    origem = OrigemLancamento.NOTIFICACAO,
                    instituicaoOrigem = InstituicaoFinanceira.CAJU,
                    confianca = 0.7,
                    motivoPendencia = MotivoPendencia.BENEFICIO_AMBIGUO.name,
                    criadoEm = agora,
                    atualizadoEm = agora,
                    textoBruto = "Recarga recebida R\$ 700,00",
                ),
            )
        }

        val viewModel = criarViewModelComVoucher()
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        composeRule.onNodeWithTag("confirmar-rec-amb-room").assertIsNotEnabled()
        composeRule.onNodeWithTag("resolver-voucher-rec-amb-room").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.painelDeVoucher != null }

        composeRule.onNodeWithTag("voucher-opcao-${vouchers.vr.publicId}").performClick()
        composeRule.onNodeWithTag("confirmar-voucher-revisao").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.vazia }

        val salvo = runBlocking { lancamentos.buscar("rec-amb-room") }
        assertEquals(StatusLancamento.CONFIRMADO, salvo?.status)
        assertEquals(vouchers.vr.publicId, salvo?.instrumentoPublicId)
        assertEquals(BeneficioVoucher.VR, salvo?.beneficio)
    }

    @Test
    fun resolveDivergenciaRecargaComRoomSubstituindoPrevisao() {
        val vouchers = runBlocking { criarVouchersCaju() }
        runBlocking {
            database.recargaProgramadaVoucherDao().upsert(
                RecargaProgramadaVoucherEntity(
                    publicId = "prog-room-1",
                    instrumentoPublicId = vouchers.va.publicId,
                    beneficio = BeneficioVoucher.VA,
                    competenciaMes = LocalDate.of(2026, 11, 1),
                    dataPrevista = LocalDate.of(2026, 11, 10),
                    valorPrevistoCentavos = 50000,
                    status = StatusRecargaProgramada.PENDENTE,
                    lancamentoPublicId = null,
                    criadoEm = agora,
                    atualizadoEm = agora,
                    excluidoEm = null,
                ),
            )
            lancamentos.salvar(
                Lancamento(
                    publicId = "rec-div-room",
                    instrumentoPublicId = vouchers.va.publicId,
                    categoriaPublicId = null,
                    valorCentavos = 65000,
                    dataHora = agora,
                    tipo = TipoLancamento.RECEITA,
                    estabelecimento = "Recarga Caju",
                    status = StatusLancamento.PENDENTE,
                    origem = OrigemLancamento.NOTIFICACAO,
                    instituicaoOrigem = InstituicaoFinanceira.CAJU,
                    beneficio = BeneficioVoucher.VA,
                    confianca = 0.7,
                    motivoPendencia = MotivoPendencia.DIVERGENCIA_RECARGA.name,
                    criadoEm = agora,
                    atualizadoEm = agora,
                    textoBruto = "Recarga VA R\$ 650,00",
                ),
            )
        }

        val viewModel = criarViewModelComVoucher()
        mostrar(viewModel)
        aguardarCarregamento(viewModel)

        composeRule.onNodeWithTag("resolver-voucher-rec-div-room").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.painelDeVoucher != null }

        composeRule.onNodeWithTag("decisao-substituir-previsao").performClick()
        composeRule.onNodeWithTag("confirmar-voucher-revisao").performClick()
        composeRule.waitUntil(TEMPO_ESPERA) { viewModel.uiState.value.vazia }

        val salvo = runBlocking { lancamentos.buscar("rec-div-room") }
        val recarga = runBlocking { database.recargaProgramadaVoucherDao().buscar("prog-room-1") }
        assertEquals(StatusLancamento.CONFIRMADO, salvo?.status)
        assertEquals(StatusRecargaProgramada.SUBSTITUIDA, recarga?.status)
        assertEquals("rec-div-room", recarga?.lancamentoPublicId)
    }

    private fun criarViewModelComVoucher(): ReviewViewModel {
        val transacao = RoomTransacaoLocal(database)
        val resolver = ResolverRecargaVoucherUseCase(
            transacaoLocal = transacao,
            lancamentoDao = database.lancamentoDao(),
            instrumentoDao = database.instrumentoDao(),
            recargaDao = database.recargaProgramadaVoucherDao(),
            categoriaDao = database.categoriaDao(),
            reconciliador = ReconciliadorFinanceiro.NoOp,
            clock = clock,
        )
        return ReviewViewModel(
            lancamentoRepository = lancamentos,
            contaRepository = contas,
            instrumentoRepository = instrumentos,
            categoriaRepository = categorias,
            confirmarPendencias = ConfirmarPendencias(lancamentos, instrumentos, clock),
            editarLancamento = EditarLancamento(
                lancamentoRepository = lancamentos,
                instrumentoRepository = instrumentos,
                categoriaRepository = categorias,
                categoriaAprendida = aprendida(),
                transacaoLocal = transacao,
                clock = clock,
            ),
            marcarTransferencia = MarcarTransferencia(
                lancamentoRepository = lancamentos,
                instrumentoRepository = instrumentos,
                transacaoLocal = transacao,
                clock = clock,
            ),
            resolverRecargaVoucher = resolver,
            clock = clock,
        )
    }

    private suspend fun criarVouchersCaju(): VouchersDeTeste {
        contas.criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.CAJU,
                    apelido = "Caju",
                    tipo = TipoConta.BENEFICIO,
                    instrumentos = listOf(
                        NovoInstrumento(tipo = TipoInstrumento.VOUCHER, apelido = "Refeição", beneficio = BeneficioVoucher.VR),
                        NovoInstrumento(tipo = TipoInstrumento.VOUCHER, apelido = "Alimentação", beneficio = BeneficioVoucher.VA),
                    ),
                ),
            ),
            agora,
        )
        val todos = instrumentos.observeInstrumentos().first()
        return VouchersDeTeste(
            vr = todos.first { it.beneficio == BeneficioVoucher.VR },
            va = todos.first { it.beneficio == BeneficioVoucher.VA },
        )
    }

    private data class VouchersDeTeste(val vr: Instrumento, val va: Instrumento)

    private fun criarViewModel(): ReviewViewModel {
        val transacao = RoomTransacaoLocal(database)
        val aprendida = aprendida()

        return ReviewViewModel(
            lancamentoRepository = lancamentos,
            contaRepository = contas,
            instrumentoRepository = instrumentos,
            categoriaRepository = categorias,
            confirmarPendencias = ConfirmarPendencias(lancamentos, instrumentos, clock),
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

    private fun mostrar(viewModel: ReviewViewModel) {
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            PingoTheme {
                ReviewContent(
                    state = state,
                    onEvent = viewModel::onEvent,
                    onCorrigir = { corrigidos += it },
                    onVoltar = {},
                )
            }
        }
    }

    private fun aguardarCarregamento(viewModel: ReviewViewModel) {
        composeRule.waitUntil(TEMPO_ESPERA) { !viewModel.uiState.value.carregando }
    }

    private fun rolarAte(tag: String) {
        composeRule.onNodeWithTag("lista-revisao").performScrollToNode(hasTestTag(tag))
    }

    private suspend fun criarDuasContas(): ContasDeTeste {
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
        val contasPorInstituicao = contas.observeContas().first().associateBy { it.instituicao }
        val todos = instrumentos.observeInstrumentos().first()

        return ContasDeTeste(
            nubank = todos.first { it.contaPublicId == contasPorInstituicao.getValue(InstituicaoFinanceira.NUBANK).publicId },
            inter = todos.first { it.contaPublicId == contasPorInstituicao.getValue(InstituicaoFinanceira.INTER).publicId },
        )
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
        motivo: MotivoPendencia,
        textoBruto: String? = null,
        categoriaPublicId: String? = null,
        estabelecimento: String = "Padaria do Zé",
        instituicaoOrigem: InstituicaoFinanceira? = InstituicaoFinanceira.NUBANK,
        valorCentavos: Long = 4290,
        tipo: TipoLancamento = TipoLancamento.DESPESA,
        dataHora: Instant = agora.minus(Duration.ofMinutes(30)),
    ): Lancamento = Lancamento(
        publicId = publicId,
        instrumentoPublicId = instrumentoPublicId,
        categoriaPublicId = categoriaPublicId,
        valorCentavos = valorCentavos,
        dataHora = dataHora,
        tipo = tipo,
        estabelecimento = estabelecimento,
        status = StatusLancamento.PENDENTE,
        origem = OrigemLancamento.NOTIFICACAO,
        instituicaoOrigem = instituicaoOrigem,
        confianca = 0.62,
        motivoPendencia = motivo.name,
        criadoEm = dataHora,
        atualizadoEm = dataHora,
        textoBruto = textoBruto,
    )

    private data class ContasDeTeste(val nubank: Instrumento, val inter: Instrumento)

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
