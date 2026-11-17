package com.pingo.feature.home.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
internal fun SeletorDeMes(state: HomeUiState, onMesAnterior: () -> Unit, onMesProximo: () -> Unit) {
    val mes = state.mes ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("seletor-de-mes"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BotaoDoMes(
            icone = PingoIcons.ChevronLeft,
            descricao = "Mês anterior",
            tag = "mes-anterior",
            habilitado = state.podeVoltarMes,
            onClick = onMesAnterior,
        )
        PingoText(
            text = RotuloDeMes.de(mes),
            style = PingoTheme.typography.subtitle,
            color = PingoTheme.colors.onBackground,
            modifier = Modifier.testTag("mes-selecionado"),
        )
        BotaoDoMes(
            icone = PingoIcons.ChevronRight,
            descricao = "Próximo mês",
            tag = "mes-proximo",
            habilitado = state.podeAvancarMes,
            onClick = onMesProximo,
        )
    }
}

@Composable
private fun BotaoDoMes(icone: ImageVector, descricao: String, tag: String, habilitado: Boolean, onClick: () -> Unit) {
    val colors = PingoTheme.colors
    val shape = RoundedCornerShape(PingoRadius.md)
    val fundo = if (habilitado) colors.surfaceVariant else Color.Transparent

    Box(
        modifier = Modifier
            .size(PingoLayout.minTouchTarget)
            .testTag(tag)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = habilitado,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(shape)
                .background(color = fundo, shape = shape),
            contentAlignment = Alignment.Center,
        ) {
            PingoIcon(
                imageVector = icone,
                contentDescription = descricao,
                modifier = Modifier.size(20.dp),
                tint = if (habilitado) colors.onBackground else colors.onDisabled,
            )
        }
    }
}
