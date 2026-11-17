package com.pingo.inicio

import android.app.Instrumentation
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.MainActivity
import com.pingo.core.data.ResultadoCategoria
import com.pingo.core.data.di.CapturaEntryPoint
import com.pingo.core.data.di.DetalheEntryPoint
import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.CapturaDataSourceImpl
import com.pingo.core.datastore.EstadoListener
import com.pingo.core.datastore.PermissaoDoPingo
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
import com.pingo.core.notifications.AcessoNotificacoesDoSistema
import com.pingo.core.notifications.CapturaController
import com.pingo.core.notifications.MonitorDaCaptura
import com.pingo.core.notifications.NotificacoesDeRevisao
import com.pingo.core.notifications.di.CapturaStatusEntryPoint
import com.pingo.core.notifications.di.RevisaoNotificacoesEntryPoint
import com.pingo.feature.home.impl.HomeContent
import com.pingo.feature.home.impl.HomeViewModel
import com.pingo.feature.transactions.api.TransactionsRoute
import com.pingo.feature.transactions.impl.DetalheDoLancamentoScreen
import com.pingo.feature.transactions.impl.DetalheDoLancamentoViewModel
import com.pingo.support.prepararListenerDeCaptura
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class RevisaoNaInicioTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val captura: CapturaEntryPoint = EntryPointAccessors.fromApplication(contexto, CapturaEntryPoint::class.java)
    private val detalhe: DetalheEntryPoint = EntryPointAccessors.fromApplication(contexto, DetalheEntryPoint::class.java)
    private val capturaStatus: CapturaStatusEntryPoint =
        EntryPointAccessors.fromApplication(contexto, CapturaStatusEntryPoint::class.java)
    private val revisao: RevisaoNotificacoesEntryPoint =
        EntryPointAccessors.fromApplication(contexto, RevisaoNotificacoesEntryPoint::class.java)
    private val dataSource: CapturaDataSource = CapturaDataSourceImpl(contexto)
    private val clock: Clock = Clock.systemDefaultZone()

    private lateinit var homeViewModel: HomeViewModel
    private var intencaoDaCorrecao by mutableStateOf<Intent?>(null)
    private var detalheViewModel: DetalheDoLancamentoViewModel? = null

    @Before
    fun preparaAmbiente() {
        captura.database().clearAllTables()
        runBlocking { dataSource.clear() }
        shell("pm grant $PACOTE android.permission.POST_NOTIFICATIONS")
        runBlocking { revisao.permissoesDataSource().registrarNotificacoesDoPingo(PermissaoDoPingo.PERMITIDA) }
        prepararListenerDeCaptura(::shell)
        runBlocking {
            controller().reconectar()
        }
        aguardarListenerAtivo()
        aguardarContadoresEstaveis()
    }

    @After
    fun limpaAmbiente() {
        notificacoesDeRevisao().forEach { gerenciador().cancel(it.tag, it.id) }
        shell("cmd notification allow_listener $COMPONENTE")
        captura.database().clearAllTables()
        runBlocking { dataSource.clear() }
        runBlocking { controller().reconectar() }
    }

    @Test
    fun confirmarPelaAcaoDaNotificacaoAtualizaAInicioEcorrigirAbreODetalheCorreto() {
        semearInstrumento()
        criarInicio()

        val valorConfirmado = valorUnico()
        postar("Compra aprovada", "pingo-teste-ITAU", textoCompra(valorConfirmado))
        val primeiro = aguardarPendente(valorConfirmado)
        val notificacao = aguardarNotificacao(primeiro.publicId)

        composeRule.waitUntil(TEMPO_ESPERA) { homeViewModel.uiState.value.pendentes == 1 }
        composeRule.onNodeWithTag("aviso-revisao").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1 lançamento para revisar").assertIsDisplayed()

        val monitor = InstrumentationRegistry.getInstrumentation()
            .addMonitor(MainActivity::class.java.name, null, false)
        acao(notificacao, NotificacoesDeRevisao.ACAO_CONFIRMAR_LABEL).actionIntent.send()

        aguardarStatus(primeiro.publicId, StatusLancamento.CONFIRMADO)
        composeRule.waitUntil(TEMPO_ESPERA) { homeViewModel.uiState.value.pendentes == 0 }
        assertTrue(notificacoesDeRevisao().none { it.id == NotificacoesDeRevisao.idDe(primeiro.publicId) })
        composeRule.onNodeWithTag("aviso-revisao").assertDoesNotExist()
        composeRule.onNodeWithTag("saldo-do-mes").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().removeMonitor(monitor)

        val valorPendente = valorUnico()
        postar("Compra aprovada", "pingo-teste-ITAU", textoCompra(valorPendente))
        val segundo = aguardarPendente(valorPendente)
        composeRule.waitUntil(TEMPO_ESPERA) { homeViewModel.uiState.value.pendentes == 1 }

        val intento = abrirCorrecaoPelaAcaoReal(segundo.publicId)

        composeRule.runOnIdle { intencaoDaCorrecao = intento }
        composeRule.waitUntil(TEMPO_ESPERA) {
            composeRule.onAllNodesWithTag("detalhe-${segundo.publicId}").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitUntil(TEMPO_ESPERA) { detalheViewModel?.uiState?.value?.origem != null }
        composeRule.onNodeWithTag("origem-instituicao").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Itaú").assertIsDisplayed()
        composeRule.onNodeWithTag("origem-texto").performScrollTo().assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText(TEXTO_ESTABELECIMENTO_ITAU, substring = true).fetchSemanticsNodes().isNotEmpty(),
        )
    }

    @Test
    fun corrigirPendenteSemInstrumentoCompletaARevisaoNaInicio() {
        semearCategoria()
        criarInicio()

        val valor = valorUnico()
        postar("Compra aprovada", "pingo-teste-NUBANK", textoCompraNubank(valor))
        val pendente = aguardarPendente(valor, InstituicaoFinanceira.NUBANK)
        assertNull(pendente.instrumentoPublicId)
        semearInstrumento()

        val notificacao = aguardarNotificacao(pendente.publicId)
        acao(notificacao, NotificacoesDeRevisao.ACAO_CONFIRMAR_LABEL).actionIntent.send()

        val reposta = aguardarTextoDaNotificacao(pendente.publicId, NotificacoesDeRevisao.AVISO_CORRIGIR)
        assertEquals(
            listOf(NotificacoesDeRevisao.ACAO_CONFIRMAR_LABEL, NotificacoesDeRevisao.ACAO_CORRIGIR_LABEL),
            reposta.notification.actions.map { it.title.toString() },
        )
        aguardarStatus(pendente.publicId, StatusLancamento.PENDENTE)
        composeRule.waitUntil(TEMPO_ESPERA) { homeViewModel.uiState.value.pendentes == 1 }

        val intento = abrirCorrecaoPelaAcaoReal(pendente.publicId)

        composeRule.runOnIdle { intencaoDaCorrecao = intento }
        composeRule.waitUntil(TEMPO_ESPERA) {
            composeRule.onAllNodesWithTag("detalhe-${pendente.publicId}").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitUntil(TEMPO_ESPERA) { detalheViewModel?.uiState?.value?.origem != null }
        composeRule.onNodeWithTag("origem-instituicao").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Nubank").assertIsDisplayed()
        composeRule.onNodeWithTag("origem-texto").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("APROVADA em DL*99 99", substring = true).assertIsDisplayed()

        val instrumento = runBlocking { captura.instrumentoRepository().observeInstrumentos().first().single() }
        composeRule.onNodeWithTag("campo-instrumento-detalhe").performScrollTo().performClick()
        composeRule.onNodeWithTag("instrumento-detalhe-${instrumento.publicId}").performClick()
        composeRule.onNodeWithTag("salvar-detalhe").performScrollTo().performClick()

        aguardarStatus(pendente.publicId, StatusLancamento.CONFIRMADO)
        val confirmado = runBlocking { captura.lancamentoRepository().buscar(pendente.publicId) }
        assertEquals(instrumento.publicId, confirmado?.instrumentoPublicId)
        assertEquals(InstituicaoFinanceira.NUBANK, confirmado?.instituicaoOrigem)
        composeRule.waitUntil(TEMPO_ESPERA) { homeViewModel.uiState.value.pendentes == 0 }
    }

    private fun criarInicio() {
        homeViewModel = HomeViewModel(
            acessoNotificacoes = AcessoNotificacoesDoSistema(contexto),
            saudeDaCaptura = MonitorDaCaptura(dataSource, clock),
            lancamentoRepository = captura.lancamentoRepository(),
            contaRepository = captura.contaRepository(),
            instrumentoRepository = captura.instrumentoRepository(),
            categoriaRepository = captura.categoriaRepository(),
            clock = clock,
        )

        composeRule.setContent {
            PingoTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = ROTA_INICIO) {
                    composable(ROTA_INICIO) {
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
                    composable(
                        route = TransactionsRoute.DETALHE_ROUTE,
                        arguments = listOf(navArgument(TransactionsRoute.ARG_PUBLIC_ID) { type = NavType.StringType }),
                        deepLinks = listOf(navDeepLink { uriPattern = NotificacoesDeRevisao.PADRAO_DEEP_LINK_DETALHE }),
                    ) { entrada ->
                        val publicId = entrada.arguments?.getString(TransactionsRoute.ARG_PUBLIC_ID).orEmpty()
                        val viewModel = detalheViewModel(publicId)
                        detalheViewModel = viewModel
                        DetalheDoLancamentoScreen(
                            publicId = publicId,
                            onBack = { navController.popBackStack() },
                            viewModel = viewModel,
                        )
                    }
                }

                val intencao = intencaoDaCorrecao
                if (intencao != null) {
                    androidx.compose.runtime.LaunchedEffect(intencao) {
                        val publicId = intencao.getStringExtra(NotificacoesDeRevisao.EXTRA_PUBLIC_ID)
                        if (publicId != null) {
                            navController.navigate(TransactionsRoute.detalhe(publicId))
                        } else {
                            navController.handleDeepLink(intencao)
                        }
                    }
                }
            }
        }
    }

    private fun controller(): CapturaController = capturaStatus.capturaController()

    private fun detalheViewModel(publicId: String): DetalheDoLancamentoViewModel = DetalheDoLancamentoViewModel(
        savedStateHandle = SavedStateHandle(mapOf(TransactionsRoute.ARG_PUBLIC_ID to publicId)),
        lancamentoRepository = captura.lancamentoRepository(),
        contaRepository = captura.contaRepository(),
        instrumentoRepository = captura.instrumentoRepository(),
        categoriaRepository = captura.categoriaRepository(),
        editarLancamento = detalhe.editarLancamento(),
        marcarTransferencia = detalhe.marcarTransferencia(),
        clock = clock,
    )

    private fun abrirCorrecaoPelaAcaoReal(publicId: String): Intent {
        val notificacao = aguardarNotificacao(publicId)
        val existencia = PendingIntent.getActivity(
            contexto,
            NotificacoesDeRevisao.idDe(publicId),
            NotificacoesDeRevisao.intencaoDeCorrecao(contexto, publicId),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        assertNotNull("a ação Corrigir não existe como PendingIntent de correção do app", existencia)

        val instrumentacao = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentacao.addMonitor(MainActivity::class.java.name, null, false)
        acao(notificacao, NotificacoesDeRevisao.ACAO_CORRIGIR_LABEL).actionIntent.send()

        val aberta = monitor.waitForActivityWithTimeout(TEMPO_ESPERA)
        instrumentacao.removeMonitor(monitor)
        assertNotNull("a ação Corrigir não abriu o app", aberta)
        val intento = aberta!!.intent
        assertEquals(NotificacoesDeRevisao.detalheUri(publicId), intento.data)
        assertEquals(publicId, intento.getStringExtra(NotificacoesDeRevisao.EXTRA_PUBLIC_ID))
        InstrumentationRegistry.getInstrumentation().runOnMainSync { aberta.finish() }
        composeRule.waitForIdle()
        return intento
    }

    private fun semearInstrumento() = runBlocking {
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
    }

    private fun semearCategoria() = runBlocking {
        val categoria = captura.categoriaRepository().criar(CATEGORIA_PADRAO, TipoCategoria.DESPESA, Instant.now())
        assertTrue(categoria is ResultadoCategoria.Sucesso)
    }

    private fun postar(titulo: String, tag: String, texto: String) {
        val script = File(contexto.getExternalFilesDir(null), "sonda_de_revisao.sh")
        script.writeText("cmd notification post -t \"$titulo\" $tag \"$texto\"\n")
        shell("sh ${script.absolutePath}")
    }

    private fun aguardarPendente(valorCentavos: Long, instituicao: InstituicaoFinanceira = InstituicaoFinanceira.ITAU): Lancamento =
        runBlocking {
            withTimeout(TEMPO_ESPERA) {
                captura.lancamentoRepository().observeLancamentos().first { lista ->
                    lista.any {
                        it.valorCentavos == valorCentavos &&
                            it.status == StatusLancamento.PENDENTE &&
                            it.origem == OrigemLancamento.NOTIFICACAO &&
                            it.instituicaoOrigem == instituicao
                    }
                }
            }.first { it.valorCentavos == valorCentavos }
        }

    private fun aguardarStatus(publicId: String, status: StatusLancamento) = runBlocking {
        withTimeout(TEMPO_ESPERA) {
            captura.lancamentoRepository().observeLancamentos().first { lista ->
                lista.firstOrNull { it.publicId == publicId }?.status == status
            }
        }
    }

    private fun aguardarListenerAtivo() = runBlocking {
        withTimeout(TEMPO_ESPERA) {
            dataSource.estadoListener.first { it.estado == EstadoListener.ATIVO }
        }
    }

    private fun aguardarContadoresEstaveis() {
        val mes = YearMonth.from(Instant.now().atZone(ZoneId.systemDefault()))
        var anterior = runBlocking { dataSource.contadoresDoMes(mes).first() }
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            Thread.sleep(INTERVALO_ESTABILIZACAO)
            val atual = runBlocking { dataSource.contadoresDoMes(mes).first() }
            if (atual == anterior) {
                return
            }
            anterior = atual
        }
        throw AssertionError("contadores do mês $mes não estabilizaram")
    }

    private fun gerenciador() = contexto.getSystemService(android.app.NotificationManager::class.java)

    private fun notificacoesDeRevisao() = gerenciador().activeNotifications
        .filter { it.notification.channelId == NotificacoesDeRevisao.CANAL }
        .filter { it.notification.extras.getString(NotificacoesDeRevisao.EXTRA_PUBLIC_ID) != null }

    private fun aguardarNotificacao(publicId: String): android.service.notification.StatusBarNotification {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            notificacoesDeRevisao().firstOrNull { it.id == NotificacoesDeRevisao.idDe(publicId) }?.let { return it }
            Thread.sleep(INTERVALO_CONSULTA)
        }
        throw AssertionError("notificação de revisão de $publicId não apareceu")
    }

    private fun aguardarTextoDaNotificacao(publicId: String, trecho: String): android.service.notification.StatusBarNotification {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            val notificacao = notificacoesDeRevisao().firstOrNull { it.id == NotificacoesDeRevisao.idDe(publicId) }
            val extras = notificacao?.notification?.extras
            val textos = listOf(android.app.Notification.EXTRA_TEXT, android.app.Notification.EXTRA_BIG_TEXT)
                .mapNotNull { extras?.getCharSequence(it)?.toString() }
            if (notificacao != null && textos.any { it.contains(trecho) }) {
                return notificacao
            }
            Thread.sleep(INTERVALO_CONSULTA)
        }
        throw AssertionError("notificação de revisão de $publicId não trouxe '$trecho'")
    }

    private fun acao(notificacao: android.service.notification.StatusBarNotification, titulo: String): android.app.Notification.Action =
        notificacao.notification.actions.first { it.title.toString() == titulo }

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

    private fun shell(comando: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(comando)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream -> stream.readBytes().decodeToString() }
    }

    private companion object {
        const val PACOTE = "com.pingo"
        const val COMPONENTE = "$PACOTE/com.pingo.core.notifications.PingoNotificationListenerService"
        const val ROTA_INICIO = "inicio"
        const val ULTIMOS_DIGITOS = "1234"
        const val TEXTO_ESTABELECIMENTO_ITAU = "CLARO *31987905690"
        const val CATEGORIA_PADRAO = "Outros"
        const val TEMPO_ESPERA = 20_000L
        const val INTERVALO_CONSULTA = 100L
        const val INTERVALO_ESTABILIZACAO = 2_000L
    }
}
