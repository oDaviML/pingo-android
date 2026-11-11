package com.pingo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

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
                .padding(horizontal = PingoSpacing.sm, vertical = PingoSpacing.xs),
        ) {
            AbaInferior.entries.forEach { aba ->
                if (onNovo != null && aba == AbaInferior.CONTAS) {
                    BotaoDoNovoLancamento(onClick = onNovo)
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
    val conteudo = if (selecionada) cores.onPrimaryContainer else cores.onSurfaceVariant

    Column(
        modifier = modifier
            .testTag("aba-${aba.name}")
            .selectable(selected = selecionada, role = Role.Tab, onClick = onClick)
            .padding(vertical = PingoSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(28.dp)
                .background(
                    color = if (selecionada) cores.primaryContainer else Color.Transparent,
                    shape = RoundedCornerShape(PingoRadius.full),
                ),
            contentAlignment = Alignment.Center,
        ) {
            PingoIcon(
                imageVector = iconeDaAba(aba),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = conteudo,
            )
        }
        PingoText(
            text = aba.rotulo,
            style = PingoTheme.typography.labelSmall,
            color = conteudo,
            maxLines = 1,
        )
    }
}

@Composable
private fun BotaoDoNovoLancamento(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(64.dp)
            .height(52.dp)
            .testTag("novo-lancamento"),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(color = PingoTheme.colors.primary, shape = CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            PingoIcon(
                imageVector = PingoIcons.Plus,
                contentDescription = "Novo lançamento",
                modifier = Modifier.size(24.dp),
                tint = PingoTheme.colors.onPrimary,
            )
        }
    }
}

private fun iconeDaAba(aba: AbaInferior): ImageVector = when (aba) {
    AbaInferior.INICIO -> PingoIcons.Home
    AbaInferior.LANCAMENTOS -> PingoIcons.Receipt
    AbaInferior.CONTAS -> PingoIcons.Wallet
    AbaInferior.RELATORIOS -> PingoIcons.PieChart
}
