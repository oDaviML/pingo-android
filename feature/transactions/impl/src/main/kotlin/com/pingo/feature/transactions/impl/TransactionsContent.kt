package com.pingo.feature.transactions.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.AbaInferior
import com.pingo.core.designsystem.component.PingoBottomBar
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoChoiceChip
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoMascot
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.ui.LancamentoItem
import com.pingo.core.ui.Valores
import kotlinx.coroutines.delay

@Composable
fun TransactionsContent(
    state: TransactionsUiState,
    onEvent: (TransactionsEvent) -> Unit,
    onOpenDetalhe: (String) -> Unit,
    onOpenNovo: () -> Unit,
    onSelecionarAba: (AbaInferior) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SecaoDaLista(
                state = state,
                onEvent = onEvent,
                onOpenDetalhe = onOpenDetalhe,
                modifier = Modifier.weight(1f),
            )
            PingoBottomBar(
                selecionada = AbaInferior.LANCAMENTOS,
                onSelecionar = onSelecionarAba,
                onNovo = onOpenNovo,
            )
        }

        if (state.filtrosVisiveis) {
            PainelDeFiltros(state = state, onEvent = onEvent)
        }

        state.trocaDeCategoria?.let { troca ->
            PainelDeTrocaDeCategoria(
                troca = troca,
                onSelecionar = { categoriaPublicId ->
                    onEvent(TransactionsEvent.TrocarCategoria(troca.lancamentoPublicId, categoriaPublicId))
                },
                onFechar = { onEvent(TransactionsEvent.FecharTrocaCategoria) },
            )
        }
    }
}

@Composable
private fun SecaoDaLista(
    state: TransactionsUiState,
    onEvent: (TransactionsEvent) -> Unit,
    onOpenDetalhe: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        CabecalhoDaTela(state = state, onEvent = onEvent)
        Box(modifier = Modifier.weight(1f)) {
            ConteudoDaLista(state = state, onEvent = onEvent, onOpenDetalhe = onOpenDetalhe)
            state.mensagemDeExclusao?.let { mensagem ->
                SnackbarDeExclusao(
                    mensagem = mensagem,
                    onDesfazer = { onEvent(TransactionsEvent.DesfazerExclusao) },
                    onDispensar = { onEvent(TransactionsEvent.DispensarMensagem) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(PingoSpacing.lg),
                )
            }
        }
    }
}

@Composable
private fun CabecalhoDaTela(state: TransactionsUiState, onEvent: (TransactionsEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PingoSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        PingoText(
            text = "Lançamentos",
            style = PingoTheme.typography.display,
            modifier = Modifier.padding(top = PingoSpacing.md, bottom = PingoSpacing.xs),
        )
        CampoDeBusca(
            texto = state.busca,
            filtrosAtivos = state.filtrosAtivos,
            onTexto = { onEvent(TransactionsEvent.BuscaAlterada(it)) },
            onAbrirFiltros = { onEvent(TransactionsEvent.AbrirFiltros) },
        )
        ChipsDeFiltrosAtivos(state = state, onAbrirFiltros = { onEvent(TransactionsEvent.AbrirFiltros) })
    }
}

@Composable
private fun CampoDeBusca(texto: String, filtrosAtivos: Int, onTexto: (String) -> Unit, onAbrirFiltros: () -> Unit) {
    val forma = RoundedCornerShape(PingoRadius.md)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CampoDeTextoDeBusca(texto = texto, onTexto = onTexto, modifier = Modifier.weight(1f))
        BotaoDeFiltros(filtrosAtivos = filtrosAtivos, onAbrirFiltros = onAbrirFiltros, forma = forma)
    }
}

@Composable
private fun CampoDeTextoDeBusca(texto: String, onTexto: (String) -> Unit, modifier: Modifier = Modifier) {
    val cores = PingoTheme.colors
    val forma = RoundedCornerShape(PingoRadius.md)

    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .background(color = cores.surface, shape = forma)
            .border(width = 1.5.dp, color = cores.outline, shape = forma)
            .padding(horizontal = PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoIcon(
            imageVector = PingoIcons.Search,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = cores.onSurfaceVariant,
        )
        CampoDeTexto(texto = texto, onTexto = onTexto, modifier = Modifier.weight(1f))
        if (texto.isNotEmpty()) {
            BotaoLimparBusca(onLimpar = { onTexto("") })
        }
    }
}

@Composable
private fun CampoDeTexto(texto: String, onTexto: (String) -> Unit, modifier: Modifier = Modifier) {
    val cores = PingoTheme.colors

    BasicTextField(
        value = texto,
        onValueChange = onTexto,
        modifier = modifier
            .padding(horizontal = PingoSpacing.sm)
            .semantics { contentDescription = "Buscar lançamentos" },
        textStyle = PingoTheme.typography.body.copy(color = cores.onBackground),
        cursorBrush = SolidColor(cores.primary),
        singleLine = true,
        decorationBox = { campo ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (texto.isEmpty()) {
                    PingoText(
                        text = "Buscar lançamentos",
                        style = PingoTheme.typography.body,
                        color = cores.onSurfaceVariant,
                    )
                }
                campo()
            }
        },
    )
}

@Composable
private fun BotaoLimparBusca(onLimpar: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onLimpar)
            .semantics { contentDescription = "Limpar busca" },
        contentAlignment = Alignment.Center,
    ) {
        PingoIcon(
            imageVector = PingoIcons.X,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = PingoTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun BotaoDeFiltros(filtrosAtivos: Int, onAbrirFiltros: () -> Unit, forma: Shape) {
    val cores = PingoTheme.colors

    Box(
        modifier = Modifier
            .size(48.dp)
            .testTag("filtros-botao")
            .background(color = if (filtrosAtivos > 0) cores.primaryContainer else cores.surface, shape = forma)
            .border(width = 1.5.dp, color = if (filtrosAtivos > 0) cores.primary else cores.outline, shape = forma)
            .clickable(onClick = onAbrirFiltros)
            .semantics { contentDescription = "Filtros" },
        contentAlignment = Alignment.Center,
    ) {
        PingoIcon(
            imageVector = PingoIcons.SlidersHorizontal,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (filtrosAtivos > 0) cores.onPrimaryContainer else cores.onBackground,
        )
    }
}

@Composable
private fun ChipsDeFiltrosAtivos(state: TransactionsUiState, onAbrirFiltros: () -> Unit) {
    val rotulos = rotulosDosFiltros(state)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        rotulos.forEach { rotulo ->
            PingoChoiceChip(text = rotulo, selected = true, onClick = onAbrirFiltros)
        }
    }
}

private fun rotulosDosFiltros(state: TransactionsUiState): List<String> = buildList {
    add(state.filtros.mes?.let(RotuloDeMes::de) ?: "Todos os meses")
    rotuloDoTipo(state.filtros.tipo)?.let(::add)
    state.filtros.vinculo?.let { vinculo ->
        state.opcoesDeVinculo.firstOrNull { it.filtro == vinculo }?.rotulo?.let(::add)
    }
    state.filtros.categoriaPublicId?.let { publicId ->
        state.opcoesDeCategoria.firstOrNull { it.publicId == publicId }?.nome?.let(::add)
    }
    state.filtros.status?.let { add(rotuloDoStatus(it)) }
}

@Composable
private fun ConteudoDaLista(state: TransactionsUiState, onEvent: (TransactionsEvent) -> Unit, onOpenDetalhe: (String) -> Unit) {
    when {
        state.carregando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PingoText(text = "Carregando…") }
        state.estadoVazio != null -> EstadoVazio(estado = state.estadoVazio, onEvent = onEvent)
        else -> ListaDeDias(state = state, onEvent = onEvent, onOpenDetalhe = onOpenDetalhe)
    }
}

@Composable
private fun ListaDeDias(state: TransactionsUiState, onEvent: (TransactionsEvent) -> Unit, onOpenDetalhe: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = PingoSpacing.lg, vertical = PingoSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        state.dias.forEach { dia ->
            item(key = "cabecalho-${dia.data}") {
                CabecalhoDoDia(dia = dia)
            }
            item(key = "dia-${dia.data}") {
                GrupoDoDia(dia = dia, onEvent = onEvent, onOpenDetalhe = onOpenDetalhe)
            }
        }
    }
}

@Composable
private fun CabecalhoDoDia(dia: DiaDeLancamentos) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cabecalho-dia-${dia.data}")
            .padding(horizontal = PingoSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(
            text = dia.rotulo,
            style = PingoTheme.typography.overline,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        PingoText(
            text = Valores.total(dia.totalCentavos),
            style = PingoTheme.typography.labelSmall,
            color = PingoTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun GrupoDoDia(dia: DiaDeLancamentos, onEvent: (TransactionsEvent) -> Unit, onOpenDetalhe: (String) -> Unit) {
    val forma = RoundedCornerShape(PingoRadius.xl)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(PingoTheme.colors.surface)
            .border(width = 1.dp, color = PingoTheme.colors.outlineVariant, shape = forma),
    ) {
        dia.itens.forEachIndexed { indice, item ->
            if (indice > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(PingoTheme.colors.outlineVariant),
                )
            }
            LinhaComGesto(
                chave = item.lancamento.publicId,
                onExcluir = { onEvent(TransactionsEvent.Excluir(item.lancamento.publicId)) },
                onTrocarCategoria = { onEvent(TransactionsEvent.AbrirTrocaCategoria(item.lancamento.publicId)) },
            ) {
                LancamentoItem(
                    lancamento = item.lancamento,
                    categoria = item.categoria,
                    instrumento = item.instrumento,
                    conta = item.conta,
                    modifier = Modifier
                        .testTag("lancamento-${item.lancamento.publicId}")
                        .background(PingoTheme.colors.surface)
                        .clickable { onOpenDetalhe(item.lancamento.publicId) },
                )
            }
        }
    }
}

@Composable
private fun EstadoVazio(estado: EstadoVazioDosLancamentos, onEvent: (TransactionsEvent) -> Unit) {
    val nadaComFiltros = estado == EstadoVazioDosLancamentos.NADA_COM_OS_FILTROS
    val forma = RoundedCornerShape(PingoRadius.xl)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PingoSpacing.xxxl)
                .background(color = PingoTheme.colors.surface, shape = forma)
                .border(width = 1.dp, color = PingoTheme.colors.outlineVariant, shape = forma)
                .padding(PingoSpacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        ) {
            PingoMascot(width = 64.dp)
            TextosDoEstadoVazio(nadaComFiltros = nadaComFiltros)
            if (nadaComFiltros) {
                PingoButton(
                    text = "Limpar filtros",
                    onClick = { onEvent(TransactionsEvent.LimparFiltros) },
                    type = PingoButtonType.Tonal,
                    fillWidth = true,
                )
            }
        }
    }
}

@Composable
private fun TextosDoEstadoVazio(nadaComFiltros: Boolean) {
    PingoText(
        text = if (nadaComFiltros) "Nada com esses filtros" else "Nenhum lançamento ainda",
        style = PingoTheme.typography.title,
        textAlign = TextAlign.Center,
    )
    PingoText(
        text = if (nadaComFiltros) {
            "Tente outro mês ou limpe os filtros."
        } else {
            "Quando o Pingo capturar uma transação, ela aparece aqui."
        },
        style = PingoTheme.typography.bodySmall,
        color = PingoTheme.colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SnackbarDeExclusao(
    mensagem: MensagemDeExclusao,
    onDesfazer: () -> Unit,
    onDispensar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = PingoTheme.colors

    LaunchedEffect(mensagem.publicId) {
        delay(6000)
        onDispensar()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("snackbar-exclusao")
            .background(color = cores.brand, shape = RoundedCornerShape(PingoRadius.md))
            .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(
            text = "Lançamento excluído",
            style = PingoTheme.typography.bodySmall,
            color = cores.onPrimary,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .clickable(onClick = onDesfazer)
                .padding(horizontal = PingoSpacing.sm, vertical = PingoSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PingoIcon(
                imageVector = PingoIcons.Undo,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = cores.onPrimary,
            )
            PingoText(text = "Desfazer", style = PingoTheme.typography.label, color = cores.onPrimary)
        }
    }
}
