package com.pingo.feature.accounts.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.CategoryAvatar
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoCategoryTone
import com.pingo.core.designsystem.component.PingoChoiceChip
import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.PingoLinearProgress
import com.pingo.core.designsystem.component.PingoSkeletonList
import com.pingo.core.designsystem.component.PingoStatusTag
import com.pingo.core.designsystem.component.PingoTag
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.PingoTopBar
import com.pingo.core.designsystem.component.finance.PingoInvoiceCard
import com.pingo.core.designsystem.component.finance.PingoInvoiceCardUi
import com.pingo.core.designsystem.component.finance.PingoInvoiceStatus
import com.pingo.core.designsystem.component.finance.PingoTabs
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.model.StatusFatura
import com.pingo.core.model.StatusLancamento
import com.pingo.core.model.TipoLancamento
import com.pingo.core.model.fatura.CicloDeFaturaDetalhado
import com.pingo.core.model.fatura.TipoCicloFatura
import com.pingo.core.ui.EstadoVazioCard
import com.pingo.core.ui.Valores
import com.pingo.core.ui.iconeDaCategoria
import com.pingo.feature.accounts.impl.FormatacaoFinanceiraContas.calcularProgressoCiclo
import com.pingo.feature.accounts.impl.FormatacaoFinanceiraContas.formatarContagemVencimento
import com.pingo.feature.accounts.impl.FormatacaoFinanceiraContas.formatarDataHoraCurta
import com.pingo.feature.accounts.impl.FormatacaoFinanceiraContas.formatarMesAnoCurto
import com.pingo.feature.accounts.impl.FormatacaoFinanceiraContas.formatarMesMaiusculo
import com.pingo.feature.accounts.impl.FormatacaoFinanceiraContas.formatarPeriodoCiclo
import com.pingo.feature.accounts.impl.FormatacaoFinanceiraContas.paraDesignSystem
import java.time.ZoneId

@Composable
internal fun FaturasContent(
    state: FaturasUiState,
    onVoltar: () -> Unit,
    onAbrirLancamento: (String) -> Unit = {},
    onEvent: (FaturasEvent) -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PingoTopBar(
                title = state.tituloCartao,
                onBack = onVoltar,
                modifier = Modifier.testTag("faturas-top-bar"),
            )
            CorpoFaturas(
                state = state,
                onAbrirLancamento = onAbrirLancamento,
                onEvent = onEvent,
                modifier = Modifier.weight(1f),
            )
        }
        state.modalCompletarDatas?.let { modal ->
            CompletarDatasBottomSheet(
                state = modal,
                onAlterarFechamento = { onEvent(FaturasEvent.AlterarFechamentoModal(it)) },
                onAlterarVencimento = { onEvent(FaturasEvent.AlterarVencimentoModal(it)) },
                onSalvar = { onEvent(FaturasEvent.SalvarDatasCartao) },
                onFechar = { onEvent(FaturasEvent.FecharModalCompletarDatas) },
            )
        }
    }
}

@Composable
private fun CorpoFaturas(
    state: FaturasUiState,
    onAbrirLancamento: (String) -> Unit,
    onEvent: (FaturasEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.carregando -> CarregandoFaturas(modifier = modifier)
        state.falha -> ErroAoCarregarFaturas(
            onTentarDeNovo = { onEvent(FaturasEvent.TentarDeNovo) },
            modifier = modifier,
        )
        state.naoEncontrada -> CartaoNaoEncontrado(modifier = modifier)
        state.configuracaoIncompleta -> GuardaConfiguracaoIncompleta(
            state = state,
            onCompletarDatas = { onEvent(FaturasEvent.AbrirCompletarDatas) },
            modifier = modifier,
        )
        else -> ConteudoFaturasConfiguradas(
            state = state,
            onAbrirLancamento = onAbrirLancamento,
            onEvent = onEvent,
            modifier = modifier,
        )
    }
}

@Composable
private fun CarregandoFaturas(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = PingoSpacing.xl, vertical = PingoSpacing.md)
            .testTag("faturas-carregando"),
    ) {
        PingoSkeletonList(count = 4)
    }
}

@Composable
private fun ErroAoCarregarFaturas(onTentarDeNovo: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = PingoSpacing.xl)
            .testTag("faturas-erro"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PingoText(
            text = "Não foi possível carregar as faturas.",
            style = PingoTheme.typography.bodyStrong,
            color = PingoTheme.colors.onBackground,
        )
        Spacer(modifier = Modifier.height(PingoSpacing.md))
        PingoButton(text = "Tentar de novo", onClick = onTentarDeNovo, type = PingoButtonType.Outline)
    }
}

@Composable
private fun CartaoNaoEncontrado(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = PingoSpacing.xl)
            .testTag("faturas-nao-encontrada"),
        contentAlignment = Alignment.Center,
    ) {
        EstadoVazioCard(
            titulo = "Cartão não encontrado",
            descricao = "Este cartão pode ter sido excluído ou não é um cartão de crédito.",
        )
    }
}

@Composable
private fun GuardaConfiguracaoIncompleta(state: FaturasUiState, onCompletarDatas: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = PingoSpacing.xl, vertical = PingoSpacing.lg)
            .testTag("faturas-incompleta"),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
    ) {
        PingoInvoiceCard(
            state = PingoInvoiceCardUi(
                institution = state.instituicao.paraDesignSystem(),
                title = state.tituloCartao,
                status = PingoInvoiceStatus.Incompleta,
                subtitle = state.ultimosDigitos?.let { "final $it" },
                incompleteMessage = "Datas não configuradas. Informe os dias de fechamento e vencimento para acompanhar as faturas.",
                completeDatesLabel = "Completar datas",
            ),
            modifier = Modifier.pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
            onCompleteDatesClick = onCompletarDatas,
        )
    }
}

@Composable
private fun ConteudoFaturasConfiguradas(
    state: FaturasUiState,
    onAbrirLancamento: (String) -> Unit,
    onEvent: (FaturasEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        PingoTabs(
            tabs = AbaFaturas.entries.map { it.rotulo },
            selectedIndex = state.abaSelecionada.indice,
            onSelectTab = { indice -> onEvent(FaturasEvent.SelecionarAba(AbaFaturas.porIndice(indice))) },
        )
        CorpoAbaFaturas(
            state = state,
            onAbrirLancamento = onAbrirLancamento,
            onEvent = onEvent,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CorpoAbaFaturas(
    state: FaturasUiState,
    onAbrirLancamento: (String) -> Unit,
    onEvent: (FaturasEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ciclo = state.cicloSelecionado
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PingoSpacing.xl, vertical = PingoSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
    ) {
        if (state.ciclosDaAbaAtual.size > 1) {
            SeletorDeCiclo(
                ciclos = state.ciclosDaAbaAtual,
                competenciaSelecionada = ciclo?.competencia,
                onSelecionar = { onEvent(FaturasEvent.SelecionarCiclo(it)) },
            )
        }
        if (ciclo == null) {
            EstadoVazioDaAba(aba = state.abaSelecionada)
        } else {
            CardResumoCiclo(ciclo = ciclo, hoje = state.hoje)
            SecaoLancamentosCiclo(
                itens = state.lancamentosDoCicloSelecionado,
                zona = state.zona,
                onAbrirLancamento = onAbrirLancamento,
            )
        }
        Spacer(modifier = Modifier.height(PingoSpacing.md))
    }
}

@Composable
private fun SeletorDeCiclo(
    ciclos: List<CicloDeFaturaDetalhado>,
    competenciaSelecionada: java.time.LocalDate?,
    onSelecionar: (java.time.LocalDate) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        ciclos.forEach { ciclo ->
            PingoChoiceChip(
                text = formatarMesAnoCurto(ciclo.competencia),
                selected = ciclo.competencia == competenciaSelecionada,
                onClick = { onSelecionar(ciclo.competencia) },
                modifier = Modifier.testTag("ciclo-chip-${ciclo.competencia}"),
            )
        }
    }
}

@Composable
private fun EstadoVazioDaAba(aba: AbaFaturas) {
    val (titulo, descricao) = when (aba) {
        AbaFaturas.ABERTA -> "Nenhuma fatura aberta" to "As compras do ciclo atual aparecerão aqui."
        AbaFaturas.PROXIMAS -> "Nenhuma fatura próxima" to "Compras parceladas ou em ciclos futuros aparecerão aqui."
        AbaFaturas.FECHADAS -> "Nenhuma fatura fechada" to "Ciclos fechados anteriores aparecerão aqui."
    }
    EstadoVazioCard(
        titulo = titulo,
        descricao = descricao,
        modifier = Modifier.testTag("faturas-aba-vazia-${aba.name}"),
    )
}

@Composable
private fun CardResumoCiclo(ciclo: CicloDeFaturaDetalhado, hoje: java.time.LocalDate) {
    val colors = PingoTheme.colors
    val shape = RoundedCornerShape(PingoRadius.xl)
    val destaqueAviso = ciclo.status == StatusFatura.FECHADA || ciclo.vencida
    val corDestaque = if (destaqueAviso) colors.warning else colors.primary

    PingoCard(
        modifier = Modifier
            .testTag("faturas-resumo-ciclo")
            .pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        gap = PingoSpacing.md,
    ) {
        CabecalhoResumoCiclo(ciclo = ciclo)
        PingoText(
            text = Valores.formatar(ciclo.totalCentavos),
            style = PingoTheme.typography.amountLarge,
            color = if (ciclo.tipoCiclo == TipoCicloFatura.ABERTA) colors.brand else colors.onBackground,
            maxLines = 1,
        )
        if (ciclo.tipoCiclo != TipoCicloFatura.PROXIMA) {
            PingoLinearProgress(progress = calcularProgressoCiclo(ciclo.dataFechamento, hoje), color = corDestaque)
        }
        RodapeResumoCiclo(ciclo = ciclo, corDestaque = corDestaque)
    }
}

@Composable
private fun CabecalhoResumoCiclo(ciclo: CicloDeFaturaDetalhado) {
    val colors = PingoTheme.colors
    val (rotuloStatus, container, conteudo) = when {
        ciclo.tipoCiclo == TipoCicloFatura.PROXIMA -> Triple("Próxima", colors.surfaceVariant, colors.onSurfaceVariant)
        ciclo.status == StatusFatura.PAGA -> Triple("Paga", colors.incomeContainer, colors.income)
        ciclo.status == StatusFatura.FECHADA -> Triple("Fechada", colors.warningContainer, colors.onWarningContainer)
        else -> Triple("Aberta", colors.primaryContainer, colors.onPrimaryContainer)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(
            text = "FATURA DE ${formatarMesMaiusculo(ciclo.competencia)}",
            style = PingoTheme.typography.overline,
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        PingoStatusTag(label = rotuloStatus, containerColor = container, contentColor = conteudo)
    }
}

@Composable
private fun RodapeResumoCiclo(ciclo: CicloDeFaturaDetalhado, corDestaque: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(
            text = formatarPeriodoCiclo(ciclo.status, ciclo.dataFechamento, ciclo.dataVencimento),
            style = PingoTheme.typography.caption,
            color = PingoTheme.colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        PingoText(
            text = formatarContagemVencimento(ciclo.status, ciclo.diasParaVencimento, ciclo.vencida),
            style = PingoTheme.typography.labelSmall,
            color = corDestaque,
            maxLines = 1,
        )
    }
}

@Composable
private fun SecaoLancamentosCiclo(itens: List<LancamentoDaFaturaUi>, zona: ZoneId, onAbrirLancamento: (String) -> Unit) {
    val contagem = if (itens.size == 1) "1 lançamento" else "${itens.size} lançamentos"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PingoSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(
            text = "COMPRAS DESTA FATURA",
            style = PingoTheme.typography.overline,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        PingoText(
            text = contagem,
            style = PingoTheme.typography.caption,
            color = PingoTheme.colors.onSurfaceVariant,
        )
    }

    if (itens.isEmpty()) {
        EstadoVazioCard(
            titulo = "Nenhum lançamento nesta fatura",
            descricao = "As compras confirmadas neste ciclo aparecerão aqui.",
            modifier = Modifier.testTag("faturas-ciclo-vazio"),
        )
    } else {
        ListaDeItensDaFatura(itens = itens, zona = zona, onAbrirLancamento = onAbrirLancamento)
    }
}

@Composable
private fun ListaDeItensDaFatura(itens: List<LancamentoDaFaturaUi>, zona: ZoneId, onAbrirLancamento: (String) -> Unit) {
    val shape = RoundedCornerShape(PingoRadius.xl)
    PingoCard(
        modifier = Modifier.pingoElevation(level = PingoElevationLevel.Level1, shape = shape),
        contentPadding = 0.dp,
        gap = 0.dp,
    ) {
        itens.forEachIndexed { indice, item ->
            ItemLancamentoFatura(
                item = item,
                zona = zona,
                onClick = { onAbrirLancamento(item.lancamento.publicId) },
            )
            if (indice < itens.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(PingoTheme.colors.outlineVariant),
                )
            }
        }
    }
}

@Composable
private fun ItemLancamentoFatura(item: LancamentoDaFaturaUi, zona: ZoneId, onClick: () -> Unit) {
    val colors = PingoTheme.colors
    val lancamento = item.lancamento
    val ehEstorno = lancamento.estornoDePublicId != null
    val ehCredito = ehEstorno || lancamento.tipo == TipoLancamento.RECEITA
    val valorTexto = if (ehCredito) {
        "+ ${Valores.formatar(lancamento.valorCentavos)}"
    } else {
        Valores.assinado(lancamento.valorCentavos, lancamento.tipo)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PingoLayout.minTouchTarget)
            .testTag("fatura-lancamento-${lancamento.publicId}")
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(
            icon = if (ehEstorno) PingoIcons.RotateCcw else iconeDaCategoria(item.categoria?.nome),
            contentDescription = item.categoria?.nome?.let { "Categoria $it" } ?: "Categoria não identificada",
            tone = if (ehCredito) PingoCategoryTone.Income else PingoCategoryTone.Default,
            size = 44.dp,
            iconSize = 22.dp,
            cornerRadius = PingoRadius.md,
        )
        DetalhesDoLancamentoFatura(item = item, zona = zona, ehEstorno = ehEstorno, modifier = Modifier.weight(1f))
        PingoText(
            text = valorTexto,
            style = PingoTheme.typography.amount,
            color = if (ehCredito) colors.income else Valores.corDoTipo(lancamento.tipo, colors),
            maxLines = 1,
        )
    }
}

@Composable
private fun DetalhesDoLancamentoFatura(item: LancamentoDaFaturaUi, zona: ZoneId, ehEstorno: Boolean, modifier: Modifier = Modifier) {
    val colors = PingoTheme.colors
    val lancamento = item.lancamento
    val subtitulo = listOfNotNull(
        formatarDataHoraCurta(lancamento.dataHora, zona),
        item.categoria?.nome,
    ).joinToString(" · ")

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
    ) {
        PingoText(
            text = lancamento.estabelecimento ?: "Compra no cartão",
            style = PingoTheme.typography.bodyStrong,
            color = colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SelosDoLancamentoFatura(
            parcelaNumero = lancamento.parcelaNumero,
            totalParcelas = lancamento.totalParcelas,
            pendente = lancamento.status == StatusLancamento.PENDENTE,
            ehEstorno = ehEstorno,
        )
        PingoText(
            text = subtitulo,
            style = PingoTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelosDoLancamentoFatura(parcelaNumero: Int, totalParcelas: Int, pendente: Boolean, ehEstorno: Boolean) {
    if (totalParcelas <= 1 && !pendente && !ehEstorno) {
        return
    }
    val colors = PingoTheme.colors
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
    ) {
        if (totalParcelas > 1) {
            PingoTag(
                label = "$parcelaNumero/$totalParcelas",
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
            )
        }
        if (pendente) {
            PingoTag(
                label = "Pendente",
                containerColor = colors.warningContainer,
                contentColor = colors.onWarningContainer,
            )
        }
        if (ehEstorno) {
            PingoTag(
                label = "Estorno",
                containerColor = colors.incomeContainer,
                contentColor = colors.income,
            )
        }
    }
}
