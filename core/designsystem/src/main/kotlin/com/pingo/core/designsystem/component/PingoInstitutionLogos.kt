package com.pingo.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

object PingoInstitutionLogos {
    private fun pathOf(path: String) = PathParser().parsePathString(path).toNodes()

    val nubank: ImageVector by lazy {
        ImageVector.Builder(
            name = "LogoNubank",
            defaultWidth = 40.dp,
            defaultHeight = 40.dp,
            viewportWidth = 40f,
            viewportHeight = 40f,
        )
            .addPath(
                pathData = pathOf("M8.5 26 V15.2 C8.5 13.4 11.4 13.2 12.6 14.8 L16.4 20.2"),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .addPath(
                pathData = pathOf("M11.6 19.8 L15.4 25.2 C16.6 26.8 19.5 26.6 19.5 24.8 V14"),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .addPath(
                pathData = pathOf("M22.8 14 V22.5 C22.8 25.2 24.7 26.5 27.2 26.5 C29.7 26.5 31.5 25.2 31.5 22.5 V14"),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .build()
    }

    val inter: ImageVector by lazy {
        ImageVector.Builder(
            name = "LogoInter",
            defaultWidth = 40.dp,
            defaultHeight = 40.dp,
            viewportWidth = 40f,
            viewportHeight = 40f,
        )
            .addPath(
                pathData = pathOf("M7.0 14.5 A1.2 1.2 0 1 0 9.4 14.5 A1.2 1.2 0 1 0 7.0 14.5 Z"),
                fill = SolidColor(Color.White),
            )
            .addPath(
                pathData = pathOf("M8.2 17.5 V25"),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
            )
            .addPath(
                pathData = pathOf("M11.2 25 V19.5 C11.2 17.8 12.6 17.2 14.0 17.2 C15.4 17.2 16.3 18.0 16.3 19.5 V25"),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .addPath(
                pathData = pathOf("M19.1 14.5 V23.2 C19.1 24.6 20.0 25.2 21.3 25"),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
            )
            .addPath(
                pathData = pathOf("M17.8 17.6 H21.1"),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
            )
            .addPath(
                pathData = pathOf(
                    "M23.0 21.2 H27.7 C27.7 18.5 26.2 17.2 24.8 17.2 C23.1 17.2 22.3 18.8 22.3 21.2 " +
                        "C22.3 23.8 23.4 25.2 25.3 25.2 C26.4 25.2 27.2 24.8 27.6 24.2",
                ),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .addPath(
                pathData = pathOf("M30.1 25 V19.6 C30.1 18.1 31.3 17.3 33.0 17.3"),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
            )
            .build()
    }

    val itau: ImageVector by lazy {
        val navy = Color(0xFF003399)
        val yellow = Color(0xFFFFCC00)
        ImageVector.Builder(
            name = "LogoItau",
            defaultWidth = 40.dp,
            defaultHeight = 40.dp,
            viewportWidth = 40f,
            viewportHeight = 40f,
        )
            .addPath(
                pathData = pathOf("M12 8 H28 A4 4 0 0 1 32 12 V28 A4 4 0 0 1 28 32 H12 A4 4 0 0 1 8 28 V12 A4 4 0 0 1 12 8 Z"),
                fill = SolidColor(navy),
            )
            .addPath(
                pathData = pathOf("M11.8 16.0 V24.0"),
                fill = null,
                stroke = SolidColor(yellow),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
            )
            .addPath(
                pathData = pathOf("M15.1 14.5 V22.4 C15.1 23.7 15.8 24.1 16.8 24.0"),
                fill = null,
                stroke = SolidColor(yellow),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
            )
            .addPath(
                pathData = pathOf("M14.0 17.2 H16.6"),
                fill = null,
                stroke = SolidColor(yellow),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
            )
            .addPath(
                pathData = pathOf(
                    "M21.3 20.2 C21.3 18.5 20.1 17.6 18.9 17.6 C17.9 17.6 17.5 18.2 17.5 18.2 " +
                        "M21.3 20.2 C21.3 22.7 20.3 24.1 18.8 24.1 C17.8 24.1 17.3 23.3 17.3 22.3 " +
                        "C17.3 21.2 18.3 20.6 21.3 20.6 V24.0",
                ),
                fill = null,
                stroke = SolidColor(yellow),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .addPath(
                pathData = pathOf("M23.7 17.8 V21.8 C23.7 23.3 24.7 24.1 25.9 24.1 C27.1 24.1 27.9 23.3 27.9 21.8 V17.8"),
                fill = null,
                stroke = SolidColor(yellow),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .addPath(
                pathData = pathOf("M25.4 15.8 L26.9 14.3"),
                fill = null,
                stroke = SolidColor(yellow),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
            )
            .build()
    }

    val mercadoPago: ImageVector by lazy {
        ImageVector.Builder(
            name = "LogoMercadoPago",
            defaultWidth = 40.dp,
            defaultHeight = 40.dp,
            viewportWidth = 40f,
            viewportHeight = 40f,
        )
            .addPath(
                pathData = pathOf("M20 11 C28.5 11 34 15.0 34 20 C34 25.0 28.5 29 20 29 C11.5 29 6 25.0 6 20 C6 15.0 11.5 11 20 11 Z"),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .addPath(
                pathData = pathOf("M7.5 18.5 L12.5 16.5 L16.8 19.2"),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .addPath(
                pathData = pathOf("M32.5 18.5 L27.5 16.5 L22.5 19.0 L18.5 16.8 L14.5 19.0"),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .addPath(
                pathData = pathOf("M14.5 19.0 L20.0 23.8 L26.5 19.0"),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .addPath(
                pathData = pathOf("M17.5 21.6 L19.8 19.8 M20.2 23.8 L22.6 21.6"),
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
            )
            .build()
    }

    val caju: ImageVector by lazy {
        ImageVector.Builder(
            name = "LogoCaju",
            defaultWidth = 40.dp,
            defaultHeight = 40.dp,
            viewportWidth = 40f,
            viewportHeight = 40f,
        )
            .addPath(
                pathData = pathOf(
                    "M24.5 14.5 C18.5 13.5 13 17.5 13 22.5 C13 26.8 16.8 30 21.2 29.5 C25.0 29.0 27.5 26.2 26.5 23.8 " +
                        "C25.7 22.0 23.2 22.2 21.8 23.2 C20.4 24.2 18.5 23.6 18.2 21.8 C17.9 19.8 19.8 18.2 22.2 18.2 " +
                        "C24.2 18.2 26.5 17.2 26.5 15.5 C26.5 14.9 25.6 14.6 24.5 14.5 Z",
                ),
                fill = SolidColor(Color.White),
            )
            .addPath(
                pathData = pathOf("M21.2 12.8 C22.5 10.2 25.2 9.5 27.0 10.2 C26.5 12.2 24.0 13.4 21.2 12.8 Z"),
                fill = SolidColor(Color(0xFFB9F6CA)),
            )
            .build()
    }

    fun para(institution: PingoInstitution): ImageVector? = when (institution) {
        PingoInstitution.Nubank -> nubank
        PingoInstitution.Inter -> inter
        PingoInstitution.Itau -> itau
        PingoInstitution.MercadoPago -> mercadoPago
        PingoInstitution.Caju -> caju
        PingoInstitution.Outra -> null
    }
}
