package com.pingo.feature.onboarding.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
internal fun PermissaoItem(titulo: String, descricao: String, concedida: Boolean, acao: String?, onAcao: () -> Unit) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoCard(
        modifier = Modifier.pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        contentPadding = PingoSpacing.lg,
        gap = 0.dp,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EstadoDaPermissao(concedida = concedida)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                PingoText(
                    text = titulo,
                    style = PingoTheme.typography.bodyStrong,
                    color = PingoTheme.colors.onBackground,
                )
                PingoText(
                    text = descricao,
                    style = PingoTheme.typography.bodySmall,
                    color = if (concedida) PingoTheme.colors.onSurfaceVariant else PingoTheme.colors.onWarningContainer,
                )
            }

            if (acao != null && !concedida) {
                PingoButton(
                    text = acao,
                    onClick = onAcao,
                    type = PingoButtonType.Tonal,
                    size = PingoButtonSize.Small,
                )
            }
        }
    }
}

@Composable
private fun EstadoDaPermissao(concedida: Boolean) {
    val colors = PingoTheme.colors

    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = if (concedida) colors.incomeContainer else colors.warningContainer,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        PingoIcon(
            imageVector = if (concedida) PingoIcons.CircleCheck else PingoIcons.TriangleAlert,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (concedida) colors.income else colors.warning,
        )
    }
}
