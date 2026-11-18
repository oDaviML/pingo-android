package com.pingo.feature.transactions.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoChoiceChip
import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.PingoIconButton
import com.pingo.core.designsystem.component.PingoIconButtonStyle
import com.pingo.core.designsystem.component.PingoSheetHandle
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.fatura.GeradorDeParcelas
import com.pingo.core.model.fatura.ModoParcelamento

private val ParcelasSheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private val PARCELAS_SUGERIDAS = listOf(1, 2, 3, 6, 10, 12, 18, 24)

@Composable
internal fun PainelDeParcelas(state: NovoLancamentoUiState, onEvent: (NovoLancamentoEvent) -> Unit) {
    val interacao = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PingoTheme.colors.scrim)
            .clickable(interactionSource = interacao, indication = null) { onEvent(NovoLancamentoEvent.FecharPainel) },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("painel-parcelas")
                .pingoElevation(level = PingoElevationLevel.Level3, shape = ParcelasSheetShape, offsetY = (-8).dp)
                .background(color = PingoTheme.colors.surface, shape = ParcelasSheetShape)
                .clickable(interactionSource = interacao, indication = null) {}
                .padding(horizontal = PingoSpacing.xl, vertical = PingoSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        ) {
            PingoSheetHandle()
            CabecalhoDeParcelas(onFechar = { onEvent(NovoLancamentoEvent.FecharPainel) })
            CorpoDeParcelas(state = state, onEvent = onEvent)
            PingoButton(
                text = "Confirmar",
                onClick = { onEvent(NovoLancamentoEvent.FecharPainel) },
                fillWidth = true,
                modifier = Modifier.testTag("parcelas-confirmar"),
            )
        }
    }
}

@Composable
private fun CabecalhoDeParcelas(onFechar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(text = "Parcelamento", style = PingoTheme.typography.title)
        PingoIconButton(
            icon = PingoIcons.X,
            contentDescription = "Fechar",
            onClick = onFechar,
            style = PingoIconButtonStyle.Standard,
        )
    }
}

@Composable
private fun CorpoDeParcelas(state: NovoLancamentoUiState, onEvent: (NovoLancamentoEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
    ) {
        ContadorDeParcelas(
            totalParcelas = state.totalParcelas,
            onAlterar = { novoTotal -> onEvent(NovoLancamentoEvent.SelecionarParcelas(novoTotal)) },
        )
        GradeDePresetsDeParcelas(
            selecionado = state.totalParcelas,
            onSelecionar = { novoTotal -> onEvent(NovoLancamentoEvent.SelecionarParcelas(novoTotal)) },
        )
        if (state.totalParcelas > 1) {
            SeletorDeModoDeParcelamento(
                modo = state.modoParcelamento,
                onSelecionar = { modo -> onEvent(NovoLancamentoEvent.SelecionarModoParcelamento(modo)) },
            )
        }
        RotulosDoNovoLancamento.resumoParcelas(state)?.let { resumo ->
            ResumoDeParcelamento(texto = resumo)
        }
    }
}

@Composable
private fun ContadorDeParcelas(totalParcelas: Int, onAlterar: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PingoTheme.colors.surfaceVariant, RoundedCornerShape(PingoRadius.md))
            .padding(horizontal = PingoSpacing.md, vertical = PingoSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoIconButton(
            icon = PingoIcons.Minus,
            contentDescription = "Diminuir parcelas",
            onClick = { onAlterar(totalParcelas - 1) },
            enabled = totalParcelas > GeradorDeParcelas.MIN_PARCELAS,
            style = PingoIconButtonStyle.Outline,
            modifier = Modifier.testTag("parcelas-menos"),
        )
        PingoText(
            text = if (totalParcelas == 1) "À vista (1x)" else "${totalParcelas}x",
            style = PingoTheme.typography.headline,
            color = PingoTheme.colors.onBackground,
            modifier = Modifier.testTag("parcelas-contagem"),
        )
        PingoIconButton(
            icon = PingoIcons.Plus,
            contentDescription = "Aumentar parcelas",
            onClick = { onAlterar(totalParcelas + 1) },
            enabled = totalParcelas < GeradorDeParcelas.MAX_PARCELAS,
            style = PingoIconButtonStyle.Outline,
            modifier = Modifier.testTag("parcelas-mais"),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GradeDePresetsDeParcelas(selecionado: Int, onSelecionar: (Int) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        PARCELAS_SUGERIDAS.forEach { quantidade ->
            PingoChoiceChip(
                text = if (quantidade == 1) "1x (à vista)" else "${quantidade}x",
                selected = selecionado == quantidade,
                onClick = { onSelecionar(quantidade) },
                modifier = Modifier.testTag("parcelas-preset-$quantidade"),
            )
        }
    }
}

@Composable
private fun SeletorDeModoDeParcelamento(modo: ModoParcelamento, onSelecionar: (ModoParcelamento) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs)) {
        PingoText(
            text = "O valor digitado acima representa:",
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        ) {
            PingoChoiceChip(
                text = "Valor total",
                selected = modo == ModoParcelamento.VALOR_TOTAL,
                onClick = { onSelecionar(ModoParcelamento.VALOR_TOTAL) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("parcelas-modo-total"),
            )
            PingoChoiceChip(
                text = "Valor da parcela",
                selected = modo == ModoParcelamento.VALOR_PARCELA,
                onClick = { onSelecionar(ModoParcelamento.VALOR_PARCELA) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("parcelas-modo-parcela"),
            )
        }
    }
}

@Composable
private fun ResumoDeParcelamento(texto: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PingoTheme.colors.surfaceVariant, RoundedCornerShape(PingoRadius.md))
            .padding(PingoSpacing.md),
    ) {
        PingoText(
            text = texto,
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.brand,
        )
    }
}
