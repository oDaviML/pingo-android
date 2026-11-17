package com.pingo.inicio

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.data.di.CapturaEntryPoint
import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.CapturaDataSourceImpl
import com.pingo.core.datastore.EstadoListener
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Lancamento
import com.pingo.core.model.NovaConta
import com.pingo.core.model.NovoInstrumento
import com.pingo.core.model.OrigemLancamento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.notifications.AcessoNotificacoesDoSistema
import com.pingo.core.notifications.CapturaController
import com.pingo.core.notifications.MonitorDaCaptura
import com.pingo.core.notifications.NotificacoesDeRevisao
import com.pingo.core.notifications.ResultadoAtualizacaoRegras
import com.pingo.core.notifications.di.CapturaStatusEntryPoint
import com.pingo.core.ui.Valores
import com.pingo.feature.capturestatus.impl.AtualizacaoDeRegras
import com.pingo.feature.capturestatus.impl.CaptureStatusScreen
import com.pingo.feature.capturestatus.impl.CaptureStatusViewModel
import com.pingo.feature.home.impl.HomeContent
import com.pingo.feature.home.impl.HomeViewModel
import com.pingo.feature.transactions.impl.TransactionsContent
import com.pingo.feature.transactions.impl.TransactionsViewModel
import com.pingo.support.prepararListenerDeCaptura
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
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
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class InicioFluxoTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val injecao: CapturaStatusEntryPoint = EntryPointAccessors.fromApplication(contexto, CapturaStatusEntryPoint::class.java)
    private val captura: CapturaEntryPoint = EntryPointAccessors.fromApplication(contexto, CapturaEntryPoint::class.java)
    private val dataSource: CapturaDataSource = CapturaDataSourceImpl(contexto)
    private val acesso = AcessoNotificacoesDoSistema(contexto)
    private val clock: Clock = Clock.systemDefaultZone()
    private val controller: CapturaController = injecao.capturaController()

    private lateinit var transactionsViewModel: TransactionsViewModel
    private lateinit var homeViewModel: HomeViewModel
    private var detalheAberto: String? = null
    private val tela = mutableStateOf(Tela.LANCAMENTOS)

    @Before
    fun preparaAmbiente() {
        limparBancoEEstado()
        shell("pm grant $PACOTE android.permission.POST_NOTIFICATIONS")
        prepararListenerDeCaptura(::shell)
        runBlocking { controller.reconectar() }
        aguardarEstado(EstadoListener.ATIVO)
        aguardarContadoresEstaveis()
    }

    @After
    fun limpaAmbiente() {
        gerenciadorDeRevisao().forEach { gerenciador().cancel(it.tag, it.id) }
        shell("cmd notification allow_listener $COMPONENTE")
        limparBancoEEstado()
        runBlocking { controller.reconectar() }
    }

    @Test
    fun capturaResolvidaApareceEmLancamentosEmAteDoisSegundosEAtualizaAInicio() {
        semearInstrumentoECategoria()
        criarViewModels()
        mostrarTelas()

        val valor = valorUnico()
        val inicio = System.nanoTime()

        postar("Sua compra foi aprovada", "pingo-teste-ITAU", textoCompra(valor))

        val lancamento = runBlocking {
            withTimeoutOrNull(TEMPO_ESPERA) {
                transactionsViewModel.uiState.first { estado ->
                    estado.dias.any { dia -> dia.itens.any { it.lancamento.valorCentavos == valor } }
                }
            }
        }
        val decorrido = Duration.ofNanos(System.nanoTime() - inicio)

        assertNotNull("o lançamento capturado não apareceu na lista de Lançamentos", lancamento)
        assertTrue("captura notificação→Lançamentos levou ${decorrido.toMillis()}ms", decorrido <= Duration.ofSeconds(2))

        val capturado = runBlocking {
            captura.lancamentoRepository().observeLancamentos().first { lista ->
                lista.any { it.valorCentavos == valor && it.origem == OrigemLancamento.NOTIFICACAO }
            }.first { it.valorCentavos == valor }
        }
        assertEquals(StatusLancamento.CONFIRMADO, capturado.status)
        assertEquals(InstituicaoFinanceira.ITAU, capturado.instituicaoOrigem)

        composeRule.waitUntil(TEMPO_ESPERA) {
            composeRule.onAllNodesWithText(TEXTO_ESTABELECIMENTO_ITAU, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        salvarCaptura("lancamentos-captura-resolvida.png")

        trocarParaInicio()

        composeRule.waitUntil(TEMPO_ESPERA) { homeViewModel.uiState.value.ultimos.any { it.lancamento.publicId == capturado.publicId } }
        assertEquals(valor, homeViewModel.uiState.value.despesasCentavos)
        assertEquals(-valor, homeViewModel.uiState.value.saldoCentavos)
        composeRule.onNodeWithTag("saldo-do-mes").assertIsDisplayed()
        composeRule.onNodeWithText(Valores.formatar(-valor)).assertIsDisplayed()
        composeRule.onNodeWithTag("ultimos-lancamentos").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(TEXTO_ESTABELECIMENTO_ITAU, substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Outros").performScrollTo().assertIsDisplayed()

        salvarCaptura("inicio-captura-resolvida.png")
    }

    @Test
    fun capturaSemInstrumentoFicaVisivelEmLancamentosUltimosRevisaoESemCartaoArbitrario() {
        criarViewModels()
        mostrarTelas()

        val valor = valorUnico()
        val inicio = System.nanoTime()

        postar("Compra aprovada", "pingo-teste-NUBANK", textoCompraNubank(valor))

        val lancamento = runBlocking {
            withTimeoutOrNull(TEMPO_ESPERA) {
                transactionsViewModel.uiState.first { estado ->
                    estado.dias.any { dia -> dia.itens.any { it.lancamento.valorCentavos == valor } }
                }
            }
        }
        val decorrido = Duration.ofNanos(System.nanoTime() - inicio)
        assertNotNull("a captura pendente não apareceu na lista de Lançamentos", lancamento)
        assertTrue("captura notificação→Lançamentos levou ${decorrido.toMillis()}ms", decorrido <= Duration.ofSeconds(2))

        val pendente = runBlocking {
            captura.lancamentoRepository().observeLancamentos().first { lista ->
                lista.any { it.valorCentavos == valor }
            }.first { it.valorCentavos == valor }
        }
        assertNull(pendente.instrumentoPublicId)
        assertEquals(StatusLancamento.PENDENTE, pendente.status)
        assertEquals(InstituicaoFinanceira.NUBANK, pendente.instituicaoOrigem)

        composeRule.waitUntil(TEMPO_ESPERA) {
            composeRule.onAllNodesWithText("Instrumento não identificado", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        salvarCaptura("lancamentos-pendente-sem-instrumento.png")
        composeRule.onNodeWithTag("lancamento-${pendente.publicId}").performClick()
        assertEquals(pendente.publicId, detalheAberto)

        trocarParaInicio()
        composeRule.waitUntil(TEMPO_ESPERA) { homeViewModel.uiState.value.pendentes == 1 }
        assertEquals(1, homeViewModel.uiState.value.pendentes)
        assertTrue(homeViewModel.uiState.value.ultimos.any { it.lancamento.publicId == pendente.publicId })
        assertEquals(-valor, homeViewModel.uiState.value.saldoCentavos)
        composeRule.onNodeWithTag("aviso-revisao").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("ultimos-lancamentos").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Instrumento não identificado", substring = true).performScrollTo().assertIsDisplayed()

        salvarCaptura("inicio-pendente-sem-instrumento.png")
    }

    @Test
    fun capturaParadaApareceNaInicioNaT10EInterrompeOsContadores() {
        shell("cmd notification disallow_listener $COMPONENTE")
        aguardarEstado(EstadoListener.PARADO, MotivoParada.PERMISSAO_REVOGADA)
        aguardarAcessoRevogado()

        val mes = YearMonth.from(Instant.now().atZone(ZoneId.systemDefault()))
        val antes = contadores(mes)
        val valor = valorUnico()
        postar("Sua compra foi aprovada", "pingo-teste-ITAU", textoCompra(valor))
        Thread.sleep(INTERVALO_ESTABILIZACAO)

        assertEquals(antes, contadores(mes))

        homeViewModel = HomeViewModel(
            acessoNotificacoes = injecao.acessoNotificacoes(),
            saudeDaCaptura = injecao.saudeDaCaptura(),
            lancamentoRepository = captura.lancamentoRepository(),
            contaRepository = captura.contaRepository(),
            instrumentoRepository = captura.instrumentoRepository(),
            categoriaRepository = captura.categoriaRepository(),
            clock = clock,
        )
        val status = CaptureStatusViewModel(
            capturaDataSource = dataSource,
            saudeDaCaptura = injecao.saudeDaCaptura(),
            regraParserRepository = injecao.regraParserRepository(),
            atualizacaoDeRegras = SemAtualizacao,
            capturaController = injecao.capturaController(),
            acessoNotificacoes = injecao.acessoNotificacoes(),
            notificacoesDoSistema = injecao.notificacoesDoSistema(),
            bateriaDoAparelho = injecao.bateriaDoAparelho(),
            clock = clock,
        )
        var correcoes = 0
        var telaDaCaptura by mutableStateOf(TelaDaCaptura.INICIO)
        composeRule.setContent {
            PingoTheme {
                when (telaDaCaptura) {
                    TelaDaCaptura.INICIO -> {
                        val state by homeViewModel.uiState.collectAsStateWithLifecycle()
                        HomeContent(
                            state = state,
                            onOpenSettings = {},
                            onOpenCaptura = { correcoes++ },
                            onOpenRevisao = {},
                            onOpenNovo = {},
                            onMesAnterior = homeViewModel::mesAnterior,
                            onMesProximo = homeViewModel::mesProximo,
                        )
                    }

                    TelaDaCaptura.STATUS -> CaptureStatusScreen(onBack = {}, viewModel = status)
                }
            }
        }

        composeRule.waitUntil(TEMPO_ESPERA) { homeViewModel.uiState.value.capturaParada }
        assertFalse(homeViewModel.uiState.value.capturaAutomatica)
        composeRule.onNodeWithText("Captura parada").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sem acesso às notificações, o Pingo está no modo manual.").assertIsDisplayed()
        composeRule.onNodeWithText("Corrigir").performScrollTo().performClick()
        assertEquals(1, correcoes)
        salvarCaptura("inicio-captura-parada.png")

        composeRule.runOnIdle { telaDaCaptura = TelaDaCaptura.STATUS }
        composeRule.waitUntil(TEMPO_ESPERA) { status.uiState.value.parada }
        composeRule.onNodeWithText("Captura parada").assertIsDisplayed()
        composeRule.onNodeWithText("Sem acesso às notificações", substring = true).assertIsDisplayed()

        salvarCaptura("status-captura-parada.png")
    }

    @Test
    fun aberturaDaInicioRenderizaEmAteUmSegundoEMeio() {
        semearInstrumentoECategoria()
        val categoria = runBlocking { captura.categoriaRepository().observeCategorias().first().single() }
        val instrumento = runBlocking { captura.instrumentoRepository().observeInstrumentos().first().single() }
        runBlocking {
            val agora = Instant.now()
            repeat(20) { indice ->
                captura.lancamentoRepository().salvar(
                    Lancamento(
                        publicId = "abertura-$indice",
                        instrumentoPublicId = instrumento.publicId,
                        categoriaPublicId = categoria.publicId,
                        valorCentavos = 1000L + indice,
                        dataHora = agora.minusSeconds(indice * 3600L),
                        tipo = TipoLancamento.DESPESA,
                        estabelecimento = "Loja $indice",
                        status = StatusLancamento.CONFIRMADO,
                        origem = OrigemLancamento.MANUAL,
                        instituicaoOrigem = InstituicaoFinanceira.ITAU,
                        confianca = 0.9,
                        criadoEm = agora,
                        atualizadoEm = agora,
                    ),
                )
            }
        }

        val inicio = System.nanoTime()
        criarViewModels()
        composeRule.setContent {
            val state by homeViewModel.uiState.collectAsStateWithLifecycle()
            PingoTheme {
                HomeContent(
                    state = state,
                    onOpenSettings = {},
                    onOpenCaptura = {},
                    onOpenRevisao = {},
                    onOpenNovo = {},
                    onMesAnterior = homeViewModel::mesAnterior,
                    onMesProximo = homeViewModel::mesProximo,
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = TEMPO_ABERTURA_MS) {
            !homeViewModel.uiState.value.carregando && homeViewModel.uiState.value.ultimos.size == 5
        }
        composeRule.onNodeWithTag("saldo-do-mes").assertIsDisplayed()
        val decorrido = Duration.ofNanos(System.nanoTime() - inicio)

        assertTrue("abertura da Início levou ${decorrido.toMillis()}ms", decorrido <= Duration.ofMillis(TEMPO_ABERTURA_MS))
    }

    private fun criarViewModels() {
        transactionsViewModel = TransactionsViewModel(
            lancamentoRepository = captura.lancamentoRepository(),
            contaRepository = captura.contaRepository(),
            instrumentoRepository = captura.instrumentoRepository(),
            categoriaRepository = captura.categoriaRepository(),
            clock = clock,
        )
        homeViewModel = HomeViewModel(
            acessoNotificacoes = acesso,
            saudeDaCaptura = MonitorDaCaptura(dataSource, clock),
            lancamentoRepository = captura.lancamentoRepository(),
            contaRepository = captura.contaRepository(),
            instrumentoRepository = captura.instrumentoRepository(),
            categoriaRepository = captura.categoriaRepository(),
            clock = clock,
        )
    }

    private fun mostrarTelas() {
        composeRule.setContent {
            PingoTheme {
                when (tela.value) {
                    Tela.LANCAMENTOS -> {
                        val state by transactionsViewModel.uiState.collectAsStateWithLifecycle()
                        TransactionsContent(
                            state = state,
                            onEvent = transactionsViewModel::onEvent,
                            onOpenDetalhe = { publicId -> detalheAberto = publicId },
                            onOpenNovo = {},
                            onSelecionarAba = {},
                        )
                    }

                    Tela.INICIO -> {
                        val state by homeViewModel.uiState.collectAsStateWithLifecycle()
                        HomeContent(
                            state = state,
                            onOpenSettings = {},
                            onOpenCaptura = {},
                            onOpenRevisao = {},
                            onOpenNovo = {},
                            onMesAnterior = homeViewModel::mesAnterior,
                            onMesProximo = homeViewModel::mesProximo,
                        )
                    }
                }
            }
        }
    }

    private fun trocarParaInicio() {
        composeRule.runOnIdle { tela.value = Tela.INICIO }
    }

    private fun semearInstrumentoECategoria() = runBlocking {
        val agora = Instant.now()
        captura.contaRepository().criarComInstrumentos(
            listOf(
                NovaConta(
                    instituicao = InstituicaoFinanceira.ITAU,
                    apelido = "Itaú",
                    tipo = TipoConta.CORRENTE,
                    instrumentos = listOf(
                        NovoInstrumento(
                            tipo = TipoInstrumento.CREDITO,
                            apelido = "Cartão Itaú",
                            ultimosDigitos = ULTIMOS_DIGITOS,
                            fechamentoDia = 5,
                            vencimentoDia = 12,
                        ),
                    ),
                ),
            ),
            agora,
        )
        val categoria = captura.categoriaRepository().criar(CATEGORIA_PADRAO, TipoCategoria.DESPESA, agora)
        assertTrue(categoria is ResultadoCategoria.Sucesso)
    }

    private fun aguardarEstado(estado: EstadoListener, motivo: MotivoParada? = null) = runBlocking {
        withTimeout(TEMPO_ESPERA) {
            dataSource.estadoListener.first { atual -> atual.estado == estado && atual.motivo == motivo }
        }
    }

    private fun aguardarAcessoRevogado() {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            if (!acesso.concedido()) {
                return
            }
            Thread.sleep(INTERVALO_CONSULTA)
        }
        throw AssertionError("acesso às notificações continuou concedido após a revogação")
    }

    private fun aguardarContadoresEstaveis() {
        val mes = YearMonth.from(Instant.now().atZone(ZoneId.systemDefault()))
        var anterior = contadores(mes)
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            Thread.sleep(INTERVALO_ESTABILIZACAO)
            val atual = contadores(mes)
            if (atual == anterior) {
                return
            }
            anterior = atual
        }
        throw AssertionError("contadores do mês $mes não estabilizaram")
    }

    private fun contadores(mes: YearMonth) = runBlocking { dataSource.contadoresDoMes(mes).first() }

    private fun limparBancoEEstado() {
        captura.database().clearAllTables()
        runBlocking { dataSource.clear() }
    }

    private fun gerenciador() = contexto.getSystemService(android.app.NotificationManager::class.java)

    private fun gerenciadorDeRevisao() = gerenciador().activeNotifications
        .filter { it.notification.channelId == NotificacoesDeRevisao.CANAL }
        .filter { it.notification.extras.getString(NotificacoesDeRevisao.EXTRA_PUBLIC_ID) != null }

    private fun postar(titulo: String, tag: String, texto: String) {
        val script = File(contexto.getExternalFilesDir(null), "sonda_de_inicio.sh")
        script.writeText("cmd notification post -t \"$titulo\" $tag \"$texto\"\n")
        shell("sh ${script.absolutePath}")
    }

    private fun valorUnico(): Long = 10_000L + Math.floorMod(System.nanoTime(), 900_000L)

    private fun textoCompra(valorCentavos: Long): String {
        val reais = valorCentavos / 100
        val centavos = (valorCentavos % 100).toString().padStart(2, '0')
        return "Ola! Compra aprovada de R$ $reais,$centavos em $TEXTO_ESTABELECIMENTO_ITAU " +
            "no dia 10/11 às 00:55 no cartão final $ULTIMOS_DIGITOS. Boas compras."
    }

    private fun textoCompraNubank(valorCentavos: Long): String {
        val reais = valorCentavos / 100
        val centavos = (valorCentavos % 100).toString().padStart(2, '0')
        return "Compra de R$ $reais,$centavos APROVADA em DL*99 99 para o cartão com final 5678."
    }

    private fun salvarCaptura(nome: String) {
        shell("screencap -p /sdcard/Download/$nome")
        val saida = shell("ls -l /sdcard/Download/$nome")
        assertTrue("screenshot não gerado: $nome ($saida)", saida.contains(nome))
    }

    private fun shell(comando: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(comando)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream -> stream.readBytes().decodeToString() }
    }

    private enum class Tela { LANCAMENTOS, INICIO }

    private enum class TelaDaCaptura { INICIO, STATUS }

    private object SemAtualizacao : AtualizacaoDeRegras {
        override suspend fun atualizar(): ResultadoAtualizacaoRegras = ResultadoAtualizacaoRegras.SemAlteracao(1)
    }

    private companion object {
        const val PACOTE = "com.pingo"
        const val COMPONENTE = "$PACOTE/com.pingo.core.notifications.PingoNotificationListenerService"
        const val ULTIMOS_DIGITOS = "1234"
        const val TEXTO_ESTABELECIMENTO_ITAU = "CLARO *31987905690"
        const val CATEGORIA_PADRAO = "Outros"
        const val TEMPO_ESPERA = 20_000L
        const val INTERVALO_CONSULTA = 100L
        const val INTERVALO_ESTABILIZACAO = 2_000L
        const val TEMPO_ABERTURA_MS = 1_500L
    }
}
