package com.pingo.status

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.ParcelFileDescriptor
import android.service.notification.StatusBarNotification
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.data.RegraParserRepository
import com.pingo.core.data.di.CapturaEntryPoint
import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.ContadoresDoMes
import com.pingo.core.datastore.EstadoListener
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.Conta
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.model.Instrumento
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoCategoria
import com.pingo.core.model.TipoConta
import com.pingo.core.model.TipoInstrumento
import com.pingo.core.notifications.AcessoNotificacoes
import com.pingo.core.notifications.BateriaDoAparelho
import com.pingo.core.notifications.CapturaController
import com.pingo.core.notifications.GatilhoDeCapturaParada
import com.pingo.core.notifications.NotificacoesDeCaptura
import com.pingo.core.notifications.NotificacoesDoSistema
import com.pingo.core.notifications.ResultadoAtualizacaoRegras
import com.pingo.core.notifications.SaudeDaCaptura
import com.pingo.core.notifications.SituacaoDaCaptura
import com.pingo.core.notifications.di.CapturaStatusEntryPoint
import com.pingo.feature.capturestatus.impl.AtualizacaoDeRegras
import com.pingo.feature.capturestatus.impl.CaptureStatusScreen
import com.pingo.feature.capturestatus.impl.CaptureStatusViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
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
import java.time.Duration
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class StatusDaCapturaTest {
    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val injecao: CapturaStatusEntryPoint =
        EntryPointAccessors.fromApplication(contexto, CapturaStatusEntryPoint::class.java)
    private val captura: CapturaEntryPoint = EntryPointAccessors.fromApplication(contexto, CapturaEntryPoint::class.java)
    private val dataSource: CapturaDataSource = injecao.capturaDataSource()
    private val controller: CapturaController = injecao.capturaController()
    private val gatilho: GatilhoDeCapturaParada = injecao.gatilhoDeCapturaParada()
    private val gerenciadorNotificacoes: NotificationManager = contexto.getSystemService(NotificationManager::class.java)

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun preparaOListener() {
        shell("pm grant $PACOTE android.permission.POST_NOTIFICATIONS")
        shell("cmd notification allow_listener $COMPONENTE")
        aguardarAcessoConcedido()
        conectarOListener()
        aguardarNotificacaoProcessada()
    }

    @After
    fun limpaOEstadoDaCaptura() {
        gerenciadorNotificacoes.cancel(NotificacoesDeCaptura.ID_ALERTA_PARADA)
        runBlocking {
            dataSource.clear()
            controller.reconectar()
        }
    }

    @Test
    fun t10MostraContadoresEstaveisComNotificacoesSinteticasEPendenteSemInstrumento() {
        limparCaptura()
        val mes = mesAtual()
        val antes = contadores(mes)
        assertTrue(
            "o teste precisa começar sem instrumentos do Itaú",
            runBlocking { captura.instrumentoRepository().ativosDaInstituicao(InstituicaoFinanceira.ITAU) }.isEmpty(),
        )

        val valorPendente = valorUnico()
        val inicioPendente = Instant.now()
        postar("Sua compra foi aprovada", "pingo-teste-ITAU-$valorPendente", textoCompra(valorPendente))

        val comPendente = aguardarContadores(mes) { it.pendentes == antes.pendentes + 1 }
        assertEquals(antes.reconhecidas, comPendente.reconhecidas)
        val pendente = runBlocking {
            withTimeout(TEMPO_ESPERA) {
                captura.lancamentoRepository().observeLancamentos()
                    .first { lista -> lista.any { it.valorCentavos == valorPendente } }
                    .first { it.valorCentavos == valorPendente }
            }
        }
        assertNull("captura sem instrumento deveria ficar pendente", pendente.instrumentoPublicId)
        assertEquals(StatusLancamento.PENDENTE, pendente.status)
        assertEquals(InstituicaoFinanceira.ITAU, pendente.instituicaoOrigem)
        aguardarLeituraApos(InstituicaoFinanceira.ITAU, inicioPendente)
        assertEquals(EstadoListener.ATIVO, estadoAtual().estado)

        postar("Seu limite", "pingo-teste-ITAU", TEXTO_RUIDO_ITAU)
        val comRuido = aguardarContadores(mes) { it.ignoradas == comPendente.ignoradas + 1 }

        postar("Aviso", "pingo-teste-ITAU", TEXTO_DESCONHECIDO)
        val comDesconhecida = aguardarContadores(mes) { it.naoEntendidas == comRuido.naoEntendidas + 1 }

        semearInstrumentoECategoria()
        val valorReconhecido = valorUnico()
        postar("Sua compra foi aprovada", "pingo-teste-ITAU-$valorReconhecido", textoCompra(valorReconhecido))
        val depois = aguardarContadores(mes) { it.reconhecidas == comDesconhecida.reconhecidas + 1 }
        assertEquals(comDesconhecida.pendentes, depois.pendentes)
        assertEquals(comDesconhecida.ignoradas, depois.ignoradas)
        assertEquals(comDesconhecida.naoEntendidas, depois.naoEntendidas)
        assertEquals(EstadoListener.ATIVO, estadoAtual().estado)

        composeRule.setContent {
            PingoTheme {
                CaptureStatusScreen(onBack = {}, viewModel = modeloDaTela())
            }
        }

        aguardarTexto("Status da captura")
        aguardarTexto("Captura ativa")
        composeRule.onAllNodesWithText("Captura parada").assertCountEquals(0)

        listOf("Neste mês", "Reconhecidas", "Para revisão", "Ignoradas", "Não entendidas").forEach { rotulo ->
            aguardarTexto(rotulo)
        }
        listOf(depois.reconhecidas, depois.pendentes, depois.ignoradas, depois.naoEntendidas).forEach { valor ->
            assertTrue(
                "o contador $valor não apareceu na T-10",
                composeRule.onAllNodesWithText(valor.toString()).fetchSemanticsNodes().isNotEmpty(),
            )
        }

        composeRule.onNodeWithText("Última notificação por app").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Itaú").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Acesso a notificações").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Ativo — lendo notificações dos bancos").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Atualizar regras").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Reconectar").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun reconectarPelaTelaRestauraACapturaAtiva() {
        runBlocking { dataSource.registrarParado(MotivoParada.DESCONECTADO, Instant.now()) }

        composeRule.setContent {
            PingoTheme {
                CaptureStatusScreen(onBack = {}, viewModel = modeloDaTela())
            }
        }

        aguardarTexto("Captura parada")
        composeRule.onNodeWithText("O Android encerrou o serviço").assertIsDisplayed()

        composeRule.onNodeWithText("Reconectar").performScrollTo().performClick()

        aguardarTexto("Captura reconectada.")
        aguardarTexto("Captura ativa")
        val estado = estadoAtual()
        assertEquals(EstadoListener.ATIVO, estado.estado)
        assertNull(estado.motivo)
    }

    @Test
    fun alertaDe48hPostaNotificacaoDeCapturaParada() {
        val agora = Instant.now()
        prepararSilencioDe48h(agora)

        try {
            val diagnostico = runBlocking { gatilho.verificar() }

            assertEquals(SituacaoDaCaptura.SEM_LEITURA, diagnostico.situacao)
            assertEquals(agora.minus(Duration.ofHours(49)), diagnostico.semLeituraDesde)

            val alerta = runBlocking { dataSource.alertaDeCaptura.first() }
            assertNotNull("o alerta de captura parada não foi registrado", alerta)
            assertEquals(agora.minus(Duration.ofHours(49)), alerta?.referencia)

            val notificacao = aguardarNotificacao(NotificacoesDeCaptura.ID_ALERTA_PARADA)
            assertEquals(
                "A captura parou",
                notificacao.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            )

            runBlocking { gatilho.verificar() }

            assertEquals(
                "o alerta de 48h não deveria repetir na mesma janela de silêncio",
                alerta?.enviadoEm,
                runBlocking { dataSource.alertaDeCaptura.first() }?.enviadoEm,
            )

            val modelo = modeloDaTela()

            composeRule.setContent {
                PingoTheme {
                    CaptureStatusScreen(onBack = {}, viewModel = modelo)
                }
            }

            try {
                aguardarTexto("Captura parada")
            } catch (erro: Throwable) {
                throw AssertionError("a T-10 não sinalizou captura parada: ${modelo.uiState.value}", erro)
            }

            composeRule.onNodeWithText("Sem leitura há 2 dias").assertIsDisplayed()
        } finally {
            gerenciadorNotificacoes.cancel(NotificacoesDeCaptura.ID_ALERTA_PARADA)
            runBlocking { controller.reconectar() }
        }
    }

    private fun prepararSilencioDe48h(agora: Instant) {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            runBlocking {
                dataSource.clear()
                dataSource.registrarAtivo(agora.minus(Duration.ofDays(10)))
                dataSource.registrarLeitura(InstituicaoFinanceira.ITAU, agora.minus(Duration.ofHours(49)))
            }
            Thread.sleep(INTERVALO_SONDAGEM)
            val leituras = runBlocking { dataSource.ultimaLeituraPorApp.first() }
            if (leituras.keys == setOf(InstituicaoFinanceira.ITAU)) {
                return
            }
        }
        throw AssertionError("a captura recebeu leituras durante a preparação do silêncio de 48h")
    }

    private fun aguardarAcessoConcedido() {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            if (injecao.acessoNotificacoes().concedido()) {
                return
            }
            Thread.sleep(INTERVALO_CONSULTA)
        }
        throw AssertionError("acesso às notificações não foi concedido")
    }

    private fun conectarOListener() {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            runBlocking { controller.reconectar() }
            val ativo = runBlocking {
                withTimeoutOrNull(INTERVALO_SONDAGEM) { dataSource.estadoListener.first { it.estado == EstadoListener.ATIVO } }
            }
            if (ativo != null) {
                return
            }
            Thread.sleep(INTERVALO_CONSULTA)
        }
        throw AssertionError("listener não ficou ativo: ${estadoAtual()}")
    }

    private fun limparCaptura() {
        captura.database().clearAllTables()
        runBlocking { dataSource.clear() }
        conectarOListener()
        aguardarNotificacaoProcessada()
        aguardarContadoresEstaveis()
    }

    private fun modeloDaTela(
        acessoNotificacoes: AcessoNotificacoes = injecao.acessoNotificacoes(),
        notificacoesDoSistema: NotificacoesDoSistema = injecao.notificacoesDoSistema(),
        bateriaDoAparelho: BateriaDoAparelho = injecao.bateriaDoAparelho(),
        atualizacaoDeRegras: AtualizacaoDeRegras = AtualizacaoSemAlteracoes,
        regraParserRepository: RegraParserRepository = injecao.regraParserRepository(),
        saudeDaCaptura: SaudeDaCaptura = injecao.saudeDaCaptura(),
    ) = CaptureStatusViewModel(
        capturaDataSource = dataSource,
        saudeDaCaptura = saudeDaCaptura,
        regraParserRepository = regraParserRepository,
        atualizacaoDeRegras = atualizacaoDeRegras,
        capturaController = controller,
        acessoNotificacoes = acessoNotificacoes,
        notificacoesDoSistema = notificacoesDoSistema,
        bateriaDoAparelho = bateriaDoAparelho,
        clock = Clock.systemDefaultZone(),
    )

    private fun semearInstrumentoECategoria() = runBlocking {
        val agora = Instant.now()
        captura.contaRepository().salvar(
            Conta(
                publicId = CONTA_PUBLIC_ID,
                instituicao = InstituicaoFinanceira.ITAU,
                apelido = "Itaú",
                tipo = TipoConta.CORRENTE,
                criadoEm = agora,
                atualizadoEm = agora,
            ),
        )
        captura.instrumentoRepository().salvar(
            Instrumento(
                publicId = INSTRUMENTO_PUBLIC_ID,
                contaPublicId = CONTA_PUBLIC_ID,
                tipo = TipoInstrumento.CREDITO,
                apelido = "Cartão Itaú",
                ultimosDigitos = ULTIMOS_DIGITOS,
                criadoEm = agora,
                atualizadoEm = agora,
            ),
        )
        captura.categoriaRepository().criar(NOME_CATEGORIA_PADRAO, TipoCategoria.DESPESA, agora)
    }

    private fun valorUnico(): Long = 1000L + Math.floorMod(System.nanoTime(), 90000L)

    private fun textoCompra(valorCentavos: Long): String {
        val reais = valorCentavos / 100
        val centavos = (valorCentavos % 100).toString().padStart(2, '0')
        return "Ola! Compra aprovada de R$ $reais,$centavos em CLARO *31987905690 " +
            "no dia 10/11 às 00:55 no cartão final $ULTIMOS_DIGITOS. Boas compras."
    }

    private fun postar(titulo: String, tag: String, texto: String) {
        val script = File(contexto.getExternalFilesDir(null), "sonda_de_status.sh")
        script.writeText("cmd notification post -t \"$titulo\" $tag \"$texto\"\n")
        shell("sh ${script.absolutePath}")
    }

    private fun aguardarNotificacaoProcessada() {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            val inicio = Instant.now()
            val sonda = "pingo-teste-CAJU-${System.nanoTime()}"
            postar("Sonda do Pingo", sonda, TEXTO_SONDA_CAJU)

            val processou = runBlocking {
                withTimeoutOrNull(INTERVALO_SONDAGEM) {
                    dataSource.ultimaLeituraPorApp.first { leituras ->
                        leituras[InstituicaoFinanceira.CAJU]?.isAfter(inicio) == true
                    }
                }
            }

            if (processou != null) {
                return
            }
        }
        throw AssertionError("listener não processou a notificação de sondagem")
    }

    private fun contadores(mes: YearMonth): ContadoresDoMes = runBlocking { dataSource.contadoresDoMes(mes).first() }

    private fun aguardarContadores(mes: YearMonth, condicao: (ContadoresDoMes) -> Boolean): ContadoresDoMes {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            val atuais = contadores(mes)
            if (condicao(atuais)) {
                return atuais
            }
            Thread.sleep(INTERVALO_CONSULTA)
        }
        throw AssertionError("contadores do mês $mes não evoluíram como esperado: ${contadores(mes)}")
    }

    private fun aguardarContadoresEstaveis() {
        val mes = mesAtual()
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
        throw AssertionError("contadores do mês $mes não estabilizaram: ${contadores(mes)}")
    }

    private fun aguardarLeituraApos(instituicao: InstituicaoFinanceira, inicio: Instant) = runBlocking {
        withTimeout(TEMPO_ESPERA) {
            dataSource.ultimaLeituraPorApp.first { leituras ->
                leituras[instituicao]?.isAfter(inicio.minusSeconds(2)) == true
            }
        }
    }

    private fun estadoAtual() = runBlocking { dataSource.estadoListener.first() }

    private fun aguardarTexto(texto: String) {
        composeRule.waitUntil(TEMPO_ESPERA) {
            composeRule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun aguardarNotificacao(id: Int): StatusBarNotification {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            gerenciadorNotificacoes.activeNotifications.firstOrNull { it.id == id }?.let { return it }
            Thread.sleep(INTERVALO_CONSULTA)
        }
        throw AssertionError("notificação $id não apareceu")
    }

    private fun mesAtual(): YearMonth = YearMonth.from(Instant.now().atZone(ZoneId.systemDefault()))

    private fun shell(comando: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(comando)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
            stream.readBytes().decodeToString()
        }
    }

    private object AcessoNegado : AcessoNotificacoes {
        override fun concedido(): Boolean = false
    }

    private object AtualizacaoSemAlteracoes : AtualizacaoDeRegras {
        override suspend fun atualizar(): ResultadoAtualizacaoRegras = ResultadoAtualizacaoRegras.SemAlteracao(1)
    }

    private companion object {
        const val PACOTE = "com.pingo"
        const val COMPONENTE = "$PACOTE/com.pingo.core.notifications.PingoNotificationListenerService"
        const val CONTA_PUBLIC_ID = "captura-conta-itau"
        const val INSTRUMENTO_PUBLIC_ID = "captura-instrumento-itau"
        const val ULTIMOS_DIGITOS = "1234"
        const val NOME_CATEGORIA_PADRAO = "Outros"
        const val TEMPO_ESPERA = 20_000L
        const val INTERVALO_CONSULTA = 100L
        const val INTERVALO_ESTABILIZACAO = 2_000L
        const val INTERVALO_SONDAGEM = 2_000L
        const val TEXTO_SONDA_CAJU = "Wellhub"
        const val TEXTO_RUIDO_ITAU = "Mais controle para gerenciar seu limite"
        const val TEXTO_DESCONHECIDO = "Seu extrato esta disponivel no aplicativo"
    }
}
