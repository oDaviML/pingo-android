package com.pingo.feature.reports.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingo.core.designsystem.component.AbaInferior
import com.pingo.core.designsystem.component.PingoBanner
import com.pingo.core.designsystem.component.PingoBannerTone
import com.pingo.core.designsystem.component.PingoBottomBar
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoDestructiveDialog
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoLargeTopBar
import com.pingo.core.designsystem.component.PingoSkeletonBox
import com.pingo.core.designsystem.component.PingoSkeletonList
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.ui.EstadoVazioCard
import java.time.YearMonth

@Composable
fun ReportsScreen(
    onSelecionarAba: (AbaInferior) -> Unit,
    onOpenNovo: (() -> Unit)? = null,
    mostrarBarraInferior: Boolean = true,
    onAbrirCategoriaNoMes: (String, YearMonth) -> Unit = { _, _ -> },
    viewModel: ReportsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ReportsContent(
        state = state,
        onEvent = viewModel::onEvent,
        onSelecionarAba = onSelecionarAba,
        onOpenNovo = onOpenNovo,
        mostrarBarraInferior = mostrarBarraInferior,
        onAbrirCategoriaNoMes = onAbrirCategoriaNoMes,
    )
}

@Suppress("LongParameterList")
@Composable
fun ReportsContent(
    state: ReportsUiState,
    onEvent: (ReportsEvent) -> Unit,
    onSelecionarAba: (AbaInferior) -> Unit,
    modifier: Modifier = Modifier,
    onOpenNovo: (() -> Unit)? = null,
    mostrarBarraInferior: Boolean = true,
    onAbrirCategoriaNoMes: (String, YearMonth) -> Unit = { _, _ -> },
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background),
    ) {
        CorpoRelatorios(
            state = state,
            onEvent = onEvent,
            onOpenNovo = onOpenNovo,
            onAbrirCategoriaNoMes = onAbrirCategoriaNoMes,
            modifier = Modifier.weight(1f),
        )
        if (mostrarBarraInferior) {
            PingoBottomBar(
                selecionada = AbaInferior.RELATORIOS,
                onSelecionar = onSelecionarAba,
                onNovo = onOpenNovo,
            )
        }
    }

    state.confirmacaoCancelamento?.let { alvo ->
        PingoDestructiveDialog(
            title = "Cancelar assinatura?",
            description = "Você deixará de acompanhar ${alvo.estabelecimento} como assinatura. " +
                "Os lançamentos já registrados serão preservados.",
            confirmText = "Remover assinatura",
            onConfirm = { onEvent(ReportsEvent.ConfirmarCancelamentoAssinatura) },
            onDismissRequest = { onEvent(ReportsEvent.DispensarCancelamentoAssinatura) },
        )
    }
}

@Composable
private fun CorpoRelatorios(
    state: ReportsUiState,
    onEvent: (ReportsEvent) -> Unit,
    onOpenNovo: (() -> Unit)?,
    onAbrirCategoriaNoMes: (String, YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        PingoLargeTopBar(title = "Relatórios")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PingoSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
        ) {
            SeletorDeMesRelatorios(state = state, onEvent = onEvent)
            state.mensagemErroAcao?.let { mensagem ->
                PingoBanner(
                    title = mensagem,
                    description = "Toque para dispensar.",
                    tone = PingoBannerTone.Warning,
                    onClick = { onEvent(ReportsEvent.DispensarMensagemErro) },
                )
            }
            ConteudoPrincipalRelatorios(
                state = state,
                onEvent = onEvent,
                onOpenNovo = onOpenNovo,
                onAbrirCategoriaNoMes = onAbrirCategoriaNoMes,
            )
            Spacer(modifier = Modifier.height(PingoSpacing.lg))
        }
    }
}

@Composable
private fun ConteudoPrincipalRelatorios(
    state: ReportsUiState,
    onEvent: (ReportsEvent) -> Unit,
    onOpenNovo: (() -> Unit)?,
    onAbrirCategoriaNoMes: (String, YearMonth) -> Unit,
) {
    when {
        state.carregando -> CarregandoRelatorios()
        state.falha -> FalhaRelatorios(onTentarDeNovo = { onEvent(ReportsEvent.TentarDeNovo) })
        state.mesVazio -> ConteudoMesVazio(state = state, onEvent = onEvent, onOpenNovo = onOpenNovo)
        else -> {
            SecaoDespesasPorCategoria(state = state, onAbrirCategoriaNoMes = onAbrirCategoriaNoMes)
            SecaoComparativoMensal(state = state, onAbrirCategoriaNoMes = onAbrirCategoriaNoMes)
            SecaoPorCategoria(state = state, onAbrirCategoriaNoMes = onAbrirCategoriaNoMes)
            SecaoEvolucaoSeisMeses(state = state, onSelecionarMes = { mes -> onEvent(ReportsEvent.SelecionarMes(mes)) })
            SecaoAssinaturas(state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun ConteudoMesVazio(state: ReportsUiState, onEvent: (ReportsEvent) -> Unit, onOpenNovo: (() -> Unit)?) {
    Spacer(modifier = Modifier.height(PingoSpacing.md))
    EstadoVazioCard(
        titulo = "Sem dados neste mês",
        descricao = "Os relatórios aparecem quando houver lançamentos no período.",
        acaoTexto = if (onOpenNovo != null) "Lançar manualmente" else null,
        onAcao = onOpenNovo,
        modifier = Modifier.testTag("relatorios-vazio"),
    )
    SecaoAssinaturas(state = state, onEvent = onEvent)
}

@Composable
private fun CarregandoRelatorios() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("relatorios-carregando"),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
    ) {
        PingoSkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp),
            cornerRadius = PingoRadius.xl,
        )
        PingoSkeletonList(count = 4)
    }
}

@Composable
private fun FalhaRelatorios(onTentarDeNovo: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("relatorios-erro")
            .padding(vertical = PingoSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        PingoText(
            text = "Não foi possível carregar os relatórios.",
            style = PingoTheme.typography.body,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        PingoButton(
            text = "Tentar de novo",
            onClick = onTentarDeNovo,
            type = PingoButtonType.Tonal,
        )
    }
}

@Composable
private fun SeletorDeMesRelatorios(state: ReportsUiState, onEvent: (ReportsEvent) -> Unit) {
    val rotulo = state.rotuloMes.ifEmpty { rotuloMesCompleto(state.mes) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("relatorios-seletor-mes"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BotaoMesRelatorio(
            icone = PingoIcons.ChevronLeft,
            descricao = "Mês anterior",
            tag = "relatorios-mes-anterior",
            habilitado = true,
            onClick = { onEvent(ReportsEvent.MesAnterior) },
        )
        PingoText(
            text = rotulo,
            style = PingoTheme.typography.subtitle,
            color = PingoTheme.colors.onBackground,
            modifier = Modifier.testTag("relatorios-mes-selecionado"),
        )
        BotaoMesRelatorio(
            icone = PingoIcons.ChevronRight,
            descricao = "Próximo mês",
            tag = "relatorios-mes-proximo",
            habilitado = state.podeAvancarMes,
            onClick = { onEvent(ReportsEvent.MesProximo) },
        )
    }
}

@Composable
private fun BotaoMesRelatorio(icone: ImageVector, descricao: String, tag: String, habilitado: Boolean, onClick: () -> Unit) {
    val colors = PingoTheme.colors
    val shape = RoundedCornerShape(PingoRadius.md)
    val fundo = if (habilitado) colors.surfaceVariant else Color.Transparent

    Box(
        modifier = Modifier
            .size(PingoLayout.minTouchTarget)
            .testTag(tag)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = habilitado,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(shape)
                .background(color = fundo, shape = shape),
            contentAlignment = Alignment.Center,
        ) {
            PingoIcon(
                imageVector = icone,
                contentDescription = descricao,
                modifier = Modifier.size(20.dp),
                tint = if (habilitado) colors.onBackground else colors.onDisabled,
            )
        }
    }
}
