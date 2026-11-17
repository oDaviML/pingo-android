package com.pingo.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.PingoMascot
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
fun EstadoVazioCard(
    titulo: String,
    descricao: String,
    modifier: Modifier = Modifier,
    acaoTexto: String? = null,
    onAcao: (() -> Unit)? = null,
    acaoModifier: Modifier = Modifier,
) {
    PingoCard(
        modifier = modifier
            .fillMaxWidth()
            .pingoElevation(level = PingoElevationLevel.Level1, shape = RoundedCornerShape(PingoRadius.xl)),
        contentPadding = PingoSpacing.xxl,
        gap = PingoSpacing.md,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        ) {
            IlustracaoVaziaComOndas()
            PingoText(
                text = titulo,
                style = PingoTheme.typography.title,
                color = PingoTheme.colors.onBackground,
                textAlign = TextAlign.Center,
            )
            PingoText(
                text = descricao,
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (acaoTexto != null && onAcao != null) {
                PingoButton(
                    text = acaoTexto,
                    onClick = onAcao,
                    type = PingoButtonType.Tonal,
                    modifier = acaoModifier.padding(top = PingoSpacing.xs),
                )
            }
        }
    }
}

@Composable
private fun IlustracaoVaziaComOndas() {
    val corOndaExterna = PingoTheme.colors.secondary.copy(alpha = 0.45f)
    val corOndaInterna = PingoTheme.colors.secondary.copy(alpha = 0.7f)

    Box(
        modifier = Modifier
            .size(width = 148.dp, height = 104.dp)
            .padding(bottom = PingoSpacing.xs),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(width = 144.dp, height = 30.dp),
        ) {
            val espessura = 2.dp.toPx()
            drawOval(
                color = corOndaExterna,
                topLeft = Offset(espessura / 2f, espessura / 2f),
                size = Size(size.width - espessura, size.height - espessura),
                style = Stroke(width = espessura),
            )
            val larguraInterna = size.width * 0.62f
            val alturaInterna = size.height * 0.6f
            drawOval(
                color = corOndaInterna,
                topLeft = Offset((size.width - larguraInterna) / 2f, (size.height - alturaInterna) / 2f),
                size = Size(larguraInterna, alturaInterna),
                style = Stroke(width = espessura),
            )
        }
        PingoMascot(width = 64.dp)
    }
}
