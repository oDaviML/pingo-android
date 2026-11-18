package com.pingo.core.notifications

import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class ServicoDeCapturaDoAndroid private constructor(private val context: Context, private val dependencias: Dependencias?) :
    ServicoDeCaptura {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        capturaController: Provider<CapturaController>,
        estadoDoServico: EstadoDoServicoDeCaptura,
        escopoDeCaptura: EscopoDeCaptura,
    ) : this(
        context = context,
        dependencias = Dependencias(capturaController, estadoDoServico, escopoDeCaptura),
    )

    constructor(context: Context) : this(context = context, dependencias = null)

    private var monitorDeAcesso: MonitorDeAcessoAoListener? = null

    @Synchronized
    override fun iniciar() {
        NotificacoesDeCaptura.criarCanais(context)
        pararServicoLegado()
        dependencias?.estadoDoServico?.atualizar(emExecucao = true)
        iniciarMonitorDeAcesso()
        agendarVerificacaoPeriodica()
    }

    @Synchronized
    override fun parar() {
        monitorDeAcesso?.parar()
        monitorDeAcesso = null
        dependencias?.estadoDoServico?.atualizar(emExecucao = false)
        runCatching { WorkManager.getInstance(context).cancelUniqueWork(WORK_VERIFICACAO_CAPTURA_PARADA) }
    }

    private fun pararServicoLegado() {
        runCatching {
            context.stopService(Intent().setClassName(context, NOME_CLASSE_SERVICO_LEGADO))
        }
    }

    private fun iniciarMonitorDeAcesso() {
        val deps = dependencias ?: return
        if (monitorDeAcesso == null) {
            monitorDeAcesso = MonitorDeAcessoAoListener(context) {
                deps.escopoDeCaptura.lancar { runCatching { deps.capturaController.get().reconectar() } }
            }
        }
        monitorDeAcesso?.iniciar()
    }

    private fun agendarVerificacaoPeriodica() {
        runCatching {
            val requisicao = PeriodicWorkRequestBuilder<VerificaCapturaParadaWorker>(
                INTERVALO_VERIFICACAO_HORAS,
                TimeUnit.HOURS,
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_VERIFICACAO_CAPTURA_PARADA,
                ExistingPeriodicWorkPolicy.KEEP,
                requisicao,
            )
        }
    }

    private class Dependencias(
        val capturaController: Provider<CapturaController>,
        val estadoDoServico: EstadoDoServicoDeCaptura,
        val escopoDeCaptura: EscopoDeCaptura,
    )

    private companion object {
        const val INTERVALO_VERIFICACAO_HORAS = 6L
        const val WORK_VERIFICACAO_CAPTURA_PARADA = "pingo_verifica_captura_parada"
        const val NOME_CLASSE_SERVICO_LEGADO = "com.pingo.core.notifications.CapturaForegroundService"
    }
}
