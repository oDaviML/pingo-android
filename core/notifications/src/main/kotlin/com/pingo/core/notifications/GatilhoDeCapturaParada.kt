package com.pingo.core.notifications

import com.pingo.core.common.AppDispatchers
import com.pingo.core.datastore.CapturaDataSource
import com.pingo.core.datastore.SyncStatus
import com.pingo.core.datastore.UserPreferencesDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
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
    private val preferences: UserPreferencesDataSource,
    private val clock: Clock,
    private val dispatchers: AppDispatchers,
) {
    constructor(
        monitorDaCaptura: MonitorDaCaptura,
        capturaDataSource: CapturaDataSource,
        notificador: NotificadorDeCapturaParada,
        clock: Clock,
        dispatchers: AppDispatchers,
    ) : this(
        monitorDaCaptura = monitorDaCaptura,
        capturaDataSource = capturaDataSource,
        notificador = notificador,
        preferences = PreferenciasPadrao,
        clock = clock,
        dispatchers = dispatchers,
    )

    suspend fun verificar(): DiagnosticoDaCaptura = withContext(dispatchers.io) {
        val diagnostico = monitorDaCaptura.atual()
        val referencia = diagnostico.semLeituraDesde

        if (referencia != null && preferences.alertaCapturaParadaAtivo.first()) {
            val jaAvisado = capturaDataSource.alertaDeCaptura.first()?.referencia == referencia
            if (!jaAvisado) {
                notificador.capturaParada()
                capturaDataSource.registrarAlertaDeCaptura(referencia, clock.instant())
            }
        }

        diagnostico
    }

    private object PreferenciasPadrao : UserPreferencesDataSource {
        override val syncStatus: Flow<SyncStatus> = MutableStateFlow(SyncStatus.IDLE)

        override suspend fun setSyncStatus(status: SyncStatus) = Unit

        override suspend fun pullCursor(): String? = null

        override suspend fun setPullCursor(cursor: String?) = Unit

        override suspend fun pushCursor(): Instant? = null

        override suspend fun setPushCursor(instant: Instant) = Unit

        override suspend fun lastSyncAt(): Instant? = null

        override suspend fun setLastSyncAt(instant: Instant) = Unit

        override suspend fun deviceId(): String = ""

        override suspend fun activatedDeviceId(): String? = null

        override suspend fun setActivatedDeviceId(deviceId: String) = Unit

        override suspend fun consentVersion(): String? = null

        override suspend fun consentAt(): Instant? = null

        override suspend fun setConsent(version: String, instant: Instant) = Unit

        override suspend fun contasConfiguradas(): Boolean = false

        override suspend fun setContasConfiguradas(configuradas: Boolean) = Unit

        override suspend fun contasOnboardingPendentes(): List<String> = emptyList()

        override suspend fun setContasOnboardingPendentes(publicIds: List<String>) = Unit

        override suspend fun clear() = Unit
    }
}
