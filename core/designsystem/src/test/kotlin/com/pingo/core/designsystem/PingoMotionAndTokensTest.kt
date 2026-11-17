package com.pingo.core.designsystem

import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.PingoInstitution
import com.pingo.core.designsystem.component.pingarFrameAt
import com.pingo.core.designsystem.theme.PingoMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PingoMotionAndTokensTest {
    @Test
    fun motionTokensMatchPrototypeSpec() {
        assertEquals(150, PingoMotion.ShortMs)
        assertEquals(250, PingoMotion.MediumMs)
        assertEquals(400, PingoMotion.LongMs)
        assertEquals(100, PingoMotion.ReducedFadeMs)
        assertEquals(PingoMotion.ShortMs, PingoMotion.SHORT)
        assertEquals(PingoMotion.MediumMs, PingoMotion.MEDIUM)
        assertEquals(PingoMotion.LongMs, PingoMotion.LONG)
        assertEquals(0.8f, PingoMotion.GentleSpring.dampingRatio)
        assertEquals(380f, PingoMotion.GentleSpring.stiffness)
        assertTrue(PingoMotion.StandardEasing.transform(0.5f) > 0.5f)
        assertTrue(PingoMotion.DecelerateEasing.transform(0.5f) > 0.5f)
        assertTrue(PingoMotion.AccelerateEasing.transform(0.5f) < 0.5f)
    }

    @Test
    fun pingarKeyframesMatchDropSquashWavesAndSlideIn() {
        val start = pingarFrameAt(0f)
        assertEquals(0f, start.dropAlpha, 0.001f)
        assertEquals(0f, start.contentAlpha, 0.001f)
        assertEquals(8f, start.contentOffsetYDp, 0.001f)

        val fall = pingarFrameAt(0.30f)
        assertEquals(1f, fall.dropAlpha, 0.001f)
        assertEquals(18f, fall.dropWidthDp, 0.001f)
        assertEquals(30f, fall.dropHeightDp, 0.001f)

        val impact = pingarFrameAt(0.50f)
        assertEquals(1f, impact.dropAlpha, 0.001f)
        assertEquals(30f, impact.dropWidthDp, 0.001f)
        assertEquals(18f, impact.dropHeightDp, 0.001f)
        assertEquals(44f, impact.wave1WidthDp, 0.001f)
        assertEquals(0.9f, impact.wave1Alpha, 0.001f)

        val waves = pingarFrameAt(0.75f)
        assertEquals(0f, waves.dropAlpha, 0.001f)
        assertEquals(170f, waves.wave1WidthDp, 0.001f)
        assertEquals(84f, waves.wave2WidthDp, 0.001f)
        assertEquals(0.8f, waves.wave2Alpha, 0.001f)
        assertEquals(0.7f, waves.contentAlpha, 0.001f)

        val end = pingarFrameAt(1f)
        assertEquals(0f, end.wave1Alpha, 0.001f)
        assertEquals(0f, end.wave2Alpha, 0.001f)
        assertEquals(1f, end.contentAlpha, 0.001f)
        assertEquals(0f, end.contentOffsetYDp, 0.001f)
    }

    @Test
    fun elevationLevelsUseDeepTealTint() {
        PingoElevationLevel.entries.forEach { level ->
            assertTrue(level.color.value != 0UL)
            assertEquals(11f / 255f, level.color.red, 0.01f)
            assertEquals(42f / 255f, level.color.green, 0.01f)
            assertEquals(51f / 255f, level.color.blue, 0.01f)
        }
    }

    @Test
    fun institutionLookupHandlesAllSupportedBanks() {
        assertEquals(PingoInstitution.Nubank, PingoInstitution.fromKey("NUBANK"))
        assertEquals(PingoInstitution.Inter, PingoInstitution.fromKey("inter"))
        assertEquals(PingoInstitution.Itau, PingoInstitution.fromKey("Itaú"))
        assertEquals(PingoInstitution.MercadoPago, PingoInstitution.fromKey("MERCADO_PAGO"))
        assertEquals(PingoInstitution.MercadoPago, PingoInstitution.fromKey("Mercado Pago"))
        assertEquals(PingoInstitution.Caju, PingoInstitution.fromKey("CAJU"))
        assertEquals(PingoInstitution.Outra, PingoInstitution.fromKey("desconhecido"))
        assertNull(PingoInstitution.Outra.containerColor)
    }
}
