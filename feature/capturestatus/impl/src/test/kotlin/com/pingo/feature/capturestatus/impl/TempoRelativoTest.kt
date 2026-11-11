package com.pingo.feature.capturestatus.impl

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class TempoRelativoTest {
    private val zona = ZoneId.of("America/Sao_Paulo")
    private val agora = Instant.parse("2026-11-10T15:30:00Z")

    @Test
    fun `poucos segundos aparecem como agora`() {
        assertEquals("agora", TempoRelativo.formatar(agora.minusSeconds(30), agora, zona))
    }

    @Test
    fun `minutos usam a unidade min`() {
        assertEquals("há 3 min", TempoRelativo.formatar(agora.minus(Duration.ofMinutes(3)), agora, zona))
        assertEquals("há 1 min", TempoRelativo.formatar(agora.minus(Duration.ofMinutes(1)), agora, zona))
    }

    @Test
    fun `horas usam a unidade h`() {
        assertEquals("há 2 h", TempoRelativo.formatar(agora.minus(Duration.ofHours(2)), agora, zona))
    }

    @Test
    fun `leitura de ontem mostra a hora`() {
        val ontem = Instant.parse("2026-11-09T22:20:00Z")

        assertEquals("ontem, 19:20", TempoRelativo.formatar(ontem, agora, zona))
    }

    @Test
    fun `dias passados usam dias no plural`() {
        assertEquals("há 3 dias", TempoRelativo.formatar(agora.minus(Duration.ofDays(3)), agora, zona))
        assertEquals("há 12 dias", TempoRelativo.formatar(agora.minus(Duration.ofDays(12)), agora, zona))
    }

    @Test
    fun `um dia usa o singular`() {
        val agoraCedo = Instant.parse("2026-11-10T00:30:00Z")
        val em = Instant.parse("2026-11-08T01:30:00Z")

        assertEquals("há 1 dia", TempoRelativo.formatar(em, agoraCedo, zona))
    }

    @Test
    fun `instante no futuro aparece como agora`() {
        assertEquals("agora", TempoRelativo.formatar(agora.plusSeconds(60), agora, zona))
    }
}
