package com.pingo.core.notifications

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.pingo.core.common.AppDispatchers
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CapturaForegroundService : Service() {
    @Inject lateinit var estadoDoServico: EstadoDoServicoDeCaptura

    @Inject lateinit var capturaController: CapturaController

    @Inject lateinit var gatilhoDeCapturaParada: GatilhoDeCapturaParada

    @Inject lateinit var dispatchers: AppDispatchers

    private var escopo: CoroutineScope? = null
    private var monitorDeAcesso: MonitorDeAcessoAoListener? = null

    override fun onCreate() {
        super.onCreate()
        estadoDoServico.atualizar(emExecucao = true)
        NotificacoesDeCaptura.criarCanais(this)

        val escopoServico = CoroutineScope(SupervisorJob() + dispatchers.io)
        escopo = escopoServico
        monitorDeAcesso = MonitorDeAcessoAoListener(this) {
            escopoServico.launch { runCatching { capturaController.reconectar() } }
        }.also { monitor -> monitor.iniciar() }

        escopoServico.launch {
            while (isActive) {
                runCatching { gatilhoDeCapturaParada.verificar() }
                delay(INTERVALO_DE_VERIFICACAO_MS)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NotificacoesDeCaptura.ID_SERVICO, NotificacoesDeCaptura.persistente(this, destino = null))
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        monitorDeAcesso?.parar()
        monitorDeAcesso = null
        escopo?.cancel()
        escopo = null
        estadoDoServico.atualizar(emExecucao = false)
        super.onDestroy()
    }

    private companion object {
        const val INTERVALO_DE_VERIFICACAO_MS = 3_600_000L
    }
}
