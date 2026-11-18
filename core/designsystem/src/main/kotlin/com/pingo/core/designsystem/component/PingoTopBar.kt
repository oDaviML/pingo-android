package com.pingo.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

private const val TOP_BAR_DROP_PATH = "M17 1 C17 1 33 19 33 26 A16 16 0 1 1 1 26 C1 19 17 1 17 1 Z"

enum class PingoIconButtonStyle {
    Filled,
    Tonal,
    Outline,
    Standard,
    Surface,
}

private data class IconButtonAppearance(val container: Color, val tint: Color, val border: Color?)

@Composable
fun PingoIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PingoIconButtonStyle = PingoIconButtonStyle.Standard,
    enabled: Boolean = true,
    showBadgeDot: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val appearance = iconButtonAppearance(style = style, enabled = enabled, highlighted = pressed || hovered)
    val shape = RoundedCornerShape(PingoRadius.md)

    Box(
        modifier = modifier
            .size(PingoLayout.minTouchTarget)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        IconButtonSurface(
            icon = icon,
            contentDescription = contentDescription,
            appearance = appearance,
            shape = shape,
            showBadgeDot = showBadgeDot,
        )
    }
}

@Composable
private fun IconButtonSurface(
    icon: ImageVector,
    contentDescription: String?,
    appearance: IconButtonAppearance,
    shape: RoundedCornerShape,
    showBadgeDot: Boolean,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(shape)
            .background(color = appearance.container, shape = shape)
            .then(
                if (appearance.border == null) {
                    Modifier
                } else {
                    Modifier.border(width = 1.5.dp, color = appearance.border, shape = shape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        PingoIcon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp),
            tint = appearance.tint,
        )
        if (showBadgeDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 8.dp)
                    .size(8.dp)
                    .background(color = PingoTheme.colors.expense, shape = CircleShape),
            )
        }
    }
}

@Composable
private fun iconButtonAppearance(style: PingoIconButtonStyle, enabled: Boolean, highlighted: Boolean): IconButtonAppearance {
    if (!enabled) return disabledIconButtonAppearance(style)
    val colors = PingoTheme.colors
    return when (style) {
        PingoIconButtonStyle.Filled -> IconButtonAppearance(
            container = if (highlighted) colors.primaryPressed else colors.primary,
            tint = colors.onPrimary,
            border = null,
        )
        PingoIconButtonStyle.Tonal -> IconButtonAppearance(
            container = if (highlighted) colors.secondary else colors.primaryContainer,
            tint = colors.onPrimaryContainer,
            border = null,
        )
        PingoIconButtonStyle.Outline -> IconButtonAppearance(
            container = if (highlighted) colors.surfaceVariant.copy(alpha = 0.6f) else Color.Transparent,
            tint = colors.onBackground,
            border = colors.outline,
        )
        PingoIconButtonStyle.Surface -> IconButtonAppearance(
            container = if (highlighted) colors.surfaceVariant else colors.surface,
            tint = colors.onBackground,
            border = null,
        )
        PingoIconButtonStyle.Standard -> IconButtonAppearance(
            container = if (highlighted) colors.surfaceVariant.copy(alpha = 0.6f) else Color.Transparent,
            tint = colors.onBackground,
            border = null,
        )
    }
}

@Composable
private fun disabledIconButtonAppearance(style: PingoIconButtonStyle): IconButtonAppearance {
    val colors = PingoTheme.colors
    val transparent = style == PingoIconButtonStyle.Outline || style == PingoIconButtonStyle.Standard
    return IconButtonAppearance(
        container = if (transparent) Color.Transparent else colors.disabledContainer,
        tint = colors.onDisabled,
        border = if (style == PingoIconButtonStyle.Outline) colors.outlineVariant else null,
    )
}

@Composable
fun PingoTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actionIcon: ImageVector? = null,
    actionContentDescription: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = PingoSpacing.sm, vertical = PingoSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoIconButton(
            icon = PingoIcons.ArrowLeft,
            contentDescription = "Voltar",
            onClick = onBack,
            style = PingoIconButtonStyle.Standard,
        )
        PingoText(
            text = title,
            style = PingoTheme.typography.subtitle,
            color = PingoTheme.colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (actionIcon != null && onAction != null) {
            PingoIconButton(
                icon = actionIcon,
                contentDescription = actionContentDescription,
                onClick = onAction,
                style = PingoIconButtonStyle.Standard,
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
fun PingoHomeTopBar(
    greeting: String,
    name: String,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasUnreadNotifications: Boolean = false,
    hasUpdateAvailable: Boolean = false,
    onUpdateClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PingoSpacing.xl, end = PingoSpacing.lg, top = PingoSpacing.md, bottom = PingoSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeDropMark()
        HomeTopBarTitles(greeting = greeting, name = name, modifier = Modifier.weight(1f))
        HomeTopBarActions(
            hasUnreadNotifications = hasUnreadNotifications,
            hasUpdateAvailable = hasUpdateAvailable,
            onNotificationsClick = onNotificationsClick,
            onUpdateClick = onUpdateClick,
            onSettingsClick = onSettingsClick,
        )
    }
}

@Composable
private fun HomeTopBarTitles(greeting: String, name: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        PingoText(
            text = greeting,
            style = PingoTheme.typography.caption,
            color = PingoTheme.colors.onSurfaceVariant,
            maxLines = 1,
        )
        PingoText(
            text = name,
            style = PingoTheme.typography.title,
            color = PingoTheme.colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HomeTopBarActions(
    hasUnreadNotifications: Boolean,
    hasUpdateAvailable: Boolean,
    onNotificationsClick: () -> Unit,
    onUpdateClick: (() -> Unit)?,
    onSettingsClick: () -> Unit,
) {
    PingoIconButton(
        icon = PingoIcons.Bell,
        contentDescription = "Notificações",
        onClick = onNotificationsClick,
        style = PingoIconButtonStyle.Surface,
        showBadgeDot = hasUnreadNotifications,
    )
    if (onUpdateClick != null) {
        PingoIconButton(
            icon = PingoIcons.Download,
            contentDescription = if (hasUpdateAvailable) "Atualização disponível" else "Atualizações do aplicativo",
            onClick = onUpdateClick,
            style = if (hasUpdateAvailable) PingoIconButtonStyle.Tonal else PingoIconButtonStyle.Surface,
            showBadgeDot = hasUpdateAvailable,
        )
    }
    PingoIconButton(
        icon = PingoIcons.Settings,
        contentDescription = "Configurações",
        onClick = onSettingsClick,
        style = PingoIconButtonStyle.Surface,
    )
}

@Composable
fun PingoLargeTopBar(title: String, modifier: Modifier = Modifier, actions: (@Composable RowScope.() -> Unit)? = null) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PingoSpacing.xl, end = PingoSpacing.xl, top = PingoSpacing.md, bottom = PingoSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
    ) {
        if (actions != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
        PingoText(
            text = title,
            style = PingoTheme.typography.display,
            color = PingoTheme.colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PingoModalTopBar(
    title: String,
    onClose: () -> Unit,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionEnabled: Boolean = true,
    actionLoading: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PingoSpacing.sm, end = PingoSpacing.md, top = PingoSpacing.sm, bottom = PingoSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoIconButton(
            icon = PingoIcons.X,
            contentDescription = "Fechar",
            onClick = onClose,
            style = PingoIconButtonStyle.Standard,
        )
        PingoText(
            text = title,
            style = PingoTheme.typography.subtitle,
            color = PingoTheme.colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        PingoButton(
            text = actionLabel,
            onClick = onAction,
            type = PingoButtonType.Primary,
            size = PingoButtonSize.Small,
            enabled = actionEnabled,
            loading = actionLoading,
        )
    }
}

@Composable
private fun HomeDropMark(modifier: Modifier = Modifier) {
    val basePath = remember { PathParser().parsePathString(TOP_BAR_DROP_PATH).toNodes().toPath() }
    val gradient = remember {
        listOf(
            Color(0xFF1AA7BC),
            Color(0xFF088395),
            Color(0xFF09637E),
        )
    }

    Canvas(modifier = modifier.size(width = 34.dp, height = 43.dp)) {
        val scaled = Path().apply {
            addPath(basePath)
            val matrix = Matrix().apply {
                scale(size.width / 34f, size.height / 43f)
            }
            transform(matrix)
        }
        drawPath(
            path = scaled,
            brush = Brush.verticalGradient(colors = gradient),
        )
    }
}
