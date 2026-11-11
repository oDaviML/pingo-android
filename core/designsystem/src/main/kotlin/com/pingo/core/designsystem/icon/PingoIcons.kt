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
}
