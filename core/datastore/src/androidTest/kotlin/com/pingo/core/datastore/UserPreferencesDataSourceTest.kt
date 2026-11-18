package com.pingo.core.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun persisteAConfiguracaoDeContasELimpaAoExcluirConta() = runBlocking {
        dataSource.clear()

        assertFalse(dataSource.contasConfiguradas())

        dataSource.setContasConfiguradas(true)

        assertTrue(dataSource.contasConfiguradas())

        dataSource.clear()

        assertFalse(dataSource.contasConfiguradas())
    }

    @Test
    fun avisoDeAssinaturasComecaDesligadoEPersisteAteLimpar() = runBlocking {
        dataSource.clear()

        assertFalse(dataSource.avisoAssinaturasAtivo.first())

        dataSource.setAvisoAssinaturasAtivo(true)

        assertTrue(dataSource.avisoAssinaturasAtivo.first())

        dataSource.setAvisoAssinaturasAtivo(false)

        assertFalse(dataSource.avisoAssinaturasAtivo.first())

        dataSource.setAvisoAssinaturasAtivo(true)
        dataSource.clear()

        assertFalse(dataSource.avisoAssinaturasAtivo.first())
    }

    @Test
    fun preferenciasF3PersistemValoresPadraoEPermitemLimpeza() = runBlocking {
        dataSource.clear()

        assertFalse(dataSource.biometriaAtiva.first())
        assertFalse(dataSource.biometriaOnboardingConcluido())
        assertEquals(300L, dataSource.tempoBloqueioSegundos.first())
        assertTrue(dataSource.revisarPelaNotificacaoAtivo.first())
        assertTrue(dataSource.alertaCapturaParadaAtivo.first())
        assertFalse(dataSource.telemetriaOptIn.first())
        assertNull(dataSource.emailUsuario.first())
        assertNull(dataSource.nomeUsuario.first())

        dataSource.setBiometriaAtiva(true)
        dataSource.setBiometriaOnboardingConcluido(true)
        dataSource.setTempoBloqueioSegundos(60L)
        dataSource.setRevisarPelaNotificacaoAtivo(false)
        dataSource.setAlertaCapturaParadaAtivo(false)
        dataSource.setTelemetriaOptIn(true)
        dataSource.salvarPerfilUsuario(email = "lucas@pingo.app", nome = "Lucas Martins")

        assertTrue(dataSource.biometriaAtiva.first())
        assertTrue(dataSource.biometriaOnboardingConcluido())
        assertEquals(60L, dataSource.tempoBloqueioSegundos.first())
        assertFalse(dataSource.revisarPelaNotificacaoAtivo.first())
        assertFalse(dataSource.alertaCapturaParadaAtivo.first())
        assertTrue(dataSource.telemetriaOptIn.first())
        assertEquals("lucas@pingo.app", dataSource.emailUsuario.first())
        assertEquals("Lucas Martins", dataSource.nomeUsuario.first())

        dataSource.clear()

        assertFalse(dataSource.biometriaAtiva.first())
        assertFalse(dataSource.biometriaOnboardingConcluido())
        assertEquals(300L, dataSource.tempoBloqueioSegundos.first())
        assertTrue(dataSource.revisarPelaNotificacaoAtivo.first())
        assertTrue(dataSource.alertaCapturaParadaAtivo.first())
        assertFalse(dataSource.telemetriaOptIn.first())
        assertNull(dataSource.emailUsuario.first())
        assertNull(dataSource.nomeUsuario.first())
    }
}
