package com.pingo.spike.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CorpusCodecTest {
    @Test
    fun roundTripPreservesFields() {
        val entry =
            CorpusEntry(
                packageName = "com.nu.production",
                postedAt = 1_700_000_000_000,
                capturedAt = 1_700_000_001_000,
                title = "Compra aprovada",
                text = "R\$ 42,90\tno Mercado",
                bigText = "linha 1\nlinha 2",
            )

        assertEquals(entry, CorpusCodec.decode(CorpusCodec.encode(entry)))
    }

    @Test
    fun roundTripPreservesEmptyFields() {
        val entry =
            CorpusEntry(
                packageName = "com.caju.employeeApp",
                postedAt = 1,
                capturedAt = 2,
                title = "",
                text = "",
                bigText = "",
            )

        assertEquals(entry, CorpusCodec.decode(CorpusCodec.encode(entry)))
    }

    @Test
    fun decodeReturnsNullForMalformedLine() {
        assertNull(CorpusCodec.decode("lixo"))
    }

    @Test
    fun decodeReturnsNullForInvalidNumber() {
        assertNull(CorpusCodec.decode("a\tb\tbX\tb\tb\tb"))
    }
}
