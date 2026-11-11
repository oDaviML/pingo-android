package com.pingo.core.notifications

import com.pingo.core.common.AppDispatchers
import com.pingo.core.datastore.CapturaDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

interface NotificadorDeCapturaParada {
    fun capturaParada()
}

@Singleton
class GatilhoDeCapturaParada @Inject constructor(
    private val monitorDaCaptura: MonitorDaCaptura,
    private val capturaDataSource: CapturaDataSource,
    private val notificador: NotificadorDeCapturaParada,
    private val clock: Clock,
    private val dispatchers: AppDispatchers,
) {
    suspend fun verificar(): DiagnosticoDaCaptura = withContext(dispatchers.io) {
        val diagnostico = monitorDaCaptura.atual()
        val referencia = diagnostico.semLeituraDesde

        if (diagnostico.situacao == SituacaoDaCaptura.SEM_LEITURA && referencia != null) {
            val jaAvisado = capturaDataSource.alertaDeCaptura.first()?.referencia == referencia
            if (!jaAvisado) {
                notificador.capturaParada()
                capturaDataSource.registrarAlertaDeCaptura(referencia, clock.instant())
            }
        }

        diagnostico
    }
}
