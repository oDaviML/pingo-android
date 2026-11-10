package com.pingo.core.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.pingo.core.common.AppDispatchers
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class PingoNotificationListenerService : NotificationListenerService() {
    @Inject lateinit var pipelineDeCaptura: PipelineDeCaptura

    @Inject lateinit var capturaController: CapturaController

    @Inject lateinit var atualizadorDeRegras: AtualizadorDeRegras

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
        capturaController.registrarConectado()
        executar { atualizadorDeRegras.atualizar() }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()

        if (desconexaoProcessada) {
            return
        }

        desconexaoProcessada = true
        capturaController.registrarDesconectado()
    }

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        super.onNotificationPosted(statusBarNotification)
        val notificacao = statusBarNotification.paraNotificacaoRecebida()
        executar { pipelineDeCaptura.processar(notificacao) }
    }

    override fun onDestroy() {
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
