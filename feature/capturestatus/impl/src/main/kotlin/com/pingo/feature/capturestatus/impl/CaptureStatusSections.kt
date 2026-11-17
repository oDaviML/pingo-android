package com.pingo.feature.capturestatus.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoCaptureStatus
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoStatusTag
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.ui.AvatarDaInstituicao
import java.time.Instant
import java.time.ZoneId

@Composable
internal fun StatusDaCapturaCard(state: CaptureStatusUiState, zona: ZoneId) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoCard(
        modifier = Modifier.pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        contentPadding = PingoSpacing.lg,
        gap = PingoSpacing.md,
    ) {
        PingoStatusTag(
            status = if (state.parada) PingoCaptureStatus.Stopped else PingoCaptureStatus.Active,
        )
        PingoText(
            text = tituloDoStatus(state, zona),
            style = PingoTheme.typography.subtitle,
            color = PingoTheme.colors.onBackground,
        )
        PingoText(
            text = descricaoDoStatus(state, zona),
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
internal fun ContadoresDoMesGrid(contadores: ContadoresDaCaptura) {
    val colors = PingoTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(PingoSpacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md)) {
            ContadorCard(
                rotulo = "Reconhecidas",
                valor = contadores.reconhecidas,
                fundo = colors.incomeContainer,
                conteudo = colors.income,
                modifier = Modifier.weight(1f),
            )
            ContadorCard(
                rotulo = "Para revisão",
                valor = contadores.paraRevisao,
                fundo = colors.warningContainer,
                conteudo = colors.onWarningContainer,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md)) {
            ContadorCard(
                rotulo = "Ignoradas",
                valor = contadores.ignoradas,
                fundo = colors.surfaceVariant,
                conteudo = colors.transfer,
                modifier = Modifier.weight(1f),
            )
            ContadorCard(
                rotulo = "Não entendidas",
                valor = contadores.naoEntendidas,
                fundo = colors.expenseContainer,
                conteudo = colors.expense,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ContadorCard(rotulo: String, valor: Int, fundo: Color, conteudo: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color = fundo, shape = RoundedCornerShape(PingoRadius.xl))
            .padding(PingoSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
    ) {
        PingoText(text = valor.toString(), style = PingoTheme.typography.display, color = conteudo)
        PingoText(text = rotulo, style = PingoTheme.typography.bodySmall, color = conteudo)
    }
}

@Composable
internal fun LeiturasPorAppCard(leituras: List<LeituraPorApp>, agora: Instant, zona: ZoneId) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoCard(
        modifier = Modifier.pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        contentPadding = PingoSpacing.lg,
        gap = PingoSpacing.md,
    ) {
        leituras.forEach { leitura ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarDaInstituicao(instituicao = leitura.instituicao, size = 36.dp)
                    PingoText(
                        text = nomeDaInstituicao(leitura.instituicao),
                        style = PingoTheme.typography.bodyStrong,
                        color = PingoTheme.colors.onBackground,
                    )
                }
                PingoText(
                    text = leitura.em?.let { em -> TempoRelativo.formatar(em, agora, zona) } ?: "Nunca",
                    style = PingoTheme.typography.bodySmall,
                    color = PingoTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun PermissaoDaCapturaCard(permissao: PermissaoDaCaptura, onCorrigir: () -> Unit) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoCard(
        modifier = Modifier.pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        contentPadding = PingoSpacing.lg,
        gap = PingoSpacing.md,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PermissaoIcon(concedida = permissao.concedida)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                PingoText(
                    text = tituloDaPermissao(permissao.item),
                    style = PingoTheme.typography.bodyStrong,
                    color = PingoTheme.colors.onBackground,
                )
                PingoText(
                    text = descricaoDaPermissao(permissao.item, permissao.concedida),
                    style = PingoTheme.typography.bodySmall,
                    color = PingoTheme.colors.onSurfaceVariant,
                )
            }
            if (!permissao.concedida) {
                PingoButton(
                    text = "Corrigir",
                    onClick = onCorrigir,
                    type = PingoButtonType.Tonal,
                    size = PingoButtonSize.Small,
                )
            }
        }
    }
}

@Composable
private fun PermissaoIcon(concedida: Boolean) {
    val fundo = if (concedida) PingoTheme.colors.incomeContainer else PingoTheme.colors.warningContainer
    val conteudo = if (concedida) PingoTheme.colors.income else PingoTheme.colors.warning

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(color = fundo, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        PingoIcon(
            imageVector = if (concedida) PingoIcons.CircleCheck else PingoIcons.TriangleAlert,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = conteudo,
        )
    }
}

@Composable
internal fun RegrasDoParserCard(state: CaptureStatusUiState, zona: ZoneId, onAtualizarRegras: () -> Unit) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoCard(
        modifier = Modifier.pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        contentPadding = PingoSpacing.lg,
        gap = PingoSpacing.md,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                PingoText(
                    text = "Regras do parser",
                    style = PingoTheme.typography.bodyStrong,
                    color = PingoTheme.colors.onBackground,
                )
                PingoText(
                    text = subtituloDasRegras(state, state.agora, zona),
                    style = PingoTheme.typography.bodySmall,
                    color = PingoTheme.colors.onSurfaceVariant,
                )
            }
            PingoButton(
                text = "Atualizar regras",
                onClick = onAtualizarRegras,
                type = PingoButtonType.Outline,
                size = PingoButtonSize.Small,
                loading = state.atualizandoRegras,
            )
        }
    }
}
