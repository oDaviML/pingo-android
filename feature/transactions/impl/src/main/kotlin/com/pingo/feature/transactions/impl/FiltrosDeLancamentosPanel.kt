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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoChoiceChip
import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoSheetHandle
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.TipoLancamento

private val FilterSheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

@Composable
internal fun PainelDeFiltros(state: TransactionsUiState, onEvent: (TransactionsEvent) -> Unit) {
    val cores = PingoTheme.colors
    val interacao = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cores.scrim)
            .clickable(interactionSource = interacao, indication = null) { onEvent(TransactionsEvent.FecharFiltros) },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .pingoElevation(level = PingoElevationLevel.Level3, shape = FilterSheetShape, offsetY = (-8).dp)
                .background(cores.surface, FilterSheetShape)
                .clickable(interactionSource = interacao, indication = null) {}
                .padding(horizontal = PingoSpacing.xl, vertical = PingoSpacing.md),
        ) {
            PingoSheetHandle()
            CabecalhoDoPainel(state = state, onEvent = onEvent)
            CorpoDoPainel(state = state, onEvent = onEvent, modifier = Modifier.weight(1f))
            AcoesDoPainel(onEvent = onEvent)
        }
    }
}

@Composable
private fun CabecalhoDoPainel(state: TransactionsUiState, onEvent: (TransactionsEvent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = PingoSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(text = "Filtros", style = PingoTheme.typography.title)
        Box(
            modifier = Modifier
                .size(PingoLayout.minTouchTarget)
                .testTag("fechar-filtros")
                .clickable { onEvent(TransactionsEvent.FecharFiltros) },
            contentAlignment = Alignment.Center,
        ) {
            PingoIcon(
                imageVector = PingoIcons.X,
                contentDescription = "Fechar filtros",
                modifier = Modifier.size(20.dp),
                tint = PingoTheme.colors.onSurfaceVariant,
            )
        }
    }
    PingoText(
        text = resumoDoPainel(state),
        style = PingoTheme.typography.bodySmall,
        color = PingoTheme.colors.onSurfaceVariant,
        modifier = Modifier.padding(top = PingoSpacing.xs),
    )
}

@Composable
private fun CorpoDoPainel(state: TransactionsUiState, onEvent: (TransactionsEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = PingoSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
    ) {
        SecaoMes(state = state, onEvent = onEvent)
        SecaoTipo(filtros = state.filtros, onEvent = onEvent)
        SecaoVinculo(state = state, onEvent = onEvent)
        SecaoCategoria(state = state, onEvent = onEvent)
        SecaoStatus(filtros = state.filtros, onEvent = onEvent)
    }
}

@Composable
private fun SecaoMes(state: TransactionsUiState, onEvent: (TransactionsEvent) -> Unit) {
    Secao(titulo = "MÊS") {
        ChipDeTodos(
            text = "Todos",
            selecionado = state.filtros.mes == null,
            testTag = "filtro-mes-todos",
            onClick = { onEvent(TransactionsEvent.SelecionarMes(null)) },
        )
        state.mesesDisponiveis.forEach { mes ->
            PingoChoiceChip(
                text = RotuloDeMes.de(mes),
                selected = state.filtros.mes == mes,
                onClick = { onEvent(TransactionsEvent.SelecionarMes(mes)) },
                modifier = Modifier.testTag("filtro-mes-$mes"),
            )
        }
    }
}

@Composable
private fun SecaoTipo(filtros: FiltrosDeLancamentos, onEvent: (TransactionsEvent) -> Unit) {
    Secao(titulo = "TIPO") {
        listOf(TipoLancamento.DESPESA, TipoLancamento.RECEITA, TipoLancamento.TRANSFERENCIA).forEach { tipo ->
            PingoChoiceChip(
                text = rotuloDoTipo(tipo).orEmpty(),
                selected = filtros.tipo == tipo,
                onClick = { onEvent(TransactionsEvent.SelecionarTipo(tipo)) },
                modifier = Modifier.testTag("filtro-tipo-${tipo.name}"),
            )
        }
    }
}

@Composable
private fun SecaoVinculo(state: TransactionsUiState, onEvent: (TransactionsEvent) -> Unit) {
    Secao(titulo = "CONTA OU INSTRUMENTO") {
        ChipDeTodos(
            text = "Todas",
            selecionado = state.filtros.vinculo == null,
            testTag = "filtro-vinculo-todas",
            onClick = { onEvent(TransactionsEvent.SelecionarVinculo(null)) },
        )
        state.opcoesDeVinculo.forEach { opcao ->
            PingoChoiceChip(
                text = opcao.rotulo,
                selected = state.filtros.vinculo == opcao.filtro,
                onClick = { onEvent(TransactionsEvent.SelecionarVinculo(opcao.filtro)) },
                modifier = Modifier.testTag("filtro-vinculo-${opcao.chave}"),
            )
        }
    }
}

@Composable
private fun SecaoCategoria(state: TransactionsUiState, onEvent: (TransactionsEvent) -> Unit) {
    Secao(titulo = "CATEGORIA") {
        ChipDeTodos(
            text = "Todas",
            selecionado = state.filtros.categoriaPublicId == null,
            testTag = "filtro-categoria-todas",
            onClick = { onEvent(TransactionsEvent.SelecionarCategoria(null)) },
        )
        state.opcoesDeCategoria.forEach { categoria ->
            PingoChoiceChip(
                text = categoria.nome,
                selected = state.filtros.categoriaPublicId == categoria.publicId,
                onClick = { onEvent(TransactionsEvent.SelecionarCategoria(categoria.publicId)) },
                modifier = Modifier.testTag("filtro-categoria-${categoria.publicId}"),
            )
        }
    }
}

@Composable
private fun SecaoStatus(filtros: FiltrosDeLancamentos, onEvent: (TransactionsEvent) -> Unit) {
    Secao(titulo = "STATUS") {
        StatusFiltro.entries.forEach { status ->
            PingoChoiceChip(
                text = rotuloDoStatus(status),
                selected = filtros.status == status,
                onClick = { onEvent(TransactionsEvent.SelecionarStatus(status)) },
                modifier = Modifier.testTag("filtro-status-${status.name}"),
            )
        }
    }
}

@Composable
private fun ChipDeTodos(text: String, selecionado: Boolean, testTag: String, onClick: () -> Unit) {
    PingoChoiceChip(
        text = text,
        selected = selecionado,
        onClick = onClick,
        modifier = Modifier.testTag(testTag),
    )
}

@Composable
private fun AcoesDoPainel(onEvent: (TransactionsEvent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = PingoSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoButton(
            text = "Limpar",
            onClick = { onEvent(TransactionsEvent.LimparFiltros) },
            type = PingoButtonType.Text,
        )
        PingoButton(
            text = "Aplicar",
            onClick = { onEvent(TransactionsEvent.FecharFiltros) },
            type = PingoButtonType.Primary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Secao(titulo: String, conteudo: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm)) {
        PingoText(
            text = titulo,
            style = PingoTheme.typography.overline,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        ) {
            conteudo()
        }
    }
}

private fun resumoDoPainel(state: TransactionsUiState): String {
    val mes = state.filtros.mes?.let(RotuloDeMes::de) ?: "Todos os meses"
    return "$mes · ${pluralDeFiltros(state.filtrosAtivos)}"
}
