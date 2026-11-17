package com.pingo.feature.home.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pingo.core.datastore.MotivoParada
import com.pingo.core.designsystem.component.PingoBanner
import com.pingo.core.designsystem.component.PingoBannerTone
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoLayout
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme
import com.pingo.core.notifications.SituacaoDaCaptura

@Composable
internal fun AvisoDeRevisao(quantidade: Int, onAbrir: () -> Unit) {
    PingoBanner(
        title = textoDaQuantidade(quantidade),
        description = "Toque para conferir o que o Pingo não teve certeza.",
        tone = PingoBannerTone.Info,
        icon = PingoIcons.Bell,
        onClick = onAbrir,
        modifier = Modifier.testTag("aviso-revisao"),
    )
}

@Composable
internal fun AvisoDeCapturaParada(descricao: String, onCorrigir: () -> Unit) {
    val colors = PingoTheme.colors
    val shape = RoundedCornerShape(PingoRadius.lg)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PingoLayout.minTouchTarget)
            .testTag("aviso-captura-parada")
            .clip(shape)
            .background(color = colors.warningContainer, shape = shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onCorrigir,
            )
            .padding(start = PingoSpacing.lg, end = 14.dp, top = 14.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoIcon(
            imageVector = PingoIcons.TriangleAlert,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = colors.onWarningContainer,
        )
        TextosDaCapturaParada(descricao = descricao, modifier = Modifier.weight(1f))
        AcaoDeCorrigir()
    }
}

@Composable
private fun TextosDaCapturaParada(descricao: String, modifier: Modifier = Modifier) {
    val colors = PingoTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        PingoText(text = "Captura parada", style = PingoTheme.typography.label, color = colors.onWarningContainer)
        PingoText(text = descricao, style = PingoTheme.typography.caption, color = colors.onWarningContainer)
    }
}

@Composable
private fun AcaoDeCorrigir() {
    val colors = PingoTheme.colors
    val shape = RoundedCornerShape(PingoRadius.sm)

    Row(
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .background(color = colors.warning.copy(alpha = 0.16f), shape = shape)
                .padding(horizontal = PingoSpacing.sm, vertical = PingoSpacing.xs),
        ) {
            PingoText(
                text = "Corrigir",
                style = PingoTheme.typography.labelSmall,
                color = colors.onWarningContainer,
                maxLines = 1,
            )
        }
        PingoIcon(
            imageVector = PingoIcons.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = colors.onWarningContainer,
        )
    }
}

internal fun descricaoDaCaptura(state: HomeUiState): String = when {
    !state.capturaAutomatica || state.motivo == MotivoParada.PERMISSAO_REVOGADA ->
        "Sem acesso às notificações, o Pingo está no modo manual."

    state.situacao == SituacaoDaCaptura.PARADA && state.motivo == MotivoParada.DESCONECTADO ->
        "O Android encerrou o serviço de captura. Toque em Corrigir para reconectar."

    state.situacao == SituacaoDaCaptura.PARADA ->
        "A captura está parada. Toque em Corrigir para reconectar."

    state.situacao == SituacaoDaCaptura.SEM_LEITURA || state.semLeituraHa48h ->
        "Nenhuma notificação lida há 2 dias. O Pingo pode não estar vendo seus bancos."

    else -> "A captura está parada. Toque em Corrigir para reconectar."
}

private fun textoDaQuantidade(quantidade: Int): String = if (quantidade == 1) {
    "1 lançamento para revisar"
} else {
    "$quantidade lançamentos para revisar"
}
