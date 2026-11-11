package com.pingo

import com.pingo.core.common.AppDispatchers
import com.pingo.core.notifications.IniciadorDeObservacaoDePendencias
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

class InicializacaoDoAppTest {
    @Test
    fun `init resolves observer and schedules sync on io without eager resolution`() = runTest {
        val harness = Harness(testScheduler)

        harness.inicializacao.iniciar()

        assertEquals(0, harness.resolvidos)
        assertEquals(0, harness.iniciados)
        assertEquals(0, harness.agendados)

        advanceUntilIdle()

        assertTrue(harness.despachadoNoIo)
        assertEquals(1, harness.resolvidos)
        assertEquals(1, harness.iniciados)
        assertEquals(1, harness.agendados)
    }

    @Test
    fun `init twice keeps a single observation and a single sync schedule`() = runTest {
        val harness = Harness(testScheduler)

        harness.inicializacao.iniciar()
        harness.inicializacao.iniciar()
        advanceUntilIdle()

        assertEquals(1, harness.resolvidos)
        assertEquals(1, harness.iniciados)
        assertEquals(1, harness.agendados)
    }

    private class Harness(scheduler: kotlinx.coroutines.test.TestCoroutineScheduler) {
        val dispatcherDeTeste = StandardTestDispatcher(scheduler)
        var despachadoNoIo = false
        var resolvidos = 0
        var iniciados = 0
        var agendados = 0

        private val dispatchers = object : AppDispatchers {
            override val io = object : CoroutineDispatcher() {
                override fun dispatch(context: CoroutineContext, block: Runnable) {
                    despachadoNoIo = true
                    dispatcherDeTeste.dispatch(context, block)
                }
            }
            override val default = dispatcherDeTeste
            override val main = dispatcherDeTeste
        }

        val inicializacao = InicializacaoDoApp(
            observador = Provider {
                resolvidos++
                IniciadorDeObservacaoDePendencias { iniciados++ }
            },
            agendadorDeSync = AgendadorDeSync { agendados++ },
            dispatchers = dispatchers,
        )
    }
}
