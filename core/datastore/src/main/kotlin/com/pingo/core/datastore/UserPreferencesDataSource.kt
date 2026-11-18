package com.pingo.core.datastore

import com.pingo.core.model.TemaDoApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant

enum class SyncStatus {
    IDLE,
    RUNNING,
    FAILED,
}

@Suppress("TooManyFunctions")
interface UserPreferencesDataSource {
    val syncStatus: Flow<SyncStatus>

    suspend fun setSyncStatus(status: SyncStatus)

    suspend fun pullCursor(): String?

    suspend fun setPullCursor(cursor: String?)

    suspend fun pushCursor(): Instant?

    suspend fun setPushCursor(instant: Instant)

    suspend fun lastSyncAt(): Instant?

    suspend fun setLastSyncAt(instant: Instant)

    suspend fun deviceId(): String

    suspend fun activatedDeviceId(): String?

    suspend fun setActivatedDeviceId(deviceId: String)

    suspend fun consentVersion(): String?

    suspend fun consentAt(): Instant?

    suspend fun setConsent(version: String, instant: Instant)

    suspend fun contasConfiguradas(): Boolean

    suspend fun setContasConfiguradas(configuradas: Boolean)

    suspend fun contasOnboardingPendentes(): List<String>

    suspend fun setContasOnboardingPendentes(publicIds: List<String>)

    val avisoAssinaturasAtivo: Flow<Boolean>
        get() = flowOf(false)

    suspend fun setAvisoAssinaturasAtivo(ativo: Boolean) = Unit

    val biometriaAtiva: Flow<Boolean>
        get() = flowOf(false)

    suspend fun setBiometriaAtiva(ativo: Boolean) = Unit

    suspend fun biometriaOnboardingConcluido(): Boolean = false

    suspend fun setBiometriaOnboardingConcluido(concluido: Boolean) = Unit

    val tempoBloqueioSegundos: Flow<Long>
        get() = flowOf(300L)

    suspend fun setTempoBloqueioSegundos(segundos: Long) = Unit

    val revisarPelaNotificacaoAtivo: Flow<Boolean>
        get() = flowOf(true)

    suspend fun setRevisarPelaNotificacaoAtivo(ativo: Boolean) = Unit

    val alertaCapturaParadaAtivo: Flow<Boolean>
        get() = flowOf(true)

    suspend fun setAlertaCapturaParadaAtivo(ativo: Boolean) = Unit

    val telemetriaOptIn: Flow<Boolean>
        get() = flowOf(false)

    suspend fun setTelemetriaOptIn(ativo: Boolean) = Unit

    val temaDoApp: Flow<TemaDoApp>
        get() = flowOf(TemaDoApp.SISTEMA)

    suspend fun setTemaDoApp(tema: TemaDoApp) = Unit

    val emailUsuario: Flow<String?>
        get() = flowOf(null)

    val nomeUsuario: Flow<String?>
        get() = flowOf(null)

    suspend fun salvarPerfilUsuario(email: String?, nome: String?) = Unit

    suspend fun clear()
}
