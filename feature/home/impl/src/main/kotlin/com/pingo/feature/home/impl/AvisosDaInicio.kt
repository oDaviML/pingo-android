package com.pingo.feature.home.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonSize
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
internal fun AvisoDeRevisao(quantidade: Int, onAbrir: () -> Unit) {
    PingoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("aviso-revisao")
            .clickable(role = Role.Button, onClick = onAbrir),
        contentPadding = PingoSpacing.lg,
        gap = 0.dp,
    ) {
        LinhaDoAvisoDeRevisao(quantidade)
    }
}

@Composable
private fun LinhaDoAvisoDeRevisao(quantidade: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconeDoAvisoDeRevisao()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            PingoText(
                text = textoDaQuantidade(quantidade),
                style = PingoTheme.typography.bodyStrong,
                color = PingoTheme.colors.brand,
            )
            PingoText(
                text = "Toque para conferir o que o Pingo não teve certeza.",
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.onSurfaceVariant,
            )
        }
        PingoIcon(
            imageVector = PingoIcons.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = PingoTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun IconeDoAvisoDeRevisao() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(PingoTheme.colors.primaryContainer, RoundedCornerShape(PingoRadius.full)),
        contentAlignment = Alignment.Center,
    ) {
        PingoIcon(
            imageVector = PingoIcons.CircleAlert,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = PingoTheme.colors.onPrimaryContainer,
        )
    }
}

@Composable
internal fun AvisoDeCapturaParada(descricao: String, onCorrigir: () -> Unit) {
    PingoCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("aviso-captura-parada"),
        contentPadding = PingoSpacing.lg,
        gap = PingoSpacing.md,
    ) {
        CabecalhoDaCapturaParada(descricao)
        PingoButton(
            text = "Corrigir",
            onClick = onCorrigir,
            type = PingoButtonType.Tonal,
            size = PingoButtonSize.Small,
        )
    }
}

@Composable
private fun CabecalhoDaCapturaParada(descricao: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoIcon(
            imageVector = PingoIcons.TriangleAlert,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = PingoTheme.colors.warning,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            PingoText(
                text = "Captura parada",
                style = PingoTheme.typography.bodyStrong,
                color = PingoTheme.colors.warning,
            )
            PingoText(
                text = descricao,
                style = PingoTheme.typography.bodySmall,
                color = PingoTheme.colors.onSurfaceVariant,
            )
        }
    }
}

internal fun descricaoDaCaptura(state: HomeUiState): String = if (!state.capturaAutomatica) {
    "Sem acesso às notificações, o Pingo está no modo manual."
} else {
    "Nenhuma notificação lida há 2 dias. O Pingo pode não estar vendo seus bancos."
}

private fun textoDaQuantidade(quantidade: Int): String = if (quantidade == 1) {
    "1 lançamento para revisar"
} else {
    "$quantidade lançamentos para revisar"
}
