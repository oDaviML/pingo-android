package com.pingo.core.notifications

import com.pingo.core.common.DefaultAppDispatchers
import com.pingo.core.datastore.EstadoListener
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.model.InstituicaoFinanceira
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class GatilhoDeCapturaParadaTest {
    private val agora = Instant.parse("2026-11-10T12:00:00Z")

    @Test
    fun `posta o alerta quando passa de quarenta e oito horas sem leitura`() = runTest {
        val dataSource = dataSourceAtivo(observandoDesde = agora.minus(Duration.ofDays(4)))
        dataSource.registrarLeitura(InstituicaoFinanceira.ITAU, agora.minus(Duration.ofHours(50)))
        val notificador = FakeNotificadorDeCapturaParada()

        val diagnostico = gatilho(dataSource, notificador).verificar()

        assertEquals(SituacaoDaCaptura.SEM_LEITURA, diagnostico.situacao)
        assertEquals(1, notificador.postados)
        assertEquals(agora.minus(Duration.ofHours(50)), dataSource.alertaDeCaptura.first()?.referencia)
        assertEquals(agora, dataSource.alertaDeCaptura.first()?.enviadoEm)
    }

    @Test
    fun `nao repete o alerta para o mesmo periodo de silencio`() = runTest {
        val dataSource = dataSourceAtivo(observandoDesde = agora.minus(Duration.ofDays(4)))
        dataSource.registrarLeitura(InstituicaoFinanceira.ITAU, agora.minus(Duration.ofHours(50)))
        val notificador = FakeNotificadorDeCapturaParada()
        val gatilho = gatilho(dataSource, notificador)

        gatilho.verificar()
        gatilho.verificar()

        assertEquals(1, notificador.postados)
    }

    @Test
    fun `nao posta com a captura ativa`() = runTest {
        val dataSource = dataSourceAtivo(observandoDesde = agora.minus(Duration.ofHours(2)))
        dataSource.registrarLeitura(InstituicaoFinanceira.NUBANK, agora.minus(Duration.ofMinutes(40)))
        val notificador = FakeNotificadorDeCapturaParada()

        val diagnostico = gatilho(dataSource, notificador).verificar()

        assertEquals(SituacaoDaCaptura.ATIVA, diagnostico.situacao)
        assertEquals(0, notificador.postados)
        assertNull(dataSource.alertaDeCaptura.first())
    }

    @Test
    fun `posta o alerta com o listener parado por permissao e preserva o motivo`() = runTest {
        val dataSource = FakeCapturaDataSource()
        dataSource.registrarAtivo(agora.minus(Duration.ofDays(10)))
        dataSource.registrarParado(MotivoParada.PERMISSAO_REVOGADA, agora.minus(Duration.ofDays(3)))
        dataSource.registrarLeitura(InstituicaoFinanceira.ITAU, agora.minus(Duration.ofHours(49)))
        val notificador = FakeNotificadorDeCapturaParada()

        val diagnostico = gatilho(dataSource, notificador).verificar()

        assertEquals(SituacaoDaCaptura.PARADA, diagnostico.situacao)
        assertEquals(MotivoParada.PERMISSAO_REVOGADA, diagnostico.motivo)
        assertEquals(agora.minus(Duration.ofHours(49)), diagnostico.semLeituraDesde)
        assertEquals(1, notificador.postados)
        assertEquals(agora.minus(Duration.ofHours(49)), dataSource.alertaDeCaptura.first()?.referencia)
    }

    @Test
    fun `estado parado com silencio recente nao alerta`() = runTest {
        val dataSource = FakeCapturaDataSource()
        dataSource.registrarAtivo(agora.minus(Duration.ofDays(10)))
        dataSource.registrarParado(MotivoParada.DESCONECTADO, agora.minus(Duration.ofHours(2)))
        dataSource.registrarLeitura(InstituicaoFinanceira.INTER, agora.minus(Duration.ofHours(47)))
        val notificador = FakeNotificadorDeCapturaParada()

        val diagnostico = gatilho(dataSource, notificador).verificar()

        assertEquals(SituacaoDaCaptura.PARADA, diagnostico.situacao)
        assertEquals(MotivoParada.DESCONECTADO, diagnostico.motivo)
        assertNull(diagnostico.semLeituraDesde)
        assertEquals(0, notificador.postados)
        assertNull(dataSource.alertaDeCaptura.first())
    }

    @Test
    fun `estado parado sem leitura alerta a partir do inicio da observacao`() = runTest {
        val dataSource = FakeCapturaDataSource()
        dataSource.registrarAtivo(agora.minus(Duration.ofHours(50)))
        dataSource.registrarParado(MotivoParada.DESCONECTADO, agora.minus(Duration.ofHours(49)))
        val notificador = FakeNotificadorDeCapturaParada()

        val diagnostico = gatilho(dataSource, notificador).verificar()

        assertEquals(SituacaoDaCaptura.PARADA, diagnostico.situacao)
        assertEquals(agora.minus(Duration.ofHours(50)), diagnostico.semLeituraDesde)
        assertEquals(1, notificador.postados)
    }

    @Test
    fun `nao repete o alerta no mesmo silencio com listener parado`() = runTest {
        val dataSource = FakeCapturaDataSource()
        dataSource.registrarAtivo(agora.minus(Duration.ofDays(10)))
        dataSource.registrarParado(MotivoParada.PERMISSAO_REVOGADA, agora.minus(Duration.ofDays(3)))
        dataSource.registrarLeitura(InstituicaoFinanceira.ITAU, agora.minus(Duration.ofHours(49)))
        val notificador = FakeNotificadorDeCapturaParada()
        val gatilho = gatilho(dataSource, notificador)

        gatilho.verificar()
        gatilho.verificar()

        assertEquals(1, notificador.postados)
    }

    @Test
    fun `nova leitura e novo silencio com listener parado voltam a alertar`() = runTest {
        val dataSource = FakeCapturaDataSource()
        dataSource.registrarAtivo(agora.minus(Duration.ofDays(10)))
        dataSource.registrarParado(MotivoParada.DESCONECTADO, agora.minus(Duration.ofDays(3)))
        dataSource.registrarLeitura(InstituicaoFinanceira.ITAU, agora.minus(Duration.ofHours(60)))
        val notificador = FakeNotificadorDeCapturaParada()
        gatilho(dataSource, notificador).verificar()

        dataSource.registrarAtivo(agora.plus(Duration.ofHours(1)))
        dataSource.registrarLeitura(InstituicaoFinanceira.INTER, agora.plus(Duration.ofHours(2)))
        gatilho(dataSource, notificador, relogio = agora.plus(Duration.ofHours(3))).verificar()

        assertEquals(1, notificador.postados)

        dataSource.registrarParado(MotivoParada.DESCONECTADO, agora.plus(Duration.ofHours(4)))
        gatilho(dataSource, notificador, relogio = agora.plus(Duration.ofHours(51))).verificar()

        assertEquals(2, notificador.postados)
        assertEquals(agora.plus(Duration.ofHours(2)), dataSource.alertaDeCaptura.first()?.referencia)
        assertEquals(EstadoListener.PARADO, dataSource.estadoListener.first().estado)
    }

    @Test
    fun `posta de novo depois de uma nova leitura e um novo silencio`() = runTest {
        val dataSource = dataSourceAtivo(observandoDesde = agora.minus(Duration.ofDays(10)))
        dataSource.registrarLeitura(InstituicaoFinanceira.CAJU, agora.minus(Duration.ofHours(60)))
        val notificador = FakeNotificadorDeCapturaParada()
        gatilho(dataSource, notificador).verificar()

        dataSource.registrarLeitura(InstituicaoFinanceira.INTER, agora.minus(Duration.ofHours(2)))
        gatilho(dataSource, notificador).verificar()

        assertEquals(1, notificador.postados)

        gatilho(dataSource, notificador, relogio = agora.plus(Duration.ofHours(50))).verificar()

        assertEquals(2, notificador.postados)
        assertNotNull(dataSource.alertaDeCaptura.first())
        assertEquals(agora.minus(Duration.ofHours(2)), dataSource.alertaDeCaptura.first()?.referencia)
    }

    private suspend fun dataSourceAtivo(observandoDesde: Instant): FakeCapturaDataSource = FakeCapturaDataSource().apply {
        registrarAtivo(agora.minus(Duration.ofDays(1)))
        observarDesde(observandoDesde)
    }

    private fun gatilho(
        dataSource: FakeCapturaDataSource,
        notificador: FakeNotificadorDeCapturaParada,
        relogio: Instant = agora,
    ): GatilhoDeCapturaParada {
        val clock = Clock.fixed(relogio, ZoneOffset.UTC)

        return GatilhoDeCapturaParada(
            monitorDaCaptura = MonitorDaCaptura(dataSource, clock),
            capturaDataSource = dataSource,
            notificador = notificador,
            clock = clock,
            dispatchers = DefaultAppDispatchers,
        )
    }

    private class FakeNotificadorDeCapturaParada : NotificadorDeCapturaParada {
        var postados: Int = 0
            private set

        override fun capturaParada() {
            postados += 1
        }
    }
}
