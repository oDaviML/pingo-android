package com.pingo.feature.home.impl

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.CategoryAvatar
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoDropAnimation
import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoLinearProgress
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.SectionHeader
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoMotion
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.designsystem.theme.rememberReducedMotion
import com.pingo.core.ui.EstadoVazioCard
import com.pingo.core.ui.LancamentoItem
import com.pingo.core.ui.Valores
import com.pingo.core.ui.iconeDaCategoria
import java.time.ZoneId
import kotlin.math.roundToLong

private val SaldoGradienteInicio = Color(0xFF088395)
private val SaldoGradienteFim = Color(0xFF09637E)

@Composable
internal fun SaldoDoMes(state: HomeUiState) {
    val mes = state.mes ?: return
    val shape = RoundedCornerShape(PingoRadius.xl)
    val saldoAnimado = centavosAnimados(state.saldoCentavos)
    val receitasAnimadas = centavosAnimados(state.receitasCentavos)
    val despesasAnimadas = centavosAnimados(state.despesasCentavos)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pingoElevation(level = PingoElevationLevel.Level2, shape = shape)
            .clip(shape)
            .background(
                brush = Brush.linearGradient(colors = listOf(SaldoGradienteInicio, SaldoGradienteFim)),
                shape = shape,
            ),
    ) {
        DecoracaoDeOndasDoSaldo(modifier = Modifier.matchParentSize())
        ConteudoDoSaldo(
            mesRotulo = RotuloDeMes.emMaiusculas(mes),
            saldoCentavos = saldoAnimado,
            receitasCentavos = receitasAnimadas,
            despesasCentavos = despesasAnimadas,
        )
    }
}

@Composable
private fun ConteudoDoSaldo(mesRotulo: String, saldoCentavos: Long, receitasCentavos: Long, despesasCentavos: Long) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PingoSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        PingoText(
            text = "SALDO DE $mesRotulo",
            style = PingoTheme.typography.overline,
            color = Color.White.copy(alpha = 0.8f),
        )
        PingoText(
            text = valorDoSaldo(saldoCentavos),
            style = PingoTheme.typography.amountLarge,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier.testTag("saldo-do-mes"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm)) {
            ResumoDaConta(
                rotulo = "Receitas",
                valor = "+ ${Valores.formatar(receitasCentavos)}",
                icone = PingoIcons.ArrowDownLeft,
                modifier = Modifier.weight(1f),
            )
            ResumoDaConta(
                rotulo = "Despesas",
                valor = "- ${Valores.formatar(despesasCentavos)}",
                icone = PingoIcons.ArrowUpRight,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DecoracaoDeOndasDoSaldo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val centro = Offset(x = size.width * 0.92f, y = size.height * 0.18f)
        val traco = 14.dp.toPx()
        drawCircle(
            color = Color.White.copy(alpha = 0.08f),
            radius = 48.dp.toPx(),
            center = centro,
            style = Stroke(width = traco),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.06f),
            radius = 84.dp.toPx(),
            center = centro,
            style = Stroke(width = traco),
        )
    }
}

@Composable
private fun ResumoDaConta(rotulo: String, valor: String, icone: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(PingoRadius.md))
            .padding(horizontal = PingoSpacing.md, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoIcon(
            imageVector = icone,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.White.copy(alpha = 0.9f),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PingoText(
                text = rotulo,
                style = PingoTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
            )
            PingoText(
                text = valor,
                style = PingoTheme.typography.amount,
                color = Color.White,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun centavosAnimados(alvo: Long): Long {
    val reducedMotion = rememberReducedMotion()
    var origem by remember { mutableLongStateOf(alvo) }
    var destino by remember { mutableLongStateOf(alvo) }
    val progresso = remember { Animatable(1f) }

    LaunchedEffect(alvo, reducedMotion) {
        if (reducedMotion || destino == alvo) {
            origem = alvo
            destino = alvo
            progresso.snapTo(1f)
            return@LaunchedEffect
        }
        origem = interpolarCentavos(origem = origem, destino = destino, progresso = progresso.value)
        destino = alvo
        progresso.snapTo(0f)
        progresso.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = PingoMotion.MEDIUM_MS, easing = PingoMotion.StandardEasing),
        )
        origem = alvo
    }

    return when {
        reducedMotion || (destino == alvo && progresso.value >= 1f) -> alvo
        else -> interpolarCentavos(origem = origem, destino = destino, progresso = progresso.value)
    }
}

internal fun interpolarCentavos(origem: Long, destino: Long, progresso: Float): Long = when {
    progresso <= 0f -> origem
    progresso >= 1f -> destino
    else -> origem + ((destino - origem).toDouble() * progresso).roundToLong()
}

@Composable
internal fun TopCategorias(state: HomeUiState, onVerRelatorio: () -> Unit = {}) {
    if (state.topCategorias.isEmpty()) {
        return
    }

    val shape = RoundedCornerShape(PingoRadius.xl)
    SectionHeader(
        title = "Top categorias",
        actionLabel = "Ver relatório",
        onAction = onVerRelatorio,
    )
    PingoCard(
        modifier = Modifier
            .testTag("top-categorias")
            .pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        contentPadding = 0.dp,
        gap = 0.dp,
    ) {
        state.topCategorias.forEachIndexed { indice, categoria ->
            LinhaDoTopo(categoria)
            if (indice < state.topCategorias.lastIndex) {
                Divisoria()
            }
        }
    }
}

@Composable
private fun LinhaDoTopo(categoria: CategoriaDoTopo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("top-categoria-${categoria.publicId}")
            .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        ResumoDaCategoria(categoria = categoria)
        PingoLinearProgress(
            progress = (categoria.percentual / 100f).coerceIn(0f, 1f),
            height = 6.dp,
        )
    }
}

@Composable
private fun ResumoDaCategoria(categoria: CategoriaDoTopo) {
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
                text = "${categoria.percentual}% das despesas",
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.onSurfaceVariant,
                maxLines = 1,
            )
        }
        PingoText(
            text = Valores.formatar(categoria.valorCentavos),
            style = PingoTheme.typography.amount,
            color = PingoTheme.colors.onBackground,
            maxLines = 1,
        )
    }
}

@Composable
internal fun UltimosLancamentos(
    state: HomeUiState,
    onVerTudo: () -> Unit,
    zona: ZoneId,
    animarTopoId: String? = null,
    onAnimacaoConcluida: () -> Unit = {},
) {
    if (state.ultimos.isEmpty()) {
        return
    }

    val shape = RoundedCornerShape(PingoRadius.xl)
    CabecalhoUltimosLancamentos(onVerTudo = onVerTudo)
    PingoCard(
        modifier = Modifier
            .testTag("ultimos-lancamentos")
            .pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        contentPadding = 0.dp,
        gap = 0.dp,
    ) {
        state.ultimos.forEachIndexed { indice, item ->
            val animar = indice == 0 && item.lancamento.publicId == animarTopoId
            if (animar) {
                PingoDropAnimation(trigger = true, onFinished = onAnimacaoConcluida) {
                    ItemDaListaInicio(item = item, zona = zona)
                }
            } else {
                ItemDaListaInicio(item = item, zona = zona)
            }
            if (indice < state.ultimos.lastIndex) {
                Divisoria()
            }
        }
    }
}

@Composable
private fun ItemDaListaInicio(item: LancamentoDaInicio, zona: ZoneId) {
    LancamentoItem(
        lancamento = item.lancamento,
        categoria = item.categoria,
        instrumento = item.instrumento,
        conta = item.conta,
        zona = zona,
    )
}

@Composable
private fun CabecalhoUltimosLancamentos(onVerTudo: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PingoSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(
            text = "Últimos lançamentos",
            style = PingoTheme.typography.subtitle,
            color = PingoTheme.colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        BotaoVerTudo(onVerTudo = onVerTudo)
    }
}

@Composable
private fun BotaoVerTudo(onVerTudo: () -> Unit) {
    Row(
        modifier = Modifier
            .heightIn(min = PingoLayout.minTouchTarget)
            .testTag("ver-tudo-lancamentos")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onVerTudo,
            )
            .padding(horizontal = PingoSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(text = "Ver tudo", style = PingoTheme.typography.label, color = PingoTheme.colors.brand, maxLines = 1)
        PingoIcon(
            imageVector = PingoIcons.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = PingoTheme.colors.brand,
        )
    }
}

@Composable
internal fun InicioVazio(onOpenNovo: () -> Unit) {
    Spacer(modifier = Modifier.height(PingoSpacing.xxl))
    EstadoVazioCard(
        titulo = "Nenhum pingo ainda",
        descricao = "Assim que um banco avisar uma compra, ela aparece aqui. Ou lance agora.",
        acaoTexto = "Lançar manualmente",
        onAcao = onOpenNovo,
        modifier = Modifier.testTag("inicio-vazio"),
        acaoModifier = Modifier.testTag("lancar-manualmente"),
    )
}

@Composable
private fun Divisoria() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(PingoTheme.colors.outlineVariant),
    )
}

private fun valorDoSaldo(saldoCentavos: Long): String = if (saldoCentavos < 0) {
    "- ${Valores.formatar(-saldoCentavos)}"
} else {
    Valores.formatar(saldoCentavos)
}
