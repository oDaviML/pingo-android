package com.pingo.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private fun pathOf(path: String) = PathParser().parsePathString(path).toNodes()

private fun strokeIcon(name: String, vararg paths: String): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )

    for (path in paths) {
        builder.addPath(
            pathData = pathOf(path),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }

    return builder.build()
}

object PingoIcons {
    val Lock: ImageVector by lazy {
        strokeIcon(
            "Lock",
            "M5 11H19A2 2 0 0 1 21 13V20A2 2 0 0 1 19 22H5A2 2 0 0 1 3 20V13A2 2 0 0 1 5 11Z",
            "M7 11V7A5 5 0 0 1 17 7V11",
        )
    }

    val Check: ImageVector by lazy { strokeIcon("Check", "M20 6 9 17l-5-5") }

    val ChevronRight: ImageVector by lazy { strokeIcon("ChevronRight", "m9 18 6-6-6-6") }

    val ArrowLeft: ImageVector by lazy {
        strokeIcon("ArrowLeft", "m12 19-7-7 7-7", "M19 12H5")
    }

    val Trash2: ImageVector by lazy {
        strokeIcon(
            "Trash2",
            "M3 6h18",
            "M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6",
            "M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2",
            "M10 11v6",
            "M14 11v6",
        )
    }

    val FileText: ImageVector by lazy {
        strokeIcon(
            "FileText",
            "M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z",
            "M14 2v4a2 2 0 0 0 2 2h4",
            "M10 9H8",
            "M16 13H8",
            "M16 17H8",
        )
    }

    val ShieldCheck: ImageVector by lazy {
        strokeIcon(
            "ShieldCheck",
            "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1" +
                "c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z",
            "m9 12 2 2 4-4",
        )
    }

    val Smartphone: ImageVector by lazy {
        strokeIcon(
            "Smartphone",
            "M7 2H17A2 2 0 0 1 19 4V20A2 2 0 0 1 17 22H7A2 2 0 0 1 5 20V4A2 2 0 0 1 7 2Z",
            "M12 18h.01",
        )
    }

    val TriangleAlert: ImageVector by lazy {
        strokeIcon(
            "TriangleAlert",
            "M21.73 18 13.73 4a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z",
            "M12 9v4",
            "M12 17h.01",
        )
    }

    val Search: ImageVector by lazy {
        strokeIcon("Search", "M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16Z", "m21 21-4.35-4.35")
    }

    val SlidersHorizontal: ImageVector by lazy {
        strokeIcon("SlidersHorizontal", "M4 6h16", "M4 12h16", "M4 18h16", "M9 3v6", "M15 9v6", "M7 15v6")
    }

    val Home: ImageVector by lazy {
        strokeIcon("Home", "M3 10.5 12 3l9 7.5", "M5 9.5V21h14V9.5", "M10 21v-6h4v6")
    }

    val Receipt: ImageVector by lazy {
        strokeIcon("Receipt", "M5 3h14v18l-3-2-3 2-3-2-3 2Z", "M9 8h6", "M9 12h6")
    }

    val Wallet: ImageVector by lazy {
        strokeIcon(
            "Wallet",
            "M4 7V5a2 2 0 0 1 2-2h11v4",
            "M3 7h17v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2Z",
            "M16 12h3v4h-3a2 2 0 0 1 0-4Z",
        )
    }

    val PieChart: ImageVector by lazy {
        strokeIcon("PieChart", "M21.2 15.9A10 10 0 1 1 8.1 2.8", "M22 12A10 10 0 0 0 12 2v10Z")
    }

    val Calendar: ImageVector by lazy {
        strokeIcon(
            "Calendar",
            "M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z",
            "M3 10h18",
            "M8 3v4",
            "M16 3v4",
        )
    }

    val ChevronDown: ImageVector by lazy { strokeIcon("ChevronDown", "m6 9 6 6 6-6") }

    val X: ImageVector by lazy { strokeIcon("X", "M18 6 6 18", "m6 6 12 12") }

    val Plus: ImageVector by lazy { strokeIcon("Plus", "M12 5v14", "M5 12h14") }

    val Landmark: ImageVector by lazy {
        strokeIcon(
            "Landmark",
            "M3 22h18",
            "M6 18V10",
            "M10 18V10",
            "M14 18V10",
            "M18 18V10",
            "M12 2 3 8h18Z",
        )
    }

    val Utensils: ImageVector by lazy {
        strokeIcon(
            "Utensils",
            "M4 3v7a2 2 0 0 0 2 2h2a2 2 0 0 0 2-2V3",
            "M7 12v9",
            "M17 3c-1.5 2-2 4-2 6 0 1.5.8 2.5 2 2.5s2-1 2-2.5c0-2-.5-4-2-6Z",
            "M17 11.5V21",
        )
    }

    val ShoppingCart: ImageVector by lazy {
        strokeIcon("ShoppingCart", "M3 4h2l2.5 11h11L21 7H6", "M9 19h.01", "M18 19h.01")
    }

    val CarFront: ImageVector by lazy {
        strokeIcon(
            "CarFront",
            "M4 17l1.5-6A3 3 0 0 1 8.4 9h7.2a3 3 0 0 1 2.9 2L20 17v3H4v-3Z",
            "M5 17h14",
            "M7 13h.01",
            "M17 13h.01",
            "M7 20v2",
            "M17 20v2",
        )
    }

    val HeartPulse: ImageVector by lazy {
        strokeIcon(
            "HeartPulse",
            "M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8Z",
            "M3 12h4l2-3 4 6 2-3h6",
        )
    }

    val Gamepad: ImageVector by lazy {
        strokeIcon(
            "Gamepad",
            "M6 8h12a4 4 0 0 1 4 4v1a4 4 0 0 1-4 4c-1.2 0-2.3-.6-3-1.5L13 13h-2l-2 2.5" +
                "C8.3 16.4 7.2 17 6 17a4 4 0 0 1-4-4v-1a4 4 0 0 1 4-4Z",
            "M8 11v2",
            "M7 12h2",
            "M16 12h.01",
            "M18 12h.01",
        )
    }

    val ShoppingBag: ImageVector by lazy {
        strokeIcon("ShoppingBag", "M6 7h12l1 14H5L6 7Z", "M9 7V5a3 3 0 0 1 6 0v2")
    }

    val Repeat: ImageVector by lazy {
        strokeIcon("Repeat", "m17 2 4 4-4 4", "M3 11V9a4 4 0 0 1 4-4h14", "m7 22-4-4 4-4", "M21 13v2a4 4 0 0 1-4 4H3")
    }

    val GraduationCap: ImageVector by lazy {
        strokeIcon(
            "GraduationCap",
            "M22 10 12 5 2 10l10 5 10-5Z",
            "M6 12v5c0 1 2.7 2.5 6 2.5s6-1.5 6-2.5v-5",
            "M22 10v6",
        )
    }

    val Tag: ImageVector by lazy {
        strokeIcon("Tag", "M20.6 13.4 12 22l-9-9V3h10l7.6 7.6a2 2 0 0 1 0 2.8Z", "M7 7h.01")
    }

    val Banknote: ImageVector by lazy {
        strokeIcon("Banknote", "M2 6h20v12H2Z", "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z", "M6 12h.01", "M18 12h.01")
    }

    val Gift: ImageVector by lazy {
        strokeIcon(
            "Gift",
            "M20 12v9H4v-9",
            "M2 7h20v5H2Z",
            "M12 22V7",
            "M12 7H7.5a2.5 2.5 0 1 1 0-5C11 2 12 7 12 7Z",
            "M12 7h4.5a2.5 2.5 0 1 0 0-5C13 2 12 7 12 7Z",
        )
    }

    val RotateCcw: ImageVector by lazy {
        strokeIcon("RotateCcw", "M3 12a9 9 0 1 0 3-6.7L3 8", "M3 3v5h5")
    }

    val CircleAlert: ImageVector by lazy {
        strokeIcon("CircleAlert", "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20Z", "M12 8v4", "M12 16h.01")
    }

    val ArrowRightLeft: ImageVector by lazy {
        strokeIcon("ArrowRightLeft", "m8 3-4 4 4 4", "M4 7h16", "m16 21 4-4-4-4", "M20 17H4")
    }

    val Undo: ImageVector by lazy {
        strokeIcon("Undo", "M9 14 4 9l5-5", "M4 9h10a6 6 0 0 1 0 12h-3")
    }

    val Backspace: ImageVector by lazy {
        strokeIcon("Backspace", "M21 4H8l-7 8 7 8h13a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2Z", "m18 9-6 6", "m12 9 6 6")
    }
}
