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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoIconButton
import com.pingo.core.designsystem.component.PingoIconButtonStyle
import com.pingo.core.designsystem.component.PingoSkeletonList
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.ui.EstadoVazioCard

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
            CabecalhoDaRevisao(onVoltar = onVoltar)
            when {
                state.carregando -> PingoSkeletonList(count = 4, modifier = Modifier.weight(1f))
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
                    .padding(PingoSpacing.xl),
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
private fun CabecalhoDaRevisao(onVoltar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = PingoSpacing.sm, vertical = PingoSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoIconButton(
            icon = PingoIcons.ArrowLeft,
            contentDescription = "Voltar",
            onClick = onVoltar,
            style = PingoIconButtonStyle.Standard,
            modifier = Modifier.testTag("voltar-revisao"),
        )
        PingoText(
            text = RotulosDaRevisao.TITULO,
            style = PingoTheme.typography.subtitle,
            color = PingoTheme.colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
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
        contentPadding = PaddingValues(horizontal = PingoSpacing.xl, vertical = PingoSpacing.sm),
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
                style = PingoTheme.typography.headline,
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
            .padding(PingoSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        EstadoVazioCard(
            titulo = RotulosDaRevisao.VAZIO,
            descricao = RotulosDaRevisao.VAZIO_DETALHE,
            modifier = Modifier.testTag("revisao-vazia"),
        )
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
