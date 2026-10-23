package com.pingo.core.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class UserPreferencesDataSourceTest {
    private val dataSource = UserPreferencesDataSourceImpl(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun geraUmDeviceIdEstavel() = runBlocking {
        val primeiro = dataSource.deviceId()

        assertTrue(primeiro.isNotBlank())
        assertEquals(primeiro, dataSource.deviceId())
    }

    @Test
    fun persisteCursoresEEstadoDeSincronizacao() = runBlocking {
        val instante = Instant.parse("2026-10-23T10:00:00Z")

        dataSource.setPullCursor("cursor-1")
        dataSource.setPushCursor(instante)
        dataSource.setSyncStatus(SyncStatus.FAILED)

        assertEquals("cursor-1", dataSource.pullCursor())
        assertEquals(instante, dataSource.pushCursor())
        assertEquals(SyncStatus.FAILED, dataSource.syncStatus.first())
    }
}
