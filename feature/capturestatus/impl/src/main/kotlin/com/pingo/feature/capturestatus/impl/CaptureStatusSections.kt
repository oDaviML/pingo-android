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
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.InstituicaoFinanceira
import java.time.Instant
import java.time.ZoneId

@Composable
internal fun StatusDaCapturaCard(state: CaptureStatusUiState, zona: ZoneId) {
    PingoCard(contentPadding = PingoSpacing.lg, gap = PingoSpacing.md) {
        StatusBadge(parada = state.parada)
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
private fun StatusBadge(parada: Boolean) {
    val conteudo = if (parada) PingoTheme.colors.expense else PingoTheme.colors.income
    val fundo = if (parada) PingoTheme.colors.expenseContainer else PingoTheme.colors.incomeContainer

    Row(
        modifier = Modifier
            .background(color = fundo, shape = RoundedCornerShape(PingoRadius.full))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = conteudo, shape = CircleShape),
        )
        PingoText(
            text = if (parada) "Captura parada" else "Captura ativa",
            style = PingoTheme.typography.labelSmall,
            color = conteudo,
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
    PingoCard(contentPadding = PingoSpacing.lg, gap = PingoSpacing.md) {
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
                    InstituicaoAvatar(instituicao = leitura.instituicao)
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
private fun InstituicaoAvatar(instituicao: InstituicaoFinanceira) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(color = PingoTheme.colors.primaryContainer, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        PingoText(
            text = letrasDaInstituicao(instituicao),
            style = PingoTheme.typography.labelSmall,
            color = PingoTheme.colors.onPrimaryContainer,
        )
    }
}

@Composable
internal fun PermissaoDaCapturaCard(permissao: PermissaoDaCaptura, onCorrigir: () -> Unit) {
    PingoCard(contentPadding = PingoSpacing.lg, gap = PingoSpacing.md) {
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
            imageVector = if (concedida) PingoIcons.Check else PingoIcons.TriangleAlert,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = conteudo,
        )
    }
}

@Composable
internal fun RegrasDoParserCard(state: CaptureStatusUiState, zona: ZoneId, onAtualizarRegras: () -> Unit) {
    PingoCard(contentPadding = PingoSpacing.lg, gap = PingoSpacing.md) {
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
