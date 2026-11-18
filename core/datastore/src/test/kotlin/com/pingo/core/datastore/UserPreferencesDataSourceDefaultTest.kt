package com.pingo.core.datastore

import com.pingo.core.model.TemaDoApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class UserPreferencesDataSourceDefaultTest {
    @Test
    fun implementacaoPadraoDoAvisoComecaDesligada() = runBlocking {
        val fake = FakeMinimo()

        assertFalse(fake.avisoAssinaturasAtivo.first())
        assertFalse(fake.biometriaAtiva.first())
        assertFalse(fake.biometriaOnboardingConcluido())
        assertEquals(300L, fake.tempoBloqueioSegundos.first())
        assertTrue(fake.revisarPelaNotificacaoAtivo.first())
        assertTrue(fake.alertaCapturaParadaAtivo.first())
        assertFalse(fake.telemetriaOptIn.first())
        assertEquals(TemaDoApp.SISTEMA, fake.temaDoApp.first())
        assertNull(fake.emailUsuario.first())
        assertNull(fake.nomeUsuario.first())

        fake.setAvisoAssinaturasAtivo(true)
        fake.setBiometriaAtiva(true)
        fake.setTelemetriaOptIn(true)

        assertFalse(fake.avisoAssinaturasAtivo.first())
        assertFalse(fake.biometriaAtiva.first())
        assertFalse(fake.telemetriaOptIn.first())
    }

    private class FakeMinimo : UserPreferencesDataSource {
        override val syncStatus: Flow<SyncStatus> = flowOf(SyncStatus.IDLE)

        override suspend fun setSyncStatus(status: SyncStatus) = Unit

        override suspend fun pullCursor(): String? = null

        override suspend fun setPullCursor(cursor: String?) = Unit

        override suspend fun pushCursor(): Instant? = null

        override suspend fun setPushCursor(instant: Instant) = Unit

        override suspend fun lastSyncAt(): Instant? = null

        override suspend fun setLastSyncAt(instant: Instant) = Unit

        override suspend fun deviceId(): String = "dev-1"

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
