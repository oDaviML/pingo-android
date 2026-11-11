package com.pingo.feature.onboarding.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
internal fun NotificacoesDoPingoBody(state: OnboardingPermissoesUiState, onEvent: (OnboardingPermissoesEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg)) {
        PingoText(
            text = "Deixe o Pingo avisar você",
            style = PingoTheme.typography.headline,
        )
        PingoText(
            text = "Para confirmar uma compra na hora, sem abrir o app. Assim:",
            style = PingoTheme.typography.body,
            color = PingoTheme.colors.onSurfaceVariant,
        )

        ExemploDeNotificacao()

        PermissaoItem(
            titulo = "Notificações do Pingo",
            descricao = if (state.notificacoesHabilitadas) {
                "Tudo certo, o Pingo pode avisar você."
            } else {
                "Para revisar compras pela notificação"
            },
            concedida = state.notificacoesHabilitadas,
            acao = "Corrigir",
            onAcao = { onEvent(OnboardingPermissoesEvent.CorrigirNotificacoes) },
        )
    }
}

@Composable
private fun ExemploDeNotificacao() {
    PingoCard {
        CabecalhoDaNotificacao()
        PingoText(
            text = "Revisar compra de R$ 42,90?",
            style = PingoTheme.typography.subtitle,
        )
        PingoText(
            text = "Padaria do Zé · Nubank crédito. Alimentação?",
            style = PingoTheme.typography.bodySmall,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(PingoSpacing.xl)) {
            PingoText(
                text = "Confirmar",
                style = PingoTheme.typography.label,
                color = PingoTheme.colors.brand,
            )
            PingoText(
                text = "Trocar categoria",
                style = PingoTheme.typography.label,
                color = PingoTheme.colors.brand,
            )
        }
    }
}

@Composable
private fun CabecalhoDaNotificacao() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = PingoTheme.colors.primary, shape = CircleShape),
        )
        PingoText(
            text = "Pingo · agora",
            style = PingoTheme.typography.caption,
            color = PingoTheme.colors.onSurfaceVariant,
        )
    }
}
