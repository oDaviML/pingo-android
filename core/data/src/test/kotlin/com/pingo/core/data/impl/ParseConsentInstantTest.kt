package com.pingo.core.data.impl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Instant

class ParseConsentInstantTest {
    @Test
    fun `usa o instante informado quando a data vem com offset`() {
        assertEquals(Instant.parse("2026-10-23T10:00:00Z"), parseConsentInstant("2026-10-23T10:00:00Z"))
    }

    @Test
    fun `data sem offset nao quebra o consentimento e usa o instante atual`() {
        val antes = Instant.now()

        val instante = parseConsentInstant("2026-10-23T10:00:00")

        assertFalse(instante.isBefore(antes))
        assertFalse(instante.isAfter(Instant.now()))
    }

    @Test
    fun `data ausente usa o instante atual`() {
        val antes = Instant.now()

        val instante = parseConsentInstant(null)

        assertFalse(instante.isBefore(antes))
        assertFalse(instante.isAfter(Instant.now()))
    }
}
