package com.pingo.feature.transactions.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.AbaInferior
import com.pingo.core.designsystem.component.PingoBottomBar
import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoLargeTopBar
import com.pingo.core.designsystem.component.PingoSkeletonList
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.ui.EstadoVazioCard
import com.pingo.core.ui.LancamentoItem
import com.pingo.core.ui.Valores
import kotlinx.coroutines.delay

@Suppress("LongParameterList")
@Composable
fun TransactionsContent(
    state: TransactionsUiState,
    onEvent: (TransactionsEvent) -> Unit,
    onOpenDetalhe: (String) -> Unit,
    onOpenNovo: () -> Unit,
    onSelecionarAba: (AbaInferior) -> Unit,
    modifier: Modifier = Modifier,
    mostrarBarraInferior: Boolean = true,
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
            if (mostrarBarraInferior) {
                PingoBottomBar(
                    selecionada = AbaInferior.LANCAMENTOS,
                    onSelecionar = onSelecionarAba,
                    onNovo = onOpenNovo,
                )
            }
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
                        .padding(PingoSpacing.xl),
                )
            }
        }
    }
}

@Composable
private fun CabecalhoDaTela(state: TransactionsUiState, onEvent: (TransactionsEvent) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PingoLargeTopBar(title = "Lançamentos")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PingoSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        ) {
            CampoDeBusca(
                texto = state.busca,
                filtrosAtivos = state.filtrosAtivos,
                onTexto = { onEvent(TransactionsEvent.BuscaAlterada(it)) },
                onAbrirFiltros = { onEvent(TransactionsEvent.AbrirFiltros) },
            )
            ChipsDeFiltrosAtivos(state = state, onAbrirFiltros = { onEvent(TransactionsEvent.AbrirFiltros) })
        }
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
    var emFoco by remember { mutableStateOf(false) }
    val fundo = if (emFoco || texto.isNotEmpty()) cores.surface else cores.surfaceVariant
    val borda = if (emFoco) cores.primary else Color.Transparent

    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(forma)
            .background(color = fundo, shape = forma)
            .then(if (borda == Color.Transparent) Modifier else Modifier.border(width = 1.5.dp, color = borda, shape = forma))
            .padding(
                start = PingoSpacing.md,
                end = if (texto.isNotEmpty()) 4.dp else PingoSpacing.md,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoIcon(
            imageVector = PingoIcons.Search,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (emFoco) cores.primary else cores.onSurfaceVariant,
        )
        CampoDeTexto(
            texto = texto,
            onTexto = onTexto,
            onFoco = { emFoco = it },
            modifier = Modifier.weight(1f),
        )
        if (texto.isNotEmpty()) {
            BotaoLimparBusca(onLimpar = { onTexto("") })
        }
    }
}

@Composable
private fun CampoDeTexto(texto: String, onTexto: (String) -> Unit, onFoco: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val cores = PingoTheme.colors

    BasicTextField(
        value = texto,
        onValueChange = onTexto,
        modifier = modifier
            .onFocusChanged { onFoco(it.isFocused) }
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
            .size(PingoLayout.minTouchTarget)
            .clickable(role = Role.Button, onClick = onLimpar)
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
    val ativo = filtrosAtivos > 0

    Box(
        modifier = Modifier
            .size(48.dp)
            .testTag("filtros-botao")
            .clip(forma)
            .background(color = if (ativo) cores.primaryContainer else cores.surfaceVariant, shape = forma)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onAbrirFiltros,
            )
            .semantics { contentDescription = "Filtros" },
        contentAlignment = Alignment.Center,
    ) {
        PingoIcon(
            imageVector = PingoIcons.SlidersHorizontal,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (ativo) cores.onPrimaryContainer else cores.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChipsDeFiltrosAtivos(state: TransactionsUiState, onAbrirFiltros: () -> Unit) {
    val itens = chipsDosFiltros(state)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        itens.forEach { item ->
            ChipDeFiltroAtivo(rotulo = item.rotulo, comSeta = item.comSeta, onClick = onAbrirFiltros)
        }
    }
}

@Composable
private fun ChipDeFiltroAtivo(rotulo: String, comSeta: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = PingoLayout.minTouchTarget)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        ConteudoDoChipDeFiltro(rotulo = rotulo, comSeta = comSeta)
    }
}

@Composable
private fun ConteudoDoChipDeFiltro(rotulo: String, comSeta: Boolean) {
    val colors = PingoTheme.colors
    val shape = RoundedCornerShape(PingoRadius.full)

    Row(
        modifier = Modifier
            .background(color = colors.primaryContainer, shape = shape)
            .border(width = 1.5.dp, color = colors.primary, shape = shape)
            .padding(horizontal = PingoSpacing.md, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoIcon(
            imageVector = PingoIcons.Check,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = colors.onPrimaryContainer,
        )
        PingoText(text = rotulo, style = PingoTheme.typography.label, color = colors.onPrimaryContainer, maxLines = 1)
        if (comSeta) {
            PingoIcon(
                imageVector = PingoIcons.ChevronDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colors.onPrimaryContainer,
            )
        }
    }
}

private data class ChipDeFiltroItem(val rotulo: String, val comSeta: Boolean = false)

private fun chipsDosFiltros(state: TransactionsUiState): List<ChipDeFiltroItem> = buildList {
    add(ChipDeFiltroItem(rotulo = state.filtros.mes?.let(RotuloDeMes::de) ?: "Todos os meses", comSeta = true))
    rotuloDoTipo(state.filtros.tipo)?.let { add(ChipDeFiltroItem(rotulo = it)) }
    state.filtros.vinculo?.let { vinculo ->
        state.opcoesDeVinculo.firstOrNull { it.filtro == vinculo }?.rotulo?.let {
            add(ChipDeFiltroItem(rotulo = it, comSeta = true))
        }
    }
    state.filtros.categoriaPublicId?.let { publicId ->
        state.opcoesDeCategoria.firstOrNull { it.publicId == publicId }?.nome?.let {
            add(ChipDeFiltroItem(rotulo = it))
        }
    }
    state.filtros.status?.let { add(ChipDeFiltroItem(rotulo = rotuloDoStatus(it))) }
}

@Composable
private fun ConteudoDaLista(state: TransactionsUiState, onEvent: (TransactionsEvent) -> Unit, onOpenDetalhe: (String) -> Unit) {
    when {
        state.carregando -> PingoSkeletonList(count = 6)
        state.estadoVazio != null -> EstadoVazio(estado = state.estadoVazio, onEvent = onEvent)
        else -> ListaDeDias(state = state, onEvent = onEvent, onOpenDetalhe = onOpenDetalhe)
    }
}

@Composable
private fun ListaDeDias(state: TransactionsUiState, onEvent: (TransactionsEvent) -> Unit, onOpenDetalhe: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = PingoSpacing.xl, vertical = PingoSpacing.sm),
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
            .pingoElevation(level = PingoElevationLevel.Level1, shape = forma)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PingoSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        EstadoVazioCard(
            titulo = if (nadaComFiltros) "Nada com esses filtros" else "Nenhum lançamento ainda",
            descricao = if (nadaComFiltros) {
                "Tente outro mês ou limpe os filtros."
            } else {
                "Quando o Pingo capturar uma transação, ela aparece aqui."
            },
            acaoTexto = if (nadaComFiltros) "Limpar filtros" else null,
            onAcao = if (nadaComFiltros) ({ onEvent(TransactionsEvent.LimparFiltros) }) else null,
        )
    }
}

@Composable
private fun SnackbarDeExclusao(
    mensagem: MensagemDeExclusao,
    onDesfazer: () -> Unit,
    onDispensar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cores = PingoTheme.colors
    val fundo = if (cores.isLight) cores.onBackground else Color(0xFF04222A)
    val forma = RoundedCornerShape(PingoRadius.md)

    LaunchedEffect(mensagem.publicId) {
        delay(6000)
        onDispensar()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("snackbar-exclusao")
            .pingoElevation(level = PingoElevationLevel.Level2, shape = forma)
            .background(color = fundo, shape = forma)
            .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(
            text = "Lançamento excluído",
            style = PingoTheme.typography.bodySmall,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        BotaoDesfazerExclusao(onDesfazer = onDesfazer, cor = cores.secondary)
    }
}

@Composable
private fun BotaoDesfazerExclusao(onDesfazer: () -> Unit, cor: Color) {
    Row(
        modifier = Modifier
            .heightIn(min = PingoLayout.minTouchTarget)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onDesfazer,
            )
            .padding(horizontal = PingoSpacing.sm, vertical = PingoSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoIcon(imageVector = PingoIcons.Undo, contentDescription = null, modifier = Modifier.size(16.dp), tint = cor)
        PingoText(text = "Desfazer", style = PingoTheme.typography.label, color = cor)
    }
}
