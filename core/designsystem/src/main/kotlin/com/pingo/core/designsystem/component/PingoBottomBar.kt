package com.pingo.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoMotion
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.designsystem.theme.rememberReducedMotion

private val AddDropShape = RoundedCornerShape(
    topStart = 6.dp,
    topEnd = 26.dp,
    bottomEnd = 26.dp,
    bottomStart = 26.dp,
)

@Composable
fun PingoBottomBar(
    selecionada: AbaInferior,
    onSelecionar: (AbaInferior) -> Unit,
    onNovo: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pingoElevation(
                level = PingoElevationLevel.Level2,
                offsetY = (-4).dp,
            )
            .background(PingoTheme.colors.surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PingoTheme.colors.outlineVariant),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PingoSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AbaInferior.entries.forEach { aba ->
                if (onNovo != null && aba == AbaInferior.CONTAS) {
                    AddDrop(onClick = onNovo)
                }
                AbaItem(
                    aba = aba,
                    selecionada = aba == selecionada,
                    onClick = { onSelecionar(aba) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AbaItem(aba: AbaInferior, selecionada: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cores = PingoTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()

    Column(
        modifier = modifier
            .heightIn(min = PingoLayout.minTouchTarget)
            .testTag("aba-${aba.name}")
            .selectable(
                selected = selecionada,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(top = PingoSpacing.md, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
    ) {
        AbaPill(
            aba = aba,
            selecionada = selecionada,
            highlighted = pressed || hovered,
        )
        PingoText(
            text = aba.rotulo,
            style = if (selecionada) {
                PingoTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
            } else {
                PingoTheme.typography.labelSmall
            },
            color = if (selecionada) cores.primary else cores.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AbaPill(aba: AbaInferior, selecionada: Boolean, highlighted: Boolean) {
    val cores = PingoTheme.colors
    val reducedMotion = rememberReducedMotion()
    val pillColor by animateColorAsState(
        targetValue = when {
            selecionada -> cores.primaryContainer
            highlighted -> cores.surfaceVariant.copy(alpha = 0.55f)
            else -> Color.Transparent
        },
        animationSpec = if (reducedMotion) {
            snap()
        } else {
            tween(durationMillis = PingoMotion.ShortMs, easing = PingoMotion.StandardEasing)
        },
        label = "aba-pill-color",
    )

    Box(
        modifier = Modifier
            .width(56.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(PingoRadius.full))
            .background(color = pillColor, shape = RoundedCornerShape(PingoRadius.full)),
        contentAlignment = Alignment.Center,
    ) {
        PingoIcon(
            imageVector = iconeDaAba(aba),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = if (selecionada) cores.onPrimaryContainer else cores.onSurfaceVariant,
        )
    }
}

@Composable
private fun AddDrop(onClick: () -> Unit) {
    val cores = PingoTheme.colors
    val reducedMotion = rememberReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = if (reducedMotion) snap() else PingoMotion.gentleSpring(),
        label = "add-drop-scale",
    )

    Box(
        modifier = Modifier
            .width(64.dp)
            .height(72.dp)
            .testTag("novo-lancamento")
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AddDropSurface(
            scale = scale,
            backgroundColor = if (pressed || hovered) cores.primaryPressed else cores.primary,
            iconTint = cores.onPrimary,
        )
    }
}

@Composable
private fun AddDropSurface(scale: Float, backgroundColor: Color, iconTint: Color) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = 45f
            }
            .pingoElevation(
                level = PingoElevationLevel.Level2,
                shape = AddDropShape,
                offsetX = 2.83.dp,
                offsetY = 2.83.dp,
            )
            .clip(AddDropShape)
            .background(color = backgroundColor, shape = AddDropShape),
        contentAlignment = Alignment.Center,
    ) {
        PingoIcon(
            imageVector = PingoIcons.Plus,
            contentDescription = "Novo lançamento",
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = -45f },
            tint = iconTint,
        )
    }
}

private fun iconeDaAba(aba: AbaInferior): ImageVector = when (aba) {
    AbaInferior.INICIO -> PingoIcons.House
    AbaInferior.LANCAMENTOS -> PingoIcons.Receipt
    AbaInferior.CONTAS -> PingoIcons.Wallet
    AbaInferior.RELATORIOS -> PingoIcons.ChartPie
}
