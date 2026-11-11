package com.pingo.feature.transactions.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
internal fun SeletorDeTipo(state: DetalheDoLancamentoUiState, onEvent: (DetalheDoLancamentoEvent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PingoTheme.colors.surfaceVariant, RoundedCornerShape(PingoRadius.md))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TipoDoLancamentoManual.entries.forEach { candidato ->
            AbaDeTipoDoDetalhe(
                tipo = candidato,
                selecionado = candidato == state.tipo,
                habilitado = state.podeAlterarTipo && candidato != TipoDoLancamentoManual.TRANSFERENCIA && !state.ehTransferencia,
                onClick = { onEvent(DetalheDoLancamentoEvent.TipoSelecionado(candidato)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AbaDeTipoDoDetalhe(
    tipo: TipoDoLancamentoManual,
    selecionado: Boolean,
    habilitado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cor = when {
        !selecionado -> PingoTheme.colors.onSurfaceVariant
        habilitado -> corDoTipo(tipo)
        else -> PingoTheme.colors.onBackground
    }

    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .background(if (selecionado) PingoTheme.colors.surface else Color.Transparent, RoundedCornerShape(PingoRadius.sm))
            .testTag("tipo-detalhe-${tipo.name}")
            .clickable(enabled = habilitado, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        PingoText(text = tipo.rotulo, style = PingoTheme.typography.label, color = cor, maxLines = 1)
    }
}

@Composable
private fun corDoTipo(tipo: TipoDoLancamentoManual): Color = when (tipo) {
    TipoDoLancamentoManual.DESPESA -> PingoTheme.colors.expense
    TipoDoLancamentoManual.RECEITA -> PingoTheme.colors.income
    TipoDoLancamentoManual.TRANSFERENCIA -> PingoTheme.colors.transfer
}
