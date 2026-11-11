package com.pingo.feature.home.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoMascot
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.SectionHeader
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.ui.LancamentoItem
import com.pingo.core.ui.Valores
import com.pingo.core.ui.iconeDaCategoria
import java.time.ZoneId

@Composable
internal fun SaldoDoMes(state: HomeUiState) {
    val mes = state.mes ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PingoTheme.colors.primary, RoundedCornerShape(PingoRadius.xl))
            .padding(PingoSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        PingoText(
            text = "SALDO DE ${RotuloDeMes.emMaiusculas(mes)}",
            style = PingoTheme.typography.overline,
            color = PingoTheme.colors.onPrimary.copy(alpha = 0.8f),
        )
        PingoText(
            text = valorDoSaldo(state.saldoCentavos),
            style = PingoTheme.typography.amountLarge,
            color = PingoTheme.colors.onPrimary,
            maxLines = 1,
            modifier = Modifier.testTag("saldo-do-mes"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm)) {
            ResumoDaConta(
                rotulo = "Receitas",
                valor = "+ ${Valores.formatar(state.receitasCentavos)}",
                modifier = Modifier.weight(1f),
            )
            ResumoDaConta(
                rotulo = "Despesas",
                valor = "- ${Valores.formatar(state.despesasCentavos)}",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ResumoDaConta(rotulo: String, valor: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(PingoTheme.colors.onPrimary.copy(alpha = 0.14f), RoundedCornerShape(PingoRadius.md))
            .padding(horizontal = PingoSpacing.md, vertical = PingoSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        PingoText(
            text = rotulo,
            style = PingoTheme.typography.labelSmall,
            color = PingoTheme.colors.onPrimary.copy(alpha = 0.8f),
        )
        PingoText(
            text = valor,
            style = PingoTheme.typography.amount,
            color = PingoTheme.colors.onPrimary,
            maxLines = 1,
        )
    }
}

@Composable
internal fun TopCategorias(state: HomeUiState) {
    if (state.topCategorias.isEmpty()) {
        return
    }

    SectionHeader("Top categorias")
    PingoCard(modifier = Modifier.testTag("top-categorias"), contentPadding = 0.dp, gap = 0.dp) {
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("top-categoria-${categoria.publicId}")
            .padding(horizontal = PingoSpacing.md, vertical = PingoSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconeDaCategoriaDoTopo(categoria.nome)
        RotulosDaCategoriaDoTopo(categoria)
        PingoText(
            text = Valores.formatar(categoria.valorCentavos),
            style = PingoTheme.typography.amount,
            color = PingoTheme.colors.onBackground,
            maxLines = 1,
        )
    }
}

@Composable
private fun IconeDaCategoriaDoTopo(nome: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(PingoTheme.colors.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        PingoIcon(
            imageVector = iconeDaCategoria(nome),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = PingoTheme.colors.onPrimaryContainer,
        )
    }
}

@Composable
private fun RowScope.RotulosDaCategoriaDoTopo(categoria: CategoriaDoTopo) {
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
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
}

@Composable
internal fun UltimosLancamentos(state: HomeUiState, onVerTudo: () -> Unit, zona: ZoneId) {
    if (state.ultimos.isEmpty()) {
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(
            text = "Últimos lançamentos",
            style = PingoTheme.typography.subtitle,
            color = PingoTheme.colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        PingoButton(
            text = "Ver tudo",
            onClick = onVerTudo,
            type = PingoButtonType.Text,
            size = PingoButtonSize.Small,
            modifier = Modifier.testTag("ver-tudo-lancamentos"),
        )
    }
    PingoCard(modifier = Modifier.testTag("ultimos-lancamentos"), contentPadding = 0.dp, gap = 0.dp) {
        state.ultimos.forEachIndexed { indice, item ->
            LancamentoItem(
                lancamento = item.lancamento,
                categoria = item.categoria,
                instrumento = item.instrumento,
                conta = item.conta,
                zona = zona,
            )
            if (indice < state.ultimos.lastIndex) {
                Divisoria()
            }
        }
    }
}

@Composable
internal fun InicioVazio(onOpenNovo: () -> Unit) {
    PingoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inicio-vazio"),
        contentPadding = PingoSpacing.xl,
        gap = PingoSpacing.md,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        ) {
            PingoMascot(width = 72.dp)
            PingoText(
                text = "Nenhum pingo ainda",
                style = PingoTheme.typography.headline,
                color = PingoTheme.colors.onBackground,
                textAlign = TextAlign.Center,
            )
            PingoText(
                text = "Assim que um banco avisar uma compra, ela aparece aqui. Ou lance agora.",
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            PingoButton(
                text = "Lançar manualmente",
                onClick = onOpenNovo,
                fillWidth = true,
                modifier = Modifier.testTag("lancar-manualmente"),
            )
        }
    }
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
