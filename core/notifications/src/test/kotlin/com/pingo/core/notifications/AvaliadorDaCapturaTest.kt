package com.pingo.core.notifications

import com.pingo.core.datastore.EstadoListener
import com.pingo.core.datastore.EstadoListenerPersistido
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.model.InstituicaoFinanceira
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class AvaliadorDaCapturaTest {
    private val agora = Instant.parse("2026-11-10T12:00:00Z")

    @Test
    fun `leitura recente mantem a captura ativa`() {
        val leitura = agora.minus(Duration.ofHours(2))

        val diagnostico = avaliar(leituras = mapOf(InstituicaoFinanceira.ITAU to leitura))

        assertEquals(SituacaoDaCaptura.ATIVA, diagnostico.situacao)
        assertFalse(diagnostico.parada)
        assertEquals(leitura, diagnostico.ultimaLeituraEm)
        assertEquals(InstituicaoFinanceira.ITAU, diagnostico.ultimaLeituraInstituicao)
        assertNull(diagnostico.semLeituraDesde)
    }

    @Test
    fun `quarente e sete horas sem leitura ainda contam como ativa`() {
        val diagnostico = avaliar(
            leituras = mapOf(InstituicaoFinanceira.NUBANK to agora.minus(Duration.ofHours(47))),
            observandoDesde = agora.minus(Duration.ofDays(3)),
        )

        assertEquals(SituacaoDaCaptura.ATIVA, diagnostico.situacao)
    }

    @Test
    fun `quarenta e oito horas sem leitura sinalizam captura parada`() {
        val leitura = agora.minus(Duration.ofHours(48))

        val diagnostico = avaliar(
            leituras = mapOf(InstituicaoFinanceira.NUBANK to leitura),
            observandoDesde = agora.minus(Duration.ofDays(3)),
        )

        assertEquals(SituacaoDaCaptura.SEM_LEITURA, diagnostico.situacao)
        assertTrue(diagnostico.parada)
        assertEquals(leitura, diagnostico.semLeituraDesde)
    }

    @Test
    fun `apenas a leitura mais recente define o silencio`() {
        val recente = agora.minus(Duration.ofMinutes(30))

        val diagnostico = avaliar(
            leituras = mapOf(
                InstituicaoFinanceira.ITAU to agora.minus(Duration.ofDays(5)),
                InstituicaoFinanceira.CAJU to recente,
            ),
        )

        assertEquals(SituacaoDaCaptura.ATIVA, diagnostico.situacao)
        assertEquals(InstituicaoFinanceira.CAJU, diagnostico.ultimaLeituraInstituicao)
        assertEquals(recente, diagnostico.ultimaLeituraEm)
    }

    @Test
    fun `sem nenhuma leitura o inicio da observacao conta como marco`() {
        val inicio = agora.minus(Duration.ofHours(49))

        val diagnostico = avaliar(leituras = emptyMap(), observandoDesde = inicio)

        assertEquals(SituacaoDaCaptura.SEM_LEITURA, diagnostico.situacao)
        assertEquals(inicio, diagnostico.semLeituraDesde)
        assertNull(diagnostico.ultimaLeituraEm)
    }

    @Test
    fun `observacao recente sem leitura continua ativa`() {
        val diagnostico = avaliar(
            leituras = emptyMap(),
            observandoDesde = agora.minus(Duration.ofHours(1)),
        )

        assertEquals(SituacaoDaCaptura.ATIVA, diagnostico.situacao)
    }

    @Test
    fun `sem observacao nem leitura nao ha silencio para alertar`() {
        val diagnostico = avaliar(leituras = emptyMap(), observandoDesde = null)

        assertEquals(SituacaoDaCaptura.ATIVA, diagnostico.situacao)
        assertNull(diagnostico.semLeituraDesde)
    }

    @Test
    fun `leitura posterior ao inicio da observacao vence o marco`() {
        val diagnostico = avaliar(
            leituras = mapOf(InstituicaoFinanceira.INTER to agora.minus(Duration.ofHours(3))),
            observandoDesde = agora.minus(Duration.ofDays(10)),
        )

        assertEquals(SituacaoDaCaptura.ATIVA, diagnostico.situacao)
    }

    @Test
    fun `estado parado tem precedencia e preserva o motivo`() {
        val diagnostico = avaliar(
            estado = EstadoListenerPersistido(EstadoListener.PARADO, MotivoParada.PERMISSAO_REVOGADA, agora.minus(Duration.ofDays(3))),
            leituras = mapOf(InstituicaoFinanceira.ITAU to agora.minus(Duration.ofDays(4))),
        )

        assertEquals(SituacaoDaCaptura.PARADA, diagnostico.situacao)
        assertTrue(diagnostico.parada)
        assertEquals(MotivoParada.PERMISSAO_REVOGADA, diagnostico.motivo)
        assertNull(diagnostico.semLeituraDesde)
        assertEquals(InstituicaoFinanceira.ITAU, diagnostico.ultimaLeituraInstituicao)
    }

    @Test
    fun `estado parado sem motivo permanece parado sem motivo inventado`() {
        val diagnostico = avaliar(estado = EstadoListenerPersistido(EstadoListener.PARADO))

        assertEquals(SituacaoDaCaptura.PARADA, diagnostico.situacao)
        assertNull(diagnostico.motivo)
    }

    private fun avaliar(
        estado: EstadoListenerPersistido = EstadoListenerPersistido(
            estado = EstadoListener.ATIVO,
            motivo = null,
            atualizadoEm = agora.minus(Duration.ofHours(1)),
        ),
        leituras: Map<InstituicaoFinanceira, Instant> = emptyMap(),
        observandoDesde: Instant? = agora.minus(Duration.ofHours(1)),
    ): DiagnosticoDaCaptura = AvaliadorDaCaptura.avaliar(
        estado = estado,
        leituras = leituras,
        observandoDesde = observandoDesde,
        agora = agora,
    )
}
