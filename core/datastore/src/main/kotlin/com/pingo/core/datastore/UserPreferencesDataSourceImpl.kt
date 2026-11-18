package com.pingo.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

@Singleton
@Suppress("TooManyFunctions")
class UserPreferencesDataSourceImpl internal constructor(private val dataStore: DataStore<Preferences>) : UserPreferencesDataSource {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context.userPreferencesDataStore)

    private val deviceIdMutex = Mutex()

    override val syncStatus: Flow<SyncStatus> = dataStore.data.map { preferences ->
        preferences[Keys.SYNC_STATUS]
            ?.let { value -> runCatching { SyncStatus.valueOf(value) }.getOrNull() }
            ?: SyncStatus.IDLE
    }

    override suspend fun setSyncStatus(status: SyncStatus) {
        dataStore.edit { preferences ->
            preferences[Keys.SYNC_STATUS] = status.name
        }
    }

    override suspend fun pullCursor(): String? = dataStore.data.first()[Keys.PULL_CURSOR]

    override suspend fun setPullCursor(cursor: String?) {
        dataStore.edit { preferences ->
            if (cursor == null) {
                preferences.remove(Keys.PULL_CURSOR)
            } else {
                preferences[Keys.PULL_CURSOR] = cursor
            }
        }
    }

    override suspend fun pushCursor(): Instant? = dataStore.data.first()[Keys.PUSH_CURSOR]?.let(Instant::parse)

    override suspend fun setPushCursor(instant: Instant) {
        dataStore.edit { preferences ->
            preferences[Keys.PUSH_CURSOR] = instant.toString()
        }
    }

    override suspend fun lastSyncAt(): Instant? = dataStore.data.first()[Keys.LAST_SYNC_AT]?.let(Instant::parse)

    override suspend fun setLastSyncAt(instant: Instant) {
        dataStore.edit { preferences ->
            preferences[Keys.LAST_SYNC_AT] = instant.toString()
        }
    }

    override suspend fun deviceId(): String = deviceIdMutex.withLock {
        val existing = dataStore.data.first()[Keys.DEVICE_ID]

        if (existing != null) {
            return@withLock existing
        }

        val generated = UUID.randomUUID().toString()

        dataStore.edit { preferences ->
            preferences[Keys.DEVICE_ID] = generated
        }

        generated
    }

    override suspend fun activatedDeviceId(): String? = dataStore.data.first()[Keys.ACTIVATED_DEVICE_ID]

    override suspend fun setActivatedDeviceId(deviceId: String) {
        dataStore.edit { preferences ->
            preferences[Keys.ACTIVATED_DEVICE_ID] = deviceId
        }
    }

    override suspend fun consentVersion(): String? = dataStore.data.first()[Keys.CONSENT_VERSION]

    override suspend fun consentAt(): Instant? = dataStore.data.first()[Keys.CONSENT_AT]?.let(Instant::parse)

    override suspend fun setConsent(version: String, instant: Instant) {
        dataStore.edit { preferences ->
            preferences[Keys.CONSENT_VERSION] = version
            preferences[Keys.CONSENT_AT] = instant.toString()
        }
    }

    override suspend fun contasConfiguradas(): Boolean = dataStore.data.first()[Keys.CONTAS_CONFIGURADAS] ?: false

    override suspend fun setContasConfiguradas(configuradas: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.CONTAS_CONFIGURADAS] = configuradas
        }
    }

    override suspend fun contasOnboardingPendentes(): List<String> =
        dataStore.data.first()[Keys.CONTAS_ONBOARDING_PENDENTES].orEmpty().toList()

    override suspend fun setContasOnboardingPendentes(publicIds: List<String>) {
        dataStore.edit { preferences ->
            if (publicIds.isEmpty()) {
                preferences.remove(Keys.CONTAS_ONBOARDING_PENDENTES)
            } else {
                preferences[Keys.CONTAS_ONBOARDING_PENDENTES] = publicIds.toSet()
            }
        }
    }

    override val avisoAssinaturasAtivo: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.AVISO_ASSINATURAS_ATIVO] ?: false
    }

    override suspend fun setAvisoAssinaturasAtivo(ativo: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.AVISO_ASSINATURAS_ATIVO] = ativo
        }
    }

    override val biometriaAtiva: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.BIOMETRIA_ATIVA] ?: false
    }

    override suspend fun setBiometriaAtiva(ativo: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.BIOMETRIA_ATIVA] = ativo
        }
    }

    override suspend fun biometriaOnboardingConcluido(): Boolean = dataStore.data.first()[Keys.BIOMETRIA_ONBOARDING_CONCLUIDO] ?: false

    override suspend fun setBiometriaOnboardingConcluido(concluido: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.BIOMETRIA_ONBOARDING_CONCLUIDO] = concluido
        }
    }

    override val tempoBloqueioSegundos: Flow<Long> = dataStore.data.map { preferences ->
        preferences[Keys.TEMPO_BLOQUEIO_SEGUNDOS] ?: TEMPO_BLOQUEIO_PADRAO_SEGUNDOS
    }

    override suspend fun setTempoBloqueioSegundos(segundos: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.TEMPO_BLOQUEIO_SEGUNDOS] = segundos
        }
    }

    override val revisarPelaNotificacaoAtivo: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.REVISAR_PELA_NOTIFICACAO_ATIVO] ?: true
    }

    override suspend fun setRevisarPelaNotificacaoAtivo(ativo: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.REVISAR_PELA_NOTIFICACAO_ATIVO] = ativo
        }
    }

    override val alertaCapturaParadaAtivo: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.ALERTA_CAPTURA_PARADA_ATIVO] ?: true
    }

    override suspend fun setAlertaCapturaParadaAtivo(ativo: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.ALERTA_CAPTURA_PARADA_ATIVO] = ativo
        }
    }

    override val telemetriaOptIn: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.TELEMETRIA_OPT_IN] ?: false
    }

    override suspend fun setTelemetriaOptIn(ativo: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.TELEMETRIA_OPT_IN] = ativo
        }
    }

    override val emailUsuario: Flow<String?> = dataStore.data.map { preferences ->
        preferences[Keys.EMAIL_USUARIO]
    }

    override val nomeUsuario: Flow<String?> = dataStore.data.map { preferences ->
        preferences[Keys.NOME_USUARIO]
    }

    override suspend fun salvarPerfilUsuario(email: String?, nome: String?) {
        dataStore.edit { preferences ->
            if (email == null) {
                preferences.remove(Keys.EMAIL_USUARIO)
            } else {
                preferences[Keys.EMAIL_USUARIO] = email
            }
            if (nome == null) {
                preferences.remove(Keys.NOME_USUARIO)
            } else {
                preferences[Keys.NOME_USUARIO] = nome
            }
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.clear() }
    }

    private companion object {
        const val TEMPO_BLOQUEIO_PADRAO_SEGUNDOS = 300L

        object Keys {
            val SYNC_STATUS = stringPreferencesKey("sync_status")
            val PULL_CURSOR = stringPreferencesKey("pull_cursor")
            val PUSH_CURSOR = stringPreferencesKey("push_cursor")
            val LAST_SYNC_AT = stringPreferencesKey("last_sync_at")
            val DEVICE_ID = stringPreferencesKey("device_id")
            val ACTIVATED_DEVICE_ID = stringPreferencesKey("activated_device_id")
            val CONSENT_VERSION = stringPreferencesKey("consent_version")
            val CONSENT_AT = stringPreferencesKey("consent_at")
            val CONTAS_CONFIGURADAS = booleanPreferencesKey("contas_configuradas")
            val CONTAS_ONBOARDING_PENDENTES = stringSetPreferencesKey("contas_onboarding_pendentes")
            val AVISO_ASSINATURAS_ATIVO = booleanPreferencesKey("aviso_assinaturas_ativo")
            val BIOMETRIA_ATIVA = booleanPreferencesKey("biometria_ativa")
            val BIOMETRIA_ONBOARDING_CONCLUIDO = booleanPreferencesKey("biometria_onboarding_concluido")
            val TEMPO_BLOQUEIO_SEGUNDOS = longPreferencesKey("tempo_bloqueio_segundos")
            val REVISAR_PELA_NOTIFICACAO_ATIVO = booleanPreferencesKey("revisar_pela_notificacao_ativo")
            val ALERTA_CAPTURA_PARADA_ATIVO = booleanPreferencesKey("alerta_captura_parada_ativo")
            val TELEMETRIA_OPT_IN = booleanPreferencesKey("telemetria_opt_in")
            val EMAIL_USUARIO = stringPreferencesKey("email_usuario")
            val NOME_USUARIO = stringPreferencesKey("nome_usuario")
        }
    }
}
