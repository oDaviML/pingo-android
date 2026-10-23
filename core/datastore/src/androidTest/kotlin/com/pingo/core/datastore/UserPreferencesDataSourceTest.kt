package com.pingo.core.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class UserPreferencesDataSourceTest {
    private val dataSource = UserPreferencesDataSourceImpl(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun geraUmDeviceIdEstavelAteLimpar() = runBlocking {
        dataSource.clear()

        val primeiro = dataSource.deviceId()

        assertTrue(primeiro.isNotBlank())
        assertEquals(primeiro, dataSource.deviceId())

        dataSource.clear()

        assertNotEquals(primeiro, dataSource.deviceId())
    }

    @Test
    fun persisteOAparelhoAtivado() = runBlocking {
        dataSource.setActivatedDeviceId("device-1")

        assertEquals("device-1", dataSource.activatedDeviceId())
    }

    @Test
    fun persisteCursoresEEstadoDeSincronizacao() = runBlocking {
        dataSource.clear()

        val instante = Instant.parse("2026-10-23T10:00:00Z")

        dataSource.setPullCursor("cursor-1")
        dataSource.setPushCursor(instante)
        dataSource.setSyncStatus(SyncStatus.FAILED)

        assertEquals("cursor-1", dataSource.pullCursor())
        assertEquals(instante, dataSource.pushCursor())
        assertEquals(SyncStatus.FAILED, dataSource.syncStatus.first())

        dataSource.clear()

        assertNull(dataSource.pullCursor())
        assertEquals(SyncStatus.IDLE, dataSource.syncStatus.first())
    }

    @Test
    fun persisteEConsentimentoELimpaAoExcluirConta() = runBlocking {
        dataSource.clear()

        val instante = Instant.parse("2026-10-23T10:00:00Z")

        dataSource.setConsent(version = "2026-10", instant = instante)

        assertEquals("2026-10", dataSource.consentVersion())
        assertEquals(instante, dataSource.consentAt())

        dataSource.clear()

        assertNull(dataSource.consentVersion())
        assertNull(dataSource.consentAt())
    }
}
