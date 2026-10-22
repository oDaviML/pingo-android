package com.pingo.spike.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KnownAppsTest {
    @Test
    fun recognizesTheFiveInstitutions() {
        assertEquals(5, KnownApps.packages.size)
        assertTrue(KnownApps.isKnown("com.nu.production"))
        assertTrue(KnownApps.isKnown("br.com.intermedium"))
        assertTrue(KnownApps.isKnown("br.com.itau.internet"))
        assertTrue(KnownApps.isKnown("com.mercadopago.wallet"))
        assertTrue(KnownApps.isKnown("com.caju.employeeApp"))
    }

    @Test
    fun ignoresUnknownPackage() {
        assertFalse(KnownApps.isKnown("com.whatsapp"))
    }

    @Test
    fun displayNameFallsBackToPackage() {
        assertEquals("Nubank", KnownApps.displayName("com.nu.production"))
        assertEquals("com.whatsapp", KnownApps.displayName("com.whatsapp"))
    }
}
