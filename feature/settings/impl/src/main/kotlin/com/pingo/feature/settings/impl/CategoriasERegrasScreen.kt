package com.pingo.feature.settings.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingo.core.data.CategoriaComContagem
import com.pingo.core.data.RegraCategoriaComUso
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoStatusTag
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.PingoTopBar
import com.pingo.core.designsystem.component.finance.PingoTabs
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.TipoCategoria

@Composable
fun CategoriasERegrasScreen(onBack: () -> Unit, viewModel: CategoriasERegrasViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CategoriasERegrasContent(
        state = state,
        onBack = onBack,
        onSelecionarAba = viewModel::selecionarAba,
        onAbrirCriar = viewModel::abrirDialogoCriar,
        onAbrirRenomear = viewModel::abrirDialogoRenomear,
        onAlternarArquivo = viewModel::alternarArquivo,
        onAbrirExcluirCategoria = viewModel::abrirDialogoExcluirCategoria,
        onSolicitarExcluirRegra = viewModel::solicitarExcluirRegra,
    )

    CategoriasERegrasDialogosHost(state = state, viewModel = viewModel)
}

@Suppress("LongParameterList")
@Composable
fun CategoriasERegrasContent(
    state: CategoriasERegrasUiState,
    onBack: () -> Unit,
    onSelecionarAba: (Int) -> Unit,
    onAbrirCriar: () -> Unit,
    onAbrirRenomear: (CategoriaComContagem) -> Unit,
    onAlternarArquivo: (CategoriaComContagem) -> Unit,
    onAbrirExcluirCategoria: (CategoriaComContagem) -> Unit,
    onSolicitarExcluirRegra: (RegraCategoriaComUso) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background),
    ) {
        PingoTopBar(title = "Categorias e regras", onBack = onBack)
        PingoTabs(
            tabs = listOf("Categorias", "Regras aprendidas"),
            selectedIndex = state.abaSelecionada,
            onSelectTab = onSelecionarAba,
        )
        if (state.abaSelecionada == 0) {
            AbaCategoriasLista(
                categorias = state.categorias,
                onAbrirCriar = onAbrirCriar,
                onAbrirRenomear = onAbrirRenomear,
                onAlternarArquivo = onAlternarArquivo,
                onAbrirExcluir = onAbrirExcluirCategoria,
                modifier = Modifier.weight(1f),
            )
        } else {
            AbaRegrasLista(
                regras = state.regras,
                onSolicitarExcluirRegra = onSolicitarExcluirRegra,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AbaCategoriasLista(
    categorias: List<CategoriaComContagem>,
    onAbrirCriar: () -> Unit,
    onAbrirRenomear: (CategoriaComContagem) -> Unit,
    onAlternarArquivo: (CategoriaComContagem) -> Unit,
    onAbrirExcluir: (CategoriaComContagem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = PingoSpacing.xl, vertical = PingoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        item {
            PingoButton(
                text = "Nova categoria",
                onClick = onAbrirCriar,
                modifier = Modifier.fillMaxWidth().testTag("botao_nova_categoria"),
            )
        }
        items(categorias, key = { it.categoria.publicId }) { item ->
            CategoriaGerenciamentoCard(
                item = item,
                onRenomear = { onAbrirRenomear(item) },
                onAlternarArquivo = { onAlternarArquivo(item) },
                onExcluir = { onAbrirExcluir(item) },
            )
        }
    }
}

@Composable
private fun CategoriaGerenciamentoCard(
    item: CategoriaComContagem,
    onRenomear: () -> Unit,
    onAlternarArquivo: () -> Unit,
    onExcluir: () -> Unit,
) {
    val cat = item.categoria
    val tipoTexto = if (cat.tipo == TipoCategoria.RECEITA) "Receita" else "Despesa"
    val usoTexto = "${item.quantidadeLancamentos} lançamentos • ${item.quantidadeRegras} regras"

    PingoCard(modifier = Modifier.fillMaxWidth(), gap = PingoSpacing.sm) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs)) {
                PingoText(text = cat.nome, style = PingoTheme.typography.bodyStrong)
                PingoText(
                    text = "$tipoTexto • $usoTexto",
                    style = PingoTheme.typography.caption,
                    color = PingoTheme.colors.onSurfaceVariant,
                )
            }
            if (cat.arquivada) {
                PingoStatusTag(
                    label = "Arquivada",
                    containerColor = PingoTheme.colors.surfaceVariant,
                    contentColor = PingoTheme.colors.onSurfaceVariant,
                )
            }
        }
        CategoriaAcoesRow(
            arquivada = cat.arquivada,
            onRenomear = onRenomear,
            onAlternarArquivo = onAlternarArquivo,
            onExcluir = onExcluir,
        )
    }
}

@Composable
private fun CategoriaAcoesRow(arquivada: Boolean, onRenomear: () -> Unit, onAlternarArquivo: () -> Unit, onExcluir: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        PingoButton(
            text = "Renomear",
            onClick = onRenomear,
            type = PingoButtonType.Outline,
            size = PingoButtonSize.Small,
            modifier = Modifier.weight(1f).heightIn(min = PingoLayout.minTouchTarget),
        )
        PingoButton(
            text = if (arquivada) "Reativar" else "Arquivar",
            onClick = onAlternarArquivo,
            type = PingoButtonType.Outline,
            size = PingoButtonSize.Small,
            modifier = Modifier.weight(1f).heightIn(min = PingoLayout.minTouchTarget),
        )
        PingoButton(
            text = "Excluir",
            onClick = onExcluir,
            type = PingoButtonType.Danger,
            size = PingoButtonSize.Small,
            modifier = Modifier.weight(1f).heightIn(min = PingoLayout.minTouchTarget),
        )
    }
}

@Composable
private fun AbaRegrasLista(
    regras: List<RegraCategoriaComUso>,
    onSolicitarExcluirRegra: (RegraCategoriaComUso) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (regras.isEmpty()) {
        Column(
            modifier = modifier.fillMaxWidth().padding(PingoSpacing.xxl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PingoText(text = "Nenhuma regra aprendida ainda", style = PingoTheme.typography.title)
            PingoText(
                text = "Quando você ajusta a categoria em Revisão, o Pingo memoriza o estabelecimento.",
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = PingoSpacing.xl, vertical = PingoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        items(regras, key = { it.regra.publicId }) { item ->
            RegraAprendidaCard(item = item, onExcluir = { onSolicitarExcluirRegra(item) })
        }
    }
}

@Composable
private fun RegraAprendidaCard(item: RegraCategoriaComUso, onExcluir: () -> Unit) {
    PingoCard(modifier = Modifier.fillMaxWidth(), gap = PingoSpacing.xs) {
        PingoText(text = item.regra.estabelecimentoNormalizado, style = PingoTheme.typography.bodyStrong)
        PingoText(
            text = "Categoria: ${item.nomeCategoria} • ${item.quantidadeLancamentos} lançamentos",
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        PingoButton(
            text = "Remover regra",
            onClick = onExcluir,
            type = PingoButtonType.Danger,
            size = PingoButtonSize.Small,
            modifier = Modifier.fillMaxWidth().heightIn(min = PingoLayout.minTouchTarget),
        )
    }
}
