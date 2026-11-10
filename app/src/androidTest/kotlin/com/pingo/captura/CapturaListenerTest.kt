package com.pingo.captura

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.CapturaDataSourceImpl
import com.pingo.core.datastore.ContadoresDoMes
import com.pingo.core.datastore.EstadoListener
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.model.InstituicaoFinanceira
import com.pingo.core.notifications.AcessoNotificacoesDoSistema
import com.pingo.core.notifications.CapturaController
import com.pingo.core.notifications.CapturaControllerImpl
import com.pingo.core.notifications.ConexaoDoListener
import com.pingo.core.notifications.NotificacoesDeCaptura
import com.pingo.core.notifications.RebindDeListenerDoSistema
import com.pingo.core.notifications.ServicoDeCapturaDoAndroid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class CapturaListenerTest {
    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val dataSource: CapturaDataSource = CapturaDataSourceImpl(contexto)
    private val acesso = AcessoNotificacoesDoSistema(contexto)
    private val controller: CapturaController = CapturaControllerImpl(
        capturaDataSource = dataSource,
        acessoNotificacoes = acesso,
        rebindDeListener = RebindDeListenerDoSistema(contexto),
        servicoDeCaptura = ServicoDeCapturaDoAndroid(contexto),
        conexaoDoListener = ConexaoDoListener(),
        clock = Clock.systemDefaultZone(),
    )
    private val gerenciadorNotificacoes: NotificationManager = contexto.getSystemService(NotificationManager::class.java)

    @Before
    fun concedeAcessoEConectaOListener() {
        shell("pm grant $PACOTE android.permission.POST_NOTIFICATIONS")
        shell("cmd notification allow_listener $COMPONENTE")
        runBlocking { controller.reconectar() }
        aguardarEstado(EstadoListener.ATIVO)
        aguardarNotificacaoProcessada()
        aguardarContadoresEstaveis()
    }

    @Test
    fun listenerRodaEmForegroundServiceComNotificacaoPersistente() {
        val persistente = aguardarNotificacaoPersistente()

        assertTrue(persistente.notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue(persistente.notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0)
        assertEquals(Notification.CATEGORY_SERVICE, persistente.notification.category)
        assertTrue(acesso.concedido())
    }

    @Test
    fun acessoRevogadoMarcaParadoComMotivoSemCrash() {
        shell("cmd notification disallow_listener $COMPONENTE")

        val estado = aguardarEstado(EstadoListener.PARADO, MotivoParada.PERMISSAO_REVOGADA)
        aguardarAcessoRevogado()

        assertFalse(acesso.concedido())
        assertNotNull(estado.atualizadoEm)
    }

    @Test
    fun reconectarRestauraAtivoSemAcaoDoUsuario() {
        shell("cmd notification disallow_listener $COMPONENTE")
        aguardarEstado(EstadoListener.PARADO, MotivoParada.PERMISSAO_REVOGADA)

        shell("cmd notification allow_listener $COMPONENTE")

        val estado = aguardarEstado(EstadoListener.ATIVO)
        assertNull(estado.motivo)
    }

    @Test
    fun notificacaoNaoFinanceiraContaComoIgnoradaEAtualizaUltimaLeitura() {
        val mes = mesAtual()
        val antes = contadores(mes)
        val inicio = Instant.now()

        postar("Seu limite", "pingo-teste-ITAU", TEXTO_RUIDO_ITAU)

        val depois = aguardarContadores(mes) { atuais -> atuais.ignoradas == antes.ignoradas + 1 }
        assertEquals(antes.naoEntendidas, depois.naoEntendidas)
        aguardarLeituraApos(InstituicaoFinanceira.ITAU, inicio)
    }

    @Test
    fun textoForaDosPadroesContaComoNaoEntendida() {
        val mes = mesAtual()
        val antes = contadores(mes)

        postar("Aviso", "pingo-teste-ITAU", TEXTO_DESCONHECIDO)

        val depois = aguardarContadores(mes) { atuais -> atuais.naoEntendidas == antes.naoEntendidas + 1 }
        assertEquals(antes.ignoradas, depois.ignoradas)
    }

    @Test
    fun compraReconhecidaNaoIncrementaContadoresDaCaptura() {
        val mes = mesAtual()
        val antes = contadores(mes)
        val inicio = Instant.now()

        postar("Sua compra foi aprovada", "pingo-teste-ITAU", TEXTO_COMPRA_ITAU)

        aguardarLeituraApos(InstituicaoFinanceira.ITAU, inicio)
        Thread.sleep(INTERVALO_ESTABILIZACAO)

        val depois = contadores(mes)
        assertEquals(antes.ignoradas, depois.ignoradas)
        assertEquals(antes.naoEntendidas, depois.naoEntendidas)
    }

    private fun postar(titulo: String, tag: String, texto: String) {
        val script = File(contexto.getExternalFilesDir(null), "sonda_de_captura.sh")
        script.writeText("cmd notification post -t \"$titulo\" $tag \"$texto\"\n")
        shell("sh ${script.absolutePath}")
    }

    private fun aguardarNotificacaoProcessada() {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            val inicio = Instant.now()
            postar("Sonda do Pingo", "pingo-teste-CAJU", TEXTO_SONDA_CAJU)

            val processou = runBlocking {
                withTimeoutOrNull(INTERVALO_SONDAGEM) {
                    dataSource.ultimaLeituraPorApp.first { leituras ->
                        leituras[InstituicaoFinanceira.CAJU]?.isAfter(inicio.minusSeconds(2)) == true
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

    private fun aguardarLeituraApos(instituicao: InstituicaoFinanceira, inicio: Instant): Instant = runBlocking {
        withTimeout(TEMPO_ESPERA) {
            dataSource.ultimaLeituraPorApp.first { leituras ->
                leituras[instituicao]?.isAfter(inicio.minusSeconds(2)) == true
            }[instituicao]!!
        }
    }

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

    private fun aguardarEstado(estado: EstadoListener, motivo: MotivoParada? = null) = runBlocking {
        withTimeout(TEMPO_ESPERA) {
            dataSource.estadoListener.first { atual -> atual.estado == estado && atual.motivo == motivo }
        }
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

    private fun aguardarNotificacaoPersistente(): android.service.notification.StatusBarNotification {
        val limite = System.currentTimeMillis() + TEMPO_ESPERA
        while (System.currentTimeMillis() < limite) {
            gerenciadorNotificacoes.activeNotifications
                .firstOrNull { notificacao -> notificacao.id == NotificacoesDeCaptura.ID_SERVICO }
                ?.let { return it }
            Thread.sleep(INTERVALO_CONSULTA)
        }
        throw AssertionError("notificação persistente do foreground service não apareceu")
    }

    private fun mesAtual(): YearMonth = YearMonth.from(Instant.now().atZone(ZoneId.systemDefault()))

    private fun shell(comando: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(comando)
        return ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
            stream.readBytes().decodeToString()
        }
    }

    private companion object {
        const val PACOTE = "com.pingo"
        const val COMPONENTE = "$PACOTE/com.pingo.core.notifications.PingoNotificationListenerService"
        const val TEMPO_ESPERA = 20_000L
        const val INTERVALO_CONSULTA = 100L
        const val INTERVALO_ESTABILIZACAO = 2_000L
        const val INTERVALO_SONDAGEM = 2_000L
        const val TEXTO_SONDA_CAJU = "Wellhub"
        const val TEXTO_RUIDO_ITAU = "Mais controle para gerenciar seu limite"
        const val TEXTO_DESCONHECIDO = "Seu extrato esta disponivel no aplicativo"
        const val TEXTO_COMPRA_ITAU =
            "Ola! Compra aprovada de R$ 37,90 em CLARO *31987905690 no dia 10/11 às 00:55 no seu cartão Itau. Boas compras."
    }
}
