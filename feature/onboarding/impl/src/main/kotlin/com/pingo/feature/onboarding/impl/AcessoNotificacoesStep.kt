package com.pingo.feature.onboarding.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pingo.core.designsystem.component.PingoBanner
import com.pingo.core.designsystem.component.PingoBannerTone
import com.pingo.core.designsystem.component.PingoCard
import com.pingo.core.designsystem.component.PingoElevationLevel
import com.pingo.core.designsystem.component.PingoMascot
import com.pingo.core.designsystem.component.PingoText
import com.pingo.core.designsystem.component.pingoElevation
import com.pingo.core.designsystem.icon.PingoIcons
import com.pingo.core.designsystem.theme.PingoRadius
import com.pingo.core.designsystem.theme.PingoSpacing
import com.pingo.core.designsystem.theme.PingoTheme

@Composable
internal fun AcessoNotificacoesBody(state: OnboardingPermissoesUiState, onEvent: (OnboardingPermissoesEvent) -> Unit) {
    if (state.modoManual && !state.acessoConcedido) {
        ModoManualBody(onEvent = onEvent)
    } else {
        AcessoNormalBody(state = state, onEvent = onEvent)
    }
}

@Composable
private fun AcessoNormalBody(state: OnboardingPermissoesUiState, onEvent: (OnboardingPermissoesEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg)) {
        PingoText(text = "Libere o acesso às notificações", style = PingoTheme.typography.headline)
        PingoText(
            text = "É assim que o Pingo vê suas compras. O Android pede essa permissão em Ajustes.",
            style = PingoTheme.typography.body,
            color = PingoTheme.colors.onSurfaceVariant,
        )

        PermissaoItem(
            titulo = "Acesso a notificações",
            descricao = if (state.acessoConcedido) {
                "Tudo certo, o Pingo já pode ler as notificações dos bancos."
            } else {
                "Falta ativar o Pingo nos Ajustes do Android"
            },
            concedida = state.acessoConcedido,
            acao = "Corrigir",
            onAcao = { onEvent(OnboardingPermissoesEvent.AbrirAjustesDeNotificacoes) },
        )

        if (state.acessoConcedido) {
            AcessoAtivoCard()
        } else {
            ComoFazerCard()
        }
    }
}

@Composable
private fun AcessoAtivoCard() {
    PingoCard(
        modifier = Modifier.pingoElevation(
            level = PingoElevationLevel.Level1,
            shape = RoundedCornerShape(PingoRadius.xl),
        ),
    ) {
        InformacaoComIcone(
            icon = PingoIcons.ShieldCheck,
            titulo = "Acesso ativo",
            descricao = "A leitura acontece no aparelho e o texto não sai daqui.",
        )
    }
}

@Composable
private fun ComoFazerCard() {
    PingoCard(
        modifier = Modifier.pingoElevation(
            level = PingoElevationLevel.Level1,
            shape = RoundedCornerShape(PingoRadius.xl),
        ),
    ) {
        PingoText(
            text = "COMO FAZER",
            style = PingoTheme.typography.overline,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        PassosNumerados(
            passos = listOf(
                "Toque em Abrir ajustes.",
                "Procure Pingo na lista de apps com acesso.",
                "Ative a chave e confirme o aviso do Android.",
            ),
        )
        PingoMock()
    }
}

@Composable
private fun PingoMock() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = PingoTheme.colors.surfaceVariant, shape = RoundedCornerShape(PingoRadius.md))
            .padding(horizontal = PingoSpacing.md, vertical = PingoSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(PingoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PingoMascot(width = 24.dp, contentDescription = null)
        PingoText(
            text = "Pingo",
            style = PingoTheme.typography.bodyStrong,
            color = PingoTheme.colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 26.dp)
                .background(color = PingoTheme.colors.outline, shape = RoundedCornerShape(PingoRadius.full)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(18.dp)
                    .background(color = PingoTheme.colors.surface, shape = CircleShape),
            )
        }
    }
}

@Composable
private fun ModoManualBody(onEvent: (OnboardingPermissoesEvent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(PingoSpacing.lg)) {
        PingoText(text = "Tudo bem, você decide", style = PingoTheme.typography.headline)
        PingoBanner(
            title = "Sem acesso às notificações",
            description = "O Pingo vai funcionar só no modo manual até você liberar.",
            tone = PingoBannerTone.Error,
            icon = PingoIcons.BellOff,
            showTrailingChevron = false,
        )
        PermissaoItem(
            titulo = "Acesso a notificações",
            descricao = "Sem isso o Pingo fica só no manual",
            concedida = false,
            acao = "Corrigir",
            onAcao = { onEvent(OnboardingPermissoesEvent.AbrirAjustesDeNotificacoes) },
        )
        ResumoDoModoManual()
    }
}

@Composable
private fun ResumoDoModoManual() {
    PingoCard(
        modifier = Modifier.pingoElevation(
            level = PingoElevationLevel.Level1,
            shape = RoundedCornerShape(PingoRadius.xl),
        ),
    ) {
        PingoText(
            text = "NO MODO MANUAL",
            style = PingoTheme.typography.overline,
            color = PingoTheme.colors.onSurfaceVariant,
        )
        InformacaoComIcone(
            icon = PingoIcons.Plus,
            titulo = "Você lança cada gasto",
            descricao = "Pelo botão + do app, em até 3 toques.",
        )
        InformacaoComIcone(
            icon = PingoIcons.BellOff,
            titulo = "Sem captura nem revisão",
            descricao = "O Pingo não vê notificações dos bancos.",
        )
        InformacaoComIcone(
            icon = PingoIcons.Settings,
            titulo = "Dá para ativar depois",
            descricao = "Em Configurações › Status da captura.",
        )
    }
}
