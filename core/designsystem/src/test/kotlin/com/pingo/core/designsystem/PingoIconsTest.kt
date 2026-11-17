package com.pingo.core.designsystem

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.icon.PingoIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PingoIconsTest {
    @Test
    fun allDeclaredIconsParseCleanlyWithLucideViewport() {
        val getters = PingoIcons::class.java.methods
            .filter { it.name.startsWith("get") && it.parameterCount == 0 && it.returnType == ImageVector::class.java }

        assertTrue(getters.size >= 85)

        getters.forEach { method ->
            val vector = method.invoke(PingoIcons) as ImageVector
            assertEquals("${method.name} width", 24.dp, vector.defaultWidth)
            assertEquals("${method.name} height", 24.dp, vector.defaultHeight)
            assertEquals("${method.name} viewportWidth", 24f, vector.viewportWidth)
            assertEquals("${method.name} viewportHeight", 24f, vector.viewportHeight)
            assertTrue("${method.name} should have vector paths", vector.root.size > 0)
        }
    }

    @Test
    fun tagIconIncludesFilledCircleDot() {
        val paths = collectPaths(PingoIcons.Tag.root)
        assertTrue(paths.any { it.fill != null })
        assertTrue(paths.any { it.stroke != null })
    }

    @Test
    fun compatibilityAliasesMatchOfficialIcons() {
        assertEquals(PingoIcons.House, PingoIcons.Home)
        assertEquals(PingoIcons.ChartPie, PingoIcons.PieChart)
        assertEquals(PingoIcons.Car, PingoIcons.CarFront)
        assertEquals(PingoIcons.Gamepad2, PingoIcons.Gamepad)
        assertEquals(PingoIcons.ArrowLeftRight, PingoIcons.ArrowRightLeft)
        assertEquals(PingoIcons.Undo2, PingoIcons.Undo)
        assertEquals(PingoIcons.Delete, PingoIcons.Backspace)
        assertEquals(PingoIcons.CircleCheck, PingoIcons.CheckCircle)
        assertEquals(PingoIcons.SlidersHorizontal, PingoIcons.Sliders)
    }

    private fun collectPaths(group: VectorGroup): List<VectorPath> {
        val out = mutableListOf<VectorPath>()
        for (node in group) {
            when (node) {
                is VectorPath -> out += node
                is VectorGroup -> out += collectPaths(node)
            }
        }
        return out
    }
}
