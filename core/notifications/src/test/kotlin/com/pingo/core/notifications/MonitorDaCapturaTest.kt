package com.pingo.core.notifications

import com.pingo.core.datastore.MotivoParada
import com.pingo.core.model.InstituicaoFinanceira
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class MonitorDaCapturaTest {
    private val agora = Instant.parse("2026-11-10T12:00:00Z")
    private val clock = Clock.fixed(agora, ZoneOffset.UTC)

    @Test
    fun `observa o silencio a partir do estado e das leituras`() = runTest {
        val dataSource = FakeCapturaDataSource().apply {
            registrarAtivo(agora.minus(Duration.ofDays(3)))
        }
        val monitor = MonitorDaCaptura(dataSource, clock)

        assertEquals(SituacaoDaCaptura.SEM_LEITURA, monitor.atual().situacao)
        assertEquals(SituacaoDaCaptura.SEM_LEITURA, monitor.observar().first().situacao)
    }

    @Test
    fun `volta a ativa quando uma leitura chega`() = runTest {
        val dataSource = FakeCapturaDataSource().apply {
            registrarAtivo(agora.minus(Duration.ofDays(3)))
        }
        val monitor = MonitorDaCaptura(dataSource, clock)

        dataSource.registrarLeitura(InstituicaoFinanceira.ITAU, agora.minus(Duration.ofMinutes(5)))

        val diagnostico = monitor.observar().first()

        assertEquals(SituacaoDaCaptura.ATIVA, diagnostico.situacao)
        assertEquals(InstituicaoFinanceira.ITAU, diagnostico.ultimaLeituraInstituicao)
    }

    @Test
    fun `reflete parada por permissao revogada`() = runTest {
        val dataSource = FakeCapturaDataSource().apply {
            registrarParado(MotivoParada.PERMISSAO_REVOGADA, agora.minus(Duration.ofHours(2)))
        }

        val diagnostico = MonitorDaCaptura(dataSource, clock).atual()

        assertEquals(SituacaoDaCaptura.PARADA, diagnostico.situacao)
        assertEquals(MotivoParada.PERMISSAO_REVOGADA, diagnostico.motivo)
    }
}
