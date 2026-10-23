package com.pingo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
fun PingoDestructiveDialog(
    title: String,
    description: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLoading: Boolean = false,
) {
    DialogShell(onDismissRequest = onDismissRequest) {
        DialogCard(
            icon = PingoIcons.Trash2,
            tint = PingoTheme.colors.expense,
            container = PingoTheme.colors.expenseContainer,
            title = title,
            modifier = modifier,
        ) {
            PingoText(
                text = description,
                style = PingoTheme.typography.body,
                color = PingoTheme.colors.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PingoButton(
                    text = "Cancelar",
                    onClick = onDismissRequest,
                    type = PingoButtonType.Text,
                )
                PingoButton(
                    text = confirmText,
                    onClick = onConfirm,
                    type = PingoButtonType.Danger,
                    enabled = !confirmLoading,
                    loading = confirmLoading,
                )
            }
        }
    }
}

@Composable
fun PingoInfoDialog(
    title: String,
    text: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = PingoIcons.FileText,
) {
    DialogShell(onDismissRequest = onDismissRequest) {
        DialogCard(
            icon = icon,
            tint = PingoTheme.colors.onPrimaryContainer,
            container = PingoTheme.colors.primaryContainer,
            title = title,
            modifier = modifier,
        ) {
            PingoText(
                text = text,
                style = PingoTheme.typography.body,
                color = PingoTheme.colors.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                PingoButton(
                    text = "Fechar",
                    onClick = onDismissRequest,
                    type = PingoButtonType.Text,
                )
            }
        }
    }
}

@Composable
private fun DialogShell(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PingoTheme.colors.scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest,
                ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun DialogCard(
    icon: ImageVector,
    tint: Color,
    container: Color,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .padding(horizontal = PingoSpacing.xxxl)
            .fillMaxWidth()
            .background(color = PingoTheme.colors.surface, shape = RoundedCornerShape(PingoRadius.xl))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(PingoSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(color = container, shape = RoundedCornerShape(PingoRadius.full)),
            contentAlignment = Alignment.Center,
        ) {
            PingoIcon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = tint,
            )
        }

        PingoText(
            text = title,
            style = PingoTheme.typography.title,
            color = PingoTheme.colors.onBackground,
        )

        content()
    }
}
