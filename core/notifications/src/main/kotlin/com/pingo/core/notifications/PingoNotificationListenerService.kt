package com.pingo.core.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.pingo.core.common.AppDispatchers
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class PingoNotificationListenerService : NotificationListenerService() {
    @Inject lateinit var pipelineDeCaptura: Lazy<PipelineDeCaptura>

    @Inject lateinit var capturaController: CapturaController

    @Inject lateinit var atualizadorDeRegras: Lazy<AtualizadorDeRegras>

    @Inject lateinit var estadoDoServico: EstadoDoServicoDeCaptura

    @Inject lateinit var dispatchers: AppDispatchers

    private var escopo: CoroutineScope? = null
    private var desconexaoProcessada = false

    override fun onCreate() {
        super.onCreate()
        escopo = CoroutineScope(SupervisorJob() + dispatchers.io)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        desconexaoProcessada = false
        if (::estadoDoServico.isInitialized) {
            estadoDoServico.atualizar(emExecucao = true)
        }
        capturaController.registrarConectado()
        executar { atualizadorDeRegras.get().atualizar() }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()

        if (desconexaoProcessada) {
            return
        }

        desconexaoProcessada = true
        if (::estadoDoServico.isInitialized) {
            estadoDoServico.atualizar(emExecucao = false)
        }
        capturaController.registrarDesconectado()
    }

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        super.onNotificationPosted(statusBarNotification)
        val notificacao = statusBarNotification.paraNotificacaoRecebida()
        executar { pipelineDeCaptura.get().processar(notificacao) }
    }

    override fun onDestroy() {
        if (!desconexaoProcessada && ::capturaController.isInitialized) {
            desconexaoProcessada = true
            capturaController.registrarDesconectado()
        }
        if (::estadoDoServico.isInitialized) {
            estadoDoServico.atualizar(emExecucao = false)
        }
        escopo?.cancel()
        escopo = null
        super.onDestroy()
    }

    private fun executar(bloco: suspend () -> Unit) {
        escopo?.launch { runCatching { bloco() } }
    }
}

private fun StatusBarNotification.paraNotificacaoRecebida(): NotificacaoRecebida {
    val extras = notification.extras
    return NotificacaoRecebida(
        pacote = packageName,
        tag = tag,
        titulo = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
        texto = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
        bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
        postTime = Instant.ofEpochMilli(postTime),
    )
}
