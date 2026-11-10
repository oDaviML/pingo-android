package com.pingo.core.parser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class DataHoraDoLancamentoTest {
    private val fuso = ZoneId.of("America/Sao_Paulo")
    private val postTime = Instant.parse("2026-11-10T13:54:48Z")

    @Test
    fun `data e hora embutidas do Itau usam o ano e o fuso do postTime`() {
        assertEquals(
            Instant.parse("2026-11-10T03:55:00Z"),
            DataHoraDoLancamento.resolver("10/11 às 00:55", postTime, fuso),
        )
    }

    @Test
    fun `data completa embutida e preservada`() {
        assertEquals(
            Instant.parse("2026-10-23T03:00:00Z"),
            DataHoraDoLancamento.resolver("23/10/2026", Instant.parse("2026-10-23T15:00:00Z"), fuso),
        )
    }

    @Test
    fun `data completa com hora embutida e preservada`() {
        assertEquals(
            Instant.parse("2026-10-23T17:30:00Z"),
            DataHoraDoLancamento.resolver("23/10/2026 às 14:30", Instant.parse("2026-10-24T12:00:00Z"), fuso),
        )
    }

    @Test
    fun `data sem ano na virada do ano usa o ano anterior`() {
        assertEquals(
            Instant.parse("2027-01-01T02:50:00Z"),
            DataHoraDoLancamento.resolver("31/12 às 23:50", Instant.parse("2027-01-01T03:10:00Z"), fuso),
        )
    }

    @Test
    fun `data do mesmo ano do postTime e mantida`() {
        assertEquals(
            Instant.parse("2026-11-05T23:44:00Z"),
            DataHoraDoLancamento.resolver("05/11 às 20:44", postTime, fuso),
        )
    }

    @Test
    fun `data logo depois da meia noite ainda usa o ano corrente`() {
        assertEquals(
            Instant.parse("2026-11-01T03:05:00Z"),
            DataHoraDoLancamento.resolver("01/11 às 00:05", Instant.parse("2026-11-01T02:30:00Z"), fuso),
        )
    }

    @Test
    fun `sem data embutida vale o postTime`() {
        assertEquals(postTime, DataHoraDoLancamento.resolver(null, postTime, fuso))
        assertEquals(postTime, DataHoraDoLancamento.resolver("", postTime, fuso))
    }

    @Test
    fun `data invalida vale o postTime`() {
        assertEquals(postTime, DataHoraDoLancamento.resolver("31/02 às 10:00", postTime, fuso))
        assertEquals(postTime, DataHoraDoLancamento.resolver("data qualquer", postTime, fuso))
    }
}
