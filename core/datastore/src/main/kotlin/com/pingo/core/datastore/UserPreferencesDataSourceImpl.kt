package com.pingo.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
class UserPreferencesDataSourceImpl @Inject constructor(@param:ApplicationContext private val context: Context) :
    UserPreferencesDataSource {
    private val deviceIdMutex = Mutex()

    override val syncStatus: Flow<SyncStatus> = context.userPreferencesDataStore.data.map { preferences ->
        preferences[Keys.SYNC_STATUS]
            ?.let { value -> runCatching { SyncStatus.valueOf(value) }.getOrNull() }
            ?: SyncStatus.IDLE
    }

    override suspend fun setSyncStatus(status: SyncStatus) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[Keys.SYNC_STATUS] = status.name
        }
    }

    override suspend fun pullCursor(): String? = context.userPreferencesDataStore.data.first()[Keys.PULL_CURSOR]

    override suspend fun setPullCursor(cursor: String?) {
        context.userPreferencesDataStore.edit { preferences ->
            if (cursor == null) {
                preferences.remove(Keys.PULL_CURSOR)
            } else {
                preferences[Keys.PULL_CURSOR] = cursor
            }
        }
    }

    override suspend fun pushCursor(): Instant? = context.userPreferencesDataStore.data.first()[Keys.PUSH_CURSOR]?.let(Instant::parse)

    override suspend fun setPushCursor(instant: Instant) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[Keys.PUSH_CURSOR] = instant.toString()
        }
    }

    override suspend fun lastSyncAt(): Instant? = context.userPreferencesDataStore.data.first()[Keys.LAST_SYNC_AT]?.let(Instant::parse)

    override suspend fun setLastSyncAt(instant: Instant) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[Keys.LAST_SYNC_AT] = instant.toString()
        }
    }

    override suspend fun deviceId(): String = deviceIdMutex.withLock {
        val existing = context.userPreferencesDataStore.data.first()[Keys.DEVICE_ID]

        if (existing != null) {
            return@withLock existing
        }

        val generated = UUID.randomUUID().toString()

        context.userPreferencesDataStore.edit { preferences ->
            preferences[Keys.DEVICE_ID] = generated
        }

        generated
    }

    override suspend fun activatedDeviceId(): String? = context.userPreferencesDataStore.data.first()[Keys.ACTIVATED_DEVICE_ID]

    override suspend fun setActivatedDeviceId(deviceId: String) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[Keys.ACTIVATED_DEVICE_ID] = deviceId
        }
    }

    override suspend fun consentVersion(): String? = context.userPreferencesDataStore.data.first()[Keys.CONSENT_VERSION]

    override suspend fun consentAt(): Instant? = context.userPreferencesDataStore.data.first()[Keys.CONSENT_AT]?.let(Instant::parse)

    override suspend fun setConsent(version: String, instant: Instant) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[Keys.CONSENT_VERSION] = version
            preferences[Keys.CONSENT_AT] = instant.toString()
        }
    }

    override suspend fun contasConfiguradas(): Boolean = context.userPreferencesDataStore.data.first()[Keys.CONTAS_CONFIGURADAS] ?: false

    override suspend fun setContasConfiguradas(configuradas: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[Keys.CONTAS_CONFIGURADAS] = configuradas
        }
    }

    override suspend fun contasOnboardingPendentes(): List<String> =
        context.userPreferencesDataStore.data.first()[Keys.CONTAS_ONBOARDING_PENDENTES].orEmpty().toList()

    override suspend fun setContasOnboardingPendentes(publicIds: List<String>) {
        context.userPreferencesDataStore.edit { preferences ->
            if (publicIds.isEmpty()) {
                preferences.remove(Keys.CONTAS_ONBOARDING_PENDENTES)
            } else {
                preferences[Keys.CONTAS_ONBOARDING_PENDENTES] = publicIds.toSet()
            }
        }
    }

    override val avisoAssinaturasAtivo: Flow<Boolean> = context.userPreferencesDataStore.data.map { preferences ->
        preferences[Keys.AVISO_ASSINATURAS_ATIVO] ?: false
    }

    override suspend fun setAvisoAssinaturasAtivo(ativo: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[Keys.AVISO_ASSINATURAS_ATIVO] = ativo
        }
    }

    override suspend fun clear() {
        context.userPreferencesDataStore.edit { preferences -> preferences.clear() }
    }

    private companion object {
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
        }
    }
}
