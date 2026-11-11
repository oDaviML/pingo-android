package com.pingo.feature.review.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.pingo.core.designsystem.component.PingoSpinner
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.PingoTopBar
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
fun ReviewContent(
    state: ReviewUiState,
    onEvent: (ReviewEvent) -> Unit,
    onCorrigir: (String) -> Unit,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("tela-revisao")
            .background(PingoTheme.colors.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PingoTopBar(title = RotulosDaRevisao.TITULO, onBack = onVoltar, modifier = Modifier.testTag("voltar-revisao"))
            when {
                state.carregando -> CarregandoDaRevisao(modifier = Modifier.weight(1f))
                state.vazia -> EstadoVazioDaRevisao(modifier = Modifier.weight(1f))
                else -> ListaDePendencias(
                    state = state,
                    onEvent = onEvent,
                    onCorrigir = onCorrigir,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        state.mensagem?.let { mensagem ->
            MensagemDaRevisaoBanner(
                mensagem = mensagem,
                onDispensar = { onEvent(ReviewEvent.DispensarMensagem) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(PingoSpacing.lg),
            )
        }

        state.painelDeCategoria?.let { painel ->
            PainelDeTrocaDeCategoriaDaRevisao(painel = painel, onEvent = onEvent)
        }

        state.painelDeTransferencia?.let { painel ->
            PainelDeMarcarTransferenciaDaRevisao(painel = painel, onEvent = onEvent)
        }
    }
}

@Composable
private fun CarregandoDaRevisao(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        PingoSpinner(size = 28.dp)
    }
}

@Composable
private fun ListaDePendencias(
    state: ReviewUiState,
    onEvent: (ReviewEvent) -> Unit,
    onCorrigir: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lista-revisao"),
        contentPadding = PaddingValues(horizontal = PingoSpacing.lg, vertical = PingoSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
    ) {
        item(key = "cabecalho") {
            CabecalhoDaRevisao(state = state, onEvent = onEvent)
        }
        items(state.pendentes, key = { pendencia -> pendencia.publicId }) { pendencia ->
            CartaoDaPendencia(pendencia = pendencia, onEvent = onEvent, onCorrigir = onCorrigir)
        }
        item(key = "respiro") {
            Box(modifier = Modifier.height(PingoSpacing.xl))
        }
    }
}

@Composable
private fun CabecalhoDaRevisao(state: ReviewUiState, onEvent: (ReviewEvent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = PingoSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            PingoText(
                text = RotulosDaRevisao.quantidade(state.quantidade),
                style = PingoTheme.typography.title,
                color = PingoTheme.colors.onBackground,
            )
            PingoText(
                text = RotulosDaRevisao.SUBTITULO,
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.onSurfaceVariant,
            )
        }
        PingoButton(
            text = RotulosDaRevisao.CONFIRMAR_TODOS,
            onClick = { onEvent(ReviewEvent.ConfirmarTodos) },
            type = PingoButtonType.Tonal,
            size = PingoButtonSize.Small,
            enabled = state.podeConfirmarTodos,
            modifier = Modifier.testTag("confirmar-todos"),
        )
    }
}

@Composable
private fun EstadoVazioDaRevisao(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(PingoSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        PingoCard(modifier = Modifier.testTag("revisao-vazia"), contentPadding = PingoSpacing.xl) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
            ) {
                PingoMascot(width = 72.dp)
                PingoText(
                    text = RotulosDaRevisao.VAZIO,
                    style = PingoTheme.typography.title,
                    color = PingoTheme.colors.onBackground,
                )
                PingoText(
                    text = RotulosDaRevisao.VAZIO_DETALHE,
                    style = PingoTheme.typography.body,
                    color = PingoTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun MensagemDaRevisaoBanner(mensagem: MensagemDaRevisao, onDispensar: () -> Unit, modifier: Modifier = Modifier) {
    PingoCard(modifier = modifier.testTag("mensagem-revisao"), contentPadding = PingoSpacing.md, gap = 0.dp) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PingoIcon(
                imageVector = PingoIcons.CircleAlert,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = PingoTheme.colors.warning,
            )
            PingoText(
                text = RotulosDaRevisao.mensagem(mensagem),
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.onBackground,
                modifier = Modifier.weight(1f),
            )
            PingoButton(
                text = "Fechar",
                onClick = onDispensar,
                type = PingoButtonType.Text,
                size = PingoButtonSize.Small,
                modifier = Modifier.testTag("dispensar-mensagem-revisao"),
            )
        }
    }
}
