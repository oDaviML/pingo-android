package com.pingo.feature.reports.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.CategoryAvatar
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.PingoLinearProgress
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.SectionHeader
import com.pingo.core.designsystem.component.chart.PingoComparisonRow
import com.pingo.core.designsystem.component.chart.PingoDonutChart
import com.pingo.core.designsystem.component.chart.PingoIncomeExpenseBars
import com.pingo.core.designsystem.component.chart.PingoSubscriptionRow
import com.pingo.core.designsystem.component.chart.PingoSubscriptionSuggestionCard
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.ui.iconeDaCategoria
import java.time.YearMonth

@Composable
internal fun SecaoDespesasPorCategoria(
    state: ReportsUiState,
    onAbrirCategoriaNoMes: (String, YearMonth) -> Unit,
) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    SectionHeader(title = "Despesas por categoria")
    PingoCard(
        modifier = Modifier
            .testTag("relatorios-donut-card")
            .pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        gap = PingoSpacing.md,
    ) {
        PingoDonutChart(
            slices = state.fatiasDonut,
            totalFormatted = state.totalDespesasFormatado,
            centerCaption = "Despesas",
            onSelectCategory = { sliceId ->
                if (sliceId != SEM_CATEGORIA_SLICE_ID) {
                    onAbrirCategoriaNoMes(sliceId, state.mes)
                }
            },
        )
        if (state.fatiasDonut.isEmpty()) {
            PingoText(
                text = "Nenhuma despesa confirmada neste mês.",
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun SecaoComparativoMensal(
    state: ReportsUiState,
    onAbrirCategoriaNoMes: (String, YearMonth) -> Unit,
) {
    val itens = buildList {
        state.comparativoGeral?.let(::add)
        addAll(state.comparativos)
    }
    if (itens.isEmpty()) return

    val shape = RoundedCornerShape(PingoRadius.xl)
    SectionHeader(title = "Comparado a ${state.rotuloMesAnterior}")
    PingoCard(
        modifier = Modifier
            .testTag("relatorios-comparativo-card")
            .pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        contentPadding = PingoSpacing.lg,
        gap = 0.dp,
    ) {
        itens.forEachIndexed { indice, item ->
            val onClick = item.categoriaPublicId?.let { id -> { onAbrirCategoriaNoMes(id, state.mes) } }
            PingoComparisonRow(
                state = item.row,
                onClick = onClick,
            )
            if (indice < itens.lastIndex) {
                DivisoriaRelatorio()
            }
        }
    }
}

@Composable
internal fun SecaoPorCategoria(
    state: ReportsUiState,
    onAbrirCategoriaNoMes: (String, YearMonth) -> Unit,
) {
    if (state.categorias.isEmpty()) return

    val shape = RoundedCornerShape(PingoRadius.xl)
    SectionHeader(title = "Por categoria")
    PingoCard(
        modifier = Modifier
            .testTag("relatorios-por-categoria-card")
            .pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        contentPadding = 0.dp,
        gap = 0.dp,
    ) {
        state.categorias.forEachIndexed { indice, categoria ->
            LinhaDeCategoriaRelatorio(
                categoria = categoria,
                onClick = categoria.categoriaPublicId?.let { id -> { onAbrirCategoriaNoMes(id, state.mes) } },
            )
            if (indice < state.categorias.lastIndex) {
                DivisoriaRelatorio()
            }
        }
    }
}

@Composable
private fun LinhaDeCategoriaRelatorio(categoria: CategoriaRelatorioUi, onClick: (() -> Unit)?) {
    val tag = "relatorios-categoria-${categoria.categoriaPublicId ?: "sem-categoria"}"
    val clickModifier = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PingoLayout.minTouchTarget)
            .testTag(tag)
            .then(clickModifier)
            .semantics(mergeDescendants = true) { contentDescription = categoria.acessibilidade }
            .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        ResumoLinhaCategoria(categoria = categoria)
        PingoLinearProgress(
            progress = (categoria.percentualInteiro / 100f).coerceIn(0f, 1f),
            height = 6.dp,
        )
    }
}

@Composable
private fun ResumoLinhaCategoria(categoria: CategoriaRelatorioUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(
            icon = iconeDaCategoria(categoria.nome),
            size = 40.dp,
            iconSize = 20.dp,
            cornerRadius = PingoRadius.md,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PingoText(
                text = categoria.nome,
                style = PingoTheme.typography.bodyStrong,
                color = PingoTheme.colors.onBackground,
                maxLines = 1,
            )
            PingoText(
                text = categoria.subtitulo,
                style = PingoTheme.typography.caption,
                color = PingoTheme.colors.onSurfaceVariant,
                maxLines = 1,
            )
        }
        PingoText(
            text = categoria.valorFormatado,
            style = PingoTheme.typography.amount,
            color = PingoTheme.colors.onBackground,
            maxLines = 1,
        )
    }
}

@Composable
internal fun SecaoEvolucaoSeisMeses(state: ReportsUiState, onSelecionarMes: (YearMonth) -> Unit) {
    if (state.serieSeisMeses.isEmpty()) return

    val shape = RoundedCornerShape(PingoRadius.xl)
    val points = state.serieSeisMeses.map { it.point }
    SectionHeader(title = "Receitas × despesas")
    PingoCard(
        modifier = Modifier
            .testTag("relatorios-evolucao-card")
            .pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
    ) {
        PingoIncomeExpenseBars(
            points = points,
            onSelectMonth = { index ->
                state.serieSeisMeses.getOrNull(index)?.let { ponto ->
                    onSelecionarMes(ponto.mes)
                }
            },
        )
    }
}

@Composable
internal fun SecaoAssinaturas(state: ReportsUiState, onEvent: (ReportsEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("relatorios-assinaturas-secao"),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        SectionHeader(title = "Assinaturas")
        if (state.sugestoesAssinatura.isEmpty() && state.assinaturasConfirmadas.isEmpty()) {
            AssinaturasVaziasCard()
            return
        }
        state.sugestoesAssinatura.forEach { sugestao ->
            val emProcessamento = sugestao.chaveTexto in state.assinaturasEmProcessamento
            PingoSubscriptionSuggestionCard(
                state = sugestao.card,
                modifier = Modifier.testTag("relatorios-sugestao-${sugestao.chaveTexto}"),
                onConfirm = {
                    if (!emProcessamento) onEvent(ReportsEvent.ConfirmarSugestao(sugestao.chave))
                },
                onDismiss = {
                    if (!emProcessamento) onEvent(ReportsEvent.RecusarSugestao(sugestao.chave))
                },
            )
        }
        if (state.assinaturasConfirmadas.isNotEmpty()) {
            ListaAssinaturasConfirmadas(state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun AssinaturasVaziasCard() {
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoCard(
        modifier = Modifier
            .testTag("relatorios-assinaturas-vazio")
            .pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        gap = PingoSpacing.xs,
    ) {
        PingoText(
            text = "Nenhuma assinatura identificada",
            style = PingoTheme.typography.bodyStrong,
            color = PingoTheme.colors.onBackground,
        )
        PingoText(
            text = "Quando detectarmos cobranças recorrentes no mesmo cartão ou conta, elas aparecem aqui.",
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun ListaAssinaturasConfirmadas(state: ReportsUiState, onEvent: (ReportsEvent) -> Unit) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoCard(
        modifier = Modifier.pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        contentPadding = 0.dp,
        gap = 0.dp,
    ) {
        CabecalhoTotalAssinaturas(totalFormatado = state.totalMensalAssinaturasFormatado)
        DivisoriaRelatorio()
        state.assinaturasConfirmadas.forEachIndexed { indice, item ->
            val emProcessamento = item.publicId in state.assinaturasEmProcessamento
            PingoSubscriptionRow(
                state = item.row,
                modifier = Modifier
                    .testTag("relatorios-assinatura-${item.publicId}")
                    .padding(horizontal = PingoSpacing.lg),
                onClick = {
                    if (!emProcessamento) onEvent(ReportsEvent.SolicitarCancelamentoAssinatura(item.publicId))
                },
            )
            if (indice < state.assinaturasConfirmadas.lastIndex) {
                DivisoriaRelatorio()
            }
        }
    }
}

@Composable
private fun CabecalhoTotalAssinaturas(totalFormatado: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("relatorios-assinaturas-total")
            .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(
            text = "Total mensal estimado",
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        PingoText(
            text = totalFormatado,
            style = PingoTheme.typography.amount,
            color = PingoTheme.colors.brand,
        )
    }
}

@Composable
private fun DivisoriaRelatorio() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(PingoTheme.colors.outlineVariant),
    )
}
