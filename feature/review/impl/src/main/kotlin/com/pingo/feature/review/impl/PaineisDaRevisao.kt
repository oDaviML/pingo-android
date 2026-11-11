package com.pingo.feature.review.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import com.pingo.core.designsystem.component.PingoCheckbox
import com.pingo.core.designsystem.component.PingoChoiceChip
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.ui.Valores

@Composable
internal fun PainelDeTrocaDeCategoriaDaRevisao(painel: TrocaDeCategoriaDaRevisao, onEvent: (ReviewEvent) -> Unit) {
    SobreposicaoDoPainel(tag = "painel-categoria-revisao", onFechar = { onEvent(ReviewEvent.FecharTrocaDeCategoria) }) {
        PuxadorDoPainel()
        LinhaDeTitulo(
            titulo = "Trocar categoria",
            testTag = "fechar-categoria-revisao",
            onFechar = { onEvent(ReviewEvent.FecharTrocaDeCategoria) },
        )
        SubtituloDoPainel(painel.estabelecimento, painel.valorCentavos)
        OpcoesDaTrocaDeCategoria(painel = painel, onEvent = onEvent, modifier = Modifier.weight(1f))
        PingoCheckbox(
            checked = painel.aplicarATodos,
            onCheckedChange = { onEvent(ReviewEvent.AlternarAplicarATodos) },
            label = "Sempre usar para este estabelecimento",
            modifier = Modifier.testTag("aplicar-a-todos-revisao"),
        )
        AcoesDaTrocaDeCategoria(painel = painel, onEvent = onEvent)
    }
}

@Composable
private fun OpcoesDaTrocaDeCategoria(painel: TrocaDeCategoriaDaRevisao, onEvent: (ReviewEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
    ) {
        painel.opcoes.forEach { opcao ->
            PingoChoiceChip(
                text = opcao.nome,
                selected = opcao.publicId == painel.selecionadaPublicId,
                onClick = { onEvent(ReviewEvent.SelecionarCategoria(opcao.publicId)) },
                modifier = Modifier.testTag("categoria-revisao-${opcao.publicId}"),
            )
        }
    }
}

@Composable
private fun AcoesDaTrocaDeCategoria(painel: TrocaDeCategoriaDaRevisao, onEvent: (ReviewEvent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoButton(
            text = "Cancelar",
            onClick = { onEvent(ReviewEvent.FecharTrocaDeCategoria) },
            type = PingoButtonType.Text,
        )
        PingoButton(
            text = "Aplicar",
            onClick = { onEvent(ReviewEvent.AplicarCategoria) },
            type = PingoButtonType.Primary,
            enabled = painel.podeAplicar,
            modifier = Modifier.testTag("aplicar-categoria-revisao"),
        )
    }
}

@Composable
internal fun PainelDeMarcarTransferenciaDaRevisao(painel: MarcarTransferenciaDaRevisao, onEvent: (ReviewEvent) -> Unit) {
    SobreposicaoDoPainel(tag = "painel-transferencia-revisao", onFechar = { onEvent(ReviewEvent.FecharMarcarTransferencia) }) {
        PuxadorDoPainel()
        LinhaDeTitulo(
            titulo = "Marcar como transferência",
            testTag = "fechar-transferencia-revisao",
            onFechar = { onEvent(ReviewEvent.FecharMarcarTransferencia) },
        )
        SubtituloDoPainel(painel.estabelecimento, painel.valorCentavos)
        PingoText(
            text = "Escolha o instrumento de destino.",
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onBackground,
        )
        if (painel.opcoes.isEmpty()) {
            PingoText(
                text = RotulosDaRevisao.SEM_OPCOES_DE_TRANSFERENCIA,
                style = PingoTheme.typography.body,
                color = PingoTheme.colors.onSurfaceVariant,
            )
        }
        OpcoesDeDestino(painel = painel, onEvent = onEvent, modifier = Modifier.weight(1f))
        PingoButton(
            text = "Cancelar",
            onClick = { onEvent(ReviewEvent.FecharMarcarTransferencia) },
            type = PingoButtonType.Text,
            fillWidth = true,
            modifier = Modifier.testTag("cancelar-transferencia-revisao"),
        )
    }
}

@Composable
private fun OpcoesDeDestino(painel: MarcarTransferenciaDaRevisao, onEvent: (ReviewEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        painel.opcoes.forEach { opcao ->
            OpcaoDeDestino(
                opcao = opcao,
                onClick = { onEvent(ReviewEvent.SelecionarDestino(opcao.publicId)) },
            )
        }
    }
}

@Composable
private fun SubtituloDoPainel(estabelecimento: String, valorCentavos: Long) {
    PingoText(
        text = "$estabelecimento · ${Valores.formatar(valorCentavos)}",
        style = PingoTheme.typography.bodySmall,
        color = PingoTheme.colors.onSurfaceVariant,
    )
}

@Composable
private fun OpcaoDeDestino(opcao: OpcaoDaRevisao, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PingoLayout.minTouchTarget)
            .testTag("destino-revisao-${opcao.publicId}")
            .clickable(onClick = onClick)
            .padding(vertical = PingoSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(
            text = opcao.nome,
            style = PingoTheme.typography.body,
            color = PingoTheme.colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        PingoIcon(
            imageVector = PingoIcons.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = PingoTheme.colors.onSurfaceVariant,
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(PingoTheme.colors.outlineVariant),
    )
}

@Composable
private fun SobreposicaoDoPainel(tag: String, onFechar: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val interacao = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PingoTheme.colors.scrim)
            .clickable(interactionSource = interacao, indication = null, onClick = onFechar),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .testTag(tag)
                .background(
                    PingoTheme.colors.surface,
                    RoundedCornerShape(topStart = PingoRadius.xl, topEnd = PingoRadius.xl),
                )
                .clickable(interactionSource = interacao, indication = null) {}
                .padding(horizontal = PingoSpacing.lg, vertical = PingoSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.md),
            content = content,
        )
    }
}

@Composable
private fun PuxadorDoPainel() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 4.dp)
                .background(color = PingoTheme.colors.outlineVariant, shape = RoundedCornerShape(PingoRadius.full)),
        )
    }
}

@Composable
private fun LinhaDeTitulo(titulo: String, testTag: String, onFechar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoText(text = titulo, style = PingoTheme.typography.title)
        Box(
            modifier = Modifier
                .size(PingoLayout.minTouchTarget)
                .testTag(testTag)
                .clickable(onClick = onFechar),
            contentAlignment = Alignment.Center,
        ) {
            PingoIcon(
                imageVector = PingoIcons.X,
                contentDescription = "Fechar",
                modifier = Modifier.size(20.dp),
                tint = PingoTheme.colors.onSurfaceVariant,
            )
        }
    }
}
