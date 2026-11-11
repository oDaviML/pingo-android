package com.pingo.feature.onboarding.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.OnboardingStepper
import com.pingo.core.designsystem.component.PingoButton
import com.pingo.core.designsystem.component.PingoButtonType
import com.pingo.core.designsystem.component.PingoIcon
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
fun OnboardingPermissoesContent(
    state: OnboardingPermissoesUiState,
    onEvent: (OnboardingPermissoesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PingoTheme.colors.background),
    ) {
        OnboardingStepper(
            currentStep = state.passo.numero,
            totalSteps = state.totalPassos,
            onSkip = { onEvent(OnboardingPermissoesEvent.Pular) },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PingoSpacing.xxl, vertical = PingoSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg),
        ) {
            when (state.passo) {
                PassoDePermissoes.ACESSO_AS_NOTIFICACOES -> AcessoNotificacoesBody(state = state, onEvent = onEvent)
                PassoDePermissoes.NOTIFICACOES_DO_PINGO -> NotificacoesDoPingoBody(state = state, onEvent = onEvent)
                PassoDePermissoes.BATERIA -> BateriaBody(state = state, onEvent = onEvent)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PingoSpacing.xxl, vertical = PingoSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        ) {
            if (state.passo == PassoDePermissoes.BATERIA) {
                AvisoDePrivacidade()
            }

            PermissoesActions(state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun PermissoesActions(state: OnboardingPermissoesUiState, onEvent: (OnboardingPermissoesEvent) -> Unit) {
    when (state.passo) {
        PassoDePermissoes.ACESSO_AS_NOTIFICACOES -> AcessoActions(state = state, onEvent = onEvent)
        PassoDePermissoes.NOTIFICACOES_DO_PINGO -> NotificacoesActions(state = state, onEvent = onEvent)
        PassoDePermissoes.BATERIA -> BateriaActions(onEvent = onEvent)
    }
}

@Composable
private fun AcessoActions(state: OnboardingPermissoesUiState, onEvent: (OnboardingPermissoesEvent) -> Unit) {
    if (state.modoManual && !state.acessoConcedido) {
        PingoButton(
            text = "Tentar de novo",
            onClick = { onEvent(OnboardingPermissoesEvent.AbrirAjustesDeNotificacoes) },
            modifier = Modifier.fillMaxWidth(),
            fillWidth = true,
        )
        PingoButton(
            text = "Continuar só no manual",
            onClick = { onEvent(OnboardingPermissoesEvent.ContinuarSemAcesso) },
            modifier = Modifier.fillMaxWidth(),
            type = PingoButtonType.Outline,
            fillWidth = true,
        )
    } else if (state.acessoConcedido) {
        PingoButton(
            text = "Continuar",
            onClick = { onEvent(OnboardingPermissoesEvent.ContinuarComAcesso) },
            modifier = Modifier.fillMaxWidth(),
            fillWidth = true,
        )
    } else {
        PingoButton(
            text = "Abrir ajustes",
            onClick = { onEvent(OnboardingPermissoesEvent.AbrirAjustesDeNotificacoes) },
            modifier = Modifier.fillMaxWidth(),
            fillWidth = true,
        )
        PingoButton(
            text = "Agora não",
            onClick = { onEvent(OnboardingPermissoesEvent.NegarAcesso) },
            modifier = Modifier.fillMaxWidth(),
            type = PingoButtonType.Text,
        )
    }
}

@Composable
private fun NotificacoesActions(state: OnboardingPermissoesUiState, onEvent: (OnboardingPermissoesEvent) -> Unit) {
    PingoButton(
        text = if (state.notificacoesHabilitadas) "Continuar" else "Permitir notificações",
        onClick = { onEvent(OnboardingPermissoesEvent.PedirNotificacoes) },
        modifier = Modifier.fillMaxWidth(),
        fillWidth = true,
    )

    if (!state.notificacoesHabilitadas) {
        PingoButton(
            text = "Agora não",
            onClick = { onEvent(OnboardingPermissoesEvent.PularNotificacoes) },
            modifier = Modifier.fillMaxWidth(),
            type = PingoButtonType.Text,
        )
    }
}

@Composable
private fun BateriaActions(onEvent: (OnboardingPermissoesEvent) -> Unit) {
    PingoButton(
        text = "Abrir ajustes de bateria",
        onClick = { onEvent(OnboardingPermissoesEvent.AbrirAjustesDeBateria) },
        modifier = Modifier.fillMaxWidth(),
        fillWidth = true,
    )
    PingoButton(
        text = "Pular por enquanto",
        onClick = { onEvent(OnboardingPermissoesEvent.Concluir) },
        modifier = Modifier.fillMaxWidth(),
        type = PingoButtonType.Text,
    )
}

@Composable
private fun AvisoDePrivacidade() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoIcon(
            imageVector = PingoIcons.Lock,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = PingoTheme.colors.onSurfaceVariant,
        )
        PingoText(
            text = "O texto lido não sai do aparelho. A leitura acontece aqui; nada do texto é enviado.",
            style = PingoTheme.typography.caption,
            color = PingoTheme.colors.onSurfaceVariant,
        )
    }
}
